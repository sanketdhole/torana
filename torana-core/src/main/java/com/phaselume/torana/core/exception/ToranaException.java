package com.phaselume.torana.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Collections;
import java.util.Map;

/**
 * Base runtime exception for all Torana errors.
 */
@Getter
public class ToranaException extends RuntimeException {

    private final String errorCode;
    private final HttpStatusCode status;
    private final Map<String, Object> details;

    public ToranaException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR, null, Collections.emptyMap());
    }

    public ToranaException(String errorCode, String message, HttpStatusCode status) {
        this(errorCode, message, status, null, Collections.emptyMap());
    }

    public ToranaException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR, cause, Collections.emptyMap());
    }

    public ToranaException(String errorCode, String message, HttpStatusCode status, Throwable cause, Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
        this.details = details != null ? details : Collections.emptyMap();
    }
}
