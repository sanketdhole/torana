package com.phaselume.torana.observability.audit;

import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.spi.AuditSink;
import com.phaselume.torana.observability.audit.model.AuditEventBuilder;
import com.phaselume.torana.observability.audit.sink.AuditSinkRegistry;
import com.phaselume.torana.observability.audit.sink.CompositeAuditSink;
import com.phaselume.torana.observability.audit.sink.LogAuditSink;
import com.phaselume.torana.observability.audit.sink.RedisStreamAuditSink;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuditSinkTest {

    @Test
    void testAuditEventBuilderSha256() {
        String hash = AuditEventBuilder.computeSha256("hello world".getBytes(StandardCharsets.UTF_8));
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void testLogAuditSinkPublish() {
        LogAuditSink sink = new LogAuditSink();
        AuditEvent event = AuditEvent.builder()
                .eventId("evt-1")
                .timestamp(Instant.now())
                .tenantId("tenant-a")
                .principalId("user-1")
                .action("EXECUTE_TOOL")
                .status("SUCCESS")
                .httpStatus(200)
                .latencyMillis(25)
                .build();

        StepVerifier.create(sink.publish(event))
                .verifyComplete();
    }

    @Test
    void testRedisStreamAuditSinkPublish() {
        ReactiveStringRedisTemplate redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        ReactiveStreamOperations streamOps = Mockito.mock(ReactiveStreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        when(streamOps.add(any())).thenReturn(Mono.just(RecordId.of("1600000000000-0")));

        RedisStreamAuditSink sink = new RedisStreamAuditSink(redisTemplate, "torana:audit:test", null);
        AuditEvent event = AuditEvent.builder()
                .eventId("evt-2")
                .tenantId("tenant-b")
                .principalId("agent-1")
                .status("SUCCESS")
                .httpStatus(200)
                .build();

        StepVerifier.create(sink.publish(event))
                .verifyComplete();
    }

    @Test
    void testCompositeAuditSinkAndRegistry() {
        AtomicBoolean sink1Called = new AtomicBoolean(false);
        AtomicBoolean sink2Called = new AtomicBoolean(false);

        AuditSink sink1 = new AuditSink() {
            @Override
            public String type() { return "sink1"; }
            @Override
            public Mono<Void> publish(AuditEvent event) {
                sink1Called.set(true);
                return Mono.empty();
            }
        };

        AuditSink sink2 = new AuditSink() {
            @Override
            public String type() { return "sink2"; }
            @Override
            public Mono<Void> publish(AuditEvent event) {
                sink2Called.set(true);
                return Mono.empty();
            }
        };

        AuditSinkRegistry registry = new AuditSinkRegistry(List.of(sink1, sink2));
        assertEquals(2, registry.getAll().size());
        assertTrue(registry.get("sink1").isPresent());
        assertTrue(registry.get("sink2").isPresent());
        assertFalse(registry.get("nonexistent").isPresent());

        CompositeAuditSink composite = registry.toComposite();
        AuditEvent event = AuditEvent.builder().eventId("evt-3").build();

        StepVerifier.create(composite.publish(event))
                .verifyComplete();

        assertTrue(sink1Called.get());
        assertTrue(sink2Called.get());
    }
}
