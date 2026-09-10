package com.phaselume.torana.ratelimit.redis;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.ratelimit.model.RateLimitKey;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves rate-limiting keys and discriminators from the AgentContext.
 */
public class RateLimitKeyResolver {

    public List<RateLimitKey> resolveKeys(AgentContext context, String keyStrategy) {
        List<RateLimitKey> keys = new ArrayList<>();
        if (keyStrategy == null || keyStrategy.isBlank()) {
            keyStrategy = "PRINCIPAL_AND_TENANT";
        }

        String routeId = resolveRouteId(context);

        // Support comma or pipe separated strategies if multiple are configured
        String[] strategies = keyStrategy.split("[,|]");
        for (String strategy : strategies) {
            String strat = strategy.trim();
            if (strat.isEmpty()) continue;

            String discriminator = resolveDiscriminator(context, strat);
            keys.add(RateLimitKey.of(strat, routeId, discriminator));
        }

        if (keys.isEmpty()) {
            keys.add(RateLimitKey.of("default", routeId, "anonymous"));
        }

        return keys;
    }

    public String resolveDiscriminator(AgentContext context, String strategy) {
        if (context == null) {
            return "anonymous";
        }

        ToranaAuthentication auth = context.getAuthentication();
        AgentRequest req = context.getRequest();
        HttpHeaders headers = (req != null && req.getHeaders() != null) ? req.getHeaders() : new HttpHeaders();

        String strat = strategy.toLowerCase();

        if (strat.contains("tenant") && strat.contains("user") || "principal_and_tenant".equalsIgnoreCase(strategy)) {
            String tenant = getTenant(context, auth, headers);
            String user = getUser(auth, headers);
            return tenant + ":" + user;
        }

        if (strat.contains("user") || strat.contains("principal")) {
            return getUser(auth, headers);
        }

        if (strat.contains("tenant")) {
            return getTenant(context, auth, headers);
        }

        if (strat.contains("api-key") || strat.contains("apikey")) {
            String apiKey = headers.getFirst("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                return apiKey;
            }
            if (auth != null && auth.getRawToken() != null) {
                return auth.getRawToken();
            }
            return "anonymous-key";
        }

        if (strat.contains("ip")) {
            String forwarded = headers.getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = headers.getFirst("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            Object clientIpAttr = context.getAttribute("clientIp");
            if (clientIpAttr != null) {
                return String.valueOf(clientIpAttr);
            }
            return "127.0.0.1";
        }

        if (strat.contains("route")) {
            return resolveRouteId(context);
        }

        // Fallback default
        return getUser(auth, headers);
    }

    private String resolveRouteId(AgentContext context) {
        if (context == null) return "global";
        if (context.getMatchedRoute() != null && context.getMatchedRoute().getId() != null) {
            return context.getMatchedRoute().getId();
        }
        String routeAttr = context.getAttribute("routeId");
        if (routeAttr != null && !routeAttr.isBlank()) {
            return routeAttr;
        }
        return "global";
    }

    private String getUser(ToranaAuthentication auth, HttpHeaders headers) {
        if (auth != null && auth.getPrincipalId() != null && !auth.getPrincipalId().isBlank()) {
            return auth.getPrincipalId();
        }
        String userHeader = headers.getFirst("X-User-Id");
        if (userHeader != null && !userHeader.isBlank()) {
            return userHeader;
        }
        return "anonymous";
    }

    private String getTenant(AgentContext context, ToranaAuthentication auth, HttpHeaders headers) {
        if (context != null && context.getTenantId() != null && !context.getTenantId().isBlank()) {
            return context.getTenantId();
        }
        if (auth != null && auth.getTenantId() != null && !auth.getTenantId().isBlank()) {
            return auth.getTenantId();
        }
        String tenantHeader = headers.getFirst("X-Tenant-Id");
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            return tenantHeader;
        }
        return "default-tenant";
    }
}
