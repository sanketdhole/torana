package com.phaselume.torana.security.authz.opa.engine;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AuthzDecision;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.security.authz.opa.cache.OpaDecisionCache;
import com.phaselume.torana.security.authz.opa.client.OpaClient;
import com.phaselume.torana.security.authz.opa.client.OpaInputBuilder;
import com.phaselume.torana.security.authz.opa.filter.ObligationProcessor;
import com.phaselume.torana.security.authz.opa.model.OpaObligation;
import com.phaselume.torana.security.authz.opa.model.OpaResponse;
import com.phaselume.torana.security.authz.opa.model.OpaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpaAuthorizationEngineTest {

    private OpaClient mockOpaClient;
    private OpaInputBuilder inputBuilder;
    private OpaDecisionCache mockDecisionCache;
    private ObligationProcessor obligationProcessor;
    private AuthzProperties properties;
    private OpaAuthorizationEngine engine;

    @BeforeEach
    void setUp() {
        mockOpaClient = mock(OpaClient.class);
        inputBuilder = new OpaInputBuilder();
        mockDecisionCache = mock(OpaDecisionCache.class);
        obligationProcessor = new ObligationProcessor();
        properties = AuthzProperties.builder()
                .enabled(true)
                .policyPackage("torana.v1")
                .build();

        engine = new OpaAuthorizationEngine(
                mockOpaClient,
                inputBuilder,
                mockDecisionCache,
                obligationProcessor,
                properties
        );
    }

    @Test
    void testAuthorizeAllowedWithObligations() {
        when(mockDecisionCache.get(any(), any())).thenReturn(Mono.empty());
        when(mockDecisionCache.put(any(), any(), any())).thenReturn(Mono.empty());

        OpaResponse opaResponse = OpaResponse.builder()
                .result(OpaResult.builder()
                        .allow(true)
                        .obligations(List.of(
                                OpaObligation.builder()
                                        .type("mask-field")
                                        .params(Map.of("field", "ssn", "mask", "REDACT"))
                                        .build()
                        ))
                        .build())
                .build();

        when(mockOpaClient.evaluate(any())).thenReturn(Mono.just(opaResponse));

        AgentContext context = AgentContext.builder()
                .authentication(ToranaAuthentication.builder().principalId("alice").build())
                .matchedRoute(RouteDefinition.builder().id("r1").path("/tools").build())
                .request(AgentRequest.builder().method(HttpMethod.GET).path("/tools").build())
                .build();

        StepVerifier.create(engine.authorize(context))
                .assertNext(decision -> {
                    assertNotNull(decision);
                    assertTrue(decision.isAllowed());
                    assertNotNull(decision.getObligations());
                    assertEquals("REDACT", decision.getObligations().getColumnMasks().get("ssn"));
                })
                .verifyComplete();
    }

    @Test
    void testAuthorizeDenied() {
        when(mockDecisionCache.get(any(), any())).thenReturn(Mono.empty());
        when(mockDecisionCache.put(any(), any(), any())).thenReturn(Mono.empty());

        OpaResponse opaResponse = OpaResponse.builder()
                .result(OpaResult.builder()
                        .allow(false)
                        .denyReason("Insufficient scope: tools:execute required")
                        .build())
                .build();

        when(mockOpaClient.evaluate(any())).thenReturn(Mono.just(opaResponse));

        AgentContext context = AgentContext.builder()
                .authentication(ToranaAuthentication.builder().principalId("bob").build())
                .matchedRoute(RouteDefinition.builder().id("r1").path("/tools").build())
                .request(AgentRequest.builder().method(HttpMethod.POST).path("/tools").build())
                .build();

        StepVerifier.create(engine.authorize(context))
                .assertNext(decision -> {
                    assertNotNull(decision);
                    assertFalse(decision.isAllowed());
                    assertEquals("Insufficient scope: tools:execute required", decision.getDenyReason());
                })
                .verifyComplete();
    }
}
