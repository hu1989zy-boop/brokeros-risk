package com.brokeros.risk.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.api.ErrorResponse;
import com.brokeros.risk.api.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {
        return errorResponse(
                exception.getResultCode(),
                exception.getMessage(),
                ErrorResponse.at(request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ErrorResponse.Violation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.Violation(error.getField(), error.getDefaultMessage()))
                .toList();
        return errorResponse(
                ResultCode.VALIDATION_ERROR,
                ResultCode.VALIDATION_ERROR.defaultMessage(),
                new ErrorResponse(request.getRequestURI(), violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ErrorResponse.Violation> violations = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.Violation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return errorResponse(
                ResultCode.VALIDATION_ERROR,
                ResultCode.VALIDATION_ERROR.defaultMessage(),
                new ErrorResponse(request.getRequestURI(), violations));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return errorResponse(
                ResultCode.MALFORMED_REQUEST,
                ResultCode.MALFORMED_REQUEST.defaultMessage(),
                ErrorResponse.at(request.getRequestURI()));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            Exception exception,
            HttpServletRequest request) {
        return errorResponse(
                ResultCode.NOT_FOUND,
                ResultCode.NOT_FOUND.defaultMessage(),
                ErrorResponse.at(request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return errorResponse(
                ResultCode.METHOD_NOT_ALLOWED,
                ResultCode.METHOD_NOT_ALLOWED.defaultMessage(),
                ErrorResponse.at(request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception);
        return errorResponse(
                ResultCode.INTERNAL_ERROR,
                ResultCode.INTERNAL_ERROR.defaultMessage(),
                ErrorResponse.at(request.getRequestURI()));
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> errorResponse(
            ResultCode resultCode,
            String message,
            ErrorResponse errorResponse) {
        return ResponseEntity
                .status(resultCode.httpStatus())
                .body(ApiResponse.failure(resultCode, message, errorResponse));
    }
}
