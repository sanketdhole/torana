package com.phaselume.torana.test.fixtures;

import com.phaselume.torana.core.model.AgentRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Pre-configured AgentRequest test instances across protocols.
 */
public final class AgentRequestFixtures {

    private AgentRequestFixtures() {}

    public static AgentRequest mcpToolsListRequest() {
        String jsonRpc = """
                {"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return AgentRequest.builder()
                .id(UUID.randomUUID().toString())
                .protocol("mcp")
                .method(HttpMethod.POST)
                .path("/mcp/v1")
                .headers(headers)
                .queryParams(new LinkedMultiValueMap<>())
                .cachedBody(jsonRpc.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    public static AgentRequest mcpToolCallRequest(String toolName, String argumentsJson) {
        String jsonRpc = String.format("""
                {"jsonrpc":"2.0","id":"2","method":"tools/call","params":{"name":"%s","arguments":%s}}
                """, toolName, argumentsJson != null ? argumentsJson : "{}");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return AgentRequest.builder()
                .id(UUID.randomUUID().toString())
                .protocol("mcp")
                .method(HttpMethod.POST)
                .path("/mcp/v1")
                .headers(headers)
                .queryParams(new LinkedMultiValueMap<>())
                .cachedBody(jsonRpc.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    public static AgentRequest chatCompletionRequest(String prompt) {
        String body = String.format("""
                {"model":"gpt-4o","messages":[{"role":"user","content":"%s"}],"stream":true}
                """, prompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return AgentRequest.builder()
                .id(UUID.randomUUID().toString())
                .protocol("http")
                .method(HttpMethod.POST)
                .path("/v1/chat/completions")
                .headers(headers)
                .queryParams(new LinkedMultiValueMap<>())
                .cachedBody(body.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    public static AgentRequest restGetRequest(String path) {
        return AgentRequest.builder()
                .id(UUID.randomUUID().toString())
                .protocol("http")
                .method(HttpMethod.GET)
                .path(path)
                .headers(new HttpHeaders())
                .queryParams(new LinkedMultiValueMap<>())
                .build();
    }
}
