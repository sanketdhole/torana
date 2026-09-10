package com.phaselume.torana.security.authz.opa.filter;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AuthzDecision;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthorizationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reactive WebFilter performing fine-grained access control via {@link AuthorizationEngine}.
 * Runs after authentication and rejects unauthorized requests with 403 Forbidden.
 */
public class AccessControlWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessControlWebFilter.class);

    public static final String TORANA_AUTH_ATTRIBUTE = "TORANA_AUTHENTICATION";
    public static final String TORANA_ROUTE_ATTRIBUTE = "TORANA_MATCHED_ROUTE";
    public static final String TORANA_OBLIGATIONS_ATTRIBUTE = "TORANA_OBLIGATIONS";

    private final AuthorizationEngine authorizationEngine;
    private final int order;

    public AccessControlWebFilter(AuthorizationEngine authorizationEngine, int order) {
        this.authorizationEngine = authorizationEngine;
        this.order = order;
    }

    public AccessControlWebFilter(AuthorizationEngine authorizationEngine) {
        this(authorizationEngine, Ordered.HIGHEST_PRECEDENCE + 20);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        AgentContext context = resolveAgentContext(exchange);

        return authorizationEngine.authorize(context)
                .flatMap(decision -> {
                    if (decision.isAllowed()) {
                        if (decision.getObligations() != null) {
                            exchange.getAttributes().put(TORANA_OBLIGATIONS_ATTRIBUTE, decision.getObligations());
                        }
                        return chain.filter(exchange)
                                .contextWrite(Context.of(AuthzDecision.class, decision));
                    } else {
                        log.debug("Access denied for principal '{}' on route '{}': {}",
                                context.getAuthentication() != null ? context.getAuthentication().getPrincipalId() : "anonymous",
                                context.getRequest() != null ? context.getRequest().getPath() : "",
                                decision.getDenyReason());
                        return writeForbiddenResponse(exchange, decision.getDenyReason());
                    }
                });
    }

    private AgentContext resolveAgentContext(ServerWebExchange exchange) {
        ToranaAuthentication auth = exchange.getAttribute(TORANA_AUTH_ATTRIBUTE);
        RouteDefinition route = exchange.getAttribute(TORANA_ROUTE_ATTRIBUTE);

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String clientIp = remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "";

        AgentRequest agentRequest = AgentRequest.builder()
                .method(exchange.getRequest().getMethod())
                .path(exchange.getRequest().getPath().value())
                .headers(exchange.getRequest().getHeaders())
                .queryParams(exchange.getRequest().getQueryParams())
                .attributes(Map.of("client_ip", clientIp))
                .rawExchange(exchange)
                .build();

        return AgentContext.builder()
                .request(agentRequest)
                .authentication(auth != null ? auth : ToranaAuthentication.anonymous())
                .matchedRoute(route)
                .tenantId(auth != null ? auth.getTenantId() : "default")
                .build();
    }

    private Mono<Void> writeForbiddenResponse(ServerWebExchange exchange, String denyReason) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String safeReason = denyReason != null ? denyReason : "Insufficient permissions";
        String json = String.format(
                "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"%s\",\"instance\":\"%s\"}",
                escapeJson(safeReason),
                escapeJson(exchange.getRequest().getPath().value())
        );

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
