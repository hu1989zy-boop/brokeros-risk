package com.brokeros.risk.tradingaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import com.brokeros.risk.tradingaccount.application.AuthorityOperationRequest;
import com.brokeros.risk.tradingaccount.application.ManifestFingerprintFactory;
import org.junit.jupiter.api.Test;

class TradingAccountDomainTests {

    private static final String UUID = "00000000-0000-4000-8000-000000000001";

    @Test
    void canonicalReferencesAcceptOnlyLowercaseUuidV4WithExactPrefix() {
        assertThat(new TradingAccountRef("ta-" + UUID).value()).isEqualTo("ta-" + UUID);
        assertThat(new AccountAuthorityScopeRef("aas-" + UUID).value()).isEqualTo("aas-" + UUID);
        assertThat(new AuthorityOperationId(UUID).value()).isEqualTo(UUID);

        assertThatThrownBy(() -> new TradingAccountRef("aas-" + UUID)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TradingAccountRef(
                "ta-AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TradingAccountRef("ta-00000000-0000-1000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthorityOperationId(" " + UUID)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sourceNamespaceIsExactAndBounded() {
        SourceNamespace namespace = new SourceNamespace(
                "platform-family", "source-instance-1", "server.1_a", "production");
        assertThat(namespace.sourceServer()).isEqualTo("server.1_a");
        assertThatThrownBy(() -> new SourceNamespace("MT5", "instance", "server", "prod"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SourceNamespace("family", "instance", "server ", "prod"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SourceNamespace("a".repeat(64), "instance", "server", "prod"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void externalAccountKeyPreservesExactUnicodeAndRedactsItself() {
        ExternalAccountKey key = new ExternalAccountKey("001 AbC");
        assertThat(key.value()).isEqualTo("001 AbC");
        assertThat(key.toString()).doesNotContain("001", "AbC");
        byte[] first = key.utf8Bytes();
        first[0] = 0;
        assertThat(key.utf8Bytes()[0]).isNotZero();

        String composed = "é";
        String decomposed = "e\u0301";
        assertThat(new ExternalAccountKey(composed)).isNotEqualTo(new ExternalAccountKey(decomposed));
        assertThat(new ExternalAccountKey("😀".repeat(128)).utf8Bytes()).hasSize(512);
        assertThatThrownBy(() -> new ExternalAccountKey("😀".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalAccountKey("\u00a0edge"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalAccountKey("bad\u0000key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalAccountKey("bad\ud800"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lifecycleAllowsOnlyApprovedTransitionsAndRetirementIsTerminal() {
        assertThat(AuthorityLifecycle.ACTIVE.transitionTo(AuthorityLifecycle.INACTIVE))
                .isEqualTo(AuthorityLifecycle.INACTIVE);
        assertThat(AuthorityLifecycle.INACTIVE.transitionTo(AuthorityLifecycle.ACTIVE))
                .isEqualTo(AuthorityLifecycle.ACTIVE);
        assertThat(AuthorityLifecycle.ACTIVE.transitionTo(AuthorityLifecycle.RETIRED))
                .isEqualTo(AuthorityLifecycle.RETIRED);
        assertThatThrownBy(() -> AuthorityLifecycle.ACTIVE.transitionTo(AuthorityLifecycle.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AuthorityLifecycle.RETIRED.transitionTo(AuthorityLifecycle.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void operationFieldMatrixAndEligibilityEvidenceAreClosed() {
        assertThatThrownBy(() -> new AuthorityOperationRequest(
                1, new AuthorityOperationId(UUID), AuthorityOperationType.REGISTER_AUTHORITY_SCOPE,
                new AccountAuthorityScopeRef("aas-" + UUID), null, null, null, null,
                attestation(), new ChangeReason("reason"), new ChangeReference("change-1")))
                .isInstanceOf(IllegalArgumentException.class);

        TradingAccountRef ref = new TradingAccountRef("ta-" + UUID);
        assertThat(new TradingAccountReferenceEligibility(
                ref, EligibilityDecision.NOT_RECOGNIZED, null, null).decision())
                .isEqualTo(EligibilityDecision.NOT_RECOGNIZED);
        assertThatThrownBy(() -> new TradingAccountReferenceEligibility(
                ref, EligibilityDecision.ELIGIBLE_FOR_NEW_ASSOCIATION, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void semanticFingerprintIsStableAndSensitiveToEverySemanticField() {
        ManifestFingerprintFactory factory = new ManifestFingerprintFactory();
        AuthorityOperationRequest first = accountRegistration(UUID, "ExactKey");
        AuthorityOperationRequest differentOperationId = accountRegistration(
                "00000000-0000-4000-8000-000000000002", "ExactKey");
        AuthorityOperationRequest differentKey = accountRegistration(UUID, "exactkey");

        assertThat(factory.create(first)).isEqualTo(factory.create(differentOperationId));
        assertThat(factory.create(first)).isNotEqualTo(factory.create(differentKey));
        assertThat(factory.create(first).value()).hasSize(32);
        assertThat(Arrays.toString(factory.create(first).value()))
                .isEqualTo("[-108, -3, -51, -57, -61, 64, 99, -106, 34, -86, -44, 14, -56, -62, 86, -103, 96, 88, 66, -11, 89, -46, -59, -51, -111, -18, 106, 76, -80, -13, 53, -60]");
    }

    private AuthorityOperationRequest accountRegistration(String operationId, String key) {
        return new AuthorityOperationRequest(
                1,
                new AuthorityOperationId(operationId),
                AuthorityOperationType.REGISTER_TRADING_ACCOUNT,
                new AccountAuthorityScopeRef("aas-" + UUID),
                null,
                new SourceNamespace("platform", "instance", "server-1", "production"),
                new ExternalAccountKey(key),
                null,
                attestation(),
                new ChangeReason("Initial registration"),
                new ChangeReference("change-1"));
    }

    private AttestationReference attestation() {
        return new AttestationReference("broker-record", "approval-1");
    }
}
