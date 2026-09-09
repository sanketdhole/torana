package com.phaselume.torana.security.authn.outbound;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.OutboundAuthnConfig;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.OutboundAuthnConfig.ServiceOutboundConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * Applies outbound authentication policies when dispatching requests to internal cloud services.
 */
public class OutboundAuthPolicyEngine {

    private final OutboundAuthnConfig config;
    private final InternalJwtMinter jwtMinter;

    public OutboundAuthPolicyEngine(OutboundAuthnConfig config, InternalJwtMinter jwtMinter) {
        this.config = config != null ? config : new OutboundAuthnConfig();
        this.jwtMinter = jwtMinter != null ? jwtMinter : new InternalJwtMinter(this.config.getJwtSigner());
    }

    public OutboundAuthPolicyEngine() {
        this(new OutboundAuthnConfig(), new InternalJwtMinter());
    }

    /**
     * Resolves the effective outbound authentication policy for a target service ID.
     */
    public OutboundAuthPolicy resolvePolicy(String serviceId) {
        if (!config.isEnabled()) {
            return OutboundAuthPolicy.NONE;
        }

        if (serviceId != null && config.getServices() != null && config.getServices().containsKey(serviceId)) {
            ServiceOutboundConfig serviceConfig = config.getServices().get(serviceId);
            if (serviceConfig.getPolicy() != null && !serviceConfig.getPolicy().isBlank()) {
                return OutboundAuthPolicy.fromString(serviceConfig.getPolicy());
            }
        }

        return OutboundAuthPolicy.fromString(config.getDefaultPolicy());
    }

    /**
     * Injects authentication credentials and identity headers into the given outbound HTTP headers.
     */
    public void applyOutboundAuth(HttpHeaders headers, String serviceId, ToranaAuthentication auth) {
        if (headers == null || !config.isEnabled()) {
            return;
        }

        OutboundAuthPolicy policy = resolvePolicy(serviceId);
        ServiceOutboundConfig serviceConfig = (serviceId != null && config.getServices() != null)
                ? config.getServices().get(serviceId)
                : null;

        String audience = serviceConfig != null && serviceConfig.getAudience() != null
                ? serviceConfig.getAudience()
                : (serviceId != null ? serviceId : "internal-service");

        switch (policy) {
            case MINT_INTERNAL_JWT -> {
                String token = jwtMinter.mintToken(auth, audience);
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            case FORWARD_CALLER_TOKEN -> {
                if (auth != null && auth.getRawToken() != null && !auth.getRawToken().isBlank()) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + auth.getRawToken());
                }
            }
            case INJECT_API_KEY -> {
                if (serviceConfig != null) {
                    String headerName = serviceConfig.getApiKeyHeader() != null ? serviceConfig.getApiKeyHeader() : "X-Api-Key";
                    String headerVal = serviceConfig.getApiKeyValue();
                    if (headerVal != null) {
                        headers.set(headerName, headerVal);
                    }
                    if (serviceConfig.getBearerToken() != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + serviceConfig.getBearerToken());
                    }
                }
            }
            case PROPAGATE_IDENTITY_HEADERS -> {
                if (auth != null) {
                    if (auth.getPrincipalId() != null) headers.set("X-Torana-User", auth.getPrincipalId());
                    if (auth.getTenantId() != null) headers.set("X-Torana-Tenant", auth.getTenantId());
                    if (auth.getRoles() != null && !auth.getRoles().isEmpty()) {
                        headers.set("X-Torana-Roles", String.join(",", auth.getRoles()));
                    }
                }
            }
            case NONE -> {
                // Do not modify auth headers
            }
        }

        // Apply any custom static headers defined for the service
        if (serviceConfig != null && serviceConfig.getCustomHeaders() != null) {
            for (Map.Entry<String, String> entry : serviceConfig.getCustomHeaders().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.set(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public InternalJwtMinter getJwtMinter() {
        return jwtMinter;
    }
}
