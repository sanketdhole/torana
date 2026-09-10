package com.phaselume.torana.observability.audit.model;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.model.ToranaAuthentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Fluent builder for compliance AuditEvent objects.
 */
public class AuditEventBuilder {

    public static AuditEvent fromContext(AgentContext context, String action, String status, int httpStatus, long latencyMillis) {
        String tenantId = "default";
        String principalId = "anonymous";
        String clientIp = "127.0.0.1";
        String traceId = null;
        String spanId = null;
        String routeId = "unknown";

        if (context != null) {
            tenantId = context.getTenantId() != null ? context.getTenantId() : "default";
            traceId = context.getTraceId();
            spanId = context.getSpanId();

            if (context.getMatchedRoute() != null) {
                routeId = context.getMatchedRoute().getId();
            }

            ToranaAuthentication auth = context.getAuthentication();
            if (auth != null && auth.getPrincipalId() != null) {
                principalId = auth.getPrincipalId();
            }

            Object clientIpAttr = context.getAttribute("clientIp");
            if (clientIpAttr != null) {
                clientIp = clientIpAttr.toString();
            }
        }

        return AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId(tenantId)
                .principalId(principalId)
                .clientIp(clientIp)
                .routeId(routeId)
                .action(action != null ? action : "GATEWAY_INVOCATION")
                .status(status != null ? status : "SUCCESS")
                .httpStatus(httpStatus)
                .latencyMillis(latencyMillis)
                .traceId(traceId)
                .spanId(spanId)
                .build();
    }

    public static String computeSha256(byte[] data) {
        if (data == null || data.length == 0) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
