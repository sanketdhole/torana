package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.ToranaAuthentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * SPI for authentication mechanisms (e.g. JWT/OIDC, API Key, mTLS, SAML).
 */
public interface AuthenticationProvider {

    /**
     * Unique identifier matching the auth configuration type (e.g. "jwt", "apikey", "mtls", "saml").
     */
    String type();

    /**
     * Extract and validate credentials from the incoming exchange.
     * Returns an empty Mono if this provider is not applicable for the request.
     */
    Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange);
}
