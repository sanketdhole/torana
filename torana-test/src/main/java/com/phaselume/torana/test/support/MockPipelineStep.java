package com.phaselume.torana.test.support;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.spi.PipelineStep;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Mock implementation of PipelineStep that tracks invocations and allows context manipulation.
 */
public class MockPipelineStep implements PipelineStep {

    private final String type;
    private Function<AgentContext, AgentContext> contextTransformer = Function.identity();
    private Throwable failure;
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final List<AgentContext> recordedContexts = Collections.synchronizedList(new ArrayList<>());

    public MockPipelineStep(String type) {
        this.type = type;
    }

    public MockPipelineStep transforms(Function<AgentContext, AgentContext> transformer) {
        this.contextTransformer = transformer != null ? transformer : Function.identity();
        this.failure = null;
        return this;
    }

    public MockPipelineStep setsAttribute(String key, Object value) {
        this.contextTransformer = ctx -> ctx.withAttribute(key, value);
        this.failure = null;
        return this;
    }

    public MockPipelineStep failsWith(Throwable failure) {
        this.failure = failure;
        return this;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Mono<AgentContext> execute(AgentContext context) {
        executionCount.incrementAndGet();
        recordedContexts.add(context);

        if (failure != null) {
            return Mono.error(failure);
        }

        AgentContext transformed = contextTransformer.apply(context);
        return Mono.just(transformed);
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public List<AgentContext> getRecordedContexts() {
        return Collections.unmodifiableList(recordedContexts);
    }

    public void reset() {
        this.executionCount.set(0);
        this.recordedContexts.clear();
        this.contextTransformer = Function.identity();
        this.failure = null;
    }
}
