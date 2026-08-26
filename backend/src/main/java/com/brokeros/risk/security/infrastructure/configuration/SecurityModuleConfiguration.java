package com.brokeros.risk.security.infrastructure.configuration;

import java.time.Clock;
import java.util.Set;

import com.brokeros.risk.security.application.ActorMappingService;
import com.brokeros.risk.security.application.ActorProvisioningService;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.RegisteredServiceDescriptor;
import com.brokeros.risk.security.application.ServiceActorContextFactory;
import com.brokeros.risk.security.application.port.ActorMappingPort;
import com.brokeros.risk.security.application.port.AuthorizationPort;
import com.brokeros.risk.security.application.port.SecurityProvisioningPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityModuleConfiguration {

    @Bean
    Clock securityClock() {
        return Clock.systemUTC();
    }

    @Bean
    ActorMappingService actorMappingService(ActorMappingPort actorMappingPort) {
        return new ActorMappingService(actorMappingPort);
    }

    @Bean
    AuthorizationGuard authorizationGuard(AuthorizationPort authorizationPort) {
        return new AuthorizationGuard(authorizationPort);
    }

    @Bean
    ActorProvisioningService actorProvisioningService(
            SecurityProvisioningPort provisioningPort,
            Clock securityClock) {
        return new ActorProvisioningService(provisioningPort, securityClock);
    }

    @Bean
    ServiceActorContextFactory serviceActorContextFactory(
            ActorMappingService actorMappingService,
            Clock securityClock) {
        Set<RegisteredServiceDescriptor> noSpeculativeServiceIdentities = Set.of();
        return new ServiceActorContextFactory(
                actorMappingService,
                noSpeculativeServiceIdentities,
                securityClock);
    }
}
