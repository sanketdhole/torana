package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when YAML configuration fails validation or contains invalid references.
 */
public class ConfigurationException extends ToranaException {

    public ConfigurationException(String message) {
        super("INVALID_CONFIGURATION", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ConfigurationException(String message, Map<String, Object> details) {
        super("INVALID_CONFIGURATION", message, HttpStatus.INTERNAL_SERVER_ERROR, null, details);
    }
}
