package com.phaselume.torana.connector.jdbc.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqlQueryExtractorTest {

    private SqlQueryExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new SqlQueryExtractor(new ObjectMapper());
    }

    @Test
    void testExtractFromContextAttributes() {
        AgentContext context = AgentContext.builder()
                .attributes(Map.of(
                        "sql", "SELECT * FROM orders WHERE user_id = :userId",
                        "params", Map.of("userId", "u-123")
                ))
                .build();

        SqlQueryExtractor.ExtractedQuery result = extractor.extract(context);

        assertEquals("SELECT * FROM orders WHERE user_id = :userId", result.getSql());
        assertEquals("u-123", result.getParameters().get("userId"));
    }

    @Test
    void testExtractFromRequestBody() {
        String json = "{\"query\":\"SELECT * FROM products WHERE price < :maxPrice\",\"parameters\":{\"maxPrice\":50}}";
        AgentRequest request = AgentRequest.builder()
                .cachedBody(json.getBytes(StandardCharsets.UTF_8))
                .build();

        AgentContext context = AgentContext.builder()
                .request(request)
                .build();

        SqlQueryExtractor.ExtractedQuery result = extractor.extract(context);

        assertEquals("SELECT * FROM products WHERE price < :maxPrice", result.getSql());
        assertEquals(50, result.getParameters().get("maxPrice"));
    }
}
