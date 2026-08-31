package com.brokeros.risk.evidence.application.port;

import java.util.Optional;

import com.brokeros.risk.evidence.application.CompletedEvidenceOperation;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;

public interface EvidenceQueryPort {

    Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id);

    Optional<EvidenceRecord> findByRef(EvidenceRef ref);
}
