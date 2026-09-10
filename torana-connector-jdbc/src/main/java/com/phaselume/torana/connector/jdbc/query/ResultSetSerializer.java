package com.phaselume.torana.connector.jdbc.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentResponse;
import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes R2DBC Result stream into JSON AgentResponse chunks.
 */
public class ResultSetSerializer {

    private static final Logger log = LoggerFactory.getLogger(ResultSetSerializer.class);
    private final ObjectMapper objectMapper;

    public ResultSetSerializer(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
        mapper.findAndRegisterModules();
        this.objectMapper = mapper;
    }

    public ResultSetSerializer() {
        this(new ObjectMapper());
    }

    public Flux<AgentResponse.Chunk> serialize(Result result) {
        return Flux.from(result.map(this::extractRowMap))
                .map(this::rowToChunk)
                .concatWith(Flux.just(AgentResponse.Chunk.last("stop")));
    }

    private Map<String, Object> extractRowMap(Row row, RowMetadata rowMetadata) {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        for (ColumnMetadata col : rowMetadata.getColumnMetadatas()) {
            String name = col.getName();
            Object value = row.get(name);
            rowMap.put(name, value);
        }
        return rowMap;
    }

    private AgentResponse.Chunk rowToChunk(Map<String, Object> rowMap) {
        try {
            String json = objectMapper.writeValueAsString(rowMap);
            return AgentResponse.Chunk.builder()
                    .textDelta(json)
                    .data(json.getBytes(StandardCharsets.UTF_8))
                    .last(false)
                    .build();
        } catch (Exception e) {
            log.error("Failed to serialize row to JSON: {}", e.getMessage());
            return AgentResponse.Chunk.text("{}");
        }
    }
}
