package com.phaselume.torana.observability.audit.sink;

import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.spi.AuditSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite fan-out sink that dispatches audit events to all configured sinks concurrently.
 */
public class CompositeAuditSink implements AuditSink {

    private static final Logger log = LoggerFactory.getLogger(CompositeAuditSink.class);

    private final List<AuditSink> sinks;

    public CompositeAuditSink(List<AuditSink> sinks) {
        this.sinks = (sinks != null) ? new ArrayList<>(sinks) : Collections.emptyList();
    }

    @Override
    public String type() {
        return "composite";
    }

    @Override
    public Mono<Void> publish(AuditEvent event) {
        if (sinks.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sinks)
                .flatMap(sink -> sink.publish(event)
                        .onErrorResume(e -> {
                            log.error("Audit sink [{}] failed for event [{}]", sink.type(), event.getEventId(), e);
                            return Mono.empty();
                        }))
                .then();
    }

    public List<AuditSink> getSinks() {
        return Collections.unmodifiableList(sinks);
    }
}
