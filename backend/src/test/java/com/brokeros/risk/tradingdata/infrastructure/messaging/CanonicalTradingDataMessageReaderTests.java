package com.brokeros.risk.tradingdata.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.brokeros.risk.tradingdata.domain.CanonicalTradingDataEvent;
import com.brokeros.risk.tradingdata.domain.PlatformTag;
import org.junit.jupiter.api.Test;

class CanonicalTradingDataMessageReaderTests {
    private final CanonicalTradingDataMessageReader reader = new CanonicalTradingDataMessageReader();

    @Test
    void everyRecordedGoldenPayloadRoundTripsByteForByte() throws Exception {
        List<String> golden = CanonicalFixtureSupport.golden();
        assertThat(golden).hasSize(12);
        for (int i = 0; i < golden.size(); i++) {
            String payload = golden.get(i);
            var result = reader.read(CanonicalFixtureSupport.account(payload),
                    CanonicalFixtureSupport.envelope(payload, i + 1));
            assertThat(result.payload()).isEqualTo(payload.getBytes(StandardCharsets.UTF_8));
            assertThat(result.sourceSequence()).isEqualTo(i + 1);
            assertThat(result.platform()).isEqualTo(PlatformTag.MT4);
        }
    }

    @Test
    void optionalRefsAndBothNeutralNonTradeKindsAcceptWithoutInventingRefs() throws Exception {
        for (String kind : List.of("ACCOUNT_STATE", "QUOTE")) {
            String payload = CanonicalFixtureSupport.state(kind);
            var result = reader.read(CanonicalFixtureSupport.account(payload),
                    CanonicalFixtureSupport.envelope(payload, 0));
            assertThat(result.payload()).isEqualTo(payload.getBytes(StandardCharsets.UTF_8));
            assertThat(result.platform()).isEqualTo(PlatformTag.MT5);
        }
    }

    @Test
    void decimalLexemesUnicodeWhitespaceAndUnknownExtensionsAreNeverRewritten() throws Exception {
        String payload = CanonicalFixtureSupport.state("QUOTE").replace(" }", """
                , "bid":1.1234567890123456789012345678900,
                  "ask":1.1234567890123456789012345678901,
                  "symbol":"合成-€", "extension":{"a":[1,2,3]} }
                """).strip();
        var result = reader.read(CanonicalFixtureSupport.account(payload),
                CanonicalFixtureSupport.envelope(payload, 1));
        assertThat(result.payload()).isEqualTo(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsMismatchedKeyProvenanceAccountAndTime() throws Exception {
        String payload = CanonicalFixtureSupport.golden().getFirst();
        byte[] message = CanonicalFixtureSupport.envelope(payload, 1);
        invalid(null, message);
        invalid("other-account", message);
        String json = new String(message, StandardCharsets.UTF_8);
        for (String changed : List.of(
                json.replaceFirst("demo-mt4-01", "other-server"),
                json.replaceFirst("MT4", "MT5"),
                json.replaceFirst("900001", "900002"),
                json.replaceFirst("15:57:18Z", "15:57:19Z"))) {
            invalid("900001", changed.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsInvalidJsonTombstonesVersionsDuplicateKeysCoercionAndInvalidEnums() throws Exception {
        String payload = CanonicalFixtureSupport.golden().getFirst();
        String json = new String(CanonicalFixtureSupport.envelope(payload, 1), StandardCharsets.UTF_8);
        invalid("900001", null);
        invalid("900001", json.getBytes(StandardCharsets.UTF_16LE));
        invalid("900001", json.getBytes(StandardCharsets.UTF_16BE));
        for (String value : List.of("", "null", "[]", "{bad", json + "{}",
                json.replace("\"envelopeVersion\":1", "\"envelopeVersion\":2"),
                json.replace("\"canonicalVersion\":1", "\"canonicalVersion\":2"),
                json.replace("\"canonicalVersion\":1", "\"canonicalVersion\":1.5"),
                json.replace("\"sourceSequence\":1", "\"sourceSequence\":1.5"),
                json.replace("\"sourceSequence\":1", "\"sourceSequence\":\"1\""),
                json.replace("\"sourceSequence\":1", "\"sourceSequence\":-1"),
                json.replace("\"sourceSequence\":1", "\"sourceSequence\":9223372036854775808"),
                json.replace("\"side\":\"BUY\"", "\"side\":0"),
                json.replace("\"side\":\"BUY\"", "\"side\":\"UNKNOWN\""),
                json.replace("\"side\":\"BUY\"", "\"side\":\"BUY\",\"side\":\"SELL\""),
                json.replace("\"sourceTime\":1788807447,", ""),
                json.replace("15:57:18Z", "18:57:18+03:00"),
                json.replace("15:57:18Z", "15:57:18"),
                json.replace("\"accountRef\":\"900001\"", "\"accountRef\":900001"),
                json.replace("\"eventKind\":\"TRADE_ACTIVITY\"", "\"eventKind\":\"NEW_KIND\""),
                json.replace("\"payload\":", "\"payloadBase64\":"))) {
            invalid("900001", value.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void enforcesPayloadAndParsingBoundsAndSafeDiagnostics() throws Exception {
        String payload = CanonicalFixtureSupport.state("QUOTE");
        String prefix = payload.substring(0, payload.length() - 1) + ",\"extension\":\"";
        String bounded = prefix + "x".repeat(65535 - prefix.getBytes(StandardCharsets.UTF_8).length - 2) + "\"}";
        assertThat(reader.read(CanonicalFixtureSupport.account(bounded),
                CanonicalFixtureSupport.envelope(bounded, 1)).payload()).hasSize(65535);
        invalid(CanonicalFixtureSupport.account(bounded),
                CanonicalFixtureSupport.envelope(bounded.replaceFirst("xxx", "xxxx"), 1));
        String nested = payload.substring(0, payload.length() - 1)
                + ",\"extension\":" + "[".repeat(33) + "0" + "]".repeat(33) + "}";
        invalid(CanonicalFixtureSupport.account(payload), CanonicalFixtureSupport.envelope(nested, 1));
        invalid("account", new byte[CanonicalTradingDataMessageReader.MAX_MESSAGE_BYTES + 1]);
        invalid("account", "{\"untrusted\":\"sensitive-sentinel".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void neutralSupersetHasOptionalRefsAndRequiresRawCodesForOther() {
        var event = new CanonicalTradingDataEvent(1, PlatformTag.MT5, "server",
                CanonicalTradingDataEvent.EventKind.TRADE_ACTIVITY,
                CanonicalTradingDataEvent.LifecycleHint.EXECUTE, "account", "order",
                "18446744073709551615", null, Instant.EPOCH, null, null,
                CanonicalTradingDataEvent.Side.OTHER, 900,
                CanonicalTradingDataEvent.Reason.OTHER, 901);
        assertThat(event.dealRef()).isEqualTo("18446744073709551615");
        assertThat(event.positionRef()).isNull();
        assertThat(event.toString()).doesNotContain("account", "18446744073709551615");
        assertThatThrownBy(() -> new CanonicalTradingDataEvent(1, PlatformTag.MT5, "server",
                event.eventKind(), event.lifecycleHint(), "account", null, null, null,
                Instant.EPOCH, null, null, event.side(), null, event.reason(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void invalid(String key, byte[] message) {
        assertThatThrownBy(() -> reader.read(key, message))
                .isInstanceOf(InvalidCanonicalMessageException.class)
                .hasCause(null)
                .hasMessageNotContaining("sensitive-sentinel");
    }
}
