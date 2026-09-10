package com.phaselume.torana.connector.nosql.mongodb;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches ReactiveMongoTemplate instances per connector configuration.
 */
public class ReactiveMongoClientFactory {

    private static final Logger log = LoggerFactory.getLogger(ReactiveMongoClientFactory.class);

    private final Map<String, ReactiveMongoTemplate> templateCache = new ConcurrentHashMap<>();

    public ReactiveMongoTemplate getTemplate(ConnectorConfig config) {
        String id = (config != null && config.getId() != null) ? config.getId() : "default-mongo";
        return templateCache.computeIfAbsent(id, k -> createTemplate(config));
    }

    public ReactiveMongoTemplate createTemplate(ConnectorConfig config) {
        Map<String, Object> props = (config != null && config.getProperties() != null)
                ? config.getProperties()
                : Map.of();

        String uri = props.getOrDefault("uri", "mongodb://localhost:27017/torana").toString();
        String database = props.getOrDefault("database", "torana").toString();

        MongoClient client = MongoClients.create(uri);
        SimpleReactiveMongoDatabaseFactory databaseFactory = new SimpleReactiveMongoDatabaseFactory(client, database);

        log.info("Initialized ReactiveMongoTemplate for uri={}", uri);
        return new ReactiveMongoTemplate(databaseFactory);
    }
}
