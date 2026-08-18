package com.brokeros.risk.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.filter.OncePerRequestFilter;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "management.health.db.enabled=false",
        "management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
@Import(RequestCorrelationIntegrationTests.ProbeConfiguration.class)
class RequestCorrelationIntegrationTests {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String SPAN_ID_MDC_KEY = "spanId";
    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorrelationProbeFilter correlationProbeFilter;

    @BeforeEach
    void setUp() {
        MDC.clear();
        correlationProbeFilter.reset();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        correlationProbeFilter.reset();
    }

    @Test
    void generatesRequestIdWhenHeaderIsAbsent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andReturn();

        String requestId = result.getResponse().getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertThat(requestId).matches(UUID_PATTERN);
        assertActiveCorrelation(requestId);
        assertMdcCleared();
    }

    @Test
    void preservesOneValidRequestId() throws Exception {
        String requestId = "caller-request_123.test";

        mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId));

        assertActiveCorrelation(requestId);
        assertMdcCleared();
    }

    @Test
    void replacesMalformedOversizedAndMultiValuedRequestIds() throws Exception {
        MvcResult malformedResult = mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe\nrequest"))
                .andExpect(status().isOk())
                .andReturn();
        String malformedReplacement = malformedResult.getResponse()
                .getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);

        MvcResult oversizedResult = mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "a".repeat(129)))
                .andExpect(status().isOk())
                .andReturn();
        String oversizedReplacement = oversizedResult.getResponse()
                .getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);

        MvcResult multiValueResult = mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "first", "second"))
                .andExpect(status().isOk())
                .andReturn();
        String multiValueReplacement = multiValueResult.getResponse()
                .getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);

        assertThat(malformedReplacement).matches(UUID_PATTERN);
        assertThat(oversizedReplacement).matches(UUID_PATTERN);
        assertThat(multiValueReplacement).matches(UUID_PATTERN);
        assertThat(Set.of(malformedReplacement, oversizedReplacement, multiValueReplacement)).hasSize(3);
        assertActiveCorrelation(malformedReplacement);
        assertActiveCorrelation(oversizedReplacement);
        assertActiveCorrelation(multiValueReplacement);
        assertMdcCleared();
    }

    @Test
    void returnsRequestIdOnStandardizedErrorResponse() throws Exception {
        String requestId = "error-request-123";

        mockMvc.perform(get("/api/missing")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isNotFound())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId));

        assertActiveCorrelation(requestId);
        assertMdcCleared();
    }

    @Test
    void continuesW3cTraceparentWithARealServerSpan() throws Exception {
        String requestId = "w3c-request-123";
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        String parentSpanId = "b7ad6b7169203331";

        mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId)
                        .header(TRACEPARENT_HEADER, "00-" + traceId + "-" + parentSpanId + "-01"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId));

        CorrelationSnapshot snapshot = correlationProbeFilter.snapshot(requestId);
        assertThat(snapshot.traceId()).isEqualTo(traceId);
        assertThat(snapshot.spanId()).matches("[0-9a-f]{16}").isNotEqualTo(parentSpanId);
        assertMdcCleared();
    }

    @Test
    void clearsMdcBetweenSequentialRequests() throws Exception {
        String firstRequestId = "sequential-first";
        String secondRequestId = "sequential-second";

        mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, firstRequestId))
                .andExpect(status().isOk());
        assertMdcCleared();

        mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, secondRequestId))
                .andExpect(status().isOk());
        assertMdcCleared();

        CorrelationSnapshot first = correlationProbeFilter.snapshot(firstRequestId);
        CorrelationSnapshot second = correlationProbeFilter.snapshot(secondRequestId);
        assertThat(first.requestId()).isEqualTo(firstRequestId);
        assertThat(second.requestId()).isEqualTo(secondRequestId);
        assertThat(first.traceId()).isNotEqualTo(second.traceId());
    }

    @Test
    void isolatesMdcAcrossConcurrentRequests() throws Exception {
        int requestCount = 4;
        correlationProbeFilter.armBarrier(requestCount);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);

        try {
            List<Future<ConcurrentResult>> futures = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                String requestId = "concurrent-" + index;
                futures.add(executor.submit(() -> performConcurrentRequest(requestId)));
            }

            List<ConcurrentResult> results = new ArrayList<>();
            for (Future<ConcurrentResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            Set<String> traceIds = ConcurrentHashMap.newKeySet();
            for (ConcurrentResult result : results) {
                assertThat(result.responseRequestId()).isEqualTo(result.requestId());
                assertThat(result.requestIdAfter()).isNull();
                assertThat(result.traceIdAfter()).isNull();
                assertThat(result.spanIdAfter()).isNull();

                CorrelationSnapshot snapshot = correlationProbeFilter.snapshot(result.requestId());
                assertThat(snapshot.requestId()).isEqualTo(result.requestId());
                assertThat(snapshot.traceId()).matches("[0-9a-f]{32}");
                assertThat(snapshot.spanId()).matches("[0-9a-f]{16}");
                traceIds.add(snapshot.traceId());
            }
            assertThat(traceIds).hasSize(requestCount);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ConcurrentResult performConcurrentRequest(String requestId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isOk())
                .andReturn();
        return new ConcurrentResult(
                requestId,
                result.getResponse().getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER),
                MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY),
                MDC.get(TRACE_ID_MDC_KEY),
                MDC.get(SPAN_ID_MDC_KEY));
    }

    private void assertActiveCorrelation(String requestId) {
        CorrelationSnapshot snapshot = correlationProbeFilter.snapshot(requestId);
        assertThat(snapshot.requestId()).isEqualTo(requestId);
        assertThat(snapshot.traceId()).matches("[0-9a-f]{32}");
        assertThat(snapshot.spanId()).matches("[0-9a-f]{16}");
    }

    private void assertMdcCleared() {
        assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(SPAN_ID_MDC_KEY)).isNull();
    }

    record ConcurrentResult(
            String requestId,
            String responseRequestId,
            String requestIdAfter,
            String traceIdAfter,
            String spanIdAfter) {
    }

    record CorrelationSnapshot(String requestId, String traceId, String spanId) {
    }

    static final class CorrelationProbeFilter extends OncePerRequestFilter {

        private final Map<String, CorrelationSnapshot> snapshots = new ConcurrentHashMap<>();
        private final AtomicReference<CyclicBarrier> barrier = new AtomicReference<>();

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            String requestId = MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY);
            snapshots.put(requestId, new CorrelationSnapshot(
                    requestId,
                    MDC.get(TRACE_ID_MDC_KEY),
                    MDC.get(SPAN_ID_MDC_KEY)));
            awaitBarrier();
            filterChain.doFilter(request, response);
        }

        void armBarrier(int participants) {
            barrier.set(new CyclicBarrier(participants));
        }

        CorrelationSnapshot snapshot(String requestId) {
            return snapshots.get(requestId);
        }

        void reset() {
            snapshots.clear();
            barrier.set(null);
        }

        private void awaitBarrier() throws ServletException {
            CyclicBarrier activeBarrier = barrier.get();
            if (activeBarrier == null) {
                return;
            }
            try {
                activeBarrier.await(5, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new ServletException("Concurrent correlation test barrier failed", exception);
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        CorrelationProbeFilter correlationProbeFilter() {
            return new CorrelationProbeFilter();
        }

        @Bean
        FilterRegistrationBean<CorrelationProbeFilter> correlationProbeFilterRegistration(
                CorrelationProbeFilter correlationProbeFilter) {
            FilterRegistrationBean<CorrelationProbeFilter> registration =
                    new FilterRegistrationBean<>(correlationProbeFilter);
            registration.setOrder(RequestCorrelationFilter.FILTER_ORDER + 1);
            return registration;
        }
    }
}
