package com.phaselume.torana.observability.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Reactive health indicator for LiteLLM proxy / model service.
 */
public class LiteLLMHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String healthUrl;

    public LiteLLMHealthIndicator(String healthUrl) {
        this(WebClient.builder().build(), healthUrl);
    }

    public LiteLLMHealthIndicator(WebClient webClient, String healthUrl) {
        this.webClient = webClient != null ? webClient : WebClient.builder().build();
        this.healthUrl = (healthUrl != null && !healthUrl.isBlank()) ? healthUrl : "http://localhost:4000/health";
    }

    @Override
    public Mono<Health> health() {
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(2))
                .map(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return Health.up()
                                .withDetail("service", "LiteLLM")
                                .withDetail("url", healthUrl)
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("service", "LiteLLM")
                                .withDetail("status", response.getStatusCode().value())
                                .build();
                    }
                })
                .onErrorResume(e -> Mono.just(Health.down(e)
                        .withDetail("service", "LiteLLM")
                        .withDetail("error", e.getMessage())
                        .build()));
    }
}
