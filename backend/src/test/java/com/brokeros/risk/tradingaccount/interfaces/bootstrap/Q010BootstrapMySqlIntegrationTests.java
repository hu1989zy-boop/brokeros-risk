package com.brokeros.risk.tradingaccount.interfaces.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q010_MYSQL_TEST_URL", matches = ".+")
class Q010BootstrapMySqlIntegrationTests {

    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";

    @TempDir
    Path tempDirectory;

    @Test
    void controlledCommandUsesTrustedServiceAuthorizationAndExactReplay() throws Exception {
        DataSource dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        provisionCommandActor(jdbc);

        Map<String, String> properties = applicationProperties();
        Map<String, String> previous = snapshot(properties);
        properties.forEach(System::setProperty);
        try {
            Path scopeManifest = tempDirectory.resolve("scope.json");
            Files.writeString(scopeManifest, scopeManifest());
            ByteArrayOutputStream scopeOutput = new ByteArrayOutputStream();
            assertThat(TradingAccountAuthorityBootstrapCommand.run(
                    new String[] {scopeManifest.toString()}, new PrintStream(scopeOutput))).isZero();
            String safeScopeOutput = scopeOutput.toString();
            String scopeRef = field(safeScopeOutput, "targetRef");
            assertThat(scopeRef).startsWith("aas-");
            assertThat(safeScopeOutput).doesNotContain(
                    "scope-approval-secret", "Controlled scope registration", ACTOR);

            Path accountManifest = tempDirectory.resolve("account.json");
            Files.writeString(accountManifest, accountManifest(scopeRef));
            ByteArrayOutputStream accountOutput = new ByteArrayOutputStream();
            assertThat(TradingAccountAuthorityBootstrapCommand.run(
                    new String[] {accountManifest.toString()}, new PrintStream(accountOutput))).isZero();
            String firstAccountOutput = accountOutput.toString();
            assertThat(firstAccountOutput).contains("outcome=CREATED");
            assertThat(firstAccountOutput).doesNotContain(
                    "SecretExternalKey", "account-approval-secret", "platform", ACTOR);

            ByteArrayOutputStream replayOutput = new ByteArrayOutputStream();
            assertThat(TradingAccountAuthorityBootstrapCommand.run(
                    new String[] {accountManifest.toString()}, new PrintStream(replayOutput))).isZero();
            assertThat(replayOutput.toString()).isEqualTo(firstAccountOutput);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trading_account_authority_history", Integer.class)).isEqualTo(2);

            jdbc.update("""
                    UPDATE security_actor_capability SET status = 'REVOKED', revoked_at = ?,
                        version = version + 1, updated_at = ?
                    WHERE capability = 'trading-account-reference:register'
                    """, java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now()));
            Path deniedManifest = tempDirectory.resolve("denied.json");
            Files.writeString(deniedManifest, scopeManifest().replace(
                    "00000000-0000-4000-8000-000000000010",
                    "00000000-0000-4000-8000-000000000012"));
            ByteArrayOutputStream deniedOutput = new ByteArrayOutputStream();
            assertThat(TradingAccountAuthorityBootstrapCommand.run(
                    new String[] {deniedManifest.toString()}, new PrintStream(deniedOutput))).isEqualTo(3);
            assertThat(deniedOutput.toString()).contains("AUTHORIZATION_DENIED")
                    .doesNotContain("scope-approval-secret", ACTOR);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trading_account_authority_scope", Integer.class)).isEqualTo(1);
        } finally {
            restore(previous, properties);
        }
    }

    private void provisionCommandActor(JdbcTemplate jdbc) {
        Instant now = Instant.parse("2026-08-27T10:00:00Z");
        var timestamp = java.sql.Timestamp.from(now);
        jdbc.update("""
                INSERT INTO security_actor (
                    actor_ref, actor_type, status, version, provisioning_source,
                    provisioning_ref, created_at, updated_at)
                VALUES (?, 'SERVICE', 'ACTIVE', 0, 'deployment', 'q010-v7-runtime', ?, ?)
                """, ACTOR, timestamp, timestamp);
        Long actorId = jdbc.queryForObject(
                "SELECT id FROM security_actor WHERE actor_ref = ?", Long.class, ACTOR);
        jdbc.update("""
                INSERT INTO security_principal_mapping (
                    actor_id, issuer, subject, principal_type, status, version,
                    provisioning_source, provisioning_ref, created_at, updated_at)
                VALUES (?, 'urn:brokeros:risk:internal-service',
                        'trading-account-reference-provisioner', 'SERVICE', 'ACTIVE', 0,
                        'deployment', 'q010-v7-runtime', ?, ?)
                """, actorId, timestamp, timestamp);
        for (String capability : java.util.List.of(
                "trading-account-reference:register",
                "trading-account-reference:change-lifecycle",
                "trading-account-reference:read")) {
            jdbc.update("""
                    INSERT INTO security_actor_capability (
                        actor_id, capability, status, version, provisioning_source,
                        provisioning_ref, granted_at, revoked_at, updated_at)
                    VALUES (?, ?, 'GRANTED', 0, 'deployment', 'q010-v7-runtime', ?, NULL, ?)
                    """, actorId, capability, timestamp, timestamp);
        }
    }

    private Map<String, String> applicationProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", required("Q010_MYSQL_TEST_URL"));
        properties.put("spring.datasource.username", required("Q010_MYSQL_TEST_USERNAME"));
        properties.put("spring.datasource.password", required("Q010_MYSQL_TEST_PASSWORD"));
        properties.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://issuer.test");
        properties.put("spring.security.oauth2.resourceserver.jwt.audiences", "brokeros-risk");
        properties.put("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "https://issuer.test/jwks");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "WARN");
        return properties;
    }

    private Map<String, String> snapshot(Map<String, String> properties) {
        Map<String, String> previous = new LinkedHashMap<>();
        properties.keySet().forEach(key -> previous.put(key, System.getProperty(key)));
        return previous;
    }

    private void restore(Map<String, String> previous, Map<String, String> applied) {
        applied.keySet().forEach(key -> {
            String value = previous.get(key);
            if (value == null) System.clearProperty(key); else System.setProperty(key, value);
        });
    }

    private String field(String output, String name) {
        for (String token : output.trim().split(" ")) {
            if (token.startsWith(name + "=")) return token.substring(name.length() + 1);
        }
        throw new IllegalArgumentException("safe result field is absent");
    }

    private String scopeManifest() {
        return """
                {
                  "schemaVersion":1,
                  "operationId":"00000000-0000-4000-8000-000000000010",
                  "operation":"REGISTER_AUTHORITY_SCOPE",
                  "authorityScopeRef":null,
                  "tradingAccountRef":null,
                  "sourceNamespace":null,
                  "externalAccountKey":null,
                  "expectedVersion":null,
                  "attestation":{"source":"broker-record","reference":"scope-approval-secret"},
                  "reason":"Controlled scope registration",
                  "changeRef":"change-runtime-1"
                }
                """;
    }

    private String accountManifest(String scopeRef) {
        return """
                {
                  "schemaVersion":1,
                  "operationId":"00000000-0000-4000-8000-000000000011",
                  "operation":"REGISTER_TRADING_ACCOUNT",
                  "authorityScopeRef":"%s",
                  "tradingAccountRef":null,
                  "sourceNamespace":{"sourceFamily":"platform","sourceInstance":"instance","server":"server-1","environment":"production"},
                  "externalAccountKey":"SecretExternalKey",
                  "expectedVersion":null,
                  "attestation":{"source":"broker-record","reference":"account-approval-secret"},
                  "reason":"Controlled account registration",
                  "changeRef":"change-runtime-2"
                }
                """.formatted(scopeRef);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q010_MYSQL_TEST_URL"));
        source.setUsername(required("Q010_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q010_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
}
