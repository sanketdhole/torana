package com.phaselume.torana.pipeline.step;

import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.step.transform.RequestTransformStep;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestTransformStepTest {

        private RequestTransformStep step;

        @BeforeEach
        void setUp() {
                step = new RequestTransformStep();
        }

        @Test
        void testDirectBodyReplacement() {
                StepDefinition stepDef = StepDefinition.builder()
                                .type(RequestTransformStep.STEP_TYPE)
                                .params(Map.of("body", "{\"transformed\": true}"))
                                .build();

                AgentRequest request = AgentRequest.builder()
                                .id("req-1")
                                .method(HttpMethod.POST)
                                .path("/api/chat")
                                .cachedBody("{\"raw\": true}".getBytes(StandardCharsets.UTF_8))
                                .build();

                AgentContext context = AgentContext.builder()
                                .request(request)
                                .build()
                                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

                StepVerifier.create(step.execute(context))
                                .assertNext(transformedCtx -> {
                                        assertNotNull(transformedCtx.getRequest());
                                        String body = new String(transformedCtx.getRequest().getCachedBody(),
                                                        StandardCharsets.UTF_8);
                                        assertEquals("{\"transformed\": true}", body);
                                })
                                .verifyComplete();
        }

        @Test
        void testSpelExpressionEvaluation() {
                StepDefinition stepDef = StepDefinition.builder()
                                .type(RequestTransformStep.STEP_TYPE)
                                .params(Map.of("expression", "'tenant-' + #tenantId"))
                                .build();

                AgentRequest request = AgentRequest.builder()
                                .id("req-1")
                                .method(HttpMethod.POST)
                                .build();

                AgentContext context = AgentContext.builder()
                                .request(request)
                                .tenantId("acme")
                                .build()
                                .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef);

                StepVerifier.create(step.execute(context))
                                .assertNext(transformedCtx -> {
                                        String body = new String(transformedCtx.getRequest().getCachedBody(),
                                                        StandardCharsets.UTF_8);
                                        assertEquals("tenant-acme", body);
                                })
                                .verifyComplete();
        }
}
