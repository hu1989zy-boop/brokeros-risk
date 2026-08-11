package com.brokeros.risk.api;

import java.time.Instant;
import java.util.Objects;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        Instant timestamp) {

    public ApiResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ResultCode.SUCCESS.code(),
                ResultCode.SUCCESS.defaultMessage(),
                data,
                Instant.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> failure(ResultCode resultCode, String message, T data) {
        Objects.requireNonNull(resultCode, "resultCode must not be null");
        String responseMessage = message == null || message.isBlank()
                ? resultCode.defaultMessage()
                : message;
        return new ApiResponse<>(resultCode.code(), responseMessage, data, Instant.now());
    }
}
