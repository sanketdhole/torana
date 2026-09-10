package com.phaselume.torana.connector.nosql.mongodb;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reactive MongoDB backend connector for document store queries.
 */
public class MongoBackendConnector implements BackendConnector {

    private static final Logger log = LoggerFactory.getLogger(MongoBackendConnector.class);

    private final ReactiveMongoClientFactory clientFactory;
    private final MongoQueryBuilder queryBuilder;
    private final MongoOperationGuard operationGuard;
    private final MongoResultSerializer resultSerializer;
    private final ResilienceDecorator resilienceDecorator;

    public MongoBackendConnector(ReactiveMongoClientFactory clientFactory,
                                 MongoQueryBuilder queryBuilder,
                                 MongoOperationGuard operationGuard,
                                 MongoResultSerializer resultSerializer,
                                 ResilienceDecorator resilienceDecorator) {
        this.clientFactory = clientFactory != null ? clientFactory : new ReactiveMongoClientFactory();
        this.queryBuilder = queryBuilder != null ? queryBuilder : new MongoQueryBuilder();
        this.operationGuard = operationGuard != null ? operationGuard : new MongoOperationGuard();
        this.resultSerializer = resultSerializer != null ? resultSerializer : new MongoResultSerializer();
        this.resilienceDecorator = resilienceDecorator;
    }

    public MongoBackendConnector() {
        this(new ReactiveMongoClientFactory(), new MongoQueryBuilder(), new MongoOperationGuard(), new MongoResultSerializer(), null);
    }

    @Override
    public String type() {
        return "mongodb";
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        if (config == null || config.getType() == null) return false;
        String t = config.getType().toLowerCase();
        return "mongodb".equals(t) || "mongo".equals(t) || "nosql".equals(t);
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        if (config == null) {
            return Flux.error(new IllegalArgumentException("ConnectorConfig must not be null"));
        }

        String defaultCollection = extractDefaultCollection(config);
        MongoQueryBuilder.MongoRequest req = queryBuilder.buildRequest(context, defaultCollection);

        List<String> allowedOps = extractList(config.getProperties(), "allowedOperations", List.of("find", "findOne", "count", "aggregate"));
        List<String> allowedCollections = extractList(config.getProperties(), "allowedCollections", List.of());

        operationGuard.validate(req.getOperation(), req.getCollection(), allowedOps, allowedCollections);

        String profileName = getResilienceProfile(config);

        if (resilienceDecorator != null) {
            return resilienceDecorator.decorate(profileName, null, () -> executeMongo(config, req));
        }

        return executeMongo(config, req);
    }

    private Flux<AgentResponse.Chunk> executeMongo(ConnectorConfig config, MongoQueryBuilder.MongoRequest req) {
        ReactiveMongoTemplate template = clientFactory.getTemplate(config);
        String op = req.getOperation().toLowerCase();

        switch (op) {
            case "count":
                return template.count(req.getQuery(), req.getCollection())
                        .map(resultSerializer::serializeCount)
                        .flux();

            case "findone":
                return template.findOne(req.getQuery(), Document.class, req.getCollection())
                        .map(doc -> {
                            AgentResponse.Chunk chunk = resultSerializer.serializeDocument(doc);
                            return chunk.toBuilder().last(true).finishReason("stop").build();
                        })
                        .switchIfEmpty(Mono.just(AgentResponse.Chunk.builder().textDelta("{}").last(true).finishReason("stop").build()))
                        .flux();

            case "find":
            default:
                return template.find(req.getQuery(), Document.class, req.getCollection())
                        .map(resultSerializer::serializeDocument)
                        .concatWith(Flux.just(AgentResponse.Chunk.last("stop")));
        }
    }

    private String extractDefaultCollection(ConnectorConfig config) {
        if (config != null && config.getProperties() != null && config.getProperties().containsKey("collection")) {
            return config.getProperties().get("collection").toString();
        }
        return "default";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> props, String key, List<String> fallback) {
        if (props == null || !props.containsKey(key)) return fallback;
        Object val = props.get(key);
        if (val instanceof List) {
            return (List<String>) val;
        }
        return fallback;
    }

    private String getResilienceProfile(ConnectorConfig config) {
        if (config != null && config.getProperties() != null && config.getProperties().containsKey("resilienceProfile")) {
            return config.getProperties().get("resilienceProfile").toString();
        }
        return "default";
    }
}
