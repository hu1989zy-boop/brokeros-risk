package com.brokeros.risk.evidence.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.brokeros.risk.api.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class EvidenceRestContractTests {

    @Test
    void controllerExposesOnlyApprovedEvidenceRoutes() throws Exception {
        RequestMapping root = EvidenceController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/api/evidence");
        assertThat(EvidenceController.class.getMethod(
                "record", RecordEvidenceRequest.class).getAnnotation(PostMapping.class).value())
                .isEmpty();
        assertThat(EvidenceController.class.getMethod(
                "correct", String.class, CorrectEvidenceRequest.class)
                .getAnnotation(PostMapping.class).value())
                .containsExactly("/{evidenceRef}/corrections");
        assertThat(EvidenceController.class.getMethod("detail", String.class)
                .getAnnotation(GetMapping.class).value())
                .containsExactly("/{evidenceRef}");
    }

    @Test
    void requestDtosAcceptNoActorTimeStatusGeneratedRefOrCorrectionSubject() {
        assertThat(componentNames(RecordEvidenceRequest.class))
                .containsExactly("operationId", "subjectRef", "observationText");
        assertThat(componentNames(CorrectEvidenceRequest.class))
                .containsExactly("operationId", "correctionReason", "observationText")
                .doesNotContain("subjectRef", "actorRef", "recordedAt", "status");
    }

    @Test
    void beanValidationAndDomainBoundaryFieldsArePresent() {
        RecordComponent recordOperation = component(
                RecordEvidenceRequest.class, "operationId");
        RecordComponent recordSubject = component(
                RecordEvidenceRequest.class, "subjectRef");
        RecordComponent observation = component(
                RecordEvidenceRequest.class, "observationText");
        RecordComponent correctionReason = component(
                CorrectEvidenceRequest.class, "correctionReason");

        assertThat(recordOperation.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(recordOperation.getAccessor().isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(recordSubject.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(recordSubject.getAccessor().isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(observation.getAccessor().getAnnotation(Size.class).max()).isEqualTo(4000);
        assertThat(correctionReason.getAccessor().getAnnotation(Size.class).max()).isEqualTo(1000);
    }

    @Test
    void resultCodesExposeTheApprovedStableHttpContract() {
        assertThat(ResultCode.EVIDENCE_REQUEST_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.EVIDENCE_CONTENT_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.EVIDENCE_SUBJECT_NOT_RECOGNIZED.httpStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ResultCode.EVIDENCE_SUBJECT_AUTHORITY_UNAVAILABLE.httpStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT.httpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResultCode.EVIDENCE_NOT_FOUND.httpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ResultCode.EVIDENCE_ALREADY_SUPERSEDED.httpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResultCode.EVIDENCE_ACTOR_TYPE_NOT_PERMITTED.httpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ResultCode.EVIDENCE_AUTHORITY_UNAVAILABLE.httpStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
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
