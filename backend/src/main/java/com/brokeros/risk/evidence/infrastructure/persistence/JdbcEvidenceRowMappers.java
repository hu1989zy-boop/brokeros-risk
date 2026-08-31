package com.brokeros.risk.evidence.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import com.brokeros.risk.evidence.application.CompletedEvidenceOperation;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.domain.EvidenceFingerprint;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceOperationType;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceSource;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.jdbc.core.RowMapper;

final class JdbcEvidenceRowMappers {

    private JdbcEvidenceRowMappers() {
    }

    static RowMapper<EvidenceRecord> evidenceRecord() {
        return (resultSet, rowNumber) -> new EvidenceRecord(
                new EvidenceRef(resultSet.getString("evidence_ref")),
                new TradingAccountRef(resultSet.getString("subject_ref")),
                EvidenceSource.valueOf(resultSet.getString("source")),
                new ObservationText(decodeUtf8(resultSet.getBytes("observation_text"))),
                EvidenceStatus.valueOf(resultSet.getString("status")),
                new ActorRef(resultSet.getString("recorded_by_actor_ref")),
                resultSet.getTimestamp("recorded_at").toInstant(),
                optionalRef(resultSet.getString("supersedes_ref")),
                optionalRef(resultSet.getString("superseded_by_ref")));
    }

    static RowMapper<CompletedEvidenceOperation> completedOperation() {
        return (resultSet, rowNumber) -> {
            String resultRef = resultSet.getString("result_evidence_ref");
            if (resultRef == null) {
                throw new EvidenceAuthorityUnavailableException();
            }
            return new CompletedEvidenceOperation(
                    EvidenceOperationType.valueOf(resultSet.getString("operation_type")),
                    new EvidenceFingerprint(resultSet.getBytes("semantic_fingerprint")),
                    new EvidenceRef(resultRef),
                    EvidenceOperationOutcome.valueOf(resultSet.getString("outcome")),
                    resultSet.getTimestamp("occurred_at").toInstant());
        };
    }

    private static EvidenceRef optionalRef(String value) {
        return value == null ? null : new EvidenceRef(value);
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("stored evidence content is not valid UTF-8", exception);
        }
    }
}
