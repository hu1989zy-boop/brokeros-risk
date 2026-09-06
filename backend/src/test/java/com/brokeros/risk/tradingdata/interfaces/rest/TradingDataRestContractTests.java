package com.brokeros.risk.tradingdata.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;
import com.brokeros.risk.tradingdata.application.IngestTradingDataCommand;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class TradingDataRestContractTests {

    @Test
    void controllerExposesOnlyTheApprovedIngestionRoute() throws Exception {
        RequestMapping root = TradingDataIngestionController.class
                .getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/api/trading-data");
        assertThat(TradingDataIngestionController.class
                .getMethod("ingest", IngestTradingDataRequest.class)
                .getAnnotation(PostMapping.class).value())
                .containsExactly("/ingest");
        assertThat(TradingDataIngestionController.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .containsExactly("ingest");
    }

    @Test
    void requestAndResponseExposeOnlyVersionedOpaqueEnvelopeContract() {
        assertThat(componentNames(IngestTradingDataRequest.class)).containsExactly(
                "envelopeVersion", "platform", "sourceServerId", "sourceSequence",
                "tradingAccountId", "occurredAt", "payloadBase64");
        assertThat(componentNames(TradingDataIngestionResponse.class))
                .containsExactly("outcome", "gapDetected");
    }

    @Test
    void beanValidationCoversEnvelopeMetadata() {
        assertThat(component("envelopeVersion").getAccessor()
                .isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(component("envelopeVersion").getAccessor()
                .getAnnotation(Min.class).value()).isEqualTo(1);
        assertThat(component("platform").getAccessor()
                .isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(component("sourceServerId").getAccessor()
                .isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(component("sourceServerId").getAccessor()
                .getAnnotation(Size.class).max()).isEqualTo(128);
        assertThat(component("sourceSequence").getAccessor()
                .isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(component("sourceSequence").getAccessor()
                .getAnnotation(Min.class).value()).isZero();
        assertThat(component("tradingAccountId").getAccessor()
                .isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(component("occurredAt").getAccessor()
                .isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(component("payloadBase64").getAccessor()
                .getAnnotation(Size.class).max()).isEqualTo(87_380);
    }

    @Test
    void base64IsDecodedToOpaqueBytesAndMalformedEncodingUsesModuleCode() {
        IngestTradingDataCommand command = request("/wDDKA==").toCommand();
        assertThat(command.payload()).containsExactly(
                (byte) 0xff, 0x00, (byte) 0xc3, 0x28);

        assertThatThrownBy(() -> request("not base64!").toCommand())
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_DATA_REQUEST_INVALID));
    }

    @Test
    void resultCodesExposeApprovedHttpSemantics() {
        assertThat(ResultCode.TRADING_DATA_REQUEST_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.TRADING_DATA_ACTOR_TYPE_NOT_PERMITTED.httpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ResultCode.TRADING_DATA_BACKPRESSURE.httpStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ResultCode.TRADING_DATA_AUTHORITY_UNAVAILABLE.httpStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private IngestTradingDataRequest request(String payloadBase64) {
        return new IngestTradingDataRequest(
                1, "MT5", "server-1", 1L, "account-1",
                Instant.parse("2026-09-06T00:00:00Z"), payloadBase64);
    }

    private java.util.List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName).toList();
    }

    private RecordComponent component(String name) {
        return Arrays.stream(IngestTradingDataRequest.class.getRecordComponents())
                .filter(component -> component.getName().equals(name))
                .findFirst().orElseThrow();
    }
}
