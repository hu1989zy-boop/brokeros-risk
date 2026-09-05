package com.brokeros.risk.decision.interfaces.rest;

import jakarta.validation.Valid;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.decision.application.DecisionDetailReadService;
import com.brokeros.risk.decision.application.DecisionRecordingService;
import com.brokeros.risk.decision.application.DecisionReferenceListService;
import com.brokeros.risk.decision.application.RecordDecisionCommand;
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
@RequestMapping("/api/decisions")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DecisionController {

    private final ActorContextProvider actorContextProvider;
    private final DecisionRecordingService recordingService;
    private final DecisionDetailReadService detailReadService;
    private final DecisionReferenceListService referenceListService;

    public DecisionController(
            ActorContextProvider actorContextProvider,
            DecisionRecordingService recordingService,
            DecisionDetailReadService detailReadService,
            DecisionReferenceListService referenceListService) {
        this.actorContextProvider = actorContextProvider;
        this.recordingService = recordingService;
        this.detailReadService = detailReadService;
        this.referenceListService = referenceListService;
    }

    @PostMapping
    @Operation(summary = "Record a decision")
    public ResponseEntity<ApiResponse<DecisionRecordedResponse>> record(
            @Valid @RequestBody RecordDecisionRequest request) {
        DecisionRecordedResponse response = DecisionRecordedResponse.from(
                recordingService.record(
                        actorContextProvider.currentContext(),
                        new RecordDecisionCommand(
                                request.operationId(), request.subjectRef(),
                                request.evidenceRefs(), request.conclusionText())));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(params = "subjectRef")
    @Operation(summary = "List decision references by subject")
    public ResponseEntity<ApiResponse<DecisionReferenceListResponse>> list(
            @RequestParam String subjectRef) {
        DecisionReferenceListResponse response = DecisionReferenceListResponse.from(
                referenceListService.listBySubject(
                        actorContextProvider.currentContext(), subjectRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{decisionRef}")
    @Operation(summary = "Read full decision detail")
    public ResponseEntity<ApiResponse<DecisionDetailResponse>> detail(
            @PathVariable String decisionRef) {
        DecisionDetailResponse response = DecisionDetailResponse.from(
                detailReadService.readDetail(
                        actorContextProvider.currentContext(), decisionRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
