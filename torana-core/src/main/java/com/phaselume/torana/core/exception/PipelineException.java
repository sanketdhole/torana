package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Map;

/**
 * Thrown when an error occurs during execution of a pipeline step or pipeline orchestration.
 */
public class PipelineException extends ToranaException {

    public enum ErrorCode {
        NOT_FOUND,
        TIMEOUT,
        EXECUTION_FAILED,
        INVALID_DEFINITION
    }

    private final String stepType;
    private final ErrorCode pipelineErrorCode;

    public PipelineException(String message) {
        this("unknown", message, ErrorCode.EXECUTION_FAILED, null);
    }

    public PipelineException(String message, ErrorCode errorCode) {
        this("unknown", message, errorCode, null);
    }

    public PipelineException(String message, ErrorCode errorCode, Throwable cause) {
        this("unknown", message, errorCode, cause);
    }

    public PipelineException(String stepType, String message) {
        this(stepType, message, ErrorCode.EXECUTION_FAILED, null);
    }

    public PipelineException(String stepType, String message, Throwable cause) {
        this(stepType, message, ErrorCode.EXECUTION_FAILED, cause);
    }

    public PipelineException(String stepType, String message, ErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.name() : "PIPELINE_EXECUTION_FAILED",
                message,
                resolveHttpStatus(errorCode),
                cause,
                Map.of("stepType", stepType != null ? stepType : "unknown"));
        this.stepType = stepType;
        this.pipelineErrorCode = errorCode;
    }

    private static HttpStatusCode resolveHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    public String getStepType() {
        return stepType;
    }

    public ErrorCode getPipelineErrorCode() {
        return pipelineErrorCode;
    }
}
