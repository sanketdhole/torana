package com.phaselume.torana.pipeline.model;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Encapsulates the complete result of executing a pipeline.
 */
@Value
@Builder(toBuilder = true)
public class PipelineResult {

    String pipelineName;
    AgentContext context;
    AgentResponse response;
    Duration executionDuration;
    boolean success;
    Throwable error;

    public static PipelineResult success(String pipelineName, AgentContext context, AgentResponse response, Duration duration) {
        return PipelineResult.builder()
                .pipelineName(pipelineName)
                .context(context)
                .response(response)
                .executionDuration(duration)
                .success(true)
                .build();
    }

    public static PipelineResult failure(String pipelineName, AgentContext context, Throwable error, Duration duration) {
        return PipelineResult.builder()
                .pipelineName(pipelineName)
                .context(context)
                .executionDuration(duration)
                .success(false)
                .error(error)
                .build();
    }
}
