package com.brokeros.risk.security.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.brokeros.risk.security.application.ActorProvisioningSpec;
import com.brokeros.risk.security.application.ProvisioningManifest;
import com.brokeros.risk.security.application.ProvisioningResult;
import com.brokeros.risk.security.application.SecurityDependencyUnavailableException;
import com.brokeros.risk.security.application.SecurityProvisioningConflictException;
import com.brokeros.risk.security.application.port.SecurityProvisioningPort;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorStatus;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.CapabilityStatus;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.PrincipalMappingStatus;
import com.brokeros.risk.security.domain.ProvisioningMetadata;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcSecurityProvisioningAdapter implements SecurityProvisioningPort {

    private static final String INSERT_ACTOR_SQL = """
            INSERT INTO security_actor (
                actor_ref, actor_type, status, version,
                provisioning_source, provisioning_ref, created_at, updated_at
            ) VALUES (?, ?, 'ACTIVE', 0, ?, ?, ?, ?)
            """;

    private static final String INSERT_MAPPING_SQL = """
            INSERT INTO security_principal_mapping (
                actor_id, issuer, subject, principal_type, status, version,
                provisioning_source, provisioning_ref, created_at, updated_at
            ) VALUES (?, ?, ?, ?, 'ACTIVE', 0, ?, ?, ?, ?)
            """;

    private static final String INSERT_CAPABILITY_SQL = """
            INSERT INTO security_actor_capability (
                actor_id, capability, status, version,
                provisioning_source, provisioning_ref,
                granted_at, revoked_at, updated_at
            ) VALUES (?, ?, 'GRANTED', 0, ?, ?, ?, NULL, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSecurityProvisioningAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public ProvisioningResult provision(ProvisioningManifest manifest, Instant occurredAt) {
        try {
            int created = 0;
            int unchanged = 0;
            for (ActorProvisioningSpec actorSpec : manifest.actors()) {
                if (resolveExistingActorId(actorSpec).isPresent()) {
                    verifyExactExistingState(actorSpec, manifest.metadata());
                    unchanged++;
                } else {
                    createActor(actorSpec, manifest.metadata(), occurredAt);
                    created++;
                }
            }
            return new ProvisioningResult(created, unchanged);
        } catch (SecurityProvisioningConflictException exception) {
            throw exception;
        } catch (DuplicateKeyException exception) {
            throw new SecurityProvisioningConflictException(exception);
        } catch (DataAccessException exception) {
            throw new SecurityDependencyUnavailableException(exception);
        }
    }

    @Override
    @Transactional
    public long changeActorStatus(
            ActorRef actorRef,
            long expectedVersion,
            ActorStatus status,
            ProvisioningMetadata metadata,
            Instant occurredAt) {
        String sql = """
                UPDATE security_actor
                SET status = ?,
                    version = version + 1,
                    provisioning_source = ?,
                    provisioning_ref = ?,
                    updated_at = ?
                WHERE actor_ref = ? AND version = ?
                """;
        return checkedVersionedUpdate(
                sql,
                status.name(),
                metadata.source(),
                metadata.reference(),
                Timestamp.from(occurredAt),
                actorRef.value(),
                expectedVersion);
    }

    @Override
    @Transactional
    public long changeMappingStatus(
            ExternalPrincipalKey principalKey,
            long expectedVersion,
            PrincipalMappingStatus status,
            ProvisioningMetadata metadata,
            Instant occurredAt) {
        String sql = """
                UPDATE security_principal_mapping
                SET status = ?,
                    version = version + 1,
                    provisioning_source = ?,
                    provisioning_ref = ?,
                    updated_at = ?
                WHERE issuer = ?
                  AND subject = ?
                  AND principal_type = ?
                  AND version = ?
                """;
        return checkedVersionedUpdate(
                sql,
                status.name(),
                metadata.source(),
                metadata.reference(),
                Timestamp.from(occurredAt),
                principalKey.issuer(),
                principalKey.subject(),
                principalKey.principalType().name(),
                expectedVersion);
    }

    @Override
    @Transactional
    public long changeCapabilityStatus(
            ActorRef actorRef,
            Capability capability,
            long expectedVersion,
            CapabilityStatus status,
            ProvisioningMetadata metadata,
            Instant occurredAt) {
        String sql = """
                UPDATE security_actor_capability c
                JOIN security_actor a ON a.id = c.actor_id
                SET c.status = ?,
                    c.version = c.version + 1,
                    c.provisioning_source = ?,
                    c.provisioning_ref = ?,
                    c.granted_at = IF(? = 'GRANTED', ?, c.granted_at),
                    c.revoked_at = IF(? = 'REVOKED', ?, NULL),
                    c.updated_at = ?
                WHERE a.actor_ref = ?
                  AND c.capability = ?
                  AND c.version = ?
                """;
        Timestamp timestamp = Timestamp.from(occurredAt);
        return checkedVersionedUpdate(
                sql,
                status.name(),
                metadata.source(),
                metadata.reference(),
                status.name(),
                timestamp,
                status.name(),
                timestamp,
                timestamp,
                actorRef.value(),
                capability.value(),
                expectedVersion);
    }

    private Optional<Long> resolveExistingActorId(ActorProvisioningSpec actorSpec) {
        Set<Long> actorIds = new HashSet<>();
        int existingMappingCount = 0;
        for (ExternalPrincipalKey key : actorSpec.principalKeys()) {
            List<Long> ids = jdbcTemplate.query(
                    """
                    SELECT actor_id
                    FROM security_principal_mapping
                    WHERE issuer = ? AND subject = ? AND principal_type = ?
                    """,
                    (resultSet, rowNumber) -> resultSet.getLong("actor_id"),
                    key.issuer(),
                    key.subject(),
                    key.principalType().name());
            if (ids.size() > 1) {
                throw new SecurityProvisioningConflictException();
            }
            if (!ids.isEmpty()) {
                existingMappingCount++;
                actorIds.add(ids.getFirst());
            }
        }

        if (existingMappingCount == 0) {
            return Optional.empty();
        }
        if (existingMappingCount != actorSpec.principalKeys().size() || actorIds.size() != 1) {
            throw new SecurityProvisioningConflictException();
        }
        return Optional.of(actorIds.iterator().next());
    }

    private void verifyExactExistingState(
            ActorProvisioningSpec actorSpec,
            ProvisioningMetadata metadata) {
        long actorId = resolveExistingActorId(actorSpec)
                .orElseThrow(SecurityProvisioningConflictException::new);

        ExistingActor actor = loadActor(actorId);
        if (actor.actorType() != actorSpec.actorType()
                || actor.status() != ActorStatus.ACTIVE
                || !actor.source().equals(metadata.source())
                || !actor.reference().equals(metadata.reference())) {
            throw new SecurityProvisioningConflictException();
        }

        Set<ExistingMapping> expectedMappings = actorSpec.principalKeys().stream()
                .map(key -> new ExistingMapping(
                        key,
                        PrincipalMappingStatus.ACTIVE,
                        metadata.source(),
                        metadata.reference()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!Set.copyOf(loadMappings(actorId)).equals(expectedMappings)) {
            throw new SecurityProvisioningConflictException();
        }

        Set<ExistingCapability> expectedCapabilities = actorSpec.capabilities().stream()
                .map(capability -> new ExistingCapability(
                        capability,
                        CapabilityStatus.GRANTED,
                        metadata.source(),
                        metadata.reference()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!Set.copyOf(loadCapabilities(actorId)).equals(expectedCapabilities)) {
            throw new SecurityProvisioningConflictException();
        }
    }

    private ExistingActor loadActor(long actorId) {
        List<ExistingActor> actors = jdbcTemplate.query(
                """
                SELECT actor_type, status, provisioning_source, provisioning_ref
                FROM security_actor
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> new ExistingActor(
                        ActorType.valueOf(resultSet.getString("actor_type")),
                        ActorStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("provisioning_source"),
                        resultSet.getString("provisioning_ref")),
                actorId);
        if (actors.size() != 1) {
            throw new SecurityProvisioningConflictException();
        }
        return actors.getFirst();
    }

    private List<ExistingMapping> loadMappings(long actorId) {
        return jdbcTemplate.query(
                """
                SELECT issuer, subject, principal_type, status,
                       provisioning_source, provisioning_ref
                FROM security_principal_mapping
                WHERE actor_id = ?
                """,
                (resultSet, rowNumber) -> new ExistingMapping(
                        new ExternalPrincipalKey(
                                resultSet.getString("issuer"),
                                resultSet.getString("subject"),
                                ActorType.valueOf(resultSet.getString("principal_type"))),
                        PrincipalMappingStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("provisioning_source"),
                        resultSet.getString("provisioning_ref")),
                actorId);
    }

    private List<ExistingCapability> loadCapabilities(long actorId) {
        return jdbcTemplate.query(
                """
                SELECT capability, status, provisioning_source, provisioning_ref
                FROM security_actor_capability
                WHERE actor_id = ?
                """,
                (resultSet, rowNumber) -> new ExistingCapability(
                        new Capability(resultSet.getString("capability")),
                        CapabilityStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("provisioning_source"),
                        resultSet.getString("provisioning_ref")),
                actorId);
    }

    private void createActor(
            ActorProvisioningSpec actorSpec,
            ProvisioningMetadata metadata,
            Instant occurredAt) {
        ActorRef actorRef = ActorRef.generate();
        Timestamp timestamp = Timestamp.from(occurredAt);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement =
                    connection.prepareStatement(INSERT_ACTOR_SQL, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, actorRef.value());
            statement.setString(2, actorSpec.actorType().name());
            statement.setString(3, metadata.source());
            statement.setString(4, metadata.reference());
            statement.setTimestamp(5, timestamp);
            statement.setTimestamp(6, timestamp);
            return statement;
        }, keyHolder);

        Number generatedKey = Objects.requireNonNull(
                keyHolder.getKey(),
                "actor insert did not return an internal identifier");
        long actorId = generatedKey.longValue();

        for (ExternalPrincipalKey key : actorSpec.principalKeys()) {
            jdbcTemplate.update(
                    INSERT_MAPPING_SQL,
                    actorId,
                    key.issuer(),
                    key.subject(),
                    key.principalType().name(),
                    metadata.source(),
                    metadata.reference(),
                    timestamp,
                    timestamp);
        }
        for (Capability capability : actorSpec.capabilities()) {
            jdbcTemplate.update(
                    INSERT_CAPABILITY_SQL,
                    actorId,
                    capability.value(),
                    metadata.source(),
                    metadata.reference(),
                    timestamp,
                    timestamp);
        }
    }

    private long checkedVersionedUpdate(String sql, Object... arguments) {
        try {
            int changedRows = jdbcTemplate.update(sql, arguments);
            if (changedRows != 1) {
                throw new SecurityProvisioningConflictException();
            }
            long expectedVersion = ((Number) arguments[arguments.length - 1]).longValue();
            return expectedVersion + 1;
        } catch (SecurityProvisioningConflictException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new SecurityDependencyUnavailableException(exception);
        }
    }

    private record ExistingActor(
            ActorType actorType,
            ActorStatus status,
            String source,
            String reference) {
    }

    private record ExistingMapping(
            ExternalPrincipalKey principalKey,
            PrincipalMappingStatus status,
            String source,
            String reference) {
    }

    private record ExistingCapability(
            Capability capability,
            CapabilityStatus status,
            String source,
            String reference) {
    }
}
