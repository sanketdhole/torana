package com.phaselume.torana.security.authz.opa.client;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ToranaAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OpaInputBuilderTest {

    @Test
    void testBuildInputFromAgentContext() {
        OpaInputBuilder builder = new OpaInputBuilder();

        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("agent-007")
                .name("James Bond")
                .tenantId("tenant-mi6")
                .authMethod("jwt")
                .roles(Set.of("admin", "special-agent"))
                .scopes(Set.of("tools:call", "tools:read"))
                .claims(Map.of("clearance", "top-secret"))
                .build();

        RouteDefinition route = RouteDefinition.builder()
                .id("route-tools-execute")
                .path("/api/v1/tools/execute")
                .backendRef("tools-service")
                .authRef("jwt-profile")
                .protocols(Set.of("http", "mcp"))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Custom-Header", "custom-value");
        headers.add("Authorization", "Bearer sensitive");

        AgentRequest request = AgentRequest.builder()
                .id("req-12345")
                .protocol("http")
                .method(HttpMethod.POST)
                .path("/api/v1/tools/execute")
                .headers(headers)
                .attributes(Map.of("client_ip", "192.168.1.100"))
                .cachedBody("{\"command\":\"status\"}".getBytes(StandardCharsets.UTF_8))
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .matchedRoute(route)
                .request(request)
                .traceId("trace-abc")
                .spanId("span-xyz")
                .build();

        Map<String, Object> input = builder.buildInput(context);
        assertNotNull(input);

        // Check Principal
        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) input.get("principal");
        assertNotNull(principal);
        assertEquals("agent-007", principal.get("principal_id"));
        assertEquals("James Bond", principal.get("name"));
        assertEquals("tenant-mi6", principal.get("tenant_id"));
        assertEquals("jwt", principal.get("auth_method"));
        assertEquals(Set.of("tools:call", "tools:read"), principal.get("scopes"));

        // Check Route
        @SuppressWarnings("unchecked")
        Map<String, Object> routeMap = (Map<String, Object>) input.get("route");
        assertNotNull(routeMap);
        assertEquals("route-tools-execute", routeMap.get("id"));
        assertEquals("tools-service", routeMap.get("backend_ref"));
        assertEquals("jwt-profile", routeMap.get("auth_ref"));

        // Check Request
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = (Map<String, Object>) input.get("request");
        assertNotNull(requestMap);
        assertEquals("POST", requestMap.get("method"));
        assertEquals("192.168.1.100", requestMap.get("client_ip"));
        assertTrue(requestMap.containsKey("body_hash"));

        // Headers should have stripped sensitive authorization header
        @SuppressWarnings("unchecked")
        Map<String, String> safeHeaders = (Map<String, String>) requestMap.get("headers");
        assertFalse(safeHeaders.containsKey("Authorization"));
        assertEquals("custom-value", safeHeaders.get("X-Custom-Header"));

        // Check Environment
        @SuppressWarnings("unchecked")
        Map<String, Object> env = (Map<String, Object>) input.get("environment");
        assertNotNull(env);
        assertEquals("trace-abc", env.get("trace_id"));
    }
}
