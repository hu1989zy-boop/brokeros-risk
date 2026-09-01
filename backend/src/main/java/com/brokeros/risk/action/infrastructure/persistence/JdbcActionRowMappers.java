package com.brokeros.risk.action.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionOperationType;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.ActionSemanticFingerprint;
import com.brokeros.risk.action.domain.ActionSource;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.action.domain.IntentText;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;
import org.springframework.jdbc.core.ResultSetExtractor;

final class JdbcActionRowMappers {

    private JdbcActionRowMappers() {
    }

    static ResultSetExtractor<Optional<ActionRecord>> actionRecord() {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            ActionRecord record = actionRecord(resultSet);
            if (resultSet.next()) {
                throw new ActionAuthorityUnavailableException();
            }
            return Optional.of(record);
        };
    }

    static ResultSetExtractor<Optional<CompletedActionOperation>> completedOperation() {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            ActionOperationId operationId = new ActionOperationId(
                    resultSet.getString("operation_id"));
            ActionOperationType operationType = ActionOperationType.valueOf(
                    resultSet.getString("operation_type"));
            ActionSemanticFingerprint fingerprint = new ActionSemanticFingerprint(
                    resultSet.getBytes("semantic_fingerprint"));
            ActionOperationOutcome outcome = ActionOperationOutcome.valueOf(
                    resultSet.getString("outcome"));
            java.time.Instant occurredAt = resultSet.getTimestamp("occurred_at").toInstant();
            ActionRecord record = actionRecord(resultSet);
            if (resultSet.next()) {
                throw new ActionAuthorityUnavailableException();
            }
            return Optional.of(new CompletedActionOperation(
                    operationId, operationType, fingerprint,
                    record.actionRef(), outcome, occurredAt, record));
        };
    }

    private static ActionRecord actionRecord(ResultSet resultSet) throws SQLException {
        return new ActionRecord(
                new ActionRef(resultSet.getString("action_ref")),
                new DecisionRef(resultSet.getString("decision_ref")),
                new IntentText(decodeUtf8(resultSet.getBytes("intent_text"))),
                ActionStatus.valueOf(resultSet.getString("status")),
                ActionSource.valueOf(resultSet.getString("source")),
                new ActorRef(resultSet.getString("recorded_by_actor_ref")),
                resultSet.getTimestamp("recorded_at").toInstant());
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(
                    "stored action content is not valid UTF-8", exception);
        }
    }
}
