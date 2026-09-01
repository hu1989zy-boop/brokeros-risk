package com.brokeros.risk.actionoutcome.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationType;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSemanticFingerprint;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSource;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import com.brokeros.risk.actionoutcome.domain.OutcomeText;
import com.brokeros.risk.security.domain.ActorRef;
import org.springframework.jdbc.core.ResultSetExtractor;

final class JdbcActionOutcomeRowMappers {

    private JdbcActionOutcomeRowMappers() {
    }

    static ResultSetExtractor<Optional<ActionOutcomeRecord>> actionOutcomeRecord() {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            ActionOutcomeRecord record = actionOutcomeRecord(resultSet);
            if (resultSet.next()) {
                throw new ActionOutcomeAuthorityUnavailableException();
            }
            return Optional.of(record);
        };
    }

    static ResultSetExtractor<Optional<CompletedActionOutcomeOperation>>
            completedOperation() {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            ActionOutcomeOperationId operationId = new ActionOutcomeOperationId(
                    resultSet.getString("operation_id"));
            ActionOutcomeOperationType operationType =
                    ActionOutcomeOperationType.valueOf(
                            resultSet.getString("operation_type"));
            ActionOutcomeSemanticFingerprint fingerprint =
                    new ActionOutcomeSemanticFingerprint(
                            resultSet.getBytes("semantic_fingerprint"));
            ActionOutcomeOperationOutcome outcome =
                    ActionOutcomeOperationOutcome.valueOf(
                            resultSet.getString("outcome"));
            java.time.Instant occurredAt =
                    resultSet.getTimestamp("occurred_at").toInstant();
            ActionOutcomeRecord record = actionOutcomeRecord(resultSet);
            if (resultSet.next()) {
                throw new ActionOutcomeAuthorityUnavailableException();
            }
            return Optional.of(new CompletedActionOutcomeOperation(
                    operationId, operationType, fingerprint,
                    record.actionOutcomeRef(), outcome, occurredAt, record));
        };
    }

    private static ActionOutcomeRecord actionOutcomeRecord(ResultSet resultSet)
            throws SQLException {
        return new ActionOutcomeRecord(
                new ActionOutcomeRef(resultSet.getString("action_outcome_ref")),
                new ActionRef(resultSet.getString("action_ref")),
                new OutcomeText(decodeUtf8(resultSet.getBytes("outcome_text"))),
                ActionOutcomeSource.valueOf(resultSet.getString("source")),
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
                    "stored action outcome content is not valid UTF-8", exception);
        }
    }
}
