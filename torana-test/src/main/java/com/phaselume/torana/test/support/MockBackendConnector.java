package com.phaselume.torana.test.support;

import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock implementation of BackendConnector for testing routing, resilience, and pipelines.
 */
public class MockBackendConnector implements BackendConnector {

    private final String type;
    private List<AgentResponse.Chunk> stubbedChunks = new ArrayList<>();
    private Throwable stubbedError;
    private final AtomicInteger executionCount = new AtomicInteger(0);

    public MockBackendConnector() {
        this("mock-connector");
    }

    public MockBackendConnector(String type) {
        this.type = type;
    }

    public MockBackendConnector(String type, List<AgentResponse.Chunk> stubbedChunks) {
        this(type);
        returnsChunks(stubbedChunks);
    }

    public MockBackendConnector returnsChunks(List<AgentResponse.Chunk> chunks) {
        this.stubbedChunks = chunks != null ? new ArrayList<>(chunks) : new ArrayList<>();
        this.stubbedError = null;
        return this;
    }

    public MockBackendConnector returnsText(String text) {
        this.stubbedChunks = List.of(
                AgentResponse.Chunk.text(text),
                AgentResponse.Chunk.last("stop")
        );
        this.stubbedError = null;
        return this;
    }

    public MockBackendConnector failsWith(Throwable error) {
        this.stubbedError = error;
        this.stubbedChunks.clear();
        return this;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        return config != null && type.equalsIgnoreCase(config.getType());
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        executionCount.incrementAndGet();
        if (stubbedError != null) {
            return Flux.error(stubbedError);
        }
        return Flux.fromIterable(stubbedChunks);
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public void reset() {
        this.executionCount.set(0);
        this.stubbedChunks.clear();
        this.stubbedError = null;
    }
}
