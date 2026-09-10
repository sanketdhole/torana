package com.phaselume.torana.connector.nfs.buffer;

import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Reads a filesystem path in configurable byte chunks on lightweight threads.
 */
public class ChunkedFileReader {

    private static final Logger log = LoggerFactory.getLogger(ChunkedFileReader.class);

    private final Executor executor;
    private final ContentTypeDetector contentTypeDetector;

    public ChunkedFileReader(Executor executor, ContentTypeDetector contentTypeDetector) {
        this.executor = executor != null ? executor : new VirtualThreadExecutor();
        this.contentTypeDetector = contentTypeDetector != null ? contentTypeDetector : new ContentTypeDetector();
    }

    public ChunkedFileReader() {
        this(new VirtualThreadExecutor(), new ContentTypeDetector());
    }

    public Flux<AgentResponse.Chunk> readFile(Path path, int chunkSize) {
        int bufferSize = chunkSize > 0 ? chunkSize : 64 * 1024;
        String contentType = contentTypeDetector.detectContentType(path);

        return Flux.create(sink -> {
            executor.execute(() -> {
                try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    int index = 0;

                    while (channel.read(buffer) > 0) {
                        if (sink.isCancelled()) {
                            log.debug("Sink cancelled during file read: {}", path);
                            return;
                        }

                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        buffer.clear();

                        AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                                .index(index++)
                                .data(bytes)
                                .textDelta(new String(bytes, StandardCharsets.UTF_8))
                                .last(false)
                                .metadata(Map.of("contentType", contentType))
                                .build();

                        sink.next(chunk);
                    }

                    sink.next(AgentResponse.Chunk.last("stop"));
                    sink.complete();
                } catch (IOException e) {
                    log.error("Error reading file {}: {}", path, e.getMessage());
                    sink.error(new ConnectorException("nfs", "Failed to read file: " + e.getMessage(), e));
                }
            });
        });
    }
}
