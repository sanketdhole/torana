package com.phaselume.torana.connector.nosql.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MongoQueryBuilderTest {

    private MongoQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new MongoQueryBuilder(new ObjectMapper());
    }

    @Test
    void testBuildRequestFromAttributes() {
        AgentContext context = AgentContext.builder()
                .attributes(Map.of("collection", "customers", "operation", "count", "limit", 25))
                .build();

        MongoQueryBuilder.MongoRequest req = queryBuilder.buildRequest(context, "default");

        assertEquals("customers", req.getCollection());
        assertEquals("count", req.getOperation());
        assertEquals(25, req.getLimit());
    }

    @Test
    void testBuildRequestFromJsonBody() {
        String json = "{\"collection\":\"documents\",\"operation\":\"find\",\"filter\":{\"status\":\"published\"},\"limit\":50}";
        AgentRequest request = AgentRequest.builder()
                .cachedBody(json.getBytes(StandardCharsets.UTF_8))
                .build();

        AgentContext context = AgentContext.builder()
                .request(request)
                .build();

        MongoQueryBuilder.MongoRequest req = queryBuilder.buildRequest(context, "default");

        assertEquals("documents", req.getCollection());
        assertEquals("find", req.getOperation());
        assertEquals("published", req.getFilter().get("status"));
        assertEquals(50, req.getLimit());
    }
}
