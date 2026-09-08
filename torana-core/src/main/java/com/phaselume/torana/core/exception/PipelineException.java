package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Thrown when an error occurs during execution of a pipeline step.
 */
public class PipelineException extends ToranaException {

    private final String stepType;

    public PipelineException(String stepType, String message) {
        super("PIPELINE_EXECUTION_FAILED", message, HttpStatus.INTERNAL_SERVER_ERROR, null, Map.of("stepType", stepType != null ? stepType : "unknown"));
        this.stepType = stepType;
    }

    public PipelineException(String stepType, String message, Throwable cause) {
        super("PIPELINE_EXECUTION_FAILED", message, HttpStatus.INTERNAL_SERVER_ERROR, cause, Map.of("stepType", stepType != null ? stepType : "unknown"));
        this.stepType = stepType;
    }

    public String getStepType() {
        return stepType;
    }
}
