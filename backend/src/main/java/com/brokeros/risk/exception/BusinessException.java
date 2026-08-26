package com.brokeros.risk.exception;

import java.util.Objects;

import com.brokeros.risk.api.ResultCode;

public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.defaultMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = Objects.requireNonNull(resultCode, "resultCode must not be null");
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
