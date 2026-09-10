package com.phaselume.torana.connector.nosql.mongodb;

import com.phaselume.torana.core.exception.ConnectorException;

import java.util.List;
import java.util.Locale;

/**
 * Security guard for MongoDB operations and collection access.
 */
public class MongoOperationGuard {

    public void validate(String operation, String collection, List<String> allowedOperations, List<String> allowedCollections) {
        // 1. Operation check
        String op = (operation != null && !operation.isBlank()) ? operation.trim().toLowerCase(Locale.ROOT) : "find";
        List<String> allowedOps = (allowedOperations != null && !allowedOperations.isEmpty())
                ? allowedOperations.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList()
                : List.of("find", "aggregate", "count", "findone");

        if (!allowedOps.contains(op)) {
            throw new ConnectorException("mongodb", "Operation '" + op + "' is not permitted on MongoDB connector. Allowed: " + allowedOps);
        }

        // 2. Collection check
        if (allowedCollections != null && !allowedCollections.isEmpty()) {
            if (collection == null || collection.isBlank()) {
                throw new ConnectorException("mongodb", "Collection name must be specified");
            }
            boolean colAllowed = allowedCollections.stream()
                    .anyMatch(c -> c.equalsIgnoreCase(collection));
            if (!colAllowed) {
                throw new ConnectorException("mongodb", "Access denied: Collection '" + collection + "' is not in allowed collections list: " + allowedCollections);
            }
        }
    }
}
