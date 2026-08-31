package com.brokeros.risk.api;

import org.springframework.http.HttpStatus;

public enum ResultCode {

    SUCCESS("SUCCESS", "Success", HttpStatus.OK),
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("MALFORMED_REQUEST", "Request body is malformed", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "HTTP method is not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    BUSINESS_ERROR("BUSINESS_ERROR", "Business operation failed", HttpStatus.UNPROCESSABLE_ENTITY),
    AUTHENTICATION_REQUIRED(
            "AUTHENTICATION_REQUIRED",
            "Authentication is required",
            HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_INVALID(
            "AUTHENTICATION_INVALID",
            "Authentication is invalid",
            HttpStatus.UNAUTHORIZED),
    ACTOR_ACCESS_DENIED(
            "ACTOR_ACCESS_DENIED",
            "Actor access is denied",
            HttpStatus.FORBIDDEN),
    AUTHORIZATION_DENIED(
            "AUTHORIZATION_DENIED",
            "Authorization is denied",
            HttpStatus.FORBIDDEN),
    SECURITY_DEPENDENCY_UNAVAILABLE(
            "SECURITY_DEPENDENCY_UNAVAILABLE",
            "Security dependency is unavailable",
            HttpStatus.SERVICE_UNAVAILABLE),
    SECURITY_PROVISIONING_CONFLICT(
            "SECURITY_PROVISIONING_CONFLICT",
            "Security provisioning conflicts with existing state",
            HttpStatus.CONFLICT),
    TRADING_ACCOUNT_REFERENCE_INVALID(
            "TRADING_ACCOUNT_REFERENCE_INVALID", "Trading account reference is invalid", HttpStatus.BAD_REQUEST),
    ACCOUNT_AUTHORITY_SCOPE_INVALID(
            "ACCOUNT_AUTHORITY_SCOPE_INVALID", "Account authority scope is invalid", HttpStatus.BAD_REQUEST),
    SOURCE_NAMESPACE_INVALID(
            "SOURCE_NAMESPACE_INVALID", "Source namespace is invalid", HttpStatus.BAD_REQUEST),
    EXTERNAL_ACCOUNT_KEY_INVALID(
            "EXTERNAL_ACCOUNT_KEY_INVALID", "External account key is invalid", HttpStatus.BAD_REQUEST),
    TRADING_ACCOUNT_MANIFEST_INVALID(
            "TRADING_ACCOUNT_MANIFEST_INVALID", "Trading account manifest is invalid", HttpStatus.BAD_REQUEST),
    TRADING_ACCOUNT_MANIFEST_SCHEMA_UNSUPPORTED(
            "TRADING_ACCOUNT_MANIFEST_SCHEMA_UNSUPPORTED", "Trading account manifest schema is unsupported", HttpStatus.BAD_REQUEST),
    TRADING_ACCOUNT_ATTESTATION_INVALID(
            "TRADING_ACCOUNT_ATTESTATION_INVALID", "Trading account attestation is invalid", HttpStatus.UNPROCESSABLE_ENTITY),
    TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT(
            "TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT", "Trading account operation conflicts with a prior operation", HttpStatus.CONFLICT),
    TRADING_ACCOUNT_MAPPING_CONFLICT(
            "TRADING_ACCOUNT_MAPPING_CONFLICT", "Trading account mapping conflicts with existing state", HttpStatus.CONFLICT),
    ACCOUNT_AUTHORITY_SCOPE_NOT_FOUND(
            "ACCOUNT_AUTHORITY_SCOPE_NOT_FOUND", "Account authority scope was not found", HttpStatus.NOT_FOUND),
    TRADING_ACCOUNT_REFERENCE_NOT_FOUND(
            "TRADING_ACCOUNT_REFERENCE_NOT_FOUND", "Trading account reference was not found", HttpStatus.NOT_FOUND),
    ACCOUNT_AUTHORITY_SCOPE_NOT_ELIGIBLE(
            "ACCOUNT_AUTHORITY_SCOPE_NOT_ELIGIBLE", "Account authority scope is not eligible", HttpStatus.UNPROCESSABLE_ENTITY),
    TRADING_ACCOUNT_VERSION_CONFLICT(
            "TRADING_ACCOUNT_VERSION_CONFLICT", "Trading account version conflicts with existing state", HttpStatus.CONFLICT),
    TRADING_ACCOUNT_INVALID_TRANSITION(
            "TRADING_ACCOUNT_INVALID_TRANSITION", "Trading account lifecycle transition is invalid", HttpStatus.UNPROCESSABLE_ENTITY),
    TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE(
            "TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE", "Trading account authority is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    EVIDENCE_REQUEST_INVALID(
            "EVIDENCE_REQUEST_INVALID", "Evidence request is invalid", HttpStatus.BAD_REQUEST),
    EVIDENCE_CONTENT_INVALID(
            "EVIDENCE_CONTENT_INVALID", "Evidence content is invalid", HttpStatus.BAD_REQUEST),
    EVIDENCE_SUBJECT_NOT_RECOGNIZED(
            "EVIDENCE_SUBJECT_NOT_RECOGNIZED", "Evidence subject is not recognized", HttpStatus.UNPROCESSABLE_ENTITY),
    EVIDENCE_SUBJECT_AUTHORITY_UNAVAILABLE(
            "EVIDENCE_SUBJECT_AUTHORITY_UNAVAILABLE", "Evidence subject authority is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    EVIDENCE_IDEMPOTENCY_CONFLICT(
            "EVIDENCE_IDEMPOTENCY_CONFLICT", "Evidence operation conflicts with a prior operation", HttpStatus.CONFLICT),
    EVIDENCE_NOT_FOUND(
            "EVIDENCE_NOT_FOUND", "Evidence was not found", HttpStatus.NOT_FOUND),
    EVIDENCE_ALREADY_SUPERSEDED(
            "EVIDENCE_ALREADY_SUPERSEDED", "Evidence is already superseded", HttpStatus.CONFLICT),
    EVIDENCE_ACTOR_TYPE_NOT_PERMITTED(
            "EVIDENCE_ACTOR_TYPE_NOT_PERMITTED", "Actor type is not permitted for evidence authoring", HttpStatus.FORBIDDEN),
    EVIDENCE_AUTHORITY_UNAVAILABLE(
            "EVIDENCE_AUTHORITY_UNAVAILABLE", "Evidence authority is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
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
