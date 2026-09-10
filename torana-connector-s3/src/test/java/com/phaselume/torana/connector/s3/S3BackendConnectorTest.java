package com.phaselume.torana.connector.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.s3.client.S3AsyncClientFactory;
import com.phaselume.torana.connector.s3.client.S3OperationGuard;
import com.phaselume.torana.connector.s3.model.S3ConnectorConfig;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class S3BackendConnectorTest {

    private S3AsyncClientFactory clientFactory;
    private S3AsyncClient s3AsyncClient;
    private S3BackendConnector connector;

    @BeforeEach
    void setUp() {
        clientFactory = Mockito.mock(S3AsyncClientFactory.class);
        s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        when(clientFactory.getClient(any(), any())).thenReturn(s3AsyncClient);

        connector = new S3BackendConnector(
                clientFactory,
                new S3OperationGuard(),
                null,
                new ObjectMapper()
        );
    }

    @Test
    void testSupports() {
        assertEquals("s3", connector.type());
        assertTrue(connector.supports(ConnectorConfig.builder().type("s3").build()));
        assertFalse(connector.supports(ConnectorConfig.builder().type("http").build()));
    }

    @Test
    void testListObjects() {
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .name("test-bucket")
                .prefix("docs/")
                .contents(List.of(
                        S3Object.builder()
                                .key("docs/file1.txt")
                                .size(1024L)
                                .lastModified(Instant.now())
                                .eTag("\"etag123\"")
                                .build()
                ))
                .isTruncated(false)
                .build();

        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        AgentContext context = AgentContext.builder()
                .attributes(Map.of("operation", "LIST_OBJECTS", "bucket", "test-bucket"))
                .build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("s3-conn")
                .type("s3")
                .properties(Map.of("bucket", "test-bucket"))
                .build();

        List<AgentResponse.Chunk> chunks = connector.execute(context, config).collectList().block();

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).isLast());
        assertTrue(chunks.get(0).getTextDelta().contains("docs/file1.txt"));
    }
}
