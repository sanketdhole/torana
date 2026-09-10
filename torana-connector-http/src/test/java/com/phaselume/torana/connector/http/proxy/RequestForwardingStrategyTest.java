package com.phaselume.torana.connector.http.proxy;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestForwardingStrategyTest {

    @Test
    void testResolveTargetUriWithPathRewriteAndQueryParams() {
        RequestForwardingStrategy strategy = new RequestForwardingStrategy();

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("query", "test");
        queryParams.add("limit", "10");

        AgentRequest request = AgentRequest.builder()
                .method(HttpMethod.GET)
                .path("/api/v1/users/list")
                .queryParams(queryParams)
                .build();

        AgentContext context = AgentContext.builder().request(request).build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("user-service")
                .type("http")
                .endpoint("https://internal.users.corp.com:8443")
                .properties(Map.of(
                        "path-rewrite", Map.of(
                                "strip-prefix", "/api/v1",
                                "prepend-prefix", "/v2"
                        )
                ))
                .build();

        URI targetUri = strategy.resolveTargetUri(context, config);
        assertNotNull(targetUri);
        assertEquals("https://internal.users.corp.com:8443/v2/users/list?query=test&limit=10", targetUri.toString());
    }

    @Test
    void testPrepareHeadersStripsHopByHopAndInjectsCustom() {
        RequestForwardingStrategy strategy = new RequestForwardingStrategy();

        HttpHeaders incoming = new HttpHeaders();
        incoming.add("Host", "gateway.example.com");
        incoming.add("Connection", "keep-alive");
        incoming.add("X-Trace-Id", "trace-12345");
        incoming.add("X-Tenant-Id", "tenant-alpha");
        incoming.add("Cookie", "session=xyz");

        AgentRequest request = AgentRequest.builder()
                .headers(incoming)
                .build();

        AgentContext context = AgentContext.builder().request(request).build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("backend-service")
                .properties(Map.of(
                        "headers", Map.of(
                                "block", List.of("cookie"),
                                "inject", Map.of("X-Torana-Gateway", "true")
                        )
                ))
                .build();

        HttpHeaders prepared = strategy.prepareHeaders(context, config);
        assertNull(prepared.getFirst("Host"));
        assertNull(prepared.getFirst("Connection"));
        assertNull(prepared.getFirst("Cookie"));
        assertEquals("trace-12345", prepared.getFirst("X-Trace-Id"));
        assertEquals("tenant-alpha", prepared.getFirst("X-Tenant-Id"));
        assertEquals("true", prepared.getFirst("X-Torana-Gateway"));
    }
}
