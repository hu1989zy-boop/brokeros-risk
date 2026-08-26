package com.brokeros.risk.security.infrastructure.authentication;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.observability.RequestCorrelationFilter;
import com.brokeros.risk.security.application.ActorAccessDeniedException;
import com.brokeros.risk.security.application.ActorMappingService;
import com.brokeros.risk.security.application.SecurityDependencyUnavailableException;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.VerifiedPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActorContextAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ActorContextAuthenticationFilter.class);

    private final JwtVerifiedPrincipalAdapter principalAdapter;
    private final ActorMappingService actorMappingService;
    private final SecurityApiResponseWriter responseWriter;

    public ActorContextAuthenticationFilter(
            JwtVerifiedPrincipalAdapter principalAdapter,
            ActorMappingService actorMappingService,
            SecurityApiResponseWriter responseWriter) {
        this.principalAdapter = principalAdapter;
        this.actorMappingService = actorMappingService;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        VerifiedPrincipal verifiedPrincipal;
        try {
            verifiedPrincipal = principalAdapter.adapt(jwtAuthentication.getToken());
        } catch (IllegalArgumentException | NullPointerException exception) {
            LOGGER.warn(
                    "security_event=authentication_failure outcome=DENY code={}",
                    ResultCode.AUTHENTICATION_INVALID.code());
            responseWriter.write(request, response, ResultCode.AUTHENTICATION_INVALID);
            return;
        }

        ActorContext actorContext;
        try {
            actorContext = actorMappingService.createContext(
                    verifiedPrincipal,
                    MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY),
                    MDC.get("traceId"));
        } catch (ActorAccessDeniedException exception) {
            LOGGER.warn(
                    "security_event=actor_mapping_denied outcome=DENY code={}",
                    ResultCode.ACTOR_ACCESS_DENIED.code());
            responseWriter.write(request, response, ResultCode.ACTOR_ACCESS_DENIED);
            return;
        } catch (SecurityDependencyUnavailableException exception) {
            LOGGER.error(
                    "security_event=security_dependency_unavailable outcome=DENY code={}",
                    ResultCode.SECURITY_DEPENDENCY_UNAVAILABLE.code());
            responseWriter.write(
                    request,
                    response,
                    ResultCode.SECURITY_DEPENDENCY_UNAVAILABLE);
            return;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "security_event=security_integrity_failure outcome=DENY code={}",
                    ResultCode.INTERNAL_ERROR.code());
            responseWriter.write(request, response, ResultCode.INTERNAL_ERROR);
            return;
        }

        SecurityContext trustedContext = SecurityContextHolder.createEmptyContext();
        trustedContext.setAuthentication(new BrokerOsAuthentication(actorContext));
        SecurityContextHolder.setContext(trustedContext);
        filterChain.doFilter(request, response);
    }
}
