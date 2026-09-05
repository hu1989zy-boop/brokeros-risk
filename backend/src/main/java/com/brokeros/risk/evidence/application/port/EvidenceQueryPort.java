package com.brokeros.risk.evidence.application.port;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.evidence.application.CompletedEvidenceOperation;
import com.brokeros.risk.evidence.application.EvidenceReferenceSummary;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public interface EvidenceQueryPort {

    Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id);

    Optional<EvidenceRecord> findByRef(EvidenceRef ref);

    List<EvidenceReferenceSummary> findSummariesBySubject(
            TradingAccountRef subjectRef,
            int limit);
}
