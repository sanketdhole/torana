package com.phaselume.torana.observability.health;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class HealthIndicatorTest {

    @Test
    void testRedisHealthIndicatorUp() {
        ReactiveStringRedisTemplate redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        ReactiveRedisConnectionFactory factory = Mockito.mock(ReactiveRedisConnectionFactory.class);
        ReactiveRedisConnection connection = Mockito.mock(ReactiveRedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.just("PONG"));

        RedisHealthIndicator indicator = new RedisHealthIndicator(redisTemplate);

        StepVerifier.create(indicator.health())
                .assertNext(health -> {
                    assertEquals(Status.UP, health.getStatus());
                    assertEquals("Redis", health.getDetails().get("service"));
                })
                .verifyComplete();
    }

    @Test
    void testRedisHealthIndicatorDownOnError() {
        ReactiveStringRedisTemplate redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        ReactiveRedisConnectionFactory factory = Mockito.mock(ReactiveRedisConnectionFactory.class);
        ReactiveRedisConnection connection = Mockito.mock(ReactiveRedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.error(new RuntimeException("Connection refused")));

        RedisHealthIndicator indicator = new RedisHealthIndicator(redisTemplate);

        StepVerifier.create(indicator.health())
                .assertNext(health -> {
                    assertEquals(Status.DOWN, health.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void testCircuitBreakerHealthIndicator() {
        CircuitBreakerHealthIndicator indicator = new CircuitBreakerHealthIndicator();

        Health initialHealth = indicator.health();
        assertEquals(Status.UP, initialHealth.getStatus());

        indicator.updateState("http-backend", "CLOSED");
        Health closedHealth = indicator.health();
        assertEquals(Status.UP, closedHealth.getStatus());

        indicator.updateState("litellm-backend", "OPEN");
        Health degradedHealth = indicator.health();
        assertEquals("DEGRADED", degradedHealth.getStatus().getCode());
    }
}
