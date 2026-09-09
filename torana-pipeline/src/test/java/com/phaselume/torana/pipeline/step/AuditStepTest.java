package com.phaselume.torana.pipeline.step;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuditSink;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.step.audit.AuditStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditStepTest {

    private List<AuditEvent> publishedEvents;
    private AuditSink mockSink;
    private AuditStep step;

    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        mockSink = new AuditSink() {
            @Override
            public String type() { return "test-sink"; }
            @Override
            public Mono<Void> publish(AuditEvent event) {
                publishedEvents.add(event);
                return Mono.empty();
            }
        };
        step = new AuditStep(List.of(mockSink));
    }

    @Test
    void testAuditStepEmitsEventWithPayloadHash() {
        StepDefinition stepDef = StepDefinition.builder()
                .type(AuditStep.STEP_TYPE)
                .params(Map.of("action", "INVOKE_LLM", "include-payload-hash", true))
                .build();

        AgentRequest request = AgentRequest.builder()
                .id("req-1")
                .method(HttpMethod.POST)
                .cachedBody("{\"query\": \"tell me a joke\"}".getBytes(StandardCharsets.UTF_8))
                .build();

        ToranaAuthentication authn = ToranaAuthentication.builder()
                .principalId("agent-007")
                .build();

        RouteDefinition route = RouteDefinition.builder()
                .id("llm-route")
                .build();

        AgentContext context = AgentContext.builder()
                .request(request)
                .tenantId("tenant-corp")
                .traceId("trace-123")
                .spanId("span-456")
                .authentication(authn)
                .matchedRoute(route)
                .build()
                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

        StepVerifier.create(step.execute(context))
                .assertNext(resCtx -> {
                    assertEquals(1, publishedEvents.size());
                    AuditEvent event = publishedEvents.get(0);
                    assertEquals("INVOKE_LLM", event.getAction());
                    assertEquals("tenant-corp", event.getTenantId());
                    assertEquals("agent-007", event.getPrincipalId());
                    assertEquals("llm-route", event.getRouteId());
                    assertEquals("trace-123", event.getTraceId());
                    assertEquals("span-456", event.getSpanId());
                    assertNotNull(event.getPayloadHashSha256());
                    assertFalse(event.getPayloadHashSha256().isBlank());
                })
                .verifyComplete();
    }
}
