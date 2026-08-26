package com.brokeros.risk.security.infrastructure.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.brokeros.risk.security.application.SecurityDependencyUnavailableException;
import com.brokeros.risk.security.application.port.AuthorizationPort;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorStatus;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.CapabilityStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthorizationAdapter implements AuthorizationPort {

    private static final String DECISION_SQL = """
            SELECT a.status AS actor_status,
                   a.version AS actor_version,
                   c.status AS capability_status,
                   c.version AS grant_version
            FROM security_actor a
            LEFT JOIN security_actor_capability c
              ON c.actor_id = a.id
             AND c.capability = ?
            WHERE a.actor_ref = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcAuthorizationAdapter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public AuthorizationDecision decide(ActorContext actorContext, Capability capability) {
        try {
            List<AuthorizationRow> rows = jdbcTemplate.query(
                    DECISION_SQL,
                    (resultSet, rowNumber) -> new AuthorizationRow(
                            ActorStatus.valueOf(resultSet.getString("actor_status")),
                            resultSet.getLong("actor_version"),
                            resultSet.getString("capability_status") == null
                                    ? null
                                    : CapabilityStatus.valueOf(
                                            resultSet.getString("capability_status")),
                            resultSet.getObject("grant_version", Long.class)),
                    capability.value(),
                    actorContext.actorRef().value());
            return toDecision(actorContext, capability, rows, clock.instant());
        } catch (DataAccessException exception) {
            throw new SecurityDependencyUnavailableException(exception);
        }
    }

    private AuthorizationDecision toDecision(
            ActorContext actorContext,
            Capability capability,
            List<AuthorizationRow> rows,
            Instant evaluatedAt) {
        if (rows.size() != 1) {
            return AuthorizationDecision.deny(
                    actorContext.actorRef(),
                    capability,
                    AuthorizationReason.ACTOR_INACTIVE,
                    evaluatedAt,
                    null,
                    null);
        }

        AuthorizationRow row = rows.getFirst();
        if (row.actorStatus() != ActorStatus.ACTIVE) {
            return AuthorizationDecision.deny(
                    actorContext.actorRef(),
                    capability,
                    AuthorizationReason.ACTOR_INACTIVE,
                    evaluatedAt,
                    row.actorVersion(),
                    row.grantVersion());
        }
        if (row.capabilityStatus() == null) {
            return AuthorizationDecision.deny(
                    actorContext.actorRef(),
                    capability,
                    AuthorizationReason.CAPABILITY_NOT_GRANTED,
                    evaluatedAt,
                    row.actorVersion(),
                    null);
        }
        if (row.capabilityStatus() == CapabilityStatus.REVOKED) {
            return AuthorizationDecision.deny(
                    actorContext.actorRef(),
                    capability,
                    AuthorizationReason.CAPABILITY_REVOKED,
                    evaluatedAt,
                    row.actorVersion(),
                    row.grantVersion());
        }
        return AuthorizationDecision.allow(
                actorContext.actorRef(),
                capability,
                evaluatedAt,
                row.actorVersion(),
                row.grantVersion());
    }

    private record AuthorizationRow(
            ActorStatus actorStatus,
            long actorVersion,
            CapabilityStatus capabilityStatus,
            Long grantVersion) {
    }
}
