package com.phaselume.torana.pipeline.step;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.step.transform.HeaderEnrichStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderEnrichStepTest {

    private HeaderEnrichStep step;

    @BeforeEach
    void setUp() {
        step = new HeaderEnrichStep();
    }

    @Test
    void testHeaderEnrichmentWithPlaceholders() {
        StepDefinition stepDef = StepDefinition.builder()
                .type(HeaderEnrichStep.STEP_TYPE)
                .params(Map.of("headers", Map.of(
                        "X-Tenant-Id", "${context.tenantId}",
                        "X-Trace-Id", "${context.traceId}",
                        "X-User", "${principal.name}",
                        "X-Route", "${route.id}",
                        "X-Static", "custom-value"
                )))
                .build();

        AgentRequest request = AgentRequest.builder()
                .id("req-1")
                .method(HttpMethod.POST)
                .headers(new HttpHeaders())
                .build();

        ToranaAuthentication authn = ToranaAuthentication.builder()
                .principalId("u-123")
                .name("Alice")
                .build();

        RouteDefinition route = RouteDefinition.builder()
                .id("chat-route")
                .build();

        AgentContext context = AgentContext.builder()
                .request(request)
                .tenantId("tenant-42")
                .traceId("trace-xyz")
                .authentication(authn)
                .matchedRoute(route)
                .build()
                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

        StepVerifier.create(step.execute(context))
                .assertNext(enrichedCtx -> {
                    HttpHeaders headers = enrichedCtx.getRequest().getHeaders();
                    assertNotNull(headers);
                    assertEquals("tenant-42", headers.getFirst("X-Tenant-Id"));
                    assertEquals("trace-xyz", headers.getFirst("X-Trace-Id"));
                    assertEquals("Alice", headers.getFirst("X-User"));
                    assertEquals("chat-route", headers.getFirst("X-Route"));
                    assertEquals("custom-value", headers.getFirst("X-Static"));
                })
                .verifyComplete();
    }
}
