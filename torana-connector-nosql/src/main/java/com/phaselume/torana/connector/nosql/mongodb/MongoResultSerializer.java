package com.phaselume.torana.connector.nosql.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentResponse;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Serializes BSON Document results into JSON AgentResponse chunks.
 */
public class MongoResultSerializer {

    private static final Logger log = LoggerFactory.getLogger(MongoResultSerializer.class);
    private final ObjectMapper objectMapper;

    public MongoResultSerializer(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
        mapper.findAndRegisterModules();
        this.objectMapper = mapper;
    }

    public MongoResultSerializer() {
        this(new ObjectMapper());
    }

    public AgentResponse.Chunk serializeDocument(Document document) {
        if (document == null) {
            return AgentResponse.Chunk.text("{}");
        }

        try {
            String json = document.toJson();
            return AgentResponse.Chunk.builder()
                    .textDelta(json)
                    .data(json.getBytes(StandardCharsets.UTF_8))
                    .last(false)
                    .build();
        } catch (Exception e) {
            log.error("Failed to serialize Document to JSON: {}", e.getMessage());
            return AgentResponse.Chunk.text("{}");
        }
    }

    public AgentResponse.Chunk serializeCount(long count) {
        String json = String.format("{\"count\":%d}", count);
        return AgentResponse.Chunk.builder()
                .textDelta(json)
                .data(json.getBytes(StandardCharsets.UTF_8))
                .last(true)
                .finishReason("stop")
                .build();
    }
}
