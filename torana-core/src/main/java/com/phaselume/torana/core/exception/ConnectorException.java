package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an error occurs during invocation of a backend connector.
 */
public class ConnectorException extends ToranaException {

    private final String connectorType;

    public ConnectorException(String message) {
        this("unknown", message, null);
    }

    public ConnectorException(String message, Throwable cause) {
        this("unknown", message, cause);
    }

    public ConnectorException(String connectorType, String message) {
        this(connectorType, message, null);
    }

    public ConnectorException(String connectorType, String message, Throwable cause) {
        super("CONNECTOR_ERROR", message, HttpStatus.BAD_GATEWAY, cause, Map.of("connectorType", connectorType != null ? connectorType : "unknown"));
        this.connectorType = connectorType;
    }

    public String getConnectorType() {
        return connectorType;
    }
}
