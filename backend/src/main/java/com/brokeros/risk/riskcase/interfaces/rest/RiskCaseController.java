package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.riskcase.application.CreateRiskCaseCommand;
import com.brokeros.risk.riskcase.application.RiskCaseAssociationService;
import com.brokeros.risk.riskcase.application.RiskCaseCommandService;
import com.brokeros.risk.riskcase.application.RiskCaseCreationService;
import com.brokeros.risk.riskcase.application.RiskCaseQueryService;
import com.brokeros.risk.riskcase.application.RiskCaseResolutionService;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/risk-cases")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RiskCaseController {

    private final ActorContextProvider actorContextProvider;
    private final RiskCaseCreationService creationService;
    private final RiskCaseCommandService commandService;
    private final RiskCaseAssociationService associationService;
    private final RiskCaseResolutionService resolutionService;
    private final RiskCaseQueryService queryService;

    public RiskCaseController(
            ActorContextProvider actorContextProvider,
            RiskCaseCreationService creationService,
            RiskCaseCommandService commandService,
            RiskCaseAssociationService associationService,
            RiskCaseResolutionService resolutionService,
            RiskCaseQueryService queryService) {
        this.actorContextProvider = actorContextProvider;
        this.creationService = creationService;
        this.commandService = commandService;
        this.associationService = associationService;
        this.resolutionService = resolutionService;
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RiskCaseListResponse>> list(
            @RequestParam(required = false)
            @Pattern(regexp = "OPEN|IN_REVIEW|ACTION_REQUIRED|RESOLVED|CLOSED|CANCELLED")
            String status,
            @RequestParam(required = false)
            @Pattern(regexp = "LOW|NORMAL|HIGH|CRITICAL")
            String priority,
            @RequestParam(required = false) String subjectRef,
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ok(RiskCaseListResponse.from(queryService.listCases(
                actorContextProvider.currentContext(), status, priority,
                subjectRef, assignee, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRiskCaseRequest request) {
        RiskCaseDetailResponse response = RiskCaseDetailResponse.from(
                creationService.create(actorContextProvider.currentContext(),
                        new CreateRiskCaseCommand(
                                request.intakeSource(), request.subjectType(), request.subjectRef(),
                                request.intakeSummary(), request.priority(), request.decisionRef(),
                                idempotencyKey)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{caseNumber}")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> detail(
            @PathVariable String caseNumber) {
        return ok(RiskCaseDetailResponse.from(queryService.detail(
                actorContextProvider.currentContext(), caseNumber)));
    }

    @GetMapping("/{caseNumber}/history")
    public ResponseEntity<ApiResponse<RiskCaseHistoryPageResponse>> history(
            @PathVariable String caseNumber,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return ok(RiskCaseHistoryPageResponse.from(queryService.history(
                actorContextProvider.currentContext(), caseNumber, cursor, limit)));
    }

    @PostMapping("/{caseNumber}/assignments")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> assignment(
            @PathVariable String caseNumber,
            @Valid @RequestBody ChangeRiskCaseAssignmentRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.changeAssignment(
                actorContextProvider.currentContext(), caseNumber, request.assigneeRef(),
                request.reason(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/review-start")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> beginReview(
            @PathVariable String caseNumber,
            @Valid @RequestBody BeginRiskCaseReviewRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.beginReview(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/evidence-associations")
    public ResponseEntity<ApiResponse<RiskCaseEvidenceAssociationResponse>> associateEvidence(
            @PathVariable String caseNumber,
            @Valid @RequestBody AssociateRiskCaseEvidenceRequest request) {
        return created(RiskCaseEvidenceAssociationResponse.from(
                associationService.associateEvidence(actorContextProvider.currentContext(),
                        caseNumber, request.evidenceRef(), request.reason(), request.source(),
                        request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/evidence-associations/{associationEventRef}/dispositions")
    public ResponseEntity<ApiResponse<RiskCaseEvidenceAssociationResponse>> evidenceDisposition(
            @PathVariable String caseNumber,
            @PathVariable String associationEventRef,
            @Valid @RequestBody ChangeEvidenceAssociationDispositionRequest request) {
        return created(RiskCaseEvidenceAssociationResponse.from(
                associationService.changeEvidenceDisposition(
                        actorContextProvider.currentContext(), caseNumber, associationEventRef,
                        request.disposition(), request.replacementEvidenceRef(), request.reason(),
                        request.source(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/decision-associations")
    public ResponseEntity<ApiResponse<RiskCaseDecisionAssociationResponse>> associateDecision(
            @PathVariable String caseNumber,
            @Valid @RequestBody AssociateRiskCaseDecisionRequest request) {
        return created(RiskCaseDecisionAssociationResponse.from(
                associationService.associateDecision(actorContextProvider.currentContext(),
                        caseNumber, request.decisionRef(), request.reason(),
                        request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/decision-selection")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> selectDecision(
            @PathVariable String caseNumber,
            @Valid @RequestBody SelectRiskCaseDecisionRequest request) {
        return ok(RiskCaseDetailResponse.from(associationService.selectCurrentDecision(
                actorContextProvider.currentContext(), caseNumber, request.decisionRef(),
                request.reason(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/action-associations")
    public ResponseEntity<ApiResponse<RiskCaseActionAssociationResponse>> associateAction(
            @PathVariable String caseNumber,
            @Valid @RequestBody AssociateRiskCaseActionRequest request) {
        return created(RiskCaseActionAssociationResponse.from(
                associationService.associateAction(actorContextProvider.currentContext(),
                        caseNumber, request.actionRef(), request.reason(),
                        request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/action-associations/{actionRef}/outcomes")
    public ResponseEntity<ApiResponse<RiskCaseActionAssociationResponse>> referenceOutcome(
            @PathVariable String caseNumber,
            @PathVariable String actionRef,
            @Valid @RequestBody ReferenceActionOutcomeRequest request) {
        return created(RiskCaseActionAssociationResponse.from(
                associationService.recordActionOutcomeReference(
                        actorContextProvider.currentContext(), caseNumber, actionRef,
                        request.outcomeRef(), request.reason(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/action-required")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> markActionRequired(
            @PathVariable String caseNumber,
            @Valid @RequestBody MarkRiskCaseActionRequiredRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.markActionRequired(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/review-return")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> returnToReview(
            @PathVariable String caseNumber,
            @Valid @RequestBody ReturnRiskCaseToReviewRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.returnToReview(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/priority-changes")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> changePriority(
            @PathVariable String caseNumber,
            @Valid @RequestBody ChangeRiskCasePriorityRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.changePriority(
                actorContextProvider.currentContext(), caseNumber, request.priority(),
                request.reason(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/notes")
    public ResponseEntity<ApiResponse<RiskCaseNoteResponse>> addNote(
            @PathVariable String caseNumber,
            @Valid @RequestBody AddRiskCaseNoteRequest request) {
        return created(RiskCaseNoteResponse.from(
                associationService.addNote(actorContextProvider.currentContext(), caseNumber,
                        request.content(), request.expectedVersion()), null));
    }

    @PostMapping("/{caseNumber}/notes/{noteRef}/corrections")
    public ResponseEntity<ApiResponse<RiskCaseNoteResponse>> correctNote(
            @PathVariable String caseNumber,
            @PathVariable String noteRef,
            @Valid @RequestBody CorrectRiskCaseNoteRequest request) {
        return created(RiskCaseNoteResponse.from(
                associationService.correctNote(actorContextProvider.currentContext(), caseNumber,
                        noteRef, request.content(), request.expectedVersion()), noteRef));
    }

    @PostMapping("/{caseNumber}/resolutions")
    public ResponseEntity<ApiResponse<RiskCaseResolutionResponse>> resolve(
            @PathVariable String caseNumber,
            @Valid @RequestBody ResolveRiskCaseRequest request) {
        return created(RiskCaseResolutionResponse.from(resolutionService.resolve(
                actorContextProvider.currentContext(), caseNumber, request.outcome(),
                request.resolutionSummary(), request.evidenceRefs(), request.actionRefs(),
                request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/closure")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> close(
            @PathVariable String caseNumber,
            @Valid @RequestBody CloseRiskCaseRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.close(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/cancellation")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> cancel(
            @PathVariable String caseNumber,
            @Valid @RequestBody CancelRiskCaseRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.cancel(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.duplicateCaseNumber(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/resume")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> resume(
            @PathVariable String caseNumber,
            @Valid @RequestBody ResumeResolvedRiskCaseRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.resumeResolved(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.assigneeRef(), request.expectedVersion())));
    }

    @PostMapping("/{caseNumber}/reopen")
    public ResponseEntity<ApiResponse<RiskCaseDetailResponse>> reopen(
            @PathVariable String caseNumber,
            @Valid @RequestBody ReopenClosedRiskCaseRequest request) {
        return ok(RiskCaseDetailResponse.from(commandService.reopenClosed(
                actorContextProvider.currentContext(), caseNumber, request.reason(),
                request.assigneeRef(), request.expectedVersion())));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T response) {
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private <T> ResponseEntity<ApiResponse<T>> created(T response) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
