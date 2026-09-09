package com.phaselume.torana.pipeline.executor;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.model.PipelineResult;
import com.phaselume.torana.pipeline.registry.PipelineRegistry;
import com.phaselume.torana.pipeline.registry.PipelineStepRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineExecutorTest {

    private PipelineRegistry pipelineRegistry;
    private PipelineStepRegistry stepRegistry;
    private FallbackPipelineResolver fallbackResolver;
    private PipelineExecutor executor;

    @BeforeEach
    void setUp() {
        pipelineRegistry = new PipelineRegistry();
        stepRegistry = new PipelineStepRegistry();
        fallbackResolver = new FallbackPipelineResolver(pipelineRegistry);
        executor = new PipelineExecutor(pipelineRegistry, stepRegistry, fallbackResolver);
    }

    @Test
    void testSequentialStepExecutionInOrder() {
        List<String> executionLog = new ArrayList<>();

        PipelineStep step1 = new PipelineStep() {
            @Override
            public String type() { return "step-1"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                executionLog.add("step-1");
                return Mono.just(context.withAttribute("step1.done", true));
            }
        };

        PipelineStep step2 = new PipelineStep() {
            @Override
            public String type() { return "step-2"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                executionLog.add("step-2");
                return Mono.just(context.withAttribute("step2.done", true));
            }
        };

        stepRegistry.register(step1);
        stepRegistry.register(step2);

        PipelineDefinition pipeline = PipelineDefinition.builder()
                .name("test-pipe")
                .steps(List.of(
                        StepDefinition.builder().type("step-1").build(),
                        StepDefinition.builder().type("step-2").build()
                ))
                .build();
        pipelineRegistry.register(pipeline);

        AgentContext initialContext = AgentContext.builder()
                .request(AgentRequest.builder().id("r1").method(HttpMethod.POST).path("/api").build())
                .build();

        StepVerifier.create(executor.execute("test-pipe", initialContext))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("test-pipe", result.getPipelineName());
                    assertEquals(List.of("step-1", "step-2"), executionLog);
                    assertEquals(Boolean.TRUE, result.getContext().getAttribute("step1.done"));
                    assertEquals(Boolean.TRUE, result.getContext().getAttribute("step2.done"));
                })
                .verifyComplete();
    }

    @Test
    void testShortCircuitSkipsDownstreamSteps() {
        List<String> executionLog = new ArrayList<>();

        PipelineStep shortCircuitStep = new PipelineStep() {
            @Override
            public String type() { return "short-circuit-step"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                executionLog.add("short-circuit");
                AgentResponse response = AgentResponse.builder().status(HttpStatus.ACCEPTED).build();
                return Mono.just(context
                        .withAttribute(PipelineConstants.ATTR_RESPONSE, response)
                        .withAttribute(PipelineConstants.ATTR_SHORT_CIRCUIT, true));
            }
        };

        PipelineStep downstreamStep = new PipelineStep() {
            @Override
            public String type() { return "downstream-step"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                executionLog.add("downstream");
                return Mono.just(context);
            }
        };

        stepRegistry.register(shortCircuitStep);
        stepRegistry.register(downstreamStep);

        PipelineDefinition pipeline = PipelineDefinition.builder()
                .name("sc-pipe")
                .steps(List.of(
                        StepDefinition.builder().type("short-circuit-step").build(),
                        StepDefinition.builder().type("downstream-step").build()
                ))
                .build();
        pipelineRegistry.register(pipeline);

        AgentContext initialContext = AgentContext.builder().build();

        StepVerifier.create(executor.execute("sc-pipe", initialContext))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals(List.of("short-circuit"), executionLog);
                    assertEquals(HttpStatus.ACCEPTED, result.getResponse().getStatus());
                })
                .verifyComplete();
    }

    @Test
    void testFallbackPipelineExecutedOnError() {
        PipelineStep failingStep = new PipelineStep() {
            @Override
            public String type() { return "failing-step"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                return Mono.error(new RuntimeException("Primary failure"));
            }
        };

        PipelineStep fallbackStep = new PipelineStep() {
            @Override
            public String type() { return "fallback-step"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                AgentResponse fallbackResponse = AgentResponse.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .build();
                return Mono.just(context.withAttribute(PipelineConstants.ATTR_RESPONSE, fallbackResponse));
            }
        };

        stepRegistry.register(failingStep);
        stepRegistry.register(fallbackStep);

        PipelineDefinition fallbackPipe = PipelineDefinition.builder()
                .name("fallback-pipe")
                .steps(List.of(StepDefinition.builder().type("fallback-step").build()))
                .build();

        PipelineDefinition primaryPipe = PipelineDefinition.builder()
                .name("primary-pipe")
                .onError("fallback-pipe")
                .steps(List.of(StepDefinition.builder().type("failing-step").build()))
                .build();

        pipelineRegistry.register(fallbackPipe);
        pipelineRegistry.register(primaryPipe);

        AgentContext initialContext = AgentContext.builder().build();

        StepVerifier.create(executor.execute("primary-pipe", initialContext))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getResponse().getStatus());
                    assertEquals("primary-pipe", result.getContext().getAttribute("torana.pipeline.fallback_from"));
                })
                .verifyComplete();
    }

    @Test
    void testTimeoutTriggersFailureResult() {
        PipelineStep slowStep = new PipelineStep() {
            @Override
            public String type() { return "slow-step"; }
            @Override
            public Mono<AgentContext> execute(AgentContext context) {
                return Mono.delay(Duration.ofMillis(300)).thenReturn(context);
            }
        };

        stepRegistry.register(slowStep);

        PipelineDefinition pipeline = PipelineDefinition.builder()
                .name("timeout-pipe")
                .timeout(Duration.ofMillis(50))
                .steps(List.of(StepDefinition.builder().type("slow-step").build()))
                .build();
        pipelineRegistry.register(pipeline);

        AgentContext initialContext = AgentContext.builder().build();

        StepVerifier.create(executor.execute("timeout-pipe", initialContext))
                .assertNext(result -> {
                    assertFalse(result.isSuccess());
                    assertNotNull(result.getError());
                    assertTrue(result.getError().getMessage().contains("timed out"));
                })
                .verifyComplete();
    }
}
