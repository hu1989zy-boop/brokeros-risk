package com.brokeros.risk.security.infrastructure.configuration;

import java.time.Clock;
import java.time.Duration;
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
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000));
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
            Set<RegisteredServiceDescriptor> registeredDescriptors,
            Clock securityClock) {
        return new ServiceActorContextFactory(
                actorMappingService,
                registeredDescriptors,
                securityClock);
    }
}
