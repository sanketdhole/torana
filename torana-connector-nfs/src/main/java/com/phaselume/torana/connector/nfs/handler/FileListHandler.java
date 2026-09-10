package com.phaselume.torana.connector.nfs.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.nfs.buffer.ContentTypeDetector;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentResponse;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles LIST_DIRECTORY operation.
 */
public class FileListHandler {

    private static final Logger log = LoggerFactory.getLogger(FileListHandler.class);

    private final ContentTypeDetector contentTypeDetector;
    private final ObjectMapper objectMapper;

    public FileListHandler(ContentTypeDetector contentTypeDetector, ObjectMapper objectMapper) {
        this.contentTypeDetector = contentTypeDetector != null ? contentTypeDetector : new ContentTypeDetector();
        ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
        mapper.findAndRegisterModules();
        this.objectMapper = mapper;
    }

    public FileListHandler() {
        this(new ContentTypeDetector(), new ObjectMapper());
    }

    @Value
    @Builder
    public static class FileItem {
        String name;
        String path;
        boolean directory;
        long size;
        Instant lastModified;
        String contentType;
    }

    public Flux<AgentResponse.Chunk> listDirectory(Path dirPath) {
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return Flux.error(new ConnectorException("nfs", "Directory not found or is not a directory: " + dirPath));
        }

        List<FileItem> items = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                boolean isDir = Files.isDirectory(entry);
                long size = isDir ? 0 : Files.size(entry);
                Instant lastModified = Files.getLastModifiedTime(entry).toInstant();
                String contentType = isDir ? "inode/directory" : contentTypeDetector.detectContentType(entry);

                items.add(FileItem.builder()
                        .name(entry.getFileName().toString())
                        .path(entry.toString())
                        .directory(isDir)
                        .size(size)
                        .lastModified(lastModified)
                        .contentType(contentType)
                        .build());
            }

            String json = objectMapper.writeValueAsString(Map.of(
                    "directory", dirPath.toString(),
                    "entries", items,
                    "count", items.size()
            ));

            AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                    .textDelta(json)
                    .data(json.getBytes(StandardCharsets.UTF_8))
                    .last(true)
                    .finishReason("stop")
                    .metadata(Map.of("itemCount", items.size()))
                    .build();

            return Flux.just(chunk);
        } catch (Exception e) {
            log.error("Failed to list directory {}: {}", dirPath, e.getMessage());
            return Flux.error(new ConnectorException("nfs", "Failed to list directory: " + e.getMessage(), e));
        }
    }
}
