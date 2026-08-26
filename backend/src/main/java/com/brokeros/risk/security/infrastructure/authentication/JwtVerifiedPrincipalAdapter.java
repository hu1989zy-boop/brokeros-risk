package com.brokeros.risk.security.infrastructure.authentication;

import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.VerifiedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtVerifiedPrincipalAdapter {

    private final Clock clock;

    public JwtVerifiedPrincipalAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public VerifiedPrincipal adapt(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");
        URL issuer = Objects.requireNonNull(jwt.getIssuer(), "JWT issuer is required");
        String subject = jwt.getSubject();
        Instant expiresAt = Objects.requireNonNull(
                jwt.getExpiresAt(),
                "JWT expiration is required");

        return new VerifiedPrincipal(
                new ExternalPrincipalKey(issuer.toString(), subject, ActorType.HUMAN),
                AuthenticationMethod.SIGNED_JWT,
                clock.instant(),
                expiresAt);
    }
}
