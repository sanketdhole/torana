package com.phaselume.torana.security.authz.opa.filter;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AuthzDecision;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthorizationEngine;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessControlWebFilterTest {

    @Test
    void testFilterAllowsWhenDecisionIsAllowed() {
        AuthorizationEngine mockEngine = mock(AuthorizationEngine.class);
        AgentContext.Obligations obligations = AgentContext.Obligations.builder()
                .columnMasks(Map.of("email", "REDACT"))
                .build();

        when(mockEngine.authorize(any())).thenReturn(Mono.just(AuthzDecision.allow(obligations)));

        AccessControlWebFilter filter = new AccessControlWebFilter(mockEngine);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tools").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(AccessControlWebFilter.TORANA_AUTH_ATTRIBUTE, ToranaAuthentication.builder().principalId("alice").build());

        AtomicBoolean nextInvoked = new AtomicBoolean(false);
        WebFilterChain filterChain = ex -> {
            nextInvoked.set(true);
            AgentContext.Obligations obs = ex.getAttribute(AccessControlWebFilter.TORANA_OBLIGATIONS_ATTRIBUTE);
            assertNotNull(obs);
            assertEquals("REDACT", obs.getColumnMasks().get("email"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertTrue(nextInvoked.get());
    }

    @Test
    void testFilterReturns403WhenDecisionIsDenied() {
        AuthorizationEngine mockEngine = mock(AuthorizationEngine.class);
        when(mockEngine.authorize(any())).thenReturn(Mono.just(AuthzDecision.deny("Role 'admin' required")));

        AccessControlWebFilter filter = new AccessControlWebFilter(mockEngine);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain filterChain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }
}
