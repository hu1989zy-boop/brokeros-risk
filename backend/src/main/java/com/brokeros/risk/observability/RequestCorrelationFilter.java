package com.brokeros.risk.observability;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(RequestCorrelationFilter.FILTER_ORDER)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        Enumeration<String> requestIds = request.getHeaders(REQUEST_ID_HEADER);
        if (requestIds == null || !requestIds.hasMoreElements()) {
            return generateRequestId();
        }

        String requestId = requestIds.nextElement();
        if (requestIds.hasMoreElements() || !VALID_REQUEST_ID.matcher(requestId).matches()) {
            return generateRequestId();
        }

        return requestId;
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
