package com.brokeros.risk.actionoutcome.interfaces.rest;

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

class ActionOutcomeRestContractTests {

    @Test
    void controllerExposesOnlyApprovedActionOutcomeRoutes() throws Exception {
        RequestMapping root =
                ActionOutcomeController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/api/action-outcomes");
        assertThat(ActionOutcomeController.class.getMethod(
                "record", RecordActionOutcomeRequest.class)
                .getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(ActionOutcomeController.class.getMethod("detail", String.class)
                .getAnnotation(GetMapping.class).value())
                .containsExactly("/{actionOutcomeRef}");
        assertThat(ActionOutcomeController.class.getMethod("list", String.class)
                .getAnnotation(GetMapping.class).params())
                .containsExactly("actionRef");
        assertThat(ActionOutcomeController.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .containsExactlyInAnyOrder("record", "detail", "list");
    }

    @Test
    void referenceListItemIsContentFree() {
        assertThat(componentNames(ActionOutcomeReferenceListResponse.Item.class))
                .containsExactly("actionOutcomeRef", "actionRef", "recordedAt")
                .doesNotContain("outcomeText", "source", "recordedByActorRef", "status");
    }

    @Test
    void requestAndResponsesExposeOnlyApprovedFields() {
        assertThat(componentNames(RecordActionOutcomeRequest.class))
                .containsExactly("operationId", "actionRef", "outcomeText")
                .doesNotContain(
                        "actionOutcomeRef", "source", "status",
                        "recordedByActorRef", "recordedAt");
        assertThat(componentNames(ActionOutcomeRecordedResponse.class))
                .containsExactly(
                        "actionOutcomeRef", "actionRef",
                        "recordedByActorRef", "recordedAt", "outcome")
                .doesNotContain("outcomeText", "source", "status");
        assertThat(componentNames(ActionOutcomeDetailResponse.class))
                .containsExactly(
                        "actionOutcomeRef", "actionRef", "source", "outcomeText",
                        "recordedByActorRef", "recordedAt")
                .doesNotContain("status", "result", "classification");
    }

    @Test
    void beanValidationCoversOperationActionAndOutcomeText() {
        RecordComponent operation =
                component(RecordActionOutcomeRequest.class, "operationId");
        RecordComponent action =
                component(RecordActionOutcomeRequest.class, "actionRef");
        RecordComponent outcomeText =
                component(RecordActionOutcomeRequest.class, "outcomeText");

        assertThat(operation.getAccessor().isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(operation.getAccessor().isAnnotationPresent(Pattern.class))
                .isTrue();
        assertThat(action.getAccessor().isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(action.getAccessor().isAnnotationPresent(Pattern.class))
                .isTrue();
        assertThat(outcomeText.getAccessor().isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(outcomeText.getAccessor().getAnnotation(Size.class).max())
                .isEqualTo(4000);
    }

    @Test
    void resultCodesExposeExactlyTheApprovedActionOutcomeHttpContract() {
        assertThat(ResultCode.values())
                .filteredOn(code -> code.name().startsWith("ACTION_OUTCOME_"))
                .containsExactly(
                        ResultCode.ACTION_OUTCOME_REQUEST_INVALID,
                        ResultCode.ACTION_OUTCOME_CONTENT_INVALID,
                        ResultCode.ACTION_OUTCOME_ACTION_NOT_RECOGNIZED,
                        ResultCode.ACTION_OUTCOME_ACTION_AUTHORITY_UNAVAILABLE,
                        ResultCode.ACTION_OUTCOME_IDEMPOTENCY_CONFLICT,
                        ResultCode.ACTION_OUTCOME_NOT_FOUND,
                        ResultCode.ACTION_OUTCOME_ACTOR_TYPE_NOT_PERMITTED,
                        ResultCode.ACTION_OUTCOME_AUTHORITY_UNAVAILABLE);
        assertThat(ResultCode.ACTION_OUTCOME_REQUEST_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.ACTION_OUTCOME_CONTENT_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.ACTION_OUTCOME_ACTION_NOT_RECOGNIZED.httpStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ResultCode.ACTION_OUTCOME_ACTION_AUTHORITY_UNAVAILABLE.httpStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ResultCode.ACTION_OUTCOME_IDEMPOTENCY_CONFLICT.httpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResultCode.ACTION_OUTCOME_NOT_FOUND.httpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ResultCode.ACTION_OUTCOME_ACTOR_TYPE_NOT_PERMITTED.httpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ResultCode.ACTION_OUTCOME_AUTHORITY_UNAVAILABLE.httpStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private java.util.List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName).toList();
    }

    private RecordComponent component(Class<?> type, String name) {
        return Arrays.stream(type.getRecordComponents())
                .filter(component -> component.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
