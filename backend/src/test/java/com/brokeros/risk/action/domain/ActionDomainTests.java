package com.brokeros.risk.action.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HexFormat;

import com.brokeros.risk.action.application.ActionFingerprintFactory;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;
import org.junit.jupiter.api.Test;

class ActionDomainTests {

    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";

    @Test
    void actionRefAndOperationIdRequireCanonicalLowercaseUuidV4() {
        assertThat(new ActionRef("act-" + UUID_ONE).value())
                .isEqualTo("act-" + UUID_ONE);
        assertThat(new ActionOperationId(UUID_ONE).value()).isEqualTo(UUID_ONE);

        assertThatThrownBy(() -> new ActionRef(UUID_ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionRef(
                "act-00000000-0000-1000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionOperationId(
                "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void intentUsesUtf8ByteBoundsAndRejectsBlankNulAndControls() {
        assertThat(new IntentText("a".repeat(4000)).value()).hasSize(4000);

        assertThatThrownBy(() -> new IntentText(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntentText("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntentText("a".repeat(4001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntentText("😀".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntentText("contains\u0000nul"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntentText("contains\ncontrol"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actionIsManualProposedAndReferencesExactlyOneDecision() {
        ActionRecord record = record();

        assertThat(record.decisionRef()).isEqualTo(new DecisionRef("dec-" + UUID_TWO));
        assertThat(record.status()).isEqualTo(ActionStatus.PROPOSED);
        assertThat(record.source()).isEqualTo(ActionSource.MANUAL);
        assertThat(ActionStatus.values()).containsExactly(ActionStatus.PROPOSED);
        assertThat(ActionSource.values()).containsExactly(ActionSource.MANUAL);
    }

    @Test
    void provenanceFactoriesEnforceCompleteRecognizedAndEmptyNotFoundShapes() {
        ActionRecord record = record();
        ActionProvenanceView recognized = ActionProvenanceView.recognized(record);
        ActionProvenanceView missing = ActionProvenanceView.notFound(record.actionRef());

        assertThat(recognized.outcome()).isEqualTo(ActionProvenanceOutcome.RECOGNIZED);
        assertThat(recognized.decisionRef()).isEqualTo(record.decisionRef());
        assertThat(recognized.status()).isEqualTo(ActionStatus.PROPOSED);
        assertThat(missing.outcome()).isEqualTo(ActionProvenanceOutcome.NOT_FOUND);
        assertThat(missing.decisionRef()).isNull();
        assertThatThrownBy(() -> new ActionProvenanceView(
                record.actionRef(), ActionProvenanceOutcome.RECOGNIZED,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fingerprintsMatchGoldenVectorAndChangeWithEachRawField() {
        ActionFingerprintFactory factory = new ActionFingerprintFactory();
        ActionSemanticFingerprint fingerprint = factory.forRecord(
                "dec-" + UUID_ONE, "intent");

        assertThat(HexFormat.of().formatHex(fingerprint.value())).isEqualTo(
                "bf5759ba4a62bb88fe2cfa32a280accaa8cd7c15e43fe464a1fdc93f2cb61c95");
        assertThat(factory.forRecord("dec-" + UUID_ONE, "intent"))
                .isEqualTo(fingerprint);
        assertThat(factory.forRecord("dec-" + UUID_TWO, "intent"))
                .isNotEqualTo(fingerprint);
        assertThat(factory.forRecord("dec-" + UUID_ONE, "changed"))
                .isNotEqualTo(fingerprint);
    }

    private ActionRecord record() {
        return new ActionRecord(
                new ActionRef("act-" + UUID_ONE),
                new DecisionRef("dec-" + UUID_TWO),
                new IntentText("intent"),
                ActionStatus.PROPOSED,
                ActionSource.MANUAL,
                new ActorRef(UUID_ONE),
                Instant.parse("2026-09-01T01:00:00Z"));
    }
}
