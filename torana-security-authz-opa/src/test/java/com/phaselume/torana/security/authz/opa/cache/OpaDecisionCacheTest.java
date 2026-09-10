package com.phaselume.torana.security.authz.opa.cache;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties.OpaCacheProperties;
import com.phaselume.torana.security.authz.opa.model.OpaObligation;
import com.phaselume.torana.security.authz.opa.model.OpaResponse;
import com.phaselume.torana.security.authz.opa.model.OpaResult;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpaDecisionCacheTest {

    @Test
    void testCachePutGetClear() {
        OpaCacheProperties cacheProps = OpaCacheProperties.builder()
                .enabled(true)
                .ttlSeconds(5)
                .build();

        OpaDecisionCache cache = new OpaDecisionCache(cacheProps);

        String policy = "torana.v1";
        Map<String, Object> input = Map.of("principal", Map.of("name", "alice"));

        OpaResponse response = OpaResponse.builder()
                .decisionId("dec-123")
                .result(OpaResult.builder()
                        .allow(true)
                        .obligations(List.of(new OpaObligation("mask-field", Map.of("field", "email"))))
                        .build())
                .build();

        // Initial miss
        StepVerifier.create(cache.get(policy, input))
                .verifyComplete();

        // Put in cache
        StepVerifier.create(cache.put(policy, input, response))
                .verifyComplete();

        // Cache hit
        StepVerifier.create(cache.get(policy, input))
                .assertNext(cached -> {
                    assertNotNull(cached);
                    assertTrue(cached.isAllowed());
                    assertEquals("dec-123", cached.getDecisionId());
                })
                .verifyComplete();

        // Clear local
        cache.clearLocal();

        // Cache miss again
        StepVerifier.create(cache.get(policy, input))
                .verifyComplete();
    }
}
