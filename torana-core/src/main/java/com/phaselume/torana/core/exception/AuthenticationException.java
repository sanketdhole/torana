package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Thrown when credentials are missing, malformed, or invalid.
 */
public class AuthenticationException extends ToranaException {

    public AuthenticationException(String message) {
        super("AUTHN_FAILED", message, HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationException(String message, Throwable cause) {
        super("AUTHN_FAILED", message, HttpStatus.UNAUTHORIZED, cause, Collections.emptyMap());
    }

    public AuthenticationException(String message, Map<String, Object> details) {
        super("AUTHN_FAILED", message, HttpStatus.UNAUTHORIZED, null, details);
    }
}
