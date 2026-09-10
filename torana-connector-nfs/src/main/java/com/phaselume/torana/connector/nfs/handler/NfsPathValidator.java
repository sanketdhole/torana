package com.phaselume.torana.connector.nfs.handler;

import com.phaselume.torana.core.exception.ConnectorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates file paths against root-path boundaries to prevent directory traversal and symlink escapes.
 */
public class NfsPathValidator {

    public Path validateAndResolve(String rootPathStr, String relativePathStr) {
        if (rootPathStr == null || rootPathStr.isBlank()) {
            throw new ConnectorException("nfs", "Configured root-path cannot be null or empty");
        }

        Path rootPath = Paths.get(rootPathStr).toAbsolutePath().normalize();
        String relPath = (relativePathStr != null) ? relativePathStr.trim() : "";

        // Strip leading slashes to prevent resolving from filesystem root
        while (relPath.startsWith("/") || relPath.startsWith("\\")) {
            relPath = relPath.substring(1);
        }

        Path targetPath = rootPath.resolve(relPath).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new ConnectorException("nfs", "Path traversal detected: Requested path is outside root boundary");
        }

        try {
            File targetFile = targetPath.toFile();
            if (targetFile.exists()) {
                String canonicalRoot = rootPath.toFile().getCanonicalPath();
                String canonicalTarget = targetFile.getCanonicalPath();
                if (!canonicalTarget.startsWith(canonicalRoot)) {
                    throw new ConnectorException("nfs", "Symlink traversal escape detected: Target resolves outside root boundary");
                }
            }
        } catch (IOException e) {
            throw new ConnectorException("nfs", "Could not verify path boundaries: " + e.getMessage(), e);
        }

        return targetPath;
    }
}
