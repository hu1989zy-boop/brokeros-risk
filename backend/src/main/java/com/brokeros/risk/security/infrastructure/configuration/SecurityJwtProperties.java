package com.brokeros.risk.security.infrastructure.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("brokeros.risk.security.jwt")
public record SecurityJwtProperties(Duration clockSkew) {

    private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Duration MAX_CLOCK_SKEW = Duration.ofSeconds(300);

    public SecurityJwtProperties {
        clockSkew = clockSkew == null ? DEFAULT_CLOCK_SKEW : clockSkew;
        if (clockSkew.isNegative() || clockSkew.compareTo(MAX_CLOCK_SKEW) > 0) {
            throw new IllegalArgumentException(
                    "brokeros.risk.security.jwt.clock-skew must be between 0s and 300s");
        }
    }
}
