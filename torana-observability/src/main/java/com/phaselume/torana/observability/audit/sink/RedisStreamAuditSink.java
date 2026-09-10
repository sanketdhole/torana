package com.phaselume.torana.observability.audit.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.spi.AuditSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Publishes AuditEvent JSON to a Redis Stream via XADD.
 */
public class RedisStreamAuditSink implements AuditSink {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamAuditSink.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final String streamKey;
    private final ObjectMapper objectMapper;

    public RedisStreamAuditSink(ReactiveStringRedisTemplate redisTemplate) {
        this(redisTemplate, "torana:audit:stream", null);
    }

    public RedisStreamAuditSink(ReactiveStringRedisTemplate redisTemplate, String streamKey, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.streamKey = (streamKey != null && !streamKey.isBlank()) ? streamKey : "torana:audit:stream";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String type() {
        return "redis-stream";
    }

    @Override
    public Mono<Void> publish(AuditEvent event) {
        if (redisTemplate == null) {
            log.warn("ReactiveStringRedisTemplate is not configured, skipping Redis stream audit publishing");
            return Mono.empty();
        }

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> {
                    MapRecord<String, String, String> record = MapRecord.create(
                            streamKey,
                            Collections.singletonMap("event", json)
                    );
                    return redisTemplate.opsForStream().add(record);
                })
                .doOnError(e -> log.error("Failed to publish audit event {} to Redis stream {}", event.getEventId(), streamKey, e))
                .then();
    }
}
