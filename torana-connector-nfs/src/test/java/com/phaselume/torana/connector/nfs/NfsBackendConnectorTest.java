package com.phaselume.torana.connector.nfs;

import com.phaselume.torana.connector.nfs.buffer.ChunkedFileReader;
import com.phaselume.torana.connector.nfs.buffer.ContentTypeDetector;
import com.phaselume.torana.connector.nfs.buffer.VirtualThreadExecutor;
import com.phaselume.torana.connector.nfs.handler.FileListHandler;
import com.phaselume.torana.connector.nfs.handler.FileReadHandler;
import com.phaselume.torana.connector.nfs.handler.NfsOperationGuard;
import com.phaselume.torana.connector.nfs.handler.NfsPathValidator;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NfsBackendConnectorTest {

    private NfsBackendConnector connector;

    @BeforeEach
    void setUp() {
        VirtualThreadExecutor executor = new VirtualThreadExecutor();
        ContentTypeDetector contentTypeDetector = new ContentTypeDetector();
        ChunkedFileReader chunkedFileReader = new ChunkedFileReader(executor, contentTypeDetector);

        connector = new NfsBackendConnector(
                new NfsPathValidator(),
                new NfsOperationGuard(),
                new FileReadHandler(chunkedFileReader),
                new FileListHandler(),
                null
        );
    }

    @Test
    void testSupports() {
        assertEquals("nfs", connector.type());
        assertTrue(connector.supports(ConnectorConfig.builder().type("nfs").build()));
        assertTrue(connector.supports(ConnectorConfig.builder().type("file").build()));
        assertTrue(connector.supports(ConnectorConfig.builder().type("nas").build()));
        assertFalse(connector.supports(ConnectorConfig.builder().type("http").build()));
    }

    @Test
    void testReadFileStreamsCorrectly(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Hello Torana NFS Connector Chunk Streaming");

        AgentRequest req = AgentRequest.builder()
                .path("sample.txt")
                .build();

        AgentContext context = AgentContext.builder()
                .request(req)
                .attributes(Map.of("operation", "READ_FILE"))
                .build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("nfs-test")
                .type("nfs")
                .properties(Map.of("rootPath", tempDir.toString()))
                .build();

        List<AgentResponse.Chunk> chunks = connector.execute(context, config).collectList().block();

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(chunks.size() - 1).isLast());
        assertTrue(chunks.get(0).getTextDelta().contains("Hello Torana NFS"));
    }

    @Test
    void testListDirectory(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("file1.txt"), "A");
        Files.writeString(tempDir.resolve("file2.pdf"), "B");

        AgentContext context = AgentContext.builder()
                .attributes(Map.of("operation", "LIST_DIRECTORY", "path", ""))
                .build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("nfs-test")
                .type("nfs")
                .properties(Map.of("rootPath", tempDir.toString()))
                .build();

        List<AgentResponse.Chunk> chunks = connector.execute(context, config).collectList().block();

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).isLast());
        assertTrue(chunks.get(0).getTextDelta().contains("file1.txt"));
        assertTrue(chunks.get(0).getTextDelta().contains("file2.pdf"));
    }
}
