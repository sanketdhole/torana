package com.phaselume.torana.pipeline.step;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.registry.BackendConnectorRegistry;
import com.phaselume.torana.pipeline.step.llm.LlmCallStep;
import com.phaselume.torana.test.support.MockBackendConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmCallStepTest {

    private BackendConnectorRegistry connectorRegistry;
    private MockBackendConnector mockConnector;
    private LlmCallStep step;

    @BeforeEach
    void setUp() {
        connectorRegistry = new BackendConnectorRegistry();
        mockConnector = new MockBackendConnector("litellm")
                .returnsChunks(List.of(
                        AgentResponse.Chunk.text("Hello "),
                        AgentResponse.Chunk.text("world!")
                ));
        connectorRegistry.registerConnector(mockConnector);
        step = new LlmCallStep(connectorRegistry);
    }

    @Test
    void testExecuteLlmCallWithExplicitBackendRef() {
        StepDefinition stepDef = StepDefinition.builder()
                .type(LlmCallStep.STEP_TYPE)
                .params(Map.of("backend-ref", "litellm", "temperature", 0.7))
                .build();

        AgentContext context = AgentContext.builder().build()
                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

        StepVerifier.create(step.execute(context))
                .assertNext(updatedCtx -> {
                    AgentResponse response = updatedCtx.getAttribute(PipelineConstants.ATTR_RESPONSE);
                    assertNotNull(response);
                    assertNotNull(response.getStream());

                    StepVerifier.create(response.getStream())
                            .assertNext(chunk -> assertEquals("Hello ", chunk.getTextDelta()))
                            .assertNext(chunk -> assertEquals("world!", chunk.getTextDelta()))
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    void testExecuteLlmCallWithRouteBackendRef() {
        StepDefinition stepDef = StepDefinition.builder()
                .type(LlmCallStep.STEP_TYPE)
                .build();

        RouteDefinition route = RouteDefinition.builder()
                .id("llm-route")
                .backendRef("litellm")
                .build();

        AgentContext context = AgentContext.builder()
                .matchedRoute(route)
                .build()
                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

        StepVerifier.create(step.execute(context))
                .assertNext(updatedCtx -> {
                    AgentResponse response = updatedCtx.getAttribute(PipelineConstants.ATTR_RESPONSE);
                    assertNotNull(response);
                    assertNotNull(response.getStream());
                })
                .verifyComplete();
    }
}
