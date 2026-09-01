package com.brokeros.risk.action.interfaces.rest;

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

class ActionRestContractTests {

    @Test
    void controllerExposesOnlyApprovedActionRoutes() throws Exception {
        RequestMapping root = ActionController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/api/actions");
        assertThat(ActionController.class.getMethod(
                "record", RecordActionRequest.class).getAnnotation(PostMapping.class).value())
                .isEmpty();
        assertThat(ActionController.class.getMethod("detail", String.class)
                .getAnnotation(GetMapping.class).value())
                .containsExactly("/{actionRef}");
        assertThat(ActionController.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .containsExactlyInAnyOrder("record", "detail");
    }

    @Test
    void requestAndRecordingResponseExposeOnlyApprovedFields() {
        assertThat(componentNames(RecordActionRequest.class))
                .containsExactly("operationId", "decisionRef", "intentText")
                .doesNotContain(
                        "actionRef", "source", "status",
                        "recordedByActorRef", "recordedAt");
        assertThat(componentNames(ActionRecordedResponse.class))
                .containsExactly(
                        "actionRef", "decisionRef", "status",
                        "recordedByActorRef", "recordedAt", "outcome")
                .doesNotContain("intentText", "source");
        assertThat(componentNames(ActionDetailResponse.class))
                .containsExactly(
                        "actionRef", "decisionRef", "source", "status",
                        "intentText", "recordedByActorRef", "recordedAt");
    }

    @Test
    void beanValidationCoversOperationDecisionAndIntent() {
        RecordComponent operation = component(RecordActionRequest.class, "operationId");
        RecordComponent decision = component(RecordActionRequest.class, "decisionRef");
        RecordComponent intent = component(RecordActionRequest.class, "intentText");

        assertThat(operation.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(operation.getAccessor().isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(decision.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(decision.getAccessor().isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(intent.getAccessor().isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(intent.getAccessor().getAnnotation(Size.class).max()).isEqualTo(4000);
    }

    @Test
    void resultCodesExposeExactlyTheApprovedActionHttpContract() {
        assertThat(ResultCode.values())
                .filteredOn(code -> code.name().startsWith("ACTION_")
                        && !code.name().startsWith("ACTION_OUTCOME_"))
                .containsExactly(
                        ResultCode.ACTION_REQUEST_INVALID,
                        ResultCode.ACTION_CONTENT_INVALID,
                        ResultCode.ACTION_DECISION_NOT_RECOGNIZED,
                        ResultCode.ACTION_DECISION_AUTHORITY_UNAVAILABLE,
                        ResultCode.ACTION_IDEMPOTENCY_CONFLICT,
                        ResultCode.ACTION_NOT_FOUND,
                        ResultCode.ACTION_ACTOR_TYPE_NOT_PERMITTED,
                        ResultCode.ACTION_AUTHORITY_UNAVAILABLE);
        assertThat(ResultCode.ACTION_REQUEST_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.ACTION_CONTENT_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ResultCode.ACTION_DECISION_NOT_RECOGNIZED.httpStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ResultCode.ACTION_DECISION_AUTHORITY_UNAVAILABLE.httpStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ResultCode.ACTION_IDEMPOTENCY_CONFLICT.httpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResultCode.ACTION_NOT_FOUND.httpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ResultCode.ACTION_ACTOR_TYPE_NOT_PERMITTED.httpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ResultCode.ACTION_AUTHORITY_UNAVAILABLE.httpStatus())
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
