package com.phaselume.torana.connector.nfs.handler;

import com.phaselume.torana.core.exception.ConnectorException;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Guards operations and file extensions for NFS connector.
 */
public class NfsOperationGuard {

    public void validate(String operation, Path path, List<String> allowedOperations, List<String> allowedExtensions) {
        String op = (operation != null && !operation.isBlank()) ? operation.trim().toUpperCase(Locale.ROOT) : "READ_FILE";

        List<String> allowedOps = (allowedOperations != null && !allowedOperations.isEmpty())
                ? allowedOperations.stream().map(s -> s.toUpperCase(Locale.ROOT)).toList()
                : List.of("READ_FILE", "LIST_DIRECTORY");

        if (!allowedOps.contains(op)) {
            throw new ConnectorException("nfs", "Operation '" + op + "' is not permitted. Allowed: " + allowedOps);
        }

        if ("READ_FILE".equals(op) && allowedExtensions != null && !allowedExtensions.isEmpty() && path != null) {
            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            boolean extensionAllowed = allowedExtensions.stream()
                    .anyMatch(ext -> fileName.endsWith(ext.toLowerCase(Locale.ROOT)));

            if (!extensionAllowed) {
                throw new ConnectorException("nfs", "File extension for '" + fileName + "' is not in allowed extensions list: " + allowedExtensions);
            }
        }
    }
}
