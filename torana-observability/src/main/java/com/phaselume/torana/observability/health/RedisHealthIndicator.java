package com.phaselume.torana.observability.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Reactive health indicator verifying Redis connectivity via PING.
 */
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisHealthIndicator(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Health> health() {
        if (redisTemplate == null) {
            return Mono.just(Health.unknown().withDetail("redis", "Not configured").build());
        }

        return redisTemplate.getConnectionFactory().getReactiveConnection().ping()
                .timeout(Duration.ofSeconds(2))
                .map(response -> Health.up()
                        .withDetail("service", "Redis")
                        .withDetail("ping", response)
                        .build())
                .onErrorResume(e -> Mono.just(Health.down(e)
                        .withDetail("service", "Redis")
                        .withDetail("error", e.getMessage())
                        .build()));
    }
}
