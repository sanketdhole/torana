package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable snapshot of a named rate limiting policy.
 */
@Value
@Builder(toBuilder = true)
public class RateLimitPolicy {

    String name;

    @Builder.Default
    long requestsPerMinute = 60;

    @Builder.Default
    long burstCapacity = 100;

    @Builder.Default
    String keyStrategy = "PRINCIPAL_AND_TENANT"; // "IP", "PRINCIPAL", "TENANT", "PRINCIPAL_AND_TENANT", "ROUTE"

    @Builder.Default
    String onExceedAction = "REJECT"; // "REJECT", "QUEUE"

    public static RateLimitPolicy defaultPolicy() {
        return RateLimitPolicy.builder().name("default").build();
    }
}
