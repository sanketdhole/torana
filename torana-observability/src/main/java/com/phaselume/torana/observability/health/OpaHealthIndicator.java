package com.phaselume.torana.observability.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Reactive health indicator verifying Open Policy Agent (OPA) availability.
 */
public class OpaHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String healthUrl;

    public OpaHealthIndicator(String healthUrl) {
        this(WebClient.builder().build(), healthUrl);
    }

    public OpaHealthIndicator(WebClient webClient, String healthUrl) {
        this.webClient = webClient != null ? webClient : WebClient.builder().build();
        this.healthUrl = (healthUrl != null && !healthUrl.isBlank()) ? healthUrl : "http://localhost:8181/health";
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
                                .withDetail("service", "OPA")
                                .withDetail("url", healthUrl)
                                .withDetail("status", response.getStatusCode().value())
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("service", "OPA")
                                .withDetail("url", healthUrl)
                                .withDetail("status", response.getStatusCode().value())
                                .build();
                    }
                })
                .onErrorResume(e -> Mono.just(Health.down(e)
                        .withDetail("service", "OPA")
                        .withDetail("url", healthUrl)
                        .withDetail("error", e.getMessage())
                        .build()));
    }
}
