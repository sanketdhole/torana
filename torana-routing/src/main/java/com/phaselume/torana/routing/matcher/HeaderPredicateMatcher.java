package com.phaselume.torana.routing.matcher;

import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates request HTTP headers against required route header criteria.
 */
public class HeaderPredicateMatcher {

    /**
     * Checks if the request headers satisfy all required route header predicates.
     */
    public boolean matches(Map<String, String> requiredHeaders, HttpHeaders requestHeaders) {
        if (requiredHeaders == null || requiredHeaders.isEmpty()) {
            return true;
        }
        if (requestHeaders == null) {
            return false;
        }

        for (Map.Entry<String, String> entry : requiredHeaders.entrySet()) {
            String headerName = entry.getKey();
            String expectedValue = entry.getValue();

            String actualValue = requestHeaders.getFirst(headerName);
            if (actualValue == null) {
                return false;
            }

            // If expected value is "*" or empty, presence of the header is sufficient
            if ("*".equals(expectedValue) || expectedValue.isEmpty()) {
                continue;
            }

            // Regex or exact match
            if (expectedValue.startsWith("^") || expectedValue.endsWith("$") || expectedValue.contains(".*")) {
                if (!Pattern.matches(expectedValue, actualValue)) {
                    return false;
                }
            } else if (!expectedValue.equalsIgnoreCase(actualValue)) {
                return false;
            }
        }

        return true;
    }
}
