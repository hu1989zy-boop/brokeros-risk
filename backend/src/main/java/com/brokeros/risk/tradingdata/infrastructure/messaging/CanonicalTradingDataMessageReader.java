package com.brokeros.risk.tradingdata.infrastructure.messaging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.brokeros.risk.tradingdata.domain.CanonicalTradingDataEvent;
import com.brokeros.risk.tradingdata.domain.PlatformTag;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public final class CanonicalTradingDataMessageReader {

    public static final int MAX_MESSAGE_BYTES = TradingDataEnvelope.MAX_PAYLOAD_BYTES + 8192;
    private final ObjectMapper mapper = JsonMapper.builder(JsonFactory.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxNestingDepth(32).maxNumberLength(128)
                            .maxStringLength(TradingDataEnvelope.MAX_PAYLOAD_BYTES).build())
                    .build())
            .addModule(new JavaTimeModule())
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    public TradingDataEnvelope read(String key, byte[] message) {
        try {
            if (message == null || message.length == 0 || message.length > MAX_MESSAGE_BYTES) {
                throw new InvalidCanonicalMessageException();
            }
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(message));
            for (byte value : message) {
                // Reject raw NUL/UTF-16/UTF-32; JSON's escaped null character remains legal.
                if (value == 0) {
                    throw new InvalidCanonicalMessageException();
                }
            }
            JsonNode root = mapper.readTree(message);
            if (!root.isObject() || integer(root, "envelopeVersion") != 1
                    || !root.path("payload").isObject()) {
                throw new InvalidCanonicalMessageException();
            }
            JsonNode sequence = root.path("sourceSequence");
            if (!sequence.isIntegralNumber() || !sequence.canConvertToLong()) {
                throw new InvalidCanonicalMessageException();
            }
            byte[] payload = payloadBytes(message);
            CanonicalTradingDataEvent event = mapper.readValue(payload, CanonicalTradingDataEvent.class);
            Instant occurredAt = utc(root, "occurredAt");
            Instant canonicalTime = utc(root.path("payload"), "occurredAt");
            TradingDataEnvelope envelope = new TradingDataEnvelope(
                    1, PlatformTag.valueOf(text(root, "platform")),
                    text(root, "sourceServerId"), sequence.longValue(),
                    text(root, "tradingAccountId"), occurredAt, payload);
            if (!envelope.tradingAccountId().equals(key)
                    || !envelope.tradingAccountId().equals(event.accountRef())
                    || envelope.platform() != event.platform()
                    || !envelope.sourceServerId().equals(event.sourceServerId())
                    || !occurredAt.equals(canonicalTime)) {
                throw new InvalidCanonicalMessageException();
            }
            return envelope;
        } catch (IOException | IllegalArgumentException | NullPointerException exception) {
            // Jackson diagnostics can include source content. Do not retain that cause.
            throw new InvalidCanonicalMessageException();
        }
    }

    private int integer(JsonNode root, String name) {
        JsonNode value = root.path(name);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new InvalidCanonicalMessageException();
        }
        return value.intValue();
    }

    private String text(JsonNode root, String name) {
        JsonNode value = root.path(name);
        if (!value.isTextual()) {
            throw new InvalidCanonicalMessageException();
        }
        return value.textValue();
    }

    private Instant utc(JsonNode root, String name) {
        OffsetDateTime time = OffsetDateTime.parse(text(root, name));
        if (!time.getOffset().equals(ZoneOffset.UTC)) {
            throw new InvalidCanonicalMessageException();
        }
        return time.toInstant();
    }

    private byte[] payloadBytes(byte[] message) throws IOException {
        // Slice the original UTF-8 object, preserving decimals, whitespace and extensions.
        // Never reconstruct storage bytes from the typed projection or a JSON tree.
        try (JsonParser parser = mapper.createParser(message)) {
            parser.nextToken();
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                parser.nextToken();
                long start = parser.currentTokenLocation().getByteOffset();
                parser.skipChildren();
                if ("payload".equals(name)) {
                    return Arrays.copyOfRange(message, Math.toIntExact(start),
                            Math.toIntExact(parser.currentLocation().getByteOffset()));
                }
            }
        }
        throw new InvalidCanonicalMessageException();
    }
}
