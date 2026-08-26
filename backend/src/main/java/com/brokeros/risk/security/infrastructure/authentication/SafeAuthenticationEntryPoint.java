package com.brokeros.risk.security.infrastructure.authentication;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.brokeros.risk.api.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class SafeAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SafeAuthenticationEntryPoint.class);

    private final SecurityApiResponseWriter responseWriter;

    public SafeAuthenticationEntryPoint(SecurityApiResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException, ServletException {
        boolean credentialPresented = request.getHeader("Authorization") != null;
        ResultCode resultCode = credentialPresented
                ? ResultCode.AUTHENTICATION_INVALID
                : ResultCode.AUTHENTICATION_REQUIRED;
        LOGGER.warn("security_event=authentication_failure outcome=DENY code={}", resultCode.code());
        response.setHeader("WWW-Authenticate", "Bearer");
        responseWriter.write(request, response, resultCode);
    }
}
