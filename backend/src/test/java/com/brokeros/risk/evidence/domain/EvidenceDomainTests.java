package com.brokeros.risk.evidence.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HexFormat;

import com.brokeros.risk.evidence.application.EvidenceFingerprintFactory;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class EvidenceDomainTests {

    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";

    @Test
    void evidenceRefAndOperationIdRequireCanonicalLowercaseUuidV4() {
        assertThat(new EvidenceRef("ev-" + UUID_ONE).value()).isEqualTo("ev-" + UUID_ONE);
        assertThat(new EvidenceOperationId(UUID_ONE).value()).isEqualTo(UUID_ONE);

        assertThatThrownBy(() -> new EvidenceRef(UUID_ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvidenceRef("ev-00000000-0000-1000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvidenceOperationId(
                "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contentUsesUtf8ByteBoundsAndRejectsBlankNulAndControls() {
        assertThat(new ObservationText("a".repeat(4000)).value()).hasSize(4000);
        assertThat(new CorrectionReason("a".repeat(1000)).value()).hasSize(1000);

        assertThatThrownBy(() -> new ObservationText(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ObservationText("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ObservationText("a".repeat(4001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ObservationText("😀".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorrectionReason("a".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ObservationText("contains\u0000nul"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorrectionReason("contains\ncontrol"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evidenceCanOnlyTransitionFromActiveToSupersededOnce() {
        EvidenceRecord active = activeRecord("ev-" + UUID_ONE);
        EvidenceRecord superseded = active.supersededBy(new EvidenceRef("ev-" + UUID_TWO));

        assertThat(superseded.status()).isEqualTo(EvidenceStatus.SUPERSEDED);
        assertThat(superseded.supersededByRef().value()).isEqualTo("ev-" + UUID_TWO);
        assertThatThrownBy(() -> superseded.supersededBy(new EvidenceRef("ev-" + UUID_ONE)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EvidenceRecord(
                active.evidenceRef(), active.subjectRef(), active.source(),
                active.observationText(), EvidenceStatus.SUPERSEDED,
                active.recordedByActorRef(), active.recordedAt(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fingerprintsMatchGoldenVectorsAndChangeWithEverySemanticField() {
        EvidenceFingerprintFactory factory = new EvidenceFingerprintFactory();
        EvidenceFingerprint record = factory.forRecord(
                "ta-" + UUID_ONE, "observation");
        EvidenceFingerprint correction = factory.forCorrection(
                "ev-" + UUID_TWO, "reason", "replacement");

        assertThat(HexFormat.of().formatHex(record.value())).isEqualTo(
                "06256a4ac361f41f938bc870a71eba56ac4aeb71ba58d9e7cb5d2409bd7fcda8");
        assertThat(HexFormat.of().formatHex(correction.value())).isEqualTo(
                "f863098bff3743aefbe61ad3069640f670c738829dcbb87251d844a1915652d2");
        assertThat(factory.forRecord("ta-" + UUID_TWO, "observation")).isNotEqualTo(record);
        assertThat(factory.forRecord("ta-" + UUID_ONE, "changed")).isNotEqualTo(record);
        assertThat(factory.forCorrection("ev-" + UUID_TWO, "changed", "replacement"))
                .isNotEqualTo(correction);
        assertThat(factory.forCorrection("ev-" + UUID_TWO, "reason", "changed"))
                .isNotEqualTo(correction);
    }

    private EvidenceRecord activeRecord(String ref) {
        return new EvidenceRecord(
                new EvidenceRef(ref), new TradingAccountRef("ta-" + UUID_ONE),
                EvidenceSource.MANUAL, new ObservationText("observation"),
                EvidenceStatus.ACTIVE, new ActorRef(UUID_ONE),
                Instant.parse("2026-08-29T01:00:00Z"), null, null);
    }
}
