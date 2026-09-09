package com.phaselume.torana.security.authn.filter;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.security.authn.chain.AuthenticationProviderChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToranaAuthenticationWebFilterTest {

    @Test
    void testFilterPopulatesContextAndAttributesOnSuccess() {
        AuthenticationProviderChain chain = mock(AuthenticationProviderChain.class);
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("test-principal")
                .tenantId("tenant-1")
                .build();
        when(chain.authenticate(any())).thenReturn(Mono.just(auth));

        ToranaAuthenticationWebFilter filter = new ToranaAuthenticationWebFilter(chain, true);

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
        AtomicBoolean nextInvoked = new AtomicBoolean(false);
        WebFilterChain filterChain = ex -> {
            nextInvoked.set(true);
            ToranaAuthentication attrAuth = ex.getAttribute(ToranaAuthenticationWebFilter.TORANA_AUTH_ATTRIBUTE);
            assertNotNull(attrAuth);
            assertEquals("test-principal", attrAuth.getPrincipalId());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertTrue(nextInvoked.get());
    }

    @Test
    void testFilterReturns401WhenEnforceAuthIsTrueAndCredentialsMissing() {
        AuthenticationProviderChain chain = mock(AuthenticationProviderChain.class);
        when(chain.authenticate(any())).thenReturn(Mono.empty());

        ToranaAuthenticationWebFilter filter = new ToranaAuthenticationWebFilter(chain, true);

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
        WebFilterChain filterChain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
