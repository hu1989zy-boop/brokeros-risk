package com.brokeros.risk.security.infrastructure.configuration;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

public final class SecurityJwtDecoderFactory {

    private SecurityJwtDecoderFactory() {
    }

    public static NimbusJwtDecoder create(
            String issuerUri,
            List<String> audiences,
            String jwkSetUri,
            Duration clockSkew) {
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalStateException(
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri is required");
        }
        if (audiences == null
                || audiences.isEmpty()
                || audiences.stream().anyMatch(audience -> !StringUtils.hasText(audience))) {
            throw new IllegalStateException(
                    "spring.security.oauth2.resourceserver.jwt.audiences is required");
        }

        NimbusJwtDecoder decoder = StringUtils.hasText(jwkSetUri)
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(clockSkew);
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<Collection<String>>(
                        JwtClaimNames.AUD,
                        tokenAudiences -> tokenAudiences != null
                                && tokenAudiences.stream().anyMatch(audiences::contains));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator));
        return decoder;
    }
}
