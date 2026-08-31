package com.brokeros.risk.evidence.application;

import com.brokeros.risk.api.ResultCode;

public final class EvidenceAuthorityUnavailableException extends EvidenceException {

    public EvidenceAuthorityUnavailableException() {
        super(ResultCode.EVIDENCE_AUTHORITY_UNAVAILABLE);
    }

    public EvidenceAuthorityUnavailableException(Throwable cause) {
        super(ResultCode.EVIDENCE_AUTHORITY_UNAVAILABLE, cause);
    }
}
