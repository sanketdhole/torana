package com.phaselume.torana.security.authn.filter;

/**
 * Exception thrown when Torana authentication fails or credentials are required.
 */
public class ToranaAuthException extends RuntimeException {

    private final String errorCode;

    public ToranaAuthException(String message) {
        this("UNAUTHORIZED", message);
    }

    public ToranaAuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "UNAUTHORIZED";
    }

    public ToranaAuthException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "UNAUTHORIZED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
