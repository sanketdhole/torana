package com.phaselume.torana.resilience.decorator;

import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ResilienceProfile;
import com.phaselume.torana.resilience.config.Resilience4jConfigFactory;
import com.phaselume.torana.resilience.config.ResilienceProfileDefinition;
import com.phaselume.torana.resilience.config.ResilienceProfileRegistry;
import com.phaselume.torana.resilience.registry.ConnectorResilienceRegistry;
import com.phaselume.torana.resilience.registry.ResilienceInstanceFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceDecoratorTest {

    private ConnectorResilienceRegistry registry;
    private ResilienceProfileRegistry profileRegistry;
    private Resilience4jConfigFactory configFactory;
    private ResilienceInstanceFactory instanceFactory;
    private FallbackResponseFactory fallbackResponseFactory;
    private ResilienceEventListener eventListener;
    private Resilience4jResilienceDecorator decorator;

    @BeforeEach
    void setUp() {
        registry = new ConnectorResilienceRegistry();
        profileRegistry = new ResilienceProfileRegistry();
        configFactory = new Resilience4jConfigFactory();
        instanceFactory = new ResilienceInstanceFactory(registry, profileRegistry, configFactory);
        fallbackResponseFactory = new FallbackResponseFactory();
        eventListener = new ResilienceEventListener(registry, null);
        decorator = new Resilience4jResilienceDecorator(instanceFactory, eventListener, fallbackResponseFactory);
    }

    @Test
    void testSuccessfulExecution() {
        AgentResponse.Chunk chunk1 = AgentResponse.Chunk.text("Hello ");
        AgentResponse.Chunk chunk2 = AgentResponse.Chunk.text("World");

        Flux<AgentResponse.Chunk> source = Flux.just(chunk1, chunk2);

        Flux<AgentResponse.Chunk> decorated = decorator.decorate("test-service", null, () -> source);

        StepVerifier.create(decorated)
                .expectNext(chunk1)
                .expectNext(chunk2)
                .verifyComplete();
    }

    @Test
    void testRetryOnTransientFailure() {
        AtomicInteger attempts = new AtomicInteger(0);

        ResilienceProfile profile = ResilienceProfile.builder()
                .name("retry-profile")
                .retryEnabled(true)
                .maxAttempts(3)
                .retryWaitDuration(Duration.ofMillis(10))
                .retryBackoffMultiplier(1.0)
                .circuitBreakerEnabled(false)
                .build();

        Flux<AgentResponse.Chunk> decorated = decorator.decorate("retry-service", profile, () -> {
            if (attempts.incrementAndGet() < 3) {
                return Flux.error(new RuntimeException("Transient failure"));
            }
            return Flux.just(AgentResponse.Chunk.text("Success on attempt 3"));
        });

        StepVerifier.create(decorated)
                .assertNext(chunk -> assertEquals("Success on attempt 3", chunk.getTextDelta()))
                .verifyComplete();

        assertEquals(3, attempts.get());
    }

    @Test
    void testCircuitBreakerOpensAndReturnsFallback() {
        ResilienceProfileDefinition.CircuitBreakerProperties cbProps = ResilienceProfileDefinition.CircuitBreakerProperties.builder()
                .minimumNumberOfCalls(2)
                .slidingWindowSize(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();

        ResilienceProfileDefinition def = ResilienceProfileDefinition.builder()
                .circuitBreaker(cbProps)
                .retry(ResilienceProfileDefinition.RetryProperties.builder().enabled(false).maxAttempts(1).build())
                .build();

        profileRegistry.register("cb-test", def);

        // Fail 2 requests to open the circuit
        for (int i = 0; i < 2; i++) {
            Flux<AgentResponse.Chunk> failingFlux = decorator.decorate("cb-test", null, () -> Flux.error(new RuntimeException("Downstream error")));
            StepVerifier.create(failingFlux)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        CircuitBreaker cb = registry.getCircuitBreakerRegistry().circuitBreaker("cb-test");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // 3rd request should fail fast with CircuitBreaker CallNotPermittedException and emit fallback chunk
        Flux<AgentResponse.Chunk> rejectedFlux = decorator.decorate("cb-test", null, () -> Flux.just(AgentResponse.Chunk.text("Should not reach")));

        StepVerifier.create(rejectedFlux)
                .assertNext(fallbackChunk -> {
                    assertTrue(fallbackChunk.isLast());
                    assertEquals("error", fallbackChunk.getFinishReason());
                    assertEquals("OPEN", fallbackChunk.getMetadata().get("X-Torana-CB-State"));
                    assertEquals(503, fallbackChunk.getMetadata().get("status"));
                })
                .verifyComplete();
    }

    @Test
    void testTimeLimiterTriggerAndFallback() {
        ResilienceProfile profile = ResilienceProfile.builder()
                .name("timeout-profile")
                .timeLimiterEnabled(true)
                .timeoutDuration(Duration.ofMillis(50))
                .retryEnabled(false)
                .maxAttempts(1)
                .build();

        Flux<AgentResponse.Chunk> slowFlux = decorator.decorate("timeout-service", profile, () ->
                Flux.just(AgentResponse.Chunk.text("slow")).delayElements(Duration.ofMillis(200))
        );

        StepVerifier.create(slowFlux)
                .assertNext(fallbackChunk -> {
                    assertTrue(fallbackChunk.isLast());
                    assertEquals(504, fallbackChunk.getMetadata().get("status"));
                    assertEquals("EXCEEDED", fallbackChunk.getMetadata().get("X-Torana-Timeout"));
                })
                .verifyComplete();
    }
}
