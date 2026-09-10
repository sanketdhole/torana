package com.phaselume.torana.connector.s3.client;

import com.phaselume.torana.connector.s3.model.S3ConnectorConfig;
import com.phaselume.torana.connector.s3.model.S3ObjectReference;
import com.phaselume.torana.core.exception.ConnectorException;

import java.util.List;

/**
 * Security guard for S3 operations and path prefixes.
 */
public class S3OperationGuard {

    public void validate(S3ConnectorConfig config, S3ObjectReference ref) {
        if (config == null || ref == null) return;

        // 1. Operation Allow-list Check
        String op = ref.getOperation() != null ? ref.getOperation().toUpperCase() : "GET_OBJECT";
        List<String> allowedOps = config.getAllowedOperations() != null ? config.getAllowedOperations() : List.of("GET_OBJECT", "LIST_OBJECTS");

        boolean allowed = allowedOps.stream().anyMatch(allowedOp -> allowedOp.equalsIgnoreCase(op));
        if (!allowed) {
            throw new ConnectorException("s3", "Operation '" + op + "' is not permitted on this S3 connector. Allowed operations: " + allowedOps);
        }

        // 2. Path Prefix Isolation Check
        String configuredPrefix = config.getPathPrefix();
        if (configuredPrefix != null && !configuredPrefix.isBlank()) {
            String key = ref.getKey();
            if (key != null && !key.startsWith(configuredPrefix)) {
                throw new ConnectorException("s3", "Access denied: Key '" + key + "' is outside permitted path prefix '" + configuredPrefix + "'");
            }
            String reqPrefix = ref.getPrefix();
            if (reqPrefix != null && !reqPrefix.startsWith(configuredPrefix)) {
                throw new ConnectorException("s3", "Access denied: Query prefix '" + reqPrefix + "' is outside permitted path prefix '" + configuredPrefix + "'");
            }
        }
    }
}
