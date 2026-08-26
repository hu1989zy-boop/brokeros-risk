package com.brokeros.risk.security.interfaces.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.brokeros.risk.BrokerOsRiskApplication;
import com.brokeros.risk.security.application.ActorProvisioningService;
import com.brokeros.risk.security.application.ActorProvisioningSpec;
import com.brokeros.risk.security.application.ProvisioningManifest;
import com.brokeros.risk.security.application.ProvisioningResult;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.ProvisioningMetadata;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public final class SecurityBootstrapCommand {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SecurityBootstrapCommand.class);

    private SecurityBootstrapCommand() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Security bootstrap requires exactly one external manifest path");
        }
        Path manifestPath = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException("Security bootstrap manifest is not a file");
        }

        SpringApplication application = new SpringApplication(BrokerOsRiskApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run()) {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class)
                    .copy()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            BootstrapManifestInput input =
                    objectMapper.readValue(manifestPath.toFile(), BootstrapManifestInput.class);
            ProvisioningManifest manifest = toManifest(input);
            ProvisioningResult result = context
                    .getBean(ActorProvisioningService.class)
                    .provision(manifest);
            LOGGER.info(
                    "security_event=security_bootstrap outcome=SUCCESS createdActors={} unchangedActors={}",
                    result.createdActors(),
                    result.unchangedActors());
        }
    }

    static ProvisioningManifest toManifest(BootstrapManifestInput input) {
        if (input == null || input.actors() == null) {
            throw new IllegalArgumentException("Security bootstrap manifest is incomplete");
        }
        ProvisioningMetadata metadata =
                new ProvisioningMetadata(input.provisioningSource(), input.provisioningRef());
        List<ActorProvisioningSpec> actors = input.actors().stream()
                .map(SecurityBootstrapCommand::toActorSpec)
                .toList();
        return new ProvisioningManifest(metadata, actors);
    }

    private static ActorProvisioningSpec toActorSpec(BootstrapActorInput input) {
        ActorType actorType;
        try {
            actorType = ActorType.valueOf(input.actorType());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "actorType must be exactly HUMAN or SERVICE",
                    exception);
        }

        if (input.principals() == null || input.capabilities() == null) {
            throw new IllegalArgumentException("actor principals and capabilities are required");
        }
        List<ExternalPrincipalKey> principalKeys = input.principals().stream()
                .map(principal -> new ExternalPrincipalKey(
                        principal.issuer(),
                        principal.subject(),
                        actorType))
                .toList();
        Set<Capability> capabilities = input.capabilities().stream()
                .map(Capability::new)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        return new ActorProvisioningSpec(actorType, principalKeys, capabilities);
    }

    public record BootstrapManifestInput(
            String provisioningSource,
            String provisioningRef,
            List<BootstrapActorInput> actors) {
    }

    public record BootstrapActorInput(
            String actorType,
            List<BootstrapPrincipalInput> principals,
            List<String> capabilities) {
    }

    public record BootstrapPrincipalInput(String issuer, String subject) {
    }
}
