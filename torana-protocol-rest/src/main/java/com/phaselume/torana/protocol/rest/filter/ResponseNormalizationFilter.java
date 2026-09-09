package com.phaselume.torana.protocol.rest.filter;

import com.phaselume.torana.core.model.AgentContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.time.Instant;

/**
 * Normalizes HTTP responses written back to downstream clients.
 * Injects Torana trace headers, latency metrics, and ensures standard
 * content-type headers.
 */
public class ResponseNormalizationFilter {

    public static final String HEADER_ROUTE_ID = "X-Torana-Route-Id";
    public static final String HEADER_REQUEST_ID = "X-Torana-Request-Id";
    public static final String HEADER_TRACE_ID = "X-Torana-Trace-Id";
    public static final String HEADER_LATENCY = "X-Torana-Latency-Ms";

    /**
     * Applies normalization headers to the outgoing ServerWebExchange response.
     */
    public void normalize(ServerWebExchange exchange, AgentContext context, Instant startTime) {
        if (exchange == null) {
            return;
        }

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

        if (context != null) {
            if (context.getRequest() != null && context.getRequest().getId() != null) {
                responseHeaders.set(HEADER_REQUEST_ID, context.getRequest().getId());
            }
            if (context.getTraceId() != null) {
                responseHeaders.set(HEADER_TRACE_ID, context.getTraceId());
            }
            String routeId = context.getAttribute("torana.route.id");
            if (routeId != null) {
                responseHeaders.set(HEADER_ROUTE_ID, routeId);
            }
        }

        if (startTime != null) {
            long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
            responseHeaders.set(HEADER_LATENCY, String.valueOf(latencyMs));
        }

        if (responseHeaders.getContentType() == null && !exchange.getResponse().isCommitted()) {
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
    }
}
