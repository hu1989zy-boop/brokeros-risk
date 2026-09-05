package com.brokeros.risk.evidence.interfaces.rest;

import jakarta.validation.Valid;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.evidence.application.CorrectEvidenceCommand;
import com.brokeros.risk.evidence.application.EvidenceCorrectionService;
import com.brokeros.risk.evidence.application.EvidenceDetailReadService;
import com.brokeros.risk.evidence.application.EvidenceRecordingService;
import com.brokeros.risk.evidence.application.EvidenceReferenceListService;
import com.brokeros.risk.evidence.application.RecordEvidenceCommand;
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
@RequestMapping("/api/evidence")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EvidenceController {

    private final ActorContextProvider actorContextProvider;
    private final EvidenceRecordingService recordingService;
    private final EvidenceCorrectionService correctionService;
    private final EvidenceDetailReadService detailReadService;
    private final EvidenceReferenceListService referenceListService;

    public EvidenceController(
            ActorContextProvider actorContextProvider,
            EvidenceRecordingService recordingService,
            EvidenceCorrectionService correctionService,
            EvidenceDetailReadService detailReadService,
            EvidenceReferenceListService referenceListService) {
        this.actorContextProvider = actorContextProvider;
        this.recordingService = recordingService;
        this.correctionService = correctionService;
        this.detailReadService = detailReadService;
        this.referenceListService = referenceListService;
    }

    @PostMapping
    @Operation(summary = "Record evidence")
    public ResponseEntity<ApiResponse<EvidenceRecordedResponse>> record(
            @Valid @RequestBody RecordEvidenceRequest request) {
        EvidenceRecordedResponse response = EvidenceRecordedResponse.from(
                recordingService.record(
                        actorContextProvider.currentContext(),
                        new RecordEvidenceCommand(
                                request.operationId(), request.subjectRef(),
                                request.observationText())));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{evidenceRef}/corrections")
    @Operation(summary = "Correct evidence")
    public ResponseEntity<ApiResponse<EvidenceRecordedResponse>> correct(
            @PathVariable String evidenceRef,
            @Valid @RequestBody CorrectEvidenceRequest request) {
        EvidenceRecordedResponse response = EvidenceRecordedResponse.from(
                correctionService.correct(
                        actorContextProvider.currentContext(),
                        new CorrectEvidenceCommand(
                                request.operationId(), evidenceRef,
                                request.correctionReason(), request.observationText())));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(params = "subjectRef")
    @Operation(summary = "List evidence references by subject")
    public ResponseEntity<ApiResponse<EvidenceReferenceListResponse>> list(
            @RequestParam String subjectRef) {
        EvidenceReferenceListResponse response = EvidenceReferenceListResponse.from(
                referenceListService.listBySubject(
                        actorContextProvider.currentContext(), subjectRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{evidenceRef}")
    @Operation(summary = "Read full evidence detail")
    public ResponseEntity<ApiResponse<EvidenceDetailResponse>> detail(
            @PathVariable String evidenceRef) {
        EvidenceDetailResponse response = EvidenceDetailResponse.from(
                detailReadService.read(
                        actorContextProvider.currentContext(), evidenceRef));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
