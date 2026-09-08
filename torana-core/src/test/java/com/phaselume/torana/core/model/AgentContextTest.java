package com.phaselume.torana.core.model;

import com.phaselume.torana.core.config.RouteDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentContextTest {

    @Test
    void testContextCreationAndImmutability() {
        AgentRequest request = AgentRequest.builder()
                .id("req-1")
                .protocol("mcp")
                .method(HttpMethod.POST)
                .path("/mcp/v1")
                .build();

        ToranaAuthentication authn = ToranaAuthentication.builder()
                .principalId("user-123")
                .name("Alice")
                .tenantId("acme-corp")
                .roles(Set.of("admin", "developer"))
                .build();

        RouteDefinition route = RouteDefinition.builder()
                .id("mcp-route")
                .path("/mcp/v1")
                .pipelineRef("mcp-pipeline")
                .build();

        AgentContext context = AgentContext.builder()
                .tenantId("acme-corp")
                .request(request)
                .authentication(authn)
                .matchedRoute(route)
                .build();

        assertNotNull(context.getId());
        assertEquals("acme-corp", context.getTenantId());
        assertEquals("user-123", context.getAuthentication().getPrincipalId());
        assertTrue(context.getAuthentication().hasRole("admin"));
        assertFalse(context.getAuthentication().hasRole("finance"));
        assertEquals("mcp-route", context.getMatchedRoute().getId());

        // Test withTenantId copy-on-write
        AgentContext updated = context.withTenantId("globex-corp");
        assertEquals("globex-corp", updated.getTenantId());
        assertEquals("acme-corp", context.getTenantId()); // Original unchanged

        // Test withTrace
        AgentContext traced = context.withTrace("trace-abc", "span-123");
        assertEquals("trace-abc", traced.getTraceId());
        assertEquals("span-123", traced.getSpanId());
        assertNull(context.getTraceId()); // Original unchanged

        // Test withObligations
        AgentContext.Obligations obligations = AgentContext.Obligations.builder()
                .rowFilters(Map.of("tenant_id", "acme-corp"))
                .columnMasks(Map.of("ssn", "REDACT"))
                .maxRows(100)
                .build();

        AgentContext withOblig = context.withObligations(obligations);
        assertNotNull(withOblig.getObligations());
        assertEquals("REDACT", withOblig.getObligations().getColumnMasks().get("ssn"));
        assertEquals(100, withOblig.getObligations().getMaxRows());
    }

    @Test
    void testAgentResponseChunkBuilder() {
        AgentResponse.Chunk chunk = AgentResponse.Chunk.text("Hello world");
        assertEquals("Hello world", chunk.getTextDelta());
        assertFalse(chunk.isLast());

        AgentResponse.Chunk lastChunk = AgentResponse.Chunk.last("stop");
        assertTrue(lastChunk.isLast());
        assertEquals("stop", lastChunk.getFinishReason());
    }
}
