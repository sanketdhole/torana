package com.phaselume.torana.ratelimit.filter;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.RateLimitDecision;
import com.phaselume.torana.core.model.RateLimitPolicy;
import com.phaselume.torana.core.spi.RateLimiter;
import com.phaselume.torana.ratelimit.redis.RateLimitPolicyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Early-stage gateway WebFilter enforcing distributed rate limiting policies before authentication/routing.
 */
public class RateLimitWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitWebFilter.class);

    public static final String CONTEXT_ATTR = "torana.context";
    public static final String POLICY_ATTR = "torana.rate-limit.policy";

    private final RateLimiter rateLimiter;
    private final RateLimitPolicyRegistry policyRegistry;
    private final RateLimitResponseWriter responseWriter;
    private final int order;

    public RateLimitWebFilter(RateLimiter rateLimiter,
                              RateLimitPolicyRegistry policyRegistry,
                              RateLimitResponseWriter responseWriter) {
        this(rateLimiter, policyRegistry, responseWriter, -50);
    }

    public RateLimitWebFilter(RateLimiter rateLimiter,
                              RateLimitPolicyRegistry policyRegistry,
                              RateLimitResponseWriter responseWriter,
                              int order) {
        this.rateLimiter = rateLimiter;
        this.policyRegistry = policyRegistry != null ? policyRegistry : new RateLimitPolicyRegistry();
        this.responseWriter = responseWriter != null ? responseWriter : new RateLimitResponseWriter();
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        AgentContext context = resolveContext(exchange);
        RateLimitPolicy policy = resolvePolicy(exchange);

        return rateLimiter.check(context, policy)
                .flatMap(decision -> {
                    if (!decision.isAllowed()) {
                        log.warn("Rate limit exceeded for key '{}'. Retrying after {} seconds",
                                decision.getLimitKey(), decision.getRetryAfterSeconds());
                        return responseWriter.writeRateLimitExceeded(exchange, decision);
                    }

                    // On allow, inject headers and proceed down filter chain
                    responseWriter.injectRateLimitHeaders(exchange.getResponse(), decision);
                    return chain.filter(exchange);
                });
    }

    private AgentContext resolveContext(ServerWebExchange exchange) {
        AgentContext existing = exchange.getAttribute(CONTEXT_ATTR);
        if (existing != null) {
            return existing;
        }

        ServerHttpRequest request = exchange.getRequest();
        Map<String, Object> attributes = new HashMap<>();

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            attributes.put("clientIp", remoteAddress.getAddress().getHostAddress());
        }

        AgentRequest agentRequest = AgentRequest.builder()
                .method(request.getMethod())
                .path(request.getPath().value())
                .headers(request.getHeaders())
                .queryParams(request.getQueryParams())
                .body(request.getBody())
                .rawExchange(exchange)
                .build();

        return AgentContext.builder()
                .request(agentRequest)
                .attributes(attributes)
                .build();
    }

    private RateLimitPolicy resolvePolicy(ServerWebExchange exchange) {
        String policyName = exchange.getAttribute(POLICY_ATTR);
        if (policyName != null) {
            return policyRegistry.get(policyName);
        }
        return policyRegistry.get("default");
    }
}
