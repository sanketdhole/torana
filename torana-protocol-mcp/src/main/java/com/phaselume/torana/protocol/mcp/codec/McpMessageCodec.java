package com.phaselume.torana.protocol.mcp.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;

/**
 * High-performance Jackson codec for encoding and decoding MCP JSON-RPC 2.0 messages.
 */
public class McpMessageCodec {

    private final ObjectMapper objectMapper;

    public McpMessageCodec() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public McpMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Decodes a raw JSON string or byte array into an McpJsonRpcRequest.
     */
    public McpJsonRpcRequest decodeRequest(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            throw new IllegalArgumentException("Payload is empty");
        }
        try {
            return objectMapper.readValue(jsonBytes, McpJsonRpcRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON-RPC request: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a raw JSON string into an McpJsonRpcRequest.
     */
    public McpJsonRpcRequest decodeRequest(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            throw new IllegalArgumentException("Payload is empty");
        }
        try {
            return objectMapper.readValue(jsonString, McpJsonRpcRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON-RPC request: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes any MCP response or event object to a JSON byte array.
     */
    public byte[] encode(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MCP message: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes any MCP response or event object to a JSON string.
     */
    public String encodeToString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MCP message: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a JsonNode params tree into a specific Java class.
     */
    public <T> T convertParams(JsonNode params, Class<T> targetClass) {
        if (params == null) {
            return null;
        }
        return objectMapper.convertValue(params, targetClass);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
