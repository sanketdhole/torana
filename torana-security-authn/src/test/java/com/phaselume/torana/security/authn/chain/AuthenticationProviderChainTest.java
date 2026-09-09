package com.phaselume.torana.security.authn.chain;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationProviderChainTest {

    @Test
    void testFirstMatchModeReturnsFirstSuccessful() {
        AuthenticationProvider provider1 = mock(AuthenticationProvider.class);
        AuthenticationProvider provider2 = mock(AuthenticationProvider.class);

        when(provider1.type()).thenReturn("provider1");
        when(provider2.type()).thenReturn("provider2");

        when(provider1.authenticate(any())).thenReturn(Mono.empty());
        when(provider2.authenticate(any())).thenReturn(Mono.just(ToranaAuthentication.builder()
                .principalId("user-from-provider2")
                .authMethod("p2")
                .build()));

        AuthenticationProviderChain chain = new AuthenticationProviderChain(
                List.of(provider1, provider2),
                AuthenticationProviderChain.Mode.FIRST_MATCH,
                false
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        StepVerifier.create(chain.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("user-from-provider2", auth.getPrincipalId());
                })
                .verifyComplete();
    }

    @Test
    void testAllRequiredModeMergesAuthenticationsWhenAllSucceed() {
        AuthenticationProvider provider1 = mock(AuthenticationProvider.class);
        AuthenticationProvider provider2 = mock(AuthenticationProvider.class);

        when(provider1.authenticate(any())).thenReturn(Mono.just(ToranaAuthentication.builder()
                .principalId("multi-factor-user")
                .roles(Set.of("role1"))
                .build()));

        when(provider2.authenticate(any())).thenReturn(Mono.just(ToranaAuthentication.builder()
                .principalId("multi-factor-user")
                .roles(Set.of("role2"))
                .scopes(Set.of("scope2"))
                .build()));

        AuthenticationProviderChain chain = new AuthenticationProviderChain(
                List.of(provider1, provider2),
                AuthenticationProviderChain.Mode.ALL_REQUIRED,
                false
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        StepVerifier.create(chain.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("multi-factor-user", auth.getPrincipalId());
                    assertTrue(auth.getRoles().contains("role1"));
                    assertTrue(auth.getRoles().contains("role2"));
                    assertTrue(auth.getScopes().contains("scope2"));
                })
                .verifyComplete();
    }

    @Test
    void testAllRequiredModeFailsIfOneFails() {
        AuthenticationProvider provider1 = mock(AuthenticationProvider.class);
        AuthenticationProvider provider2 = mock(AuthenticationProvider.class);

        when(provider1.authenticate(any())).thenReturn(Mono.just(ToranaAuthentication.builder()
                .principalId("user-1")
                .build()));
        when(provider2.authenticate(any())).thenReturn(Mono.empty());

        AuthenticationProviderChain chain = new AuthenticationProviderChain(
                List.of(provider1, provider2),
                AuthenticationProviderChain.Mode.ALL_REQUIRED,
                false
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        StepVerifier.create(chain.authenticate(exchange))
                .verifyComplete();
    }

    @Test
    void testAnonymousFallbackWhenEnabled() {
        AuthenticationProviderChain chain = new AuthenticationProviderChain(
                List.of(),
                AuthenticationProviderChain.Mode.FIRST_MATCH,
                true
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        StepVerifier.create(chain.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("anonymous", auth.getPrincipalId());
                    assertEquals("anonymous", auth.getAuthMethod());
                })
                .verifyComplete();
    }
}
