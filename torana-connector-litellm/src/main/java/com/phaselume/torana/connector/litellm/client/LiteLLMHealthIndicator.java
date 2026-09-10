package com.phaselume.torana.connector.litellm.client;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator verifying LiteLLM backend proxy availability.
 */
public class LiteLLMHealthIndicator {

    private final WebClient webClient;
    private final String healthUrl;

    public LiteLLMHealthIndicator(WebClient.Builder webClientBuilder, String baseUrl) {
        String base = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "http://localhost:4000";
        this.healthUrl = base + "/health";
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();
    }

    public LiteLLMHealthIndicator() {
        this(null, "http://localhost:4000");
    }

    /**
     * Checks if LiteLLM proxy is responsive.
     */
    public Mono<Map<String, Object>> health() {
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(2))
                .map(response -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
                    map.put("httpStatus", response.getStatusCode().value());
                    map.put("url", healthUrl);
                    return map;
                })
                .onErrorResume(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", "DOWN");
                    map.put("error", e.getMessage());
                    map.put("url", healthUrl);
                    return Mono.just(map);
                });
    }
}
