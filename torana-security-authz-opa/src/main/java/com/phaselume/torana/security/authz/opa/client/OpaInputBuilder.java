package com.phaselume.torana.security.authz.opa.client;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ToranaAuthentication;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Serializes {@link AgentContext} and request metadata into the OPA standard input JSON document.
 */
public class OpaInputBuilder {

    /**
     * Builds the OPA input map from an {@link AgentContext}.
     */
    public Map<String, Object> buildInput(AgentContext context) {
        Map<String, Object> input = new HashMap<>();

        if (context == null) {
            return input;
        }

        // 1. Principal Information
        ToranaAuthentication auth = context.getAuthentication();
        Map<String, Object> principalMap = new HashMap<>();
        if (auth != null) {
            principalMap.put("principal_id", auth.getPrincipalId());
            principalMap.put("name", auth.getName() != null ? auth.getName() : auth.getPrincipalId());
            principalMap.put("tenant_id", auth.getTenantId() != null ? auth.getTenantId() : "default");
            principalMap.put("auth_method", auth.getAuthMethod() != null ? auth.getAuthMethod() : "unknown");
            principalMap.put("roles", auth.getRoles() != null ? auth.getRoles() : Collections.emptySet());
            principalMap.put("scopes", auth.getScopes() != null ? auth.getScopes() : Collections.emptySet());
            principalMap.put("claims", auth.getClaims() != null ? auth.getClaims() : Collections.emptyMap());
        } else {
            principalMap.put("principal_id", "anonymous");
            principalMap.put("name", "Anonymous User");
            principalMap.put("tenant_id", context.getTenantId() != null ? context.getTenantId() : "default");
            principalMap.put("auth_method", "anonymous");
            principalMap.put("roles", Collections.emptySet());
            principalMap.put("scopes", Collections.emptySet());
            principalMap.put("claims", Collections.emptyMap());
        }
        input.put("principal", principalMap);

        // 2. Route Information
        RouteDefinition route = context.getMatchedRoute();
        Map<String, Object> routeMap = new HashMap<>();
        if (route != null) {
            routeMap.put("id", route.getId());
            routeMap.put("path", route.getPath());
            routeMap.put("protocols", route.getProtocols() != null ? route.getProtocols() : Collections.emptySet());
            routeMap.put("backend_ref", route.getBackendRef());
            routeMap.put("auth_ref", route.getAuthRef());
            routeMap.put("metadata", route.getMetadata() != null ? route.getMetadata() : Collections.emptyMap());
        } else {
            routeMap.put("id", "unmatched-route");
            routeMap.put("path", context.getRequest() != null ? context.getRequest().getPath() : "");
            routeMap.put("protocols", Collections.emptySet());
            routeMap.put("metadata", Collections.emptyMap());
        }
        input.put("route", routeMap);

        // 3. Request Information
        AgentRequest req = context.getRequest();
        Map<String, Object> requestMap = new HashMap<>();
        if (req != null) {
            requestMap.put("id", req.getId());
            requestMap.put("protocol", req.getProtocol() != null ? req.getProtocol() : "http");
            requestMap.put("method", req.getMethod() != null ? req.getMethod().name() : "UNKNOWN");
            requestMap.put("path", req.getPath());
            requestMap.put("headers", sanitizeHeaders(req.getHeaders()));
            requestMap.put("query_params", sanitizeQueryParams(req.getQueryParams()));

            String clientIp = req.getAttribute("client_ip");
            if (clientIp == null && req.getRawExchange() != null && req.getRawExchange().getRequest().getRemoteAddress() != null) {
                clientIp = req.getRawExchange().getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            requestMap.put("client_ip", clientIp != null ? clientIp : "");

            if (req.getCachedBody() != null && req.getCachedBody().length > 0) {
                requestMap.put("body_hash", computeSha256(req.getCachedBody()));
                requestMap.put("body_size", req.getCachedBody().length);
            }
        }
        input.put("request", requestMap);

        // 4. Environment Information
        Map<String, Object> envMap = new HashMap<>();
        envMap.put("timestamp", Instant.now().toString());
        envMap.put("trace_id", context.getTraceId() != null ? context.getTraceId() : "");
        envMap.put("span_id", context.getSpanId() != null ? context.getSpanId() : "");
        input.put("environment", envMap);

        return input;
    }

    private Map<String, String> sanitizeHeaders(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> safe = new HashMap<>();
        headers.forEach((k, v) -> {
            String lower = k.toLowerCase();
            // Filter sensitive secrets
            if (!lower.contains("authorization") && !lower.contains("password") && !lower.contains("secret")) {
                safe.put(k, String.join(",", v));
            }
        });
        return safe;
    }

    private Map<String, String> sanitizeQueryParams(MultiValueMap<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> safe = new HashMap<>();
        params.forEach((k, v) -> {
            String lower = k.toLowerCase();
            if (!lower.contains("token") && !lower.contains("secret") && !lower.contains("key")) {
                safe.put(k, String.join(",", v));
            }
        });
        return safe;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return "";
        }
    }
}
