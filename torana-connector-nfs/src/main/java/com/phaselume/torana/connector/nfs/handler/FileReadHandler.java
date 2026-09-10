package com.phaselume.torana.connector.nfs.handler;

import com.phaselume.torana.connector.nfs.buffer.ChunkedFileReader;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentResponse;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles READ_FILE operation streaming.
 */
public class FileReadHandler {

    private final ChunkedFileReader fileReader;

    public FileReadHandler(ChunkedFileReader fileReader) {
        this.fileReader = fileReader != null ? fileReader : new ChunkedFileReader();
    }

    public FileReadHandler() {
        this(new ChunkedFileReader());
    }

    public Flux<AgentResponse.Chunk> readFile(Path path, int chunkSize, long maxSizeBytes) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return Flux.error(new ConnectorException("nfs", "File not found or is not a regular file: " + path));
        }

        try {
            long size = Files.size(path);
            if (maxSizeBytes > 0 && size > maxSizeBytes) {
                return Flux.error(new ConnectorException("nfs", "File size (" + size + " bytes) exceeds maximum limit (" + maxSizeBytes + " bytes)"));
            }
        } catch (Exception e) {
            return Flux.error(new ConnectorException("nfs", "Could not check file size: " + e.getMessage(), e));
        }

        return fileReader.readFile(path, chunkSize);
    }
}
