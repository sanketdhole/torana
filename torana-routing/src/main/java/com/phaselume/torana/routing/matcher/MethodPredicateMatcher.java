package com.phaselume.torana.routing.matcher;

import org.springframework.http.HttpMethod;

import java.util.Set;

/**
 * Validates request HTTP methods against allowed route methods.
 */
public class MethodPredicateMatcher {

    /**
     * Checks if the incoming method matches the allowed methods set on the route.
     * An empty or null set implies all methods are allowed.
     */
    public boolean matches(Set<HttpMethod> allowedMethods, HttpMethod requestMethod) {
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            return true;
        }
        if (requestMethod == null) {
            return false;
        }
        return allowedMethods.contains(requestMethod);
    }
}
