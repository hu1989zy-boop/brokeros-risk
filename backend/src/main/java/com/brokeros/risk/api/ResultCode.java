package com.brokeros.risk.api;

import org.springframework.http.HttpStatus;

public enum ResultCode {

    SUCCESS("SUCCESS", "Success", HttpStatus.OK),
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("MALFORMED_REQUEST", "Request body is malformed", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "HTTP method is not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    BUSINESS_ERROR("BUSINESS_ERROR", "Business operation failed", HttpStatus.UNPROCESSABLE_ENTITY),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ResultCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
