package com.phaselume.torana.security.authn.chain;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Fallback authentication provider for anonymous or public access.
 */
public class AnonymousAuthenticationProvider implements AuthenticationProvider {

    @Override
    public String type() {
        return "anonymous";
    }

    @Override
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        return Mono.just(ToranaAuthentication.anonymous());
    }
}
