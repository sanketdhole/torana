package com.phaselume.torana.security.authz.opa.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties;
import com.phaselume.torana.security.authz.opa.model.OpaObligation;
import com.phaselume.torana.security.authz.opa.model.OpaRequest;
import com.phaselume.torana.security.authz.opa.model.OpaResponse;
import com.phaselume.torana.security.authz.opa.model.OpaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reactive client interfacing with Open Policy Agent (OPA) Data API over HTTP.
 */
public class OpaClient {

    private static final Logger log = LoggerFactory.getLogger(OpaClient.class);

    private final WebClient webClient;
    private final AuthzProperties properties;
    private final ObjectMapper objectMapper;

    public OpaClient(WebClient.Builder webClientBuilder, AuthzProperties properties, ObjectMapper objectMapper) {
        this.properties = properties != null ? properties : new AuthzProperties();
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public OpaClient(AuthzProperties properties) {
        this(null, properties, new ObjectMapper());
    }

    public OpaClient() {
        this(null, new AuthzProperties(), new ObjectMapper());
    }

    /**
     * Evaluates policy for the given input map.
     */
    public Mono<OpaResponse> evaluate(Map<String, Object> input) {
        if (!properties.isEnabled()) {
            return Mono.just(OpaResponse.builder()
                    .result(OpaResult.builder().allow(true).build())
                    .build());
        }

        String targetUrl = resolveOpaUrl();
        OpaRequest requestPayload = OpaRequest.builder().input(input).build();

        return webClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .map(this::parseOpaResponse)
                .doOnError(err -> log.warn("OPA authorization evaluation error at {}: {}", targetUrl, err.getMessage()))
                .onErrorResume(err -> {
                    if (properties.isFailOpen()) {
                        log.warn("OPA unreachable but fail-open is true. Allowing request.");
                        return Mono.just(OpaResponse.builder()
                                .result(OpaResult.builder().allow(true).build())
                                .build());
                    }
                    return Mono.just(OpaResponse.builder()
                            .result(OpaResult.builder()
                                    .allow(false)
                                    .denyReason("Authorization service unavailable: " + err.getMessage())
                                    .build())
                            .build());
                });
    }

    private String resolveOpaUrl() {
        String base = properties.getOpaUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:8181/v1/data/" + properties.getPolicyPackage().replace('.', '/');
        }
        return base;
    }

    /**
     * Parses OPA response which could be `{ "result": true }` or `{ "result": { "allow": true, ... } }`.
     */
    private OpaResponse parseOpaResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String decisionId = root.has("decision_id") ? root.get("decision_id").asText() : null;
            JsonNode resultNode = root.get("result");

            if (resultNode == null || resultNode.isNull()) {
                // Undefined rule in OPA evaluates to no result (default deny)
                return OpaResponse.builder()
                        .decisionId(decisionId)
                        .result(OpaResult.builder().allow(false).denyReason("Policy undefined").build())
                        .build();
            }

            if (resultNode.isBoolean()) {
                return OpaResponse.builder()
                        .decisionId(decisionId)
                        .result(OpaResult.builder().allow(resultNode.asBoolean()).build())
                        .build();
            }

            if (resultNode.isObject()) {
                boolean allow = resultNode.has("allow") && resultNode.get("allow").asBoolean();
                String denyReason = resultNode.has("deny_reason") ? resultNode.get("deny_reason").asText() : null;
                if (denyReason == null && resultNode.has("reason")) {
                    denyReason = resultNode.get("reason").asText();
                }

                List<OpaObligation> obligations = new ArrayList<>();
                if (resultNode.has("obligations") && resultNode.get("obligations").isArray()) {
                    for (JsonNode obNode : resultNode.get("obligations")) {
                        if (obNode.isObject()) {
                            String type = obNode.has("type") ? obNode.get("type").asText() : "custom";
                            Map<String, Object> params = obNode.has("params")
                                    ? objectMapper.convertValue(obNode.get("params"), Map.class)
                                    : objectMapper.convertValue(obNode, Map.class);
                            obligations.add(new OpaObligation(type, params));
                        } else if (obNode.isTextual()) {
                            obligations.add(new OpaObligation(obNode.asText(), Collections.emptyMap()));
                        }
                    }
                }

                return OpaResponse.builder()
                        .decisionId(decisionId)
                        .result(OpaResult.builder()
                                .allow(allow)
                                .denyReason(denyReason)
                                .obligations(obligations)
                                .build())
                        .build();
            }

            return OpaResponse.builder()
                    .decisionId(decisionId)
                    .result(OpaResult.builder().allow(false).denyReason("Unrecognized OPA result format").build())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse OPA JSON response: {}", e.getMessage());
            return OpaResponse.builder()
                    .result(OpaResult.builder().allow(false).denyReason("OPA response parsing error: " + e.getMessage()).build())
                    .build();
        }
    }
}
