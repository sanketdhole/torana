package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Thrown when authorization policy denies the request.
 */
public class AuthorizationException extends ToranaException {

    private final String denyReason;

    public AuthorizationException(String message, String denyReason) {
        super("AUTHZ_DENIED", message, HttpStatus.FORBIDDEN, null, Map.of("denyReason", denyReason != null ? denyReason : ""));
        this.denyReason = denyReason;
    }

    public AuthorizationException(String message, String denyReason, Map<String, Object> details) {
        super("AUTHZ_DENIED", message, HttpStatus.FORBIDDEN, null, details);
        this.denyReason = denyReason;
    }

    public String getDenyReason() {
        return denyReason;
    }
}
