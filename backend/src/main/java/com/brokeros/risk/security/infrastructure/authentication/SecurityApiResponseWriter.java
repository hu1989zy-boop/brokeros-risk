package com.brokeros.risk.security.infrastructure.authentication;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.api.ErrorResponse;
import com.brokeros.risk.api.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

public class SecurityApiResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityApiResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(
                        resultCode,
                        resultCode.defaultMessage(),
                        ErrorResponse.at(request.getRequestURI())));
    }
}
