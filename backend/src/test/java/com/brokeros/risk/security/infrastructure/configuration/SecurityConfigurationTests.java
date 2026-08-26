package com.brokeros.risk.security.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityConfigurationTests {

    @Test
    void clockSkewDefaultsAndStaysWithinApprovedBounds() {
        assertThat(new SecurityJwtProperties(null).clockSkew()).isEqualTo(Duration.ofSeconds(60));
        assertThat(new SecurityJwtProperties(Duration.ZERO).clockSkew()).isEqualTo(Duration.ZERO);
        assertThat(new SecurityJwtProperties(Duration.ofSeconds(300)).clockSkew())
                .isEqualTo(Duration.ofSeconds(300));
        assertThatThrownBy(() -> new SecurityJwtProperties(Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityJwtProperties(Duration.ofSeconds(301)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decoderFactoryFailsBeforeNetworkForMissingTrustContract() {
        assertThatThrownBy(() -> SecurityJwtDecoderFactory.create(
                        null,
                        List.of("brokeros-risk"),
                        "https://issuer.test/jwks",
                        Duration.ofSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuer-uri");
        assertThatThrownBy(() -> SecurityJwtDecoderFactory.create(
                        "https://issuer.test",
                        List.of(),
                        "https://issuer.test/jwks",
                        Duration.ofSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audiences");
    }

    @Test
    void explicitJwkLocationBuildsDecoderWithoutDiscovery() {
        assertThat(SecurityJwtDecoderFactory.create(
                        "https://issuer.test",
                        List.of("brokeros-risk"),
                        "https://issuer.test/jwks",
                        Duration.ofSeconds(60)))
                .isNotNull();
    }
}
