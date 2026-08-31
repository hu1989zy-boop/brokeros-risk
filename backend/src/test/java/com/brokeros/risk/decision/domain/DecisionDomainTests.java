package com.brokeros.risk.decision.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import com.brokeros.risk.decision.application.DecisionFingerprintFactory;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class DecisionDomainTests {

    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";
    private static final String UUID_THREE = "00000000-0000-4000-8000-000000000003";

    @Test
    void decisionRefAndOperationIdRequireCanonicalLowercaseUuidV4() {
        assertThat(new DecisionRef("dec-" + UUID_ONE).value())
                .isEqualTo("dec-" + UUID_ONE);
        assertThat(new DecisionOperationId(UUID_ONE).value()).isEqualTo(UUID_ONE);

        assertThatThrownBy(() -> new DecisionRef(UUID_ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionRef(
                "dec-00000000-0000-1000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionOperationId(
                "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conclusionUsesUtf8ByteBoundsAndRejectsBlankNulAndControls() {
        assertThat(new ConclusionText("a".repeat(4000)).value()).hasSize(4000);

        assertThatThrownBy(() -> new ConclusionText(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConclusionText("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConclusionText("a".repeat(4001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConclusionText("😀".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConclusionText("contains\u0000nul"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConclusionText("contains\ncontrol"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decisionRequiresEvidenceAndPreservesAnImmutableDistinctSet() {
        EvidenceRef first = new EvidenceRef("ev-" + UUID_TWO);
        DecisionRecord record = record(Set.of(first));

        assertThat(record.evidenceRefs()).containsExactly(first);
        assertThatThrownBy(() -> record(Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> record.evidenceRefs().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void provenanceFactoriesEnforceCompleteRecognizedAndEmptyNotFoundShapes() {
        DecisionRecord record = record(Set.of(new EvidenceRef("ev-" + UUID_TWO)));
        DecisionProvenanceView recognized = DecisionProvenanceView.recognized(record);
        DecisionProvenanceView missing = DecisionProvenanceView.notFound(record.decisionRef());

        assertThat(recognized.outcome()).isEqualTo(DecisionProvenanceOutcome.RECOGNIZED);
        assertThat(recognized.evidenceRefs()).isEqualTo(record.evidenceRefs());
        assertThat(missing.outcome()).isEqualTo(DecisionProvenanceOutcome.NOT_FOUND);
        assertThat(missing.subjectRef()).isNull();
        assertThatThrownBy(() -> new DecisionProvenanceView(
                record.decisionRef(), DecisionProvenanceOutcome.RECOGNIZED,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fingerprintsMatchGoldenVectorDeduplicateSortAndChangeWithEveryField() {
        DecisionFingerprintFactory factory = new DecisionFingerprintFactory();
        DecisionSemanticFingerprint fingerprint = factory.forRecord(
                "ta-" + UUID_ONE,
                List.of("ev-" + UUID_THREE, "ev-" + UUID_TWO, "ev-" + UUID_TWO),
                "conclusion");

        assertThat(HexFormat.of().formatHex(fingerprint.value())).isEqualTo(
                "9f350ab090a26025e06e4de780e3b7fdfe2d967ab7c7ffff5e246ebac9f37e97");
        assertThat(factory.forRecord(
                "ta-" + UUID_ONE,
                List.of("ev-" + UUID_TWO, "ev-" + UUID_THREE),
                "conclusion")).isEqualTo(fingerprint);
        assertThat(factory.forRecord(
                "ta-" + UUID_TWO,
                List.of("ev-" + UUID_TWO, "ev-" + UUID_THREE),
                "conclusion")).isNotEqualTo(fingerprint);
        assertThat(factory.forRecord(
                "ta-" + UUID_ONE,
                List.of("ev-" + UUID_TWO),
                "conclusion")).isNotEqualTo(fingerprint);
        assertThat(factory.forRecord(
                "ta-" + UUID_ONE,
                List.of("ev-" + UUID_TWO, "ev-" + UUID_THREE),
                "changed")).isNotEqualTo(fingerprint);
    }

    private DecisionRecord record(Set<EvidenceRef> evidenceRefs) {
        return new DecisionRecord(
                new DecisionRef("dec-" + UUID_ONE),
                new TradingAccountRef("ta-" + UUID_ONE),
                evidenceRefs,
                new ConclusionText("conclusion"),
                DecisionSource.MANUAL,
                new ActorRef(UUID_ONE),
                Instant.parse("2026-08-31T01:00:00Z"));
    }
}
