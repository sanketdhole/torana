package com.phaselume.torana.observability.tracing;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TracingFilterTest {

    @Test
    void testTraceContextPropagatorInjectAndExtract() {
        HttpHeaders headers = new HttpHeaders();
        String traceId = TraceContextPropagator.generateTraceId();
        String spanId = TraceContextPropagator.generateSpanId();

        TraceContextPropagator.injectTraceparent(headers, traceId, spanId);

        assertNotNull(headers.getFirst(TraceContextPropagator.TRACEPARENT_HEADER));
        assertNotNull(headers.getFirst(TraceContextPropagator.X_TRACE_ID_HEADER));

        String extractedTraceId = TraceContextPropagator.extractTraceId(headers);
        assertEquals(traceId, extractedTraceId);
    }

    @Test
    void testRequestTracingFilterCreatesTraceIdIfMissing() {
        RequestTracingFilter filter = new RequestTracingFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resource").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = ex -> {
            assertNotNull(ex.getAttribute(RequestTracingFilter.TRACE_ID_KEY));
            assertNotNull(ex.getAttribute(RequestTracingFilter.SPAN_ID_KEY));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNotNull(exchange.getResponse().getHeaders().getFirst(TraceContextPropagator.TRACEPARENT_HEADER));
    }

    @Test
    void testRequestTracingFilterPreservesInboundTraceId() {
        RequestTracingFilter filter = new RequestTracingFilter();
        String inboundTrace = "4bf92f3577b34da6a3ce929d0e0e4736";
        String inboundSpan = "00f067aa0ba902b7";

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resource")
                .header(TraceContextPropagator.TRACEPARENT_HEADER, String.format("00-%s-%s-01", inboundTrace, inboundSpan))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = ex -> {
            assertEquals(inboundTrace, ex.getAttribute(RequestTracingFilter.TRACE_ID_KEY));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
