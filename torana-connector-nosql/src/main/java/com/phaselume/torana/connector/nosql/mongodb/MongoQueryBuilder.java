package com.phaselume.torana.connector.nosql.mongodb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import lombok.Builder;
import lombok.Value;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Builds Spring Data Mongo Query objects from AgentContext.
 */
public class MongoQueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(MongoQueryBuilder.class);
    private final ObjectMapper objectMapper;

    public MongoQueryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public MongoQueryBuilder() {
        this(new ObjectMapper());
    }

    @Value
    @Builder
    public static class MongoRequest {
        String collection;
        String operation; // find, findOne, count, aggregate
        Query query;
        Document filter;
        int limit;
        int skip;
    }

    public MongoRequest buildRequest(AgentContext context, String defaultCollection) {
        String collection = defaultCollection;
        String operation = "find";
        Document filterDoc = new Document();
        Document projectionDoc = new Document();
        int limit = 100;
        int skip = 0;

        if (context != null) {
            // Check context attributes
            Object colAttr = context.getAttribute("collection");
            if (colAttr != null) collection = colAttr.toString();

            Object opAttr = context.getAttribute("operation");
            if (opAttr != null) operation = opAttr.toString();

            Object limitAttr = context.getAttribute("limit");
            if (limitAttr != null) {
                try { limit = Integer.parseInt(limitAttr.toString()); } catch (NumberFormatException ignored) {}
            }

            // Check request body
            AgentRequest req = context.getRequest();
            if (req != null && req.getCachedBody() != null && req.getCachedBody().length > 0) {
                try {
                    String bodyStr = new String(req.getCachedBody(), StandardCharsets.UTF_8).trim();
                    if (bodyStr.startsWith("{")) {
                        Map<String, Object> bodyMap = objectMapper.readValue(bodyStr, new TypeReference<>() {});
                        if (bodyMap.containsKey("collection")) {
                            collection = String.valueOf(bodyMap.get("collection"));
                        }
                        if (bodyMap.containsKey("operation")) {
                            operation = String.valueOf(bodyMap.get("operation"));
                        }
                        if (bodyMap.get("filter") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> f = (Map<String, Object>) bodyMap.get("filter");
                            filterDoc = new Document(f);
                        }
                        if (bodyMap.get("projection") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> p = (Map<String, Object>) bodyMap.get("projection");
                            projectionDoc = new Document(p);
                        }
                        if (bodyMap.containsKey("limit")) {
                            try { limit = Integer.parseInt(bodyMap.get("limit").toString()); } catch (NumberFormatException ignored) {}
                        }
                        if (bodyMap.containsKey("skip")) {
                            try { skip = Integer.parseInt(bodyMap.get("skip").toString()); } catch (NumberFormatException ignored) {}
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not parse request body as Mongo JSON query: {}", e.getMessage());
                }
            }
        }

        BasicQuery basicQuery = new BasicQuery(filterDoc, projectionDoc);
        if (limit > 0) basicQuery.limit(limit);
        if (skip > 0) basicQuery.skip(skip);

        return MongoRequest.builder()
                .collection(collection != null ? collection : "default")
                .operation(operation != null ? operation : "find")
                .query(basicQuery)
                .filter(filterDoc)
                .limit(limit)
                .skip(skip)
                .build();
    }
}
