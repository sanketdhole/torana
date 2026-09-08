package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when no route matches the incoming request path/method/protocol.
 */
public class RouteNotFoundException extends ToranaException {

    public RouteNotFoundException(String path, String protocol) {
        super("ROUTE_NOT_FOUND", "No route found for path '" + path + "' and protocol '" + protocol + "'", HttpStatus.NOT_FOUND, null, Map.of("path", path, "protocol", protocol));
    }
}
