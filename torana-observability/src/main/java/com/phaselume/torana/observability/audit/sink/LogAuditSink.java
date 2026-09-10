package com.phaselume.torana.observability.audit.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.spi.AuditSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Default Audit Sink: outputs structured JSON audit records to the SLF4J logger "torana.audit".
 */
public class LogAuditSink implements AuditSink {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("torana.audit");
    private final ObjectMapper objectMapper;

    public LogAuditSink() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public LogAuditSink(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String type() {
        return "log";
    }

    @Override
    public Mono<Void> publish(AuditEvent event) {
        return Mono.fromRunnable(() -> {
            try {
                String json = objectMapper.writeValueAsString(event);
                AUDIT_LOG.info(json);
            } catch (Exception e) {
                AUDIT_LOG.error("Failed to serialize audit event: {}", event.getEventId(), e);
            }
        });
    }
}
