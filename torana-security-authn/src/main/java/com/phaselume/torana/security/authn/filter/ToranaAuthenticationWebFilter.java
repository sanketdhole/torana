package com.phaselume.torana.security.authn.filter;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.security.authn.chain.AuthenticationProviderChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * Reactive WebFilter that intercepts incoming requests, validates caller credentials using
 * the configured {@link AuthenticationProviderChain}, and attaches the resulting {@link ToranaAuthentication}
 * to the exchange attributes and Reactor context.
 */
public class ToranaAuthenticationWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ToranaAuthenticationWebFilter.class);

    public static final String TORANA_AUTH_ATTRIBUTE = "TORANA_AUTHENTICATION";

    private final AuthenticationProviderChain providerChain;
    private final boolean enforceAuth;
    private final int order;

    public ToranaAuthenticationWebFilter(AuthenticationProviderChain providerChain, boolean enforceAuth, int order) {
        this.providerChain = providerChain;
        this.enforceAuth = enforceAuth;
        this.order = order;
    }

    public ToranaAuthenticationWebFilter(AuthenticationProviderChain providerChain, boolean enforceAuth) {
        this(providerChain, enforceAuth, Ordered.HIGHEST_PRECEDENCE + 10);
    }

    public ToranaAuthenticationWebFilter(AuthenticationProviderChain providerChain) {
        this(providerChain, false, Ordered.HIGHEST_PRECEDENCE + 10);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return providerChain.authenticate(exchange)
                .flatMap(auth -> {
                    exchange.getAttributes().put(TORANA_AUTH_ATTRIBUTE, auth);
                    return chain.filter(exchange)
                            .contextWrite(Context.of(ToranaAuthentication.class, auth));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (enforceAuth) {
                        return writeUnauthorizedResponse(exchange, "Missing or invalid authentication credentials");
                    }
                    // Continue with anonymous context if enforcement is false
                    ToranaAuthentication anon = ToranaAuthentication.anonymous();
                    exchange.getAttributes().put(TORANA_AUTH_ATTRIBUTE, anon);
                    return chain.filter(exchange)
                            .contextWrite(Context.of(ToranaAuthentication.class, anon));
                }))
                .onErrorResume(ToranaAuthException.class, ex -> writeUnauthorizedResponse(exchange, ex.getMessage()));
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String detail) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");

        String json = String.format(
                "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"%s\",\"instance\":\"%s\"}",
                escapeJson(detail),
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
