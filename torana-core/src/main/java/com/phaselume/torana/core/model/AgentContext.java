package com.phaselume.torana.core.model;

import com.phaselume.torana.core.config.RouteDefinition;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable propagation object flowing through the entire reactive pipeline chain.
 * Carries normalized request, security credentials, matched route, policy obligations, and telemetry traces.
 */
@Value
@Builder(toBuilder = true)
public class AgentContext {

    @Builder.Default
    String id = UUID.randomUUID().toString();

    String traceId;
    String spanId;
    String tenantId;

    AgentRequest request;
    ToranaAuthentication authentication;
    RouteDefinition matchedRoute;
    Obligations obligations;

    @Builder.Default
    Map<String, Object> attributes = Collections.emptyMap();

    @Builder.Default
    Instant createdAt = Instant.now();

    /**
     * Policy obligations emitted by OPA or security engines (e.g. data masking, row filtering).
     */
    @Value
    @Builder(toBuilder = true)
    public static class Obligations {
        @Builder.Default
        Map<String, String> rowFilters = Collections.emptyMap(); // e.g. "tenant_id" -> "acme-corp"
        
        @Builder.Default
        Map<String, String> columnMasks = Collections.emptyMap(); // e.g. "ssn" -> "REDACT"
        
        Integer maxRows;
        
        @Builder.Default
        List<String> allowedPathPrefixes = Collections.emptyList();
        
        @Builder.Default
        Map<String, Object> additionalObligations = Collections.emptyMap();

        public static Obligations empty() {
            return Obligations.builder().build();
        }
    }

    // ─── Fluent Copy-on-Write Methods ──────────────────────────────────────────

    public AgentContext withRequest(AgentRequest newRequest) {
        return this.toBuilder().request(newRequest).build();
    }

    public AgentContext withAuthentication(ToranaAuthentication newAuthn) {
        return this.toBuilder().authentication(newAuthn).build();
    }

    public AgentContext withMatchedRoute(RouteDefinition newRoute) {
        return this.toBuilder().matchedRoute(newRoute).build();
    }

    public AgentContext withObligations(Obligations newObligations) {
        return this.toBuilder().obligations(newObligations).build();
    }

    public AgentContext withTenantId(String newTenantId) {
        return this.toBuilder().tenantId(newTenantId).build();
    }

    public AgentContext withTrace(String newTraceId, String newSpanId) {
        return this.toBuilder().traceId(newTraceId).spanId(newSpanId).build();
    }

    public AgentContext withAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.attributes != null ? this.attributes : Collections.emptyMap());
        newAttributes.put(key, value);
        return this.toBuilder().attributes(Collections.unmodifiableMap(newAttributes)).build();
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return attributes != null ? (T) attributes.get(key) : null;
    }
}
