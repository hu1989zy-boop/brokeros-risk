package com.brokeros.risk.security.infrastructure.persistence;

import java.util.List;

import com.brokeros.risk.security.application.ActorAccessDeniedException;
import com.brokeros.risk.security.application.SecurityDependencyUnavailableException;
import com.brokeros.risk.security.application.port.ActorMappingPort;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.MappedActor;
import com.brokeros.risk.security.domain.VerifiedPrincipal;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcActorMappingAdapter implements ActorMappingPort {

    private static final String RESOLVE_ACTIVE_ACTOR_SQL = """
            SELECT a.actor_ref, a.actor_type, a.version
            FROM security_principal_mapping m
            JOIN security_actor a ON a.id = m.actor_id
            WHERE m.issuer = ?
              AND m.subject = ?
              AND m.principal_type = ?
              AND m.status = 'ACTIVE'
              AND a.status = 'ACTIVE'
              AND a.actor_type = m.principal_type
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcActorMappingAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MappedActor resolveActiveActor(VerifiedPrincipal verifiedPrincipal) {
        ExternalPrincipalKey key = verifiedPrincipal.externalPrincipalKey();
        try {
            List<MappedActor> actors = jdbcTemplate.query(
                    RESOLVE_ACTIVE_ACTOR_SQL,
                    (resultSet, rowNumber) -> new MappedActor(
                            new ActorRef(resultSet.getString("actor_ref")),
                            ActorType.valueOf(resultSet.getString("actor_type")),
                            resultSet.getLong("version")),
                    key.issuer(),
                    key.subject(),
                    key.principalType().name());
            if (actors.size() != 1) {
                throw new ActorAccessDeniedException();
            }
            return actors.getFirst();
        } catch (DataAccessException exception) {
            throw new SecurityDependencyUnavailableException(exception);
        }
    }
}
