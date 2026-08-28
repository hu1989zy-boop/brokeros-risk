package com.brokeros.risk.tradingaccount.interfaces.bootstrap;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.brokeros.risk.BrokerOsRiskApplication;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;
import com.brokeros.risk.security.application.ServiceActorContextFactory;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.application.AccountProvisioningResult;
import com.brokeros.risk.tradingaccount.application.AuthorityOperationRequest;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeLifecycleService;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeProvisioningService;
import com.brokeros.risk.tradingaccount.application.LifecycleChangeResult;
import com.brokeros.risk.tradingaccount.application.ScopeProvisioningResult;
import com.brokeros.risk.tradingaccount.application.TradingAccountLifecycleService;
import com.brokeros.risk.tradingaccount.application.TradingAccountRegistrationService;
import com.brokeros.risk.tradingaccount.application.TradingAccountCapabilities;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.brokeros.risk.tradingaccount.domain.ChangeReason;
import com.brokeros.risk.tradingaccount.domain.ChangeReference;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountKey;
import com.brokeros.risk.tradingaccount.domain.SourceNamespace;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import com.brokeros.risk.tradingaccount.infrastructure.configuration.TradingAccountReferenceProvisionerDescriptor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public final class TradingAccountAuthorityBootstrapCommand {

    static final long MAX_MANIFEST_BYTES = 64L * 1024L;

    private TradingAccountAuthorityBootstrapCommand() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out));
    }

    public static int run(String[] args, PrintStream output) {
        try {
            Path manifestPath = validatePath(args);
            SpringApplication application = new SpringApplication(BrokerOsRiskApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            try (ConfigurableApplicationContext context = application.run()) {
                ObjectMapper objectMapper = strictMapper(context.getBean(ObjectMapper.class));
                BootstrapManifestInput input = objectMapper.readValue(
                        Files.readAllBytes(manifestPath), BootstrapManifestInput.class);
                AuthorityOperationType operationType = preliminaryOperation(input);
                ActorContext actorContext = context.getBean(ServiceActorContextFactory.class)
                        .create(context.getBean(TradingAccountReferenceProvisionerDescriptor.class));
                context.getBean(AuthorizationGuard.class).requireAllowed(
                        actorContext, capabilityFor(operationType));
                AuthorityOperationRequest request = toRequest(input, operationType);
                SafeResult result = execute(context, actorContext, request);
                output.printf(
                        "schemaVersion=1 operationId=%s operation=%s outcome=%s targetRef=%s resultingVersion=%d occurredAt=%s%n",
                        request.operationId().value(), request.operationType(), result.outcome(),
                        result.targetRef(), result.version(), result.occurredAt());
                return 0;
            }
        } catch (BusinessException exception) {
            output.printf("outcome=FAIL resultCode=%s%n", exception.getResultCode().code());
            return exitCode(exception.getResultCode());
        } catch (IOException | IllegalArgumentException exception) {
            output.printf("outcome=FAIL resultCode=%s%n", ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID.code());
            return 2;
        } catch (RuntimeException exception) {
            output.printf("outcome=FAIL resultCode=%s%n", ResultCode.INTERNAL_ERROR.code());
            return 10;
        }
    }

    private static com.brokeros.risk.security.domain.Capability capabilityFor(
            AuthorityOperationType operationType) {
        return operationType.isRegistration()
                ? TradingAccountCapabilities.REGISTER
                : TradingAccountCapabilities.CHANGE_LIFECYCLE;
    }

    static ObjectMapper strictMapper(ObjectMapper applicationMapper) {
        return applicationMapper.copy()
                .deactivateDefaultTyping()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    private static Path validatePath(String[] args) throws IOException {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("exactly one manifest path is required");
        }
        Path path = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(path)
                || Files.size(path) > MAX_MANIFEST_BYTES) {
            throw new IllegalArgumentException("manifest path is invalid");
        }
        return path;
    }

    private static AuthorityOperationType preliminaryOperation(BootstrapManifestInput input) {
        if (input == null || input.schemaVersion() == null || input.schemaVersion() != 1) {
            throw new BusinessException(ResultCode.TRADING_ACCOUNT_MANIFEST_SCHEMA_UNSUPPORTED);
        }
        try {
            return AuthorityOperationType.valueOf(input.operation());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID);
        }
    }

    static AuthorityOperationRequest toRequest(
            BootstrapManifestInput input,
            AuthorityOperationType operationType) {
        try {
            return new AuthorityOperationRequest(
                    input.schemaVersion(),
                    field(() -> new AuthorityOperationId(input.operationId()),
                            ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID),
                    operationType,
                    input.authorityScopeRef() == null
                            ? null : field(() -> new AccountAuthorityScopeRef(input.authorityScopeRef()),
                                    ResultCode.ACCOUNT_AUTHORITY_SCOPE_INVALID),
                    input.tradingAccountRef() == null
                            ? null : field(() -> new TradingAccountRef(input.tradingAccountRef()),
                                    ResultCode.TRADING_ACCOUNT_REFERENCE_INVALID),
                    input.sourceNamespace() == null ? null : field(() -> new SourceNamespace(
                            input.sourceNamespace().sourceFamily(),
                            input.sourceNamespace().sourceInstance(),
                            input.sourceNamespace().server(),
                            input.sourceNamespace().environment()), ResultCode.SOURCE_NAMESPACE_INVALID),
                    input.externalAccountKey() == null
                            ? null : field(() -> new ExternalAccountKey(input.externalAccountKey()),
                                    ResultCode.EXTERNAL_ACCOUNT_KEY_INVALID),
                    input.expectedVersion(),
                    input.attestation() == null ? null : field(() -> new AttestationReference(
                            input.attestation().source(), input.attestation().reference()),
                            ResultCode.TRADING_ACCOUNT_ATTESTATION_INVALID),
                    field(() -> new ChangeReason(input.reason()),
                            ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID),
                    field(() -> new ChangeReference(input.changeRef()),
                            ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID));
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID,
                    ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID.defaultMessage(), exception);
        }
    }

    private static <T> T field(Supplier<T> supplier, ResultCode resultCode) {
        try {
            return supplier.get();
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new BusinessException(resultCode, resultCode.defaultMessage(), exception);
        }
    }

    private static SafeResult execute(
            ConfigurableApplicationContext context,
            ActorContext actorContext,
            AuthorityOperationRequest request) {
        return switch (request.operationType()) {
            case REGISTER_AUTHORITY_SCOPE -> safe(context
                    .getBean(AuthorityScopeProvisioningService.class)
                    .register(actorContext, request));
            case REGISTER_TRADING_ACCOUNT -> safe(context
                    .getBean(TradingAccountRegistrationService.class)
                    .register(actorContext, request));
            case DEACTIVATE_AUTHORITY_SCOPE, REACTIVATE_AUTHORITY_SCOPE, RETIRE_AUTHORITY_SCOPE ->
                    safe(context.getBean(AuthorityScopeLifecycleService.class)
                            .change(actorContext, request));
            case DEACTIVATE_TRADING_ACCOUNT, REACTIVATE_TRADING_ACCOUNT, RETIRE_TRADING_ACCOUNT ->
                    safe(context.getBean(TradingAccountLifecycleService.class)
                            .change(actorContext, request));
        };
    }

    private static SafeResult safe(ScopeProvisioningResult result) {
        return new SafeResult(result.scopeRef().value(), result.outcome(),
                result.resultingVersion(), result.occurredAt());
    }

    private static SafeResult safe(AccountProvisioningResult result) {
        return new SafeResult(result.tradingAccountRef().value(), result.outcome(),
                result.resultingVersion(), result.occurredAt());
    }

    private static SafeResult safe(LifecycleChangeResult result) {
        return new SafeResult(result.targetRef(), result.outcome(),
                result.resultingVersion(), result.occurredAt());
    }

    private static int exitCode(ResultCode code) {
        return switch (code) {
            case TRADING_ACCOUNT_REFERENCE_INVALID, ACCOUNT_AUTHORITY_SCOPE_INVALID,
                    SOURCE_NAMESPACE_INVALID, EXTERNAL_ACCOUNT_KEY_INVALID,
                    TRADING_ACCOUNT_MANIFEST_INVALID,
                    TRADING_ACCOUNT_MANIFEST_SCHEMA_UNSUPPORTED,
                    TRADING_ACCOUNT_ATTESTATION_INVALID -> 2;
            case ACTOR_ACCESS_DENIED, AUTHORIZATION_DENIED -> 3;
            case TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT, TRADING_ACCOUNT_MAPPING_CONFLICT,
                    TRADING_ACCOUNT_VERSION_CONFLICT, TRADING_ACCOUNT_INVALID_TRANSITION -> 4;
            case ACCOUNT_AUTHORITY_SCOPE_NOT_FOUND, TRADING_ACCOUNT_REFERENCE_NOT_FOUND,
                    ACCOUNT_AUTHORITY_SCOPE_NOT_ELIGIBLE -> 5;
            case SECURITY_DEPENDENCY_UNAVAILABLE, TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE -> 6;
            default -> 10;
        };
    }

    public record BootstrapManifestInput(
            Integer schemaVersion,
            String operationId,
            String operation,
            String authorityScopeRef,
            String tradingAccountRef,
            SourceNamespaceInput sourceNamespace,
            String externalAccountKey,
            Long expectedVersion,
            AttestationInput attestation,
            String reason,
            String changeRef) {
    }

    public record SourceNamespaceInput(
            String sourceFamily,
            String sourceInstance,
            String server,
            String environment) {
    }

    public record AttestationInput(String source, String reference) {
    }

    private record SafeResult(
            String targetRef,
            AuthorityOperationOutcome outcome,
            long version,
            java.time.Instant occurredAt) {
    }
}
