package com.brokeros.risk.reference;

import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.flywaydb.core.Flyway;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public final class Q020ReferenceListMySqlSupport {

    public static final Instant NOW = Instant.parse("2026-09-05T10:00:00Z");
    public static final String ACTOR_REF =
            "20000000-0000-4000-8000-000000000001";

    private Q020ReferenceListMySqlSupport() {
    }

    public static DataSource migratedDataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q011_MYSQL_TEST_URL"));
        source.setUsername(required("Q011_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q011_MYSQL_TEST_PASSWORD"));
        Flyway flyway = Flyway.configure().dataSource(source)
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        return source;
    }

    public static ActorContext actorContext() {
        ActorRef actorRef = new ActorRef(ACTOR_REF);
        return new ActorContext(
                actorRef,
                ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:q020-test",
                        "operator",
                        ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS,
                NOW,
                null,
                UUID.fromString("20000000-0000-4000-8000-000000000002"),
                "q020-request",
                "20000000000000000000000000000001");
    }

    public static AuthorizationGuard authorizationGuard(boolean allowed) {
        return new AuthorizationGuard((context, capability) -> allowed
                ? AuthorizationDecision.allow(
                        context.actorRef(), capability, NOW, 1, 1)
                : AuthorizationDecision.deny(
                        context.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED,
                        NOW, 1L, null));
    }

    public static String reference(String prefix, int value) {
        return prefix + "20000000-0000-4000-8000-" + String.format("%012d", value);
    }

    public static MappingJackson2HttpMessageConverter jsonConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
