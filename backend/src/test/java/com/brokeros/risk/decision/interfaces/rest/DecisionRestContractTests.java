package com.brokeros.risk.decision.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.brokeros.risk.api.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class DecisionRestContractTests {

    @Test
    void controllerExposesOnlyApprovedDecisionRoutes() throws Exception {
        RequestMapping root = DecisionController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/api/decisions");
        assertThat(DecisionController.class.getMethod(
                "record", RecordDecisionRequest.class).getAnnotation(PostMapping.class).value())
                .isEmpty();
        assertThat(DecisionController.class.getMethod("detail", String.class)
                .getAnnotation(GetMapping.class).value())
                .containsExactly("/{decisionRef}");
        assertThat(DecisionController.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .containsExactlyInAnyOrder("record", "detail");
    }

    @Test
    void requestAndRecordingResponseExposeOnlyApprovedFields() {
        assertThat(componentNames(RecordDecisionRequest.class))
                .containsExactly("operationId", "subjectRef", "evidenceRefs", "conclusionText")
                .doesNotContain("decisionRef", "source", "recordedByActorRef", "recordedAt");
        assertThat(componentNames(DecisionRecordedResponse.class))
                .containsExactly(
                        "decisionRef", "subjectRef", "evidenceRefs",
                        "recordedByActorRef", "recordedAt", "outcome")
                .doesNotContain("conclusionText");
    }

    @Test
    void beanValidationCoversOperationSubjectEvidenceAndConclusion() {
        RecordComponent operation = component(RecordDecisionRequest.class, "operationId");
        RecordComponent subject = component(RecordDecisionRequest.class, "subjectRef");
        RecordComponent evidence = component(RecordDecisionRequest.class, "evidenceRefs");
        RecordComponent conclusion = component(RecordDecisionRequest.class, "conclusionText");

        assertThat(operation.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(operation.getAccessor().isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(subject.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(subject.getAccessor().isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(evidence.getAccessor().isAnnotationPresent(NotEmpty.class)).isTrue();
        assertThat(conclusion.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(conclusion.getAccessor().getAnnotation(Size.class).max()).isEqualTo(4000);
    }

    @Test
    void resultCodesExposeExactlyTheApprovedDecisionHttpContract() {
        assertThat(ResultCode.values())
                .filteredOn(code -> code.name().startsWith("DECISION_"))
                .containsExactly(
                        ResultCode.DECISION_REQUEST_INVALID,
                        ResultCode.DECISION_CONTENT_INVALID,
                        ResultCode.DECISION_SUBJECT_NOT_RECOGNIZED,
                        ResultCode.DECISION_SUBJECT_AUTHORITY_UNAVAILABLE,
                        ResultCode.DECISION_EVIDENCE_NOT_RECOGNIZED,
                        ResultCode.DECISION_EVIDENCE_AUTHORITY_UNAVAILABLE,
                        ResultCode.DECISION_IDEMPOTENCY_CONFLICT,
                        ResultCode.DECISION_NOT_FOUND,
                        ResultCode.DECISION_ACTOR_TYPE_NOT_PERMITTED,
                        ResultCode.DECISION_AUTHORITY_UNAVAILABLE);
        assertThat(ResultCode.DECISION_REQUEST_INVALID.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.DECISION_CONTENT_INVALID.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.DECISION_SUBJECT_NOT_RECOGNIZED.httpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ResultCode.DECISION_SUBJECT_AUTHORITY_UNAVAILABLE.httpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ResultCode.DECISION_EVIDENCE_NOT_RECOGNIZED.httpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ResultCode.DECISION_EVIDENCE_AUTHORITY_UNAVAILABLE.httpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ResultCode.DECISION_IDEMPOTENCY_CONFLICT.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResultCode.DECISION_NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ResultCode.DECISION_ACTOR_TYPE_NOT_PERMITTED.httpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ResultCode.DECISION_AUTHORITY_UNAVAILABLE.httpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private java.util.List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private RecordComponent component(Class<?> type, String name) {
        return Arrays.stream(type.getRecordComponents())
                .filter(component -> component.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
