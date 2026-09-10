package com.phaselume.torana.connector.litellm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.litellm.model.ChatCompletionRequest;
import com.phaselume.torana.connector.litellm.model.ChatMessage;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ConnectorConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds OpenAI-compatible {@link ChatCompletionRequest} objects from {@link AgentContext} and {@link ConnectorConfig}.
 */
public class LiteLLMRequestBuilder {

    private final ObjectMapper objectMapper;

    public LiteLLMRequestBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public LiteLLMRequestBuilder() {
        this(new ObjectMapper());
    }

    /**
     * Constructs a {@link ChatCompletionRequest} from the context and connector config.
     */
    public ChatCompletionRequest buildRequest(AgentContext context, ConnectorConfig config) {
        String model = config != null ? config.getProperty("defaultModel", "gpt-4o") : "gpt-4o";
        Double temperature = config != null ? config.getProperty("temperature", 0.7) : 0.7;
        Integer maxTokens = config != null ? config.getProperty("maxTokens", null) : null;
        boolean stream = config != null ? config.getProperty("stream", true) : true;

        List<ChatMessage> messages = new ArrayList<>();

        if (context != null) {
            // Check context attributes for custom messages or prompt
            Object customMessages = context.getAttribute("llm.messages");
            if (customMessages instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof ChatMessage cm) {
                        messages.add(cm);
                    } else if (item instanceof Map<?, ?> map) {
                        String role = map.get("role") != null ? map.get("role").toString() : "user";
                        String content = map.get("content") != null ? map.get("content").toString() : "";
                        messages.add(ChatMessage.builder().role(role).content(content).build());
                    }
                }
            } else {
                // Try to parse request body as chat completion JSON or text
                AgentRequest req = context.getRequest();
                if (req != null && req.getCachedBody() != null && req.getCachedBody().length > 0) {
                    try {
                        String bodyStr = new String(req.getCachedBody(), StandardCharsets.UTF_8);
                        JsonNode root = objectMapper.readTree(bodyStr);
                        if (root.has("model")) {
                            model = root.get("model").asText();
                        }
                        if (root.has("temperature")) {
                            temperature = root.get("temperature").asDouble();
                        }
                        if (root.has("max_tokens")) {
                            maxTokens = root.get("max_tokens").asInt();
                        }
                        if (root.has("stream")) {
                            stream = root.get("stream").asBoolean();
                        }
                        if (root.has("messages") && root.get("messages").isArray()) {
                            for (JsonNode m : root.get("messages")) {
                                String role = m.has("role") ? m.get("role").asText() : "user";
                                String content = m.has("content") ? m.get("content").asText() : "";
                                messages.add(ChatMessage.builder().role(role).content(content).build());
                            }
                        } else if (root.has("prompt")) {
                            messages.add(ChatMessage.user(root.get("prompt").asText()));
                        }
                    } catch (Exception ignored) {
                        // If not JSON, treat body as plain prompt text
                        String plainText = new String(req.getCachedBody(), StandardCharsets.UTF_8);
                        messages.add(ChatMessage.user(plainText));
                    }
                }
            }
        }

        if (messages.isEmpty()) {
            messages.add(ChatMessage.user("Hello"));
        }

        return ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .stream(stream)
                .build();
    }
}
