package com.phaselume.torana.observability.tracing;

import com.phaselume.torana.core.model.AgentContext;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Root WebFilter initializing W3C distributed tracing context for every inbound request.
 */
public class RequestTracingFilter implements WebFilter, Ordered {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String inboundTraceId = TraceContextPropagator.extractTraceId(exchange.getRequest().getHeaders());
        String traceId = (inboundTraceId != null && !inboundTraceId.isBlank())
                ? inboundTraceId
                : TraceContextPropagator.generateTraceId();
        String spanId = TraceContextPropagator.generateSpanId();

        // Echo traceparent in response
        TraceContextPropagator.injectTraceparent(exchange.getResponse().getHeaders(), traceId, spanId);

        // Store attributes in exchange
        exchange.getAttributes().put(TRACE_ID_KEY, traceId);
        exchange.getAttributes().put(SPAN_ID_KEY, spanId);

        AgentContext context = exchange.getAttribute("torana.context");
        if (context != null) {
            exchange.getAttributes().put("torana.context", context.withTrace(traceId, spanId));
        }

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(TRACE_ID_KEY, traceId).put(SPAN_ID_KEY, spanId))
                .doOnEach(signal -> {
                    MDC.put(TRACE_ID_KEY, traceId);
                    MDC.put(SPAN_ID_KEY, spanId);
                });
    }
}
