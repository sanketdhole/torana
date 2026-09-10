package com.phaselume.torana.connector.jdbc.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Extracts structured SQL queries and parameter bindings from incoming AgentContext.
 */
public class SqlQueryExtractor {

    private static final Logger log = LoggerFactory.getLogger(SqlQueryExtractor.class);
    private final ObjectMapper objectMapper;

    public SqlQueryExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public SqlQueryExtractor() {
        this(new ObjectMapper());
    }

    @Value
    @Builder
    public static class ExtractedQuery {
        String sql;
        Map<String, Object> parameters;
    }

    public ExtractedQuery extract(AgentContext context) {
        if (context == null) {
            return ExtractedQuery.builder().sql("").parameters(Collections.emptyMap()).build();
        }

        // 1. Check attribute directly
        Object queryAttr = context.getAttribute("sql");
        if (queryAttr == null) {
            queryAttr = context.getAttribute("query");
        }

        if (queryAttr != null) {
            Map<String, Object> params = context.getAttribute("parameters");
            if (params == null) {
                params = context.getAttribute("params");
            }
            return ExtractedQuery.builder()
                    .sql(queryAttr.toString())
                    .parameters(params != null ? params : Collections.emptyMap())
                    .build();
        }

        // 2. Check JSON request body
        AgentRequest req = context.getRequest();
        if (req != null && req.getCachedBody() != null && req.getCachedBody().length > 0) {
            try {
                String bodyStr = new String(req.getCachedBody(), StandardCharsets.UTF_8).trim();
                if (bodyStr.startsWith("{")) {
                    Map<String, Object> bodyMap = objectMapper.readValue(bodyStr, new TypeReference<>() {});
                    String sql = null;
                    if (bodyMap.containsKey("query")) sql = String.valueOf(bodyMap.get("query"));
                    if (bodyMap.containsKey("sql")) sql = String.valueOf(bodyMap.get("sql"));

                    Map<String, Object> params = Collections.emptyMap();
                    if (bodyMap.get("parameters") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> p = (Map<String, Object>) bodyMap.get("parameters");
                        params = p;
                    } else if (bodyMap.get("params") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> p = (Map<String, Object>) bodyMap.get("params");
                        params = p;
                    }

                    if (sql != null && !sql.isBlank()) {
                        return ExtractedQuery.builder().sql(sql).parameters(params).build();
                    }
                } else if (!bodyStr.isEmpty()) {
                    return ExtractedQuery.builder().sql(bodyStr).parameters(Collections.emptyMap()).build();
                }
            } catch (Exception e) {
                log.debug("Could not parse request body as JSON query: {}", e.getMessage());
            }
        }

        // 3. Check query param
        if (req != null && req.getQueryParams() != null) {
            String q = req.getQueryParams().getFirst("query");
            if (q == null) q = req.getQueryParams().getFirst("sql");
            if (q != null && !q.isBlank()) {
                return ExtractedQuery.builder().sql(q).parameters(Collections.emptyMap()).build();
            }
        }

        return ExtractedQuery.builder().sql("").parameters(Collections.emptyMap()).build();
    }
}
