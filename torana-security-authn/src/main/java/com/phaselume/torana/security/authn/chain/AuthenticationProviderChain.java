package com.phaselume.torana.security.authn.chain;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Composable authentication provider chain executing configured authentication mechanisms.
 */
public class AuthenticationProviderChain {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationProviderChain.class);

    public enum Mode {
        FIRST_MATCH,
        ALL_REQUIRED,
        ANY_OF;

        public static Mode fromString(String val) {
            if (val == null) return FIRST_MATCH;
            return switch (val.trim().toLowerCase()) {
                case "all-required", "all_required", "all" -> ALL_REQUIRED;
                case "any-of", "any_of", "any" -> ANY_OF;
                default -> FIRST_MATCH;
            };
        }
    }

    private final List<AuthenticationProvider> providers;
    private final Mode mode;
    private final boolean allowAnonymous;
    private final AnonymousAuthenticationProvider anonymousProvider = new AnonymousAuthenticationProvider();

    public AuthenticationProviderChain(List<AuthenticationProvider> providers, Mode mode, boolean allowAnonymous) {
        this.providers = providers != null ? List.copyOf(providers) : Collections.emptyList();
        this.mode = mode != null ? mode : Mode.FIRST_MATCH;
        this.allowAnonymous = allowAnonymous;
    }

    public AuthenticationProviderChain(List<AuthenticationProvider> providers) {
        this(providers, Mode.FIRST_MATCH, false);
    }

    /**
     * Executes the authentication chain against the given exchange according to the configured mode.
     */
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        if (providers.isEmpty()) {
            return allowAnonymous ? anonymousProvider.authenticate(exchange) : Mono.empty();
        }

        return switch (mode) {
            case FIRST_MATCH -> executeFirstMatch(exchange);
            case ALL_REQUIRED -> executeAllRequired(exchange);
            case ANY_OF -> executeAnyOf(exchange);
        };
    }

    private Mono<ToranaAuthentication> executeFirstMatch(ServerWebExchange exchange) {
        return Flux.fromIterable(providers)
                .concatMap(provider -> provider.authenticate(exchange)
                        .onErrorResume(e -> {
                            log.debug("Provider {} failed: {}", provider.type(), e.getMessage());
                            return Mono.empty();
                        }))
                .next()
                .switchIfEmpty(Mono.defer(() -> allowAnonymous ? anonymousProvider.authenticate(exchange) : Mono.empty()));
    }

    private Mono<ToranaAuthentication> executeAllRequired(ServerWebExchange exchange) {
        return Flux.fromIterable(providers)
                .flatMap(provider -> provider.authenticate(exchange))
                .collectList()
                .flatMap(authList -> {
                    if (authList.size() < providers.size()) {
                        return Mono.empty(); // Not all providers succeeded
                    }
                    return Mono.just(mergeAuthentications(authList));
                })
                .switchIfEmpty(Mono.defer(() -> allowAnonymous ? anonymousProvider.authenticate(exchange) : Mono.empty()));
    }

    private Mono<ToranaAuthentication> executeAnyOf(ServerWebExchange exchange) {
        return Flux.fromIterable(providers)
                .flatMap(provider -> provider.authenticate(exchange)
                        .onErrorResume(e -> Mono.empty()))
                .collectList()
                .flatMap(authList -> {
                    if (authList.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.just(mergeAuthentications(authList));
                })
                .switchIfEmpty(Mono.defer(() -> allowAnonymous ? anonymousProvider.authenticate(exchange) : Mono.empty()));
    }

    private ToranaAuthentication mergeAuthentications(List<ToranaAuthentication> list) {
        if (list.size() == 1) {
            return list.get(0);
        }

        ToranaAuthentication primary = list.get(0);
        Set<String> roles = new HashSet<>();
        Set<String> scopes = new HashSet<>();
        Map<String, Object> claims = new HashMap<>();

        for (ToranaAuthentication auth : list) {
            if (auth.getRoles() != null) roles.addAll(auth.getRoles());
            if (auth.getScopes() != null) scopes.addAll(auth.getScopes());
            if (auth.getClaims() != null) claims.putAll(auth.getClaims());
        }

        return primary.toBuilder()
                .roles(Collections.unmodifiableSet(roles))
                .scopes(Collections.unmodifiableSet(scopes))
                .claims(Collections.unmodifiableMap(claims))
                .build();
    }
}
