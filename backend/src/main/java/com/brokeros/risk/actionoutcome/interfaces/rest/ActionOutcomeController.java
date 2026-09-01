package com.brokeros.risk.actionoutcome.interfaces.rest;

import jakarta.validation.Valid;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeDetailReadService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeRecordingService;
import com.brokeros.risk.actionoutcome.application.RecordActionOutcomeCommand;
import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/action-outcomes")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ActionOutcomeController {

    private final ActorContextProvider actorContextProvider;
    private final ActionOutcomeRecordingService recordingService;
    private final ActionOutcomeDetailReadService detailReadService;

    public ActionOutcomeController(
            ActorContextProvider actorContextProvider,
            ActionOutcomeRecordingService recordingService,
            ActionOutcomeDetailReadService detailReadService) {
        this.actorContextProvider = actorContextProvider;
        this.recordingService = recordingService;
        this.detailReadService = detailReadService;
    }

    @PostMapping
    @Operation(summary = "Record an action outcome")
    public ResponseEntity<ApiResponse<ActionOutcomeRecordedResponse>> record(
            @Valid @RequestBody RecordActionOutcomeRequest request) {
        ActionOutcomeRecordedResponse response = ActionOutcomeRecordedResponse.from(
                recordingService.record(
                        actorContextProvider.currentContext(),
                        new RecordActionOutcomeCommand(
                                request.operationId(),
                                request.actionRef(),
                                request.outcomeText())));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{actionOutcomeRef}")
    @Operation(summary = "Read full action outcome detail")
    public ResponseEntity<ApiResponse<ActionOutcomeDetailResponse>> detail(
            @PathVariable String actionOutcomeRef) {
        ActionOutcomeDetailResponse response = ActionOutcomeDetailResponse.from(
                detailReadService.readDetail(
                        actorContextProvider.currentContext(), actionOutcomeRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
