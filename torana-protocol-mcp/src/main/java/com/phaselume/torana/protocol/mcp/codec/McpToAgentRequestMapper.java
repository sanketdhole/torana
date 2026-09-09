package com.phaselume.torana.protocol.mcp.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps an incoming MCP JSON-RPC 2.0 request into a normalized Torana AgentRequest.
 */
public class McpToAgentRequestMapper {

    private final McpMessageCodec codec;

    public McpToAgentRequestMapper(McpMessageCodec codec) {
        this.codec = codec != null ? codec : new McpMessageCodec();
    }

    /**
     * Maps an McpJsonRpcRequest to an AgentRequest.
     */
    public AgentRequest map(McpJsonRpcRequest rpcRequest, ServerWebExchange rawExchange, byte[] rawBytes) {
        if (rpcRequest == null) {
            throw new IllegalArgumentException("McpJsonRpcRequest cannot be null");
        }

        String requestId = rpcRequest.getId() != null ? String.valueOf(rpcRequest.getId()) : UUID.randomUUID().toString();
        String method = rpcRequest.getMethod() != null ? rpcRequest.getMethod() : "unknown";
        String normalizedPath = "/mcp/" + method;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("mcp.jsonrpc", rpcRequest.getJsonrpc());
        attributes.put("mcp.id", rpcRequest.getId());
        attributes.put("mcp.method", method);
        attributes.put("mcp.is_notification", rpcRequest.isNotification());

        JsonNode params = rpcRequest.getParams();
        if (params != null) {
            attributes.put("mcp.params", params);

            // Extract method specific parameters
            if ("tools/call".equals(method)) {
                if (params.has("name")) {
                    attributes.put("mcp.tool_name", params.get("name").asText());
                }
                if (params.has("arguments")) {
                    attributes.put("mcp.arguments", params.get("arguments"));
                }
            } else if ("resources/read".equals(method) || "resources/subscribe".equals(method)) {
                if (params.has("uri")) {
                    attributes.put("mcp.resource_uri", params.get("uri").asText());
                }
            } else if ("prompts/get".equals(method)) {
                if (params.has("name")) {
                    attributes.put("mcp.prompt_name", params.get("name").asText());
                }
                if (params.has("arguments")) {
                    attributes.put("mcp.prompt_arguments", params.get("arguments"));
                }
            }
        }

        HttpHeaders headers = rawExchange != null ? HttpHeaders.readOnlyHttpHeaders(rawExchange.getRequest().getHeaders()) : new HttpHeaders();

        byte[] bodyBytes = rawBytes != null ? rawBytes : (rpcRequest.getParams() != null ? codec.encode(rpcRequest.getParams()) : new byte[0]);

        return AgentRequest.builder()
                .id(requestId)
                .protocol("mcp")
                .method(HttpMethod.POST)
                .path(normalizedPath)
                .headers(headers)
                .cachedBody(bodyBytes)
                .rawExchange(rawExchange)
                .attributes(attributes)
                .timestamp(Instant.now())
                .build();
    }
}
