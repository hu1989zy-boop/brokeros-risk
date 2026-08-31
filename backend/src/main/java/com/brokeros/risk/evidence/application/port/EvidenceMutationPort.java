package com.brokeros.risk.evidence.application.port;

import com.brokeros.risk.evidence.application.AuthorizedMutationContext;
import com.brokeros.risk.evidence.application.CorrectEvidenceSpec;
import com.brokeros.risk.evidence.application.EvidenceCorrectionResult;
import com.brokeros.risk.evidence.application.EvidenceRecordingResult;
import com.brokeros.risk.evidence.application.RecordEvidenceSpec;

public interface EvidenceMutationPort {

    EvidenceRecordingResult record(
            RecordEvidenceSpec spec,
            AuthorizedMutationContext context);

    EvidenceCorrectionResult correct(
            CorrectEvidenceSpec spec,
            AuthorizedMutationContext context);
}
