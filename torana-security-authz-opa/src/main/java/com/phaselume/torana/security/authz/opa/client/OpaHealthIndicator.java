package com.phaselume.torana.security.authz.opa.client;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Health checker for Open Policy Agent sidecar service.
 */
public class OpaHealthIndicator {

    private final WebClient webClient;
    private final AuthzProperties properties;

    public OpaHealthIndicator(WebClient.Builder webClientBuilder, AuthzProperties properties) {
        this.properties = properties != null ? properties : new AuthzProperties();
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();
    }

    public OpaHealthIndicator(AuthzProperties properties) {
        this(null, properties);
    }

    /**
     * Checks OPA health by pinging /health or root path.
     */
    public Mono<Map<String, Object>> health() {
        if (!properties.isEnabled()) {
            Map<String, Object> map = new HashMap<>();
            map.put("status", "DISABLED");
            return Mono.just(map);
        }

        String healthUrl = resolveHealthUrl();

        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(2))
                .map(response -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("status", response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
                    status.put("httpStatus", response.getStatusCode().value());
                    status.put("opaUrl", properties.getOpaUrl());
                    return status;
                })
                .onErrorResume(e -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("status", "DOWN");
                    status.put("error", e.getMessage());
                    status.put("opaUrl", properties.getOpaUrl());
                    return Mono.just(status);
                });
    }

    private String resolveHealthUrl() {
        try {
            URI uri = URI.create(properties.getOpaUrl());
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() > 0 ? uri.getPort() : 8181;
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            return scheme + "://" + host + ":" + port + "/health";
        } catch (Exception e) {
            return "http://localhost:8181/health";
        }
    }
}
