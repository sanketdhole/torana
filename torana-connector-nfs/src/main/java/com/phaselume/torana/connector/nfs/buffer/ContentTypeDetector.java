package com.phaselume.torana.connector.nfs.buffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects MIME types for filesystem objects.
 */
public class ContentTypeDetector {

    public String detectContentType(Path path) {
        if (path == null) {
            return "application/octet-stream";
        }

        try {
            String probe = Files.probeContentType(path);
            if (probe != null && !probe.isBlank()) {
                return probe;
            }
        } catch (IOException ignored) {}

        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".md")) return "text/markdown";
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".csv")) return "text/csv";
        if (fileName.endsWith(".xml")) return "application/xml";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";

        return "application/octet-stream";
    }
}
