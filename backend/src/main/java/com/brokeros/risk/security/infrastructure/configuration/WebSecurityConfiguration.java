package com.brokeros.risk.security.infrastructure.configuration;

import java.time.Clock;

import com.brokeros.risk.security.application.ActorMappingService;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import com.brokeros.risk.security.infrastructure.authentication.ActorContextAuthenticationFilter;
import com.brokeros.risk.security.infrastructure.authentication.JwtVerifiedPrincipalAdapter;
import com.brokeros.risk.security.infrastructure.authentication.SafeAccessDeniedHandler;
import com.brokeros.risk.security.infrastructure.authentication.SafeAuthenticationEntryPoint;
import com.brokeros.risk.security.infrastructure.authentication.SecurityApiResponseWriter;
import com.brokeros.risk.security.infrastructure.authentication.SpringSecurityActorContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class WebSecurityConfiguration {

    @Bean
    JwtDecoder securityJwtDecoder(
            OAuth2ResourceServerProperties resourceServerProperties,
            SecurityJwtProperties securityJwtProperties) {
        OAuth2ResourceServerProperties.Jwt jwt = resourceServerProperties.getJwt();
        return SecurityJwtDecoderFactory.create(
                jwt.getIssuerUri(),
                jwt.getAudiences(),
                jwt.getJwkSetUri(),
                securityJwtProperties.clockSkew());
    }

    @Bean
    ActorContextProvider actorContextProvider() {
        return new SpringSecurityActorContextProvider();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            Clock securityClock,
            ActorMappingService actorMappingService) throws Exception {
        SecurityApiResponseWriter responseWriter = new SecurityApiResponseWriter(objectMapper);
        SafeAuthenticationEntryPoint authenticationEntryPoint =
                new SafeAuthenticationEntryPoint(responseWriter);
        SafeAccessDeniedHandler accessDeniedHandler =
                new SafeAccessDeniedHandler(responseWriter);
        ActorContextAuthenticationFilter actorContextFilter =
                new ActorContextAuthenticationFilter(
                        new JwtVerifiedPrincipalAdapter(securityClock),
                        actorMappingService,
                        responseWriter);

        http
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/health",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/error")
                        .permitAll()
                        .requestMatchers(
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(actorContextFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
