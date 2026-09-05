package com.brokeros.risk.action.interfaces.rest;

import jakarta.validation.Valid;

import com.brokeros.risk.action.application.ActionDetailReadService;
import com.brokeros.risk.action.application.ActionRecordingService;
import com.brokeros.risk.action.application.ActionReferenceListService;
import com.brokeros.risk.action.application.RecordActionCommand;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ActionController {

    private final ActorContextProvider actorContextProvider;
    private final ActionRecordingService recordingService;
    private final ActionDetailReadService detailReadService;
    private final ActionReferenceListService referenceListService;

    public ActionController(
            ActorContextProvider actorContextProvider,
            ActionRecordingService recordingService,
            ActionDetailReadService detailReadService,
            ActionReferenceListService referenceListService) {
        this.actorContextProvider = actorContextProvider;
        this.recordingService = recordingService;
        this.detailReadService = detailReadService;
        this.referenceListService = referenceListService;
    }

    @PostMapping
    @Operation(summary = "Record an action")
    public ResponseEntity<ApiResponse<ActionRecordedResponse>> record(
            @Valid @RequestBody RecordActionRequest request) {
        ActionRecordedResponse response = ActionRecordedResponse.from(
                recordingService.record(
                        actorContextProvider.currentContext(),
                        new RecordActionCommand(
                                request.operationId(),
                                request.decisionRef(),
                                request.intentText())));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(params = "decisionRef")
    @Operation(summary = "List action references by decision")
    public ResponseEntity<ApiResponse<ActionReferenceListResponse>> list(
            @RequestParam String decisionRef) {
        ActionReferenceListResponse response = ActionReferenceListResponse.from(
                referenceListService.listByDecision(
                        actorContextProvider.currentContext(), decisionRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{actionRef}")
    @Operation(summary = "Read full action detail")
    public ResponseEntity<ApiResponse<ActionDetailResponse>> detail(
            @PathVariable String actionRef) {
        ActionDetailResponse response = ActionDetailResponse.from(
                detailReadService.readDetail(
                        actorContextProvider.currentContext(), actionRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
