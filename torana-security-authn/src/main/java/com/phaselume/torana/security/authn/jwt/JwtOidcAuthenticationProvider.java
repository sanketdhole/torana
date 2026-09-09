package com.phaselume.torana.security.authn.jwt;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Authentication provider for JWT and OpenID Connect Bearer tokens.
 */
public class JwtOidcAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtOidcAuthenticationProvider.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final MultiIssuerJwtDecoder jwtDecoder;
    private final JwtClaimsExtractor claimsExtractor;

    public JwtOidcAuthenticationProvider(MultiIssuerJwtDecoder jwtDecoder, JwtClaimsExtractor claimsExtractor) {
        this.jwtDecoder = jwtDecoder;
        this.claimsExtractor = claimsExtractor;
    }

    @Override
    public String type() {
        return "jwt";
    }

    @Override
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        if (exchange == null || exchange.getRequest() == null) {
            return Mono.empty();
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Mono.empty();
        }

        String rawToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (rawToken.isEmpty()) {
            return Mono.empty();
        }

        return jwtDecoder.decode(rawToken)
                .map(result -> claimsExtractor.extract(result.getClaims(), rawToken, result.getIssuerConfig()))
                .doOnError(err -> log.debug("JWT authentication failed: {}", err.getMessage()));
    }
}
