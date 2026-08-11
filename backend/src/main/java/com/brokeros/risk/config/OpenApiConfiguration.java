package com.brokeros.risk.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BrokerOS Risk API",
                version = "0.1.0",
                description = "Broker-neutral Forex/CFD risk management platform API"))
public class OpenApiConfiguration {
}
