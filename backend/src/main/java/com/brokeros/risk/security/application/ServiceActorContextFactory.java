package com.brokeros.risk.security.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.VerifiedPrincipal;

public class ServiceActorContextFactory {

    public static final String INTERNAL_SERVICE_ISSUER =
            "urn:brokeros:risk:internal-service";

    private static final Pattern VALID_SERVICE_CODE =
            Pattern.compile("[a-z][a-z0-9-]{0,62}");

    private final ActorMappingService actorMappingService;
    private final Set<RegisteredServiceDescriptor> registeredDescriptors;
    private final Clock clock;

    public ServiceActorContextFactory(
            ActorMappingService actorMappingService,
            Set<RegisteredServiceDescriptor> registeredDescriptors,
            Clock clock) {
        this.actorMappingService = Objects.requireNonNull(
                actorMappingService,
                "actorMappingService must not be null");
        this.registeredDescriptors = Set.copyOf(Objects.requireNonNull(
                registeredDescriptors,
                "registeredDescriptors must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ActorContext create(RegisteredServiceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        boolean registeredInstance = registeredDescriptors.stream()
                .anyMatch(registered -> registered == descriptor);
        if (!registeredInstance
                || descriptor.serviceCode() == null
                || !VALID_SERVICE_CODE.matcher(descriptor.serviceCode()).matches()
                || "system".equals(descriptor.serviceCode())) {
            throw new ActorAccessDeniedException();
        }

        Instant authenticatedAt = clock.instant();
        VerifiedPrincipal verifiedPrincipal = new VerifiedPrincipal(
                new ExternalPrincipalKey(
                        INTERNAL_SERVICE_ISSUER,
                        descriptor.serviceCode(),
                        ActorType.SERVICE),
                AuthenticationMethod.TRUSTED_IN_PROCESS,
                authenticatedAt,
                null);
        return actorMappingService.createContext(verifiedPrincipal, null, null);
    }
}
