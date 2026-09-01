package com.brokeros.risk.actionoutcome.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.HexFormat;
import java.util.stream.Stream;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeFingerprintFactory;
import com.brokeros.risk.security.domain.ActorRef;
import org.junit.jupiter.api.Test;

class ActionOutcomeDomainTests {

    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";

    @Test
    void refsAndOperationIdRequireCanonicalLowercaseUuidV4() {
        assertThat(new ActionOutcomeRef("aoc-" + UUID_ONE).value())
                .isEqualTo("aoc-" + UUID_ONE);
        assertThat(new ActionOutcomeOperationId(UUID_ONE).value())
                .isEqualTo(UUID_ONE);

        assertThatThrownBy(() -> new ActionOutcomeRef(UUID_ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionOutcomeRef(
                "aoc-00000000-0000-1000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionOutcomeOperationId(
                "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outcomeTextUsesUtf8ByteBoundsAndRejectsBlankNulAndControls() {
        assertThat(new OutcomeText("a".repeat(4000)).value()).hasSize(4000);

        assertThatThrownBy(() -> new OutcomeText(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutcomeText("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutcomeText("a".repeat(4001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutcomeText("😀".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutcomeText("contains" + (char) 0 + "nul"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutcomeText(
                "contains" + System.lineSeparator() + "control"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordIsManualImmutableAndReferencesExactlyOneActionWithoutStatus() {
        ActionOutcomeRecord record = record();

        assertThat(record.actionRef()).isEqualTo(new ActionRef("act-" + UUID_TWO));
        assertThat(record.source()).isEqualTo(ActionOutcomeSource.MANUAL);
        assertThat(ActionOutcomeSource.values())
                .containsExactly(ActionOutcomeSource.MANUAL);
        assertThat(Stream.of(ActionOutcomeRecord.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("status", "result", "classification");
    }

    @Test
    void provenanceFactoriesEnforceCompleteRecognizedAndEmptyNotFoundShapes() {
        ActionOutcomeRecord record = record();
        ActionOutcomeProvenanceView recognized =
                ActionOutcomeProvenanceView.recognized(record);
        ActionOutcomeProvenanceView missing =
                ActionOutcomeProvenanceView.notFound(record.actionOutcomeRef());

        assertThat(recognized.outcome())
                .isEqualTo(ActionOutcomeProvenanceOutcome.RECOGNIZED);
        assertThat(recognized.actionRef()).isEqualTo(record.actionRef());
        assertThat(missing.outcome())
                .isEqualTo(ActionOutcomeProvenanceOutcome.NOT_FOUND);
        assertThat(missing.actionRef()).isNull();
        assertThatThrownBy(() -> new ActionOutcomeProvenanceView(
                record.actionOutcomeRef(),
                ActionOutcomeProvenanceOutcome.RECOGNIZED,
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fingerprintsMatchGoldenVectorAndChangeWithEachRawField() {
        ActionOutcomeFingerprintFactory factory =
                new ActionOutcomeFingerprintFactory();
        ActionOutcomeSemanticFingerprint fingerprint = factory.forRecord(
                "act-" + UUID_ONE, "outcome");

        assertThat(HexFormat.of().formatHex(fingerprint.value())).isEqualTo(
                "bf1ea7481ba370857f3557a5b9c60bc6a710d8cd809656e66ec13212507641ed");
        assertThat(factory.forRecord("act-" + UUID_ONE, "outcome"))
                .isEqualTo(fingerprint);
        assertThat(factory.forRecord("act-" + UUID_TWO, "outcome"))
                .isNotEqualTo(fingerprint);
        assertThat(factory.forRecord("act-" + UUID_ONE, "changed"))
                .isNotEqualTo(fingerprint);
    }

    private ActionOutcomeRecord record() {
        return new ActionOutcomeRecord(
                new ActionOutcomeRef("aoc-" + UUID_ONE),
                new ActionRef("act-" + UUID_TWO),
                new OutcomeText("observed outcome"),
                ActionOutcomeSource.MANUAL,
                new ActorRef(UUID_ONE),
                Instant.parse("2026-09-01T01:00:00Z"));
    }
}
