package com.brokeros.risk.riskcase.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.riskcase.application.RiskCaseResultCodes;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class RiskCaseRestContractTests {

    @Test
    void controllerExposesExactlyTheApprovedNamedRoutesAndNoDelete() {
        assertThat(RiskCaseController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/risk-cases");
        Set<String> posts = Arrays.stream(RiskCaseController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .flatMap(method -> {
                    String[] values = method.getAnnotation(PostMapping.class).value();
                    return values.length == 0
                            ? java.util.stream.Stream.of("")
                            : Arrays.stream(values);
                })
                .collect(Collectors.toSet());
        Set<String> gets = Arrays.stream(RiskCaseController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .flatMap(method -> {
                    String[] values = method.getAnnotation(GetMapping.class).value();
                    return values.length == 0
                            ? java.util.stream.Stream.of("")
                            : Arrays.stream(values);
                })
                .collect(Collectors.toSet());

        assertThat(posts).containsExactlyInAnyOrder(
                "", "/{caseNumber}/assignments", "/{caseNumber}/review-start",
                "/{caseNumber}/evidence-associations",
                "/{caseNumber}/evidence-associations/{associationEventRef}/dispositions",
                "/{caseNumber}/decision-associations",
                "/{caseNumber}/decision-selection",
                "/{caseNumber}/action-associations",
                "/{caseNumber}/action-associations/{actionRef}/outcomes",
                "/{caseNumber}/action-required", "/{caseNumber}/review-return",
                "/{caseNumber}/priority-changes", "/{caseNumber}/notes",
                "/{caseNumber}/notes/{noteRef}/corrections",
                "/{caseNumber}/resolutions", "/{caseNumber}/closure",
                "/{caseNumber}/cancellation", "/{caseNumber}/resume",
                "/{caseNumber}/reopen");
        assertThat(gets).containsExactlyInAnyOrder(
                "", "/{caseNumber}", "/{caseNumber}/history");
        assertThat(Arrays.stream(RiskCaseController.class.getDeclaredMethods())
                .noneMatch(method -> method.isAnnotationPresent(DeleteMapping.class))).isTrue();
    }

    @Test
    void requestDtosNeverAcceptActorTimeStatusInternalIdOrExecutionCommand() {
        Set<Class<?>> requests = Set.of(
                CreateRiskCaseRequest.class, ChangeRiskCaseAssignmentRequest.class,
                BeginRiskCaseReviewRequest.class, AssociateRiskCaseEvidenceRequest.class,
                ChangeEvidenceAssociationDispositionRequest.class,
                AssociateRiskCaseDecisionRequest.class, SelectRiskCaseDecisionRequest.class,
                AssociateRiskCaseActionRequest.class, ReferenceActionOutcomeRequest.class,
                MarkRiskCaseActionRequiredRequest.class, ReturnRiskCaseToReviewRequest.class,
                ChangeRiskCasePriorityRequest.class, AddRiskCaseNoteRequest.class,
                CorrectRiskCaseNoteRequest.class, ResolveRiskCaseRequest.class,
                CloseRiskCaseRequest.class, CancelRiskCaseRequest.class,
                ResumeResolvedRiskCaseRequest.class, ReopenClosedRiskCaseRequest.class);
        Set<String> forbidden = Set.of(
                "actorRef", "createdBy", "createdAt", "updatedBy", "updatedAt",
                "status", "id", "executionCommand", "severity", "riskLevel", "team");
        requests.forEach(request -> assertThat(componentNames(request))
                .doesNotContainAnyElementsOf(forbidden));
    }

    @Test
    void responsesNeverExposeInternalBigintIdentity() {
        Set<Class<?>> responses = Set.of(
                RiskCaseDetailResponse.class, RiskCaseEvidenceAssociationResponse.class,
                RiskCaseDecisionAssociationResponse.class,
                RiskCaseActionAssociationResponse.class, RiskCaseNoteResponse.class,
                RiskCaseResolutionResponse.class, RiskCaseHistoryEntryResponse.class,
                RiskCaseHistoryPageResponse.class, RiskCaseSummaryResponse.class,
                RiskCaseListResponse.class);
        responses.forEach(response -> assertThat(componentNames(response))
                .doesNotContain("id", "caseId", "rowId", "resolutionId"));
    }

    @Test
    void resultCodesExposeExactlyTheNineApprovedRiskCaseContracts() {
        Set<ResultCode> expected = Set.of(
                ResultCode.RISK_CASE_NOT_FOUND,
                ResultCode.RISK_CASE_INVALID_TRANSITION,
                ResultCode.RISK_CASE_INVARIANT_VIOLATION,
                ResultCode.RISK_CASE_VERSION_CONFLICT,
                ResultCode.RISK_CASE_IDEMPOTENCY_CONFLICT,
                ResultCode.RISK_CASE_PRIMARY_DECISION_CONFLICT,
                ResultCode.RISK_CASE_REFERENCE_NOT_FOUND,
                ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE,
                ResultCode.RISK_CASE_SUBJECT_NOT_ELIGIBLE);
        assertThat(RiskCaseResultCodes.APPROVED)
                .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(expected).allMatch(code -> code.httpStatus().is4xxClientError()
                || code.httpStatus().is5xxServerError());
    }

    private Set<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }
}
