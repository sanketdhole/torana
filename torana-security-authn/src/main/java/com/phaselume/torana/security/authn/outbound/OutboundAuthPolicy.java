package com.phaselume.torana.security.authn.outbound;

/**
 * Declarative strategies for authenticating egress requests to internal cloud services.
 */
public enum OutboundAuthPolicy {

    /**
     * Mint a short-lived internal JWT signed by Torana containing caller identity, tenant, and roles.
     */
    MINT_INTERNAL_JWT,

    /**
     * Forward the caller's raw inbound Bearer token downstream.
     */
    FORWARD_CALLER_TOKEN,

    /**
     * Inject a configured service API key or shared secret.
     */
    INJECT_API_KEY,

    /**
     * Propagate caller identity via sanitized HTTP headers (X-Torana-User, X-Torana-Tenant, X-Torana-Roles).
     */
    PROPAGATE_IDENTITY_HEADERS,

    /**
     * Do not inject or modify outbound authentication credentials.
     */
    NONE;

    public static OutboundAuthPolicy fromString(String val) {
        if (val == null || val.isBlank()) {
            return MINT_INTERNAL_JWT;
        }
        try {
            return OutboundAuthPolicy.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MINT_INTERNAL_JWT;
        }
    }
}
