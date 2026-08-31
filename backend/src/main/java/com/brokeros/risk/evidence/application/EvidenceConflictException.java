package com.brokeros.risk.evidence.application;

import com.brokeros.risk.api.ResultCode;

public final class EvidenceConflictException extends EvidenceException {

    public EvidenceConflictException(ResultCode resultCode) {
        super(resultCode);
        if (resultCode != ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT
                && resultCode != ResultCode.EVIDENCE_ALREADY_SUPERSEDED) {
            throw new IllegalArgumentException("result code is not a Q-011 conflict");
        }
    }
}
