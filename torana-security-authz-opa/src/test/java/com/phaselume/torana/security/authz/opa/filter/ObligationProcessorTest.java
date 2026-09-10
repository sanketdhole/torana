package com.phaselume.torana.security.authz.opa.filter;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.security.authz.opa.model.OpaObligation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObligationProcessorTest {

    @Test
    void testProcessObligations() {
        ObligationProcessor processor = new ObligationProcessor();

        List<OpaObligation> obligations = List.of(
                OpaObligation.builder()
                        .type("mask-field")
                        .params(Map.of("field", "ssn", "mask", "HASH"))
                        .build(),
                OpaObligation.builder()
                        .type("row-filter")
                        .params(Map.of("field", "tenant_id", "value", "acme-corp"))
                        .build(),
                OpaObligation.builder()
                        .type("max-rows")
                        .params(Map.of("limit", 100))
                        .build(),
                OpaObligation.builder()
                        .type("allowed-prefix")
                        .params(Map.of("prefix", "/api/v1/tools/safe"))
                        .build(),
                OpaObligation.builder()
                        .type("inject-header")
                        .params(Map.of("header", "X-Custom", "value", "123"))
                        .build()
        );

        AgentContext.Obligations result = processor.process(obligations);
        assertNotNull(result);

        assertEquals("HASH", result.getColumnMasks().get("ssn"));
        assertEquals("acme-corp", result.getRowFilters().get("tenant_id"));
        assertEquals(100, result.getMaxRows());
        assertTrue(result.getAllowedPathPrefixes().contains("/api/v1/tools/safe"));
        assertTrue(result.getAdditionalObligations().containsKey("inject-header"));
    }
}
