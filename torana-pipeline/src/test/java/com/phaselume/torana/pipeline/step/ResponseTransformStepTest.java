package com.phaselume.torana.pipeline.step;

import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.step.transform.ResponseTransformStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTransformStepTest {

    private ResponseTransformStep step;

    @BeforeEach
    void setUp() {
        step = new ResponseTransformStep();
    }

    @Test
    void testStaticResponseShortCircuit() {
        StepDefinition stepDef = StepDefinition.builder()
                .type(ResponseTransformStep.STEP_TYPE)
                .params(Map.of("static-response", Map.of(
                        "status", 503,
                        "body", "{\"error\":\"temporarily_unavailable\"}",
                        "headers", Map.of("Retry-After", "30")
                )))
                .build();

        AgentContext context = AgentContext.builder().build()
                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

        StepVerifier.create(step.execute(context))
                .assertNext(transformedCtx -> {
                    assertEquals(Boolean.TRUE, transformedCtx.getAttribute(PipelineConstants.ATTR_SHORT_CIRCUIT));
                    AgentResponse response = transformedCtx.getAttribute(PipelineConstants.ATTR_RESPONSE);
                    assertNotNull(response);
                    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatus());
                    assertEquals("30", response.getHeaders().getFirst("Retry-After"));
                    assertNotNull(response.getBufferedBody());
                })
                .verifyComplete();
    }
}
