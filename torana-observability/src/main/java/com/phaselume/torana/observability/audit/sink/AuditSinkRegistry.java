package com.phaselume.torana.observability.audit.sink;

import com.phaselume.torana.core.spi.AuditSink;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of available AuditSink implementations.
 */
public class AuditSinkRegistry {

    private final Map<String, AuditSink> sinks = new ConcurrentHashMap<>();

    public AuditSinkRegistry() {
    }

    public AuditSinkRegistry(Collection<AuditSink> sinks) {
        if (sinks != null) {
            sinks.forEach(this::register);
        }
    }

    public void register(AuditSink sink) {
        if (sink != null && sink.type() != null) {
            sinks.put(sink.type().toLowerCase(), sink);
        }
    }

    public Optional<AuditSink> get(String type) {
        if (type == null) return Optional.empty();
        return Optional.ofNullable(sinks.get(type.toLowerCase()));
    }

    public Collection<AuditSink> getAll() {
        return sinks.values();
    }

    public CompositeAuditSink toComposite() {
        return new CompositeAuditSink(sinks.values().stream().toList());
    }
}
