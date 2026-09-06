package com.brokeros.risk.tradingdata.interfaces.rest;

import java.time.Instant;
import java.util.Base64;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.tradingdata.application.IngestTradingDataCommand;
import com.brokeros.risk.tradingdata.application.TradingDataException;

public record IngestTradingDataRequest(
        @NotNull @Min(1) Integer envelopeVersion,
        @NotBlank @Pattern(regexp = "MT4|MT5") String platform,
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
        String sourceServerId,
        @NotNull @Min(0) Long sourceSequence,
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
        String tradingAccountId,
        @NotNull Instant occurredAt,
        @NotBlank @Size(max = 87_380) String payloadBase64) {

    IngestTradingDataCommand toCommand() {
        try {
            return new IngestTradingDataCommand(
                    envelopeVersion,
                    platform,
                    sourceServerId,
                    sourceSequence,
                    tradingAccountId,
                    occurredAt,
                    Base64.getDecoder().decode(payloadBase64));
        } catch (IllegalArgumentException exception) {
            throw new TradingDataException(
                    ResultCode.TRADING_DATA_REQUEST_INVALID, exception);
        }
    }
}
