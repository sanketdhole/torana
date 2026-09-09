package com.phaselume.torana.test.fixtures;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ToranaAuthentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pre-configured AgentContext instances for pipeline, security, and connector tests.
 */
public final class AgentContextFixtures {

    private AgentContextFixtures() {}

    public static AgentContext authenticatedContext() {
        return AgentContext.builder()
                .id(UUID.randomUUID().toString())
                .traceId("trace-" + UUID.randomUUID())
                .spanId("span-" + UUID.randomUUID())
                .tenantId("acme-corp")
                .request(AgentRequestFixtures.mcpToolsListRequest())
                .authentication(ToranaAuthenticationFixtures.aliceUser())
                .matchedRoute(RouteDefinitionFixtures.mcpRoute())
                .obligations(AgentContext.Obligations.empty())
                .build();
    }

    public static AgentContext tenantContext(String tenantId) {
        return AgentContext.builder()
                .id(UUID.randomUUID().toString())
                .traceId("trace-" + UUID.randomUUID())
                .spanId("span-" + UUID.randomUUID())
                .tenantId(tenantId)
                .request(AgentRequestFixtures.chatCompletionRequest("Hello"))
                .authentication(ToranaAuthenticationFixtures.tenantUser("test_user", tenantId, null))
                .matchedRoute(RouteDefinitionFixtures.llmProxyRoute())
                .obligations(AgentContext.Obligations.empty())
                .build();
    }

    public static AgentContext contextWithObligations(Map<String, String> rowFilters, Map<String, String> columnMasks) {
        AgentContext.Obligations obligations = AgentContext.Obligations.builder()
                .rowFilters(rowFilters != null ? rowFilters : Map.of())
                .columnMasks(columnMasks != null ? columnMasks : Map.of())
                .maxRows(100)
                .allowedPathPrefixes(List.of("s3://acme-bucket/*"))
                .build();

        return authenticatedContext().withObligations(obligations);
    }

    public static AgentContext unauthenticatedContext() {
        return AgentContext.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("default")
                .request(AgentRequestFixtures.restGetRequest("/public/health"))
                .authentication(ToranaAuthenticationFixtures.anonymous())
                .matchedRoute(RouteDefinitionFixtures.restRoute("/public/health"))
                .obligations(AgentContext.Obligations.empty())
                .build();
    }
}
