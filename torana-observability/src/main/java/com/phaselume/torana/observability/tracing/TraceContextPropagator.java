package com.phaselume.torana.observability.tracing;

import org.springframework.http.HttpHeaders;

import java.util.UUID;

/**
 * W3C Trace Context Propagator (traceparent, tracestate).
 */
public class TraceContextPropagator {

    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String X_TRACE_ID_HEADER = "X-Trace-Id";

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static void injectTraceparent(HttpHeaders headers, String traceId, String spanId) {
        if (headers == null) return;
        String tid = (traceId != null && !traceId.isBlank()) ? traceId : generateTraceId();
        String sid = (spanId != null && !spanId.isBlank()) ? spanId : generateSpanId();

        String traceparent = String.format("00-%s-%s-01", tid, sid);
        headers.set(TRACEPARENT_HEADER, traceparent);
        headers.set(X_TRACE_ID_HEADER, tid);
    }

    public static String extractTraceId(HttpHeaders headers) {
        if (headers == null) return null;
        String traceparent = headers.getFirst(TRACEPARENT_HEADER);
        if (traceparent != null && traceparent.startsWith("00-")) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return parts[1];
            }
        }
        return headers.getFirst(X_TRACE_ID_HEADER);
    }
}
