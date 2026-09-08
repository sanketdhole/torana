package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Structured security and compliance audit record.
 * Never stores unmasked raw payloads — stores SHA-256 hashes, timestamps, outcomes, and latencies.
 */
@Value
@Builder(toBuilder = true)
public class AuditEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    Instant timestamp = Instant.now();

    String tenantId;
    String principalId;
    String clientIp;
    String routeId;
    String connectorId;
    String action; // "EXECUTE_TOOL", "QUERY_DATABASE", "INVOKE_LLM"
    String status; // "SUCCESS", "DENIED", "RATE_LIMITED", "ERROR"
    int httpStatus;
    long latencyMillis;
    String payloadHashSha256;
    String traceId;
    String spanId;

    @Builder.Default
    Map<String, Object> metadata = Collections.emptyMap();
}
