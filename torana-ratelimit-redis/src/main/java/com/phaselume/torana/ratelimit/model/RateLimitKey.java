package com.phaselume.torana.ratelimit.model;

import lombok.Builder;
import lombok.Value;

/**
 * Computed Redis rate limit key containing the strategy, route, and discriminator.
 */
@Value
@Builder
public class RateLimitKey {

    String strategy;
    String routeId;
    String discriminator;
    String redisKey;

    public static RateLimitKey of(String strategy, String routeId, String discriminator) {
        String route = (routeId != null && !routeId.isBlank()) ? routeId : "global";
        String disc = (discriminator != null && !discriminator.isBlank()) ? discriminator : "anonymous";
        String strat = (strategy != null && !strategy.isBlank()) ? strategy : "default";

        String key = String.format("torana:rl:%s:%s:%s", strat, route, disc);
        return RateLimitKey.builder()
                .strategy(strat)
                .routeId(route)
                .discriminator(disc)
                .redisKey(key)
                .build();
    }
}
