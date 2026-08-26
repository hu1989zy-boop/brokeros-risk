package com.brokeros.risk.security.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.security.application.ActorAccessDeniedException;
import com.brokeros.risk.security.application.ActorProvisioningService;
import com.brokeros.risk.security.application.ActorProvisioningSpec;
import com.brokeros.risk.security.application.ProvisioningManifest;
import com.brokeros.risk.security.application.SecurityProvisioningConflictException;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.ProvisioningMetadata;
import com.brokeros.risk.security.domain.VerifiedPrincipal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "Q009_MYSQL_TEST_URL", matches = ".+")
class Q009MySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    @Test
    void verifiesMigrationConstraintsQueryPlansAndPersistenceLifecycle() {
        String url = requiredEnvironment("Q009_MYSQL_TEST_URL");
        String username = requiredEnvironment("Q009_MYSQL_TEST_USERNAME");
        String password = requiredEnvironment("Q009_MYSQL_TEST_PASSWORD");

        Flyway v1Flyway = Flyway.configure()
                .dataSource(url, username, password)
                .cleanDisabled(false)
                .target("1")
                .load();
        v1Flyway.clean();
        assertThat(v1Flyway.migrate().migrationsExecuted).isEqualTo(1);

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .cleanDisabled(false)
                .load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();

        Flyway restartedFlyway = Flyway.configure()
                .dataSource(url, username, password)
                .cleanDisabled(false)
                .load();
        assertThat(restartedFlyway.migrate().migrationsExecuted).isZero();
        restartedFlyway.validate();

        DataSource dataSource = dataSource(url, username, password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        verifySchemaConstraintsAndPlans(jdbcTemplate);
        verifyAdaptersAndLifecycle(dataSource, jdbcTemplate);
    }

    private void verifySchemaConstraintsAndPlans(JdbcTemplate jdbcTemplate) {
        long actorId = insertActor(jdbcTemplate, ActorType.HUMAN, "schema-check");
        insertMapping(jdbcTemplate, actorId, "ExactSubject", ActorType.HUMAN, "schema-check");
        insertCapability(jdbcTemplate, actorId, "security-test:read", "schema-check");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_principal_mapping WHERE subject = ?",
                Integer.class,
                "exactsubject")).isZero();

        insertMapping(jdbcTemplate, actorId, "exactsubject", ActorType.HUMAN, "schema-case");
        assertThatThrownBy(() -> insertMapping(
                        jdbcTemplate,
                        actorId,
                        "ExactSubject",
                        ActorType.HUMAN,
                        "schema-duplicate"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO security_actor (
                            actor_ref, actor_type, status, version,
                            provisioning_source, provisioning_ref, created_at, updated_at
                        ) VALUES (?, 'SYSTEM', 'ACTIVE', 0, 'test', 'invalid-type', ?, ?)
                        """,
                        UUID.randomUUID().toString(),
                        NOW,
                        NOW))
                .satisfies(Q009MySqlIntegrationTests::assertMySqlCheckConstraintViolation);

        assertThatThrownBy(() -> insertCapability(
                        jdbcTemplate,
                        actorId,
                        "security-test:*",
                        "invalid-capability"))
                .satisfies(Q009MySqlIntegrationTests::assertMySqlCheckConstraintViolation);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "DELETE FROM security_actor WHERE id = ?",
                        actorId))
                .isInstanceOf(DataIntegrityViolationException.class);

        List<Map<String, Object>> mappingPlan = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT a.actor_ref, a.actor_type, a.version
                FROM security_principal_mapping m
                JOIN security_actor a ON a.id = m.actor_id
                WHERE m.issuer = ?
                  AND m.subject = ?
                  AND m.principal_type = ?
                  AND m.status = 'ACTIVE'
                  AND a.status = 'ACTIVE'
                  AND a.actor_type = m.principal_type
                """,
                "https://issuer.brokeros.test",
                "ExactSubject",
                "HUMAN");
        assertThat(mappingPlan)
                .anySatisfy(row -> assertThat(String.valueOf(row.get("key")))
                        .contains("uk_security_principal_mapping_external_key"));

        List<Map<String, Object>> authorizationPlan = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT a.status, a.version, c.status, c.version
                FROM security_actor a
                LEFT JOIN security_actor_capability c
                  ON c.actor_id = a.id AND c.capability = ?
                WHERE a.actor_ref = ?
                """,
                "security-test:read",
                actorRefForId(jdbcTemplate, actorId));
        assertThat(authorizationPlan)
                .allSatisfy(row -> assertThat(String.valueOf(row.get("type")))
                        .isNotEqualToIgnoringCase("ALL"));
    }

    private void verifyAdaptersAndLifecycle(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        JdbcSecurityProvisioningAdapter provisioningAdapter =
                new JdbcSecurityProvisioningAdapter(jdbcTemplate);
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(new JdbcTransactionManager(dataSource));
        ActorProvisioningService provisioningService =
                new ActorProvisioningService(provisioningAdapter, Clock.systemUTC());
        ExternalPrincipalKey principalKey = new ExternalPrincipalKey(
                "https://issuer.brokeros.test",
                "provisioned-subject",
                ActorType.HUMAN);
        Capability capability = new Capability("security-test:read");
        ProvisioningMetadata metadata =
                new ProvisioningMetadata("integration-test", "manifest-v1");
        ProvisioningManifest manifest = new ProvisioningManifest(
                metadata,
                List.of(new ActorProvisioningSpec(
                        ActorType.HUMAN,
                        List.of(principalKey),
                        Set.of(capability))));

        com.brokeros.risk.security.application.ProvisioningResult created =
                transactionTemplate.execute(status -> provisioningService.provision(manifest));
        assertThat(created.createdActors()).isEqualTo(1);
        assertThat(created.unchangedActors()).isZero();

        com.brokeros.risk.security.application.ProvisioningResult unchanged =
                transactionTemplate.execute(status -> provisioningService.provision(manifest));
        assertThat(unchanged.createdActors()).isZero();
        assertThat(unchanged.unchangedActors()).isEqualTo(1);

        ProvisioningManifest conflictingManifest = new ProvisioningManifest(
                metadata,
                List.of(new ActorProvisioningSpec(
                        ActorType.HUMAN,
                        List.of(principalKey),
                        Set.of(new Capability("security-test:write")))));
        assertThatThrownBy(() -> transactionTemplate.execute(
                        status -> provisioningService.provision(conflictingManifest)))
                .isInstanceOf(SecurityProvisioningConflictException.class);

        JdbcActorMappingAdapter mappingAdapter = new JdbcActorMappingAdapter(jdbcTemplate);
        VerifiedPrincipal verifiedPrincipal = new VerifiedPrincipal(
                principalKey,
                AuthenticationMethod.SIGNED_JWT,
                NOW,
                NOW.plusSeconds(300));
        var mappedActor = mappingAdapter.resolveActiveActor(verifiedPrincipal);
        ActorContext actorContext = new ActorContext(
                mappedActor.actorRef(),
                mappedActor.actorType(),
                principalKey,
                AuthenticationMethod.SIGNED_JWT,
                NOW,
                NOW.plusSeconds(300),
                UUID.randomUUID(),
                null,
                null);
        JdbcAuthorizationAdapter authorizationAdapter =
                new JdbcAuthorizationAdapter(jdbcTemplate, Clock.systemUTC());
        assertThat(authorizationAdapter.decide(actorContext, capability).isAllowed()).isTrue();

        long grantVersion = jdbcTemplate.queryForObject(
                """
                SELECT c.version
                FROM security_actor_capability c
                JOIN security_actor a ON a.id = c.actor_id
                WHERE a.actor_ref = ? AND c.capability = ?
                """,
                Long.class,
                mappedActor.actorRef().value(),
                capability.value());
        assertThat(provisioningService.revokeCapability(
                mappedActor.actorRef(),
                capability,
                grantVersion,
                new ProvisioningMetadata("integration-test", "revoke-v1")))
                .isEqualTo(grantVersion + 1);
        assertThat(authorizationAdapter.decide(actorContext, capability).reason())
                .isEqualTo(AuthorizationReason.CAPABILITY_REVOKED);

        long regrantedVersion = provisioningService.regrantCapability(
                mappedActor.actorRef(),
                capability,
                grantVersion + 1,
                new ProvisioningMetadata("integration-test", "regrant-v1"));
        assertThat(regrantedVersion).isEqualTo(grantVersion + 2);
        assertThat(authorizationAdapter.decide(actorContext, capability).isAllowed()).isTrue();

        long mappingVersion = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM security_principal_mapping
                WHERE issuer = ? AND subject = ? AND principal_type = ?
                """,
                Long.class,
                principalKey.issuer(),
                principalKey.subject(),
                principalKey.principalType().name());
        long disabledMappingVersion = provisioningService.disableMapping(
                principalKey,
                mappingVersion,
                new ProvisioningMetadata("integration-test", "disable-mapping-v1"));
        assertThatThrownBy(() -> mappingAdapter.resolveActiveActor(verifiedPrincipal))
                .isInstanceOf(ActorAccessDeniedException.class);
        assertThat(provisioningService.reactivateMapping(
                principalKey,
                disabledMappingVersion,
                new ProvisioningMetadata("integration-test", "reactivate-mapping-v1")))
                .isEqualTo(disabledMappingVersion + 1);
        assertThat(mappingAdapter.resolveActiveActor(verifiedPrincipal).actorRef())
                .isEqualTo(mappedActor.actorRef());

        long actorVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM security_actor WHERE actor_ref = ?",
                Long.class,
                mappedActor.actorRef().value());
        provisioningService.disableActor(
                mappedActor.actorRef(),
                actorVersion,
                new ProvisioningMetadata("integration-test", "disable-v1"));
        assertThatThrownBy(() -> mappingAdapter.resolveActiveActor(verifiedPrincipal))
                .isInstanceOf(ActorAccessDeniedException.class);
        assertThatThrownBy(() -> provisioningService.disableActor(
                        mappedActor.actorRef(),
                        actorVersion,
                        new ProvisioningMetadata("integration-test", "stale-v1")))
                .isInstanceOf(SecurityProvisioningConflictException.class);
        assertThat(provisioningService.reactivateActor(
                mappedActor.actorRef(),
                actorVersion + 1,
                new ProvisioningMetadata("integration-test", "reactivate-v1")))
                .isEqualTo(actorVersion + 2);
        assertThat(mappingAdapter.resolveActiveActor(verifiedPrincipal).actorRef())
                .isEqualTo(mappedActor.actorRef());
    }

    private long insertActor(
            JdbcTemplate jdbcTemplate,
            ActorType actorType,
            String provisioningRef) {
        String actorRef = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                INSERT INTO security_actor (
                    actor_ref, actor_type, status, version,
                    provisioning_source, provisioning_ref, created_at, updated_at
                ) VALUES (?, ?, 'ACTIVE', 0, 'integration-test', ?, ?, ?)
                """,
                actorRef,
                actorType.name(),
                provisioningRef,
                NOW,
                NOW);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM security_actor WHERE actor_ref = ?",
                Long.class,
                actorRef);
    }

    private void insertMapping(
            JdbcTemplate jdbcTemplate,
            long actorId,
            String subject,
            ActorType actorType,
            String provisioningRef) {
        jdbcTemplate.update(
                """
                INSERT INTO security_principal_mapping (
                    actor_id, issuer, subject, principal_type, status, version,
                    provisioning_source, provisioning_ref, created_at, updated_at
                ) VALUES (?, 'https://issuer.brokeros.test', ?, ?, 'ACTIVE', 0,
                          'integration-test', ?, ?, ?)
                """,
                actorId,
                subject,
                actorType.name(),
                provisioningRef,
                NOW,
                NOW);
    }

    private void insertCapability(
            JdbcTemplate jdbcTemplate,
            long actorId,
            String capability,
            String provisioningRef) {
        jdbcTemplate.update(
                """
                INSERT INTO security_actor_capability (
                    actor_id, capability, status, version,
                    provisioning_source, provisioning_ref,
                    granted_at, revoked_at, updated_at
                ) VALUES (?, ?, 'GRANTED', 0, 'integration-test', ?, ?, NULL, ?)
                """,
                actorId,
                capability,
                provisioningRef,
                NOW,
                NOW);
    }

    private String actorRefForId(JdbcTemplate jdbcTemplate, long actorId) {
        return jdbcTemplate.queryForObject(
                "SELECT actor_ref FROM security_actor WHERE id = ?",
                String.class,
                actorId);
    }

    private static DataSource dataSource(String url, String username, String password) {
        return new DriverManagerDataSource(url, username, password);
    }

    private static void assertMySqlCheckConstraintViolation(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOf(DataAccessException.class)
                .hasRootCauseInstanceOf(SQLException.class);

        Throwable rootCause = throwable;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        SQLException sqlException = (SQLException) rootCause;
        assertThat(sqlException.getErrorCode()).isEqualTo(3819);
        assertThat(sqlException.getSQLState()).isEqualTo("HY000");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be provided for disposable MySQL test");
        }
        return value;
    }
}
