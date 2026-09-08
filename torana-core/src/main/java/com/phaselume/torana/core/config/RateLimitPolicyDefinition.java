package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition of a named rate limit policy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicyDefinition {

    private String name;

    @Builder.Default
    private long requestsPerMinute = 60;

    @Builder.Default
    private long burstCapacity = 100;

    @Builder.Default
    private String keyStrategy = "PRINCIPAL_AND_TENANT"; // "IP", "PRINCIPAL", "TENANT", "PRINCIPAL_AND_TENANT", "ROUTE"

    @Builder.Default
    private String onExceedAction = "REJECT"; // "REJECT", "QUEUE"
}
