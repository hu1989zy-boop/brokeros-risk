package com.brokeros.risk.security.infrastructure.authentication;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.brokeros.risk.api.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class SafeAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SafeAccessDeniedHandler.class);

    private final SecurityApiResponseWriter responseWriter;

    public SafeAccessDeniedHandler(SecurityApiResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        LOGGER.warn(
                "security_event=request_access_denied outcome=DENY code={}",
                ResultCode.AUTHORIZATION_DENIED.code());
        responseWriter.write(request, response, ResultCode.AUTHORIZATION_DENIED);
    }
}
