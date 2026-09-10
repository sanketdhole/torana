package com.phaselume.torana.streaming.backpressure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Configuration for reactive streaming backpressure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackpressureConfig {

    public enum Strategy {
        BUFFER,
        DROP,
        LATEST,
        ERROR
    }

    @Builder.Default
    private Strategy strategy = Strategy.BUFFER;

    @Builder.Default
    private int bufferSize = 256;

    @Builder.Default
    private Duration slowConsumerTimeout = Duration.ofSeconds(60);
}
