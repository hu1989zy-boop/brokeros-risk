package com.brokeros.risk.tradingdata.infrastructure.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class CanonicalFixtureSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CanonicalFixtureSupport() { }

    static List<String> golden() throws IOException {
        try (var input = CanonicalFixtureSupport.class.getResourceAsStream(
                "/q015/mt4-canonical-golden.jsonl")) {
            if (input == null) {
                throw new IOException("Missing Q-015 golden fixture");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    static byte[] envelope(String payload, long sequence) throws IOException {
        JsonNode event = MAPPER.readTree(payload);
        return ("{\"envelopeVersion\":1,\"platform\":" + event.get("platform")
                + ",\"sourceServerId\":" + event.get("sourceServerId")
                + ",\"sourceSequence\":" + sequence
                + ",\"tradingAccountId\":" + event.get("accountRef")
                + ",\"occurredAt\":" + event.get("occurredAt")
                + ",\"payload\":" + payload + "}").getBytes(StandardCharsets.UTF_8);
    }

    static String account(String payload) throws IOException {
        return MAPPER.readTree(payload).get("accountRef").textValue();
    }

    static String state(String kind) {
        return """
                { "canonicalVersion":1,"platform":"MT5","sourceServerId":"synthetic-server",
                  "eventKind":"%s","lifecycleHint":"STATE","accountRef":"18446744073709551615",
                  "occurredAt":"2026-09-07T15:57:18Z" }
                """.formatted(kind).strip();
    }
}
