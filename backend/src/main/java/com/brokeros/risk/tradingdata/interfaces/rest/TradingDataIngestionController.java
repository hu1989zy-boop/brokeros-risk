package com.brokeros.risk.tradingdata.interfaces.rest;

import jakarta.validation.Valid;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import com.brokeros.risk.tradingdata.application.TradingDataIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trading-data")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "brokeros.risk.tradingdata.http-test-aid-enabled", havingValue = "true")
public class TradingDataIngestionController {

    private final ActorContextProvider actorContextProvider;
    private final TradingDataIngestionService ingestionService;

    public TradingDataIngestionController(
            ActorContextProvider actorContextProvider,
            TradingDataIngestionService ingestionService) {
        this.actorContextProvider = actorContextProvider;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Test aid: ingest one legacy opaque envelope", deprecated = true,
            description = "Disabled by default. Production canonical ingestion uses Kafka.")
    public ResponseEntity<ApiResponse<TradingDataIngestionResponse>> ingest(
            @Valid @RequestBody IngestTradingDataRequest request) {
        TradingDataIngestionResponse response = TradingDataIngestionResponse.from(
                ingestionService.ingest(
                        actorContextProvider.currentContext(), request.toCommand()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
