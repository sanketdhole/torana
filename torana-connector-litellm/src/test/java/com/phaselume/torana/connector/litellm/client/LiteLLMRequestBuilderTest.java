package com.phaselume.torana.connector.litellm.client;

import com.phaselume.torana.connector.litellm.model.ChatCompletionRequest;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LiteLLMRequestBuilderTest {

    private final LiteLLMRequestBuilder builder = new LiteLLMRequestBuilder();

    @Test
    void testBuildRequestFromJsonBody() {
        String jsonBody = """
                {
                    "model": "claude-3-5-sonnet",
                    "temperature": 0.2,
                    "max_tokens": 1024,
                    "messages": [
                        {"role": "system", "content": "You are an assistant."},
                        {"role": "user", "content": "Explain quantum computing."}
                    ]
                }
                """;

        AgentRequest request = AgentRequest.builder()
                .cachedBody(jsonBody.getBytes(StandardCharsets.UTF_8))
                .build();

        AgentContext context = AgentContext.builder().request(request).build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("litellm-backend")
                .type("litellm")
                .properties(Map.of("defaultModel", "gpt-4o"))
                .build();

        ChatCompletionRequest req = builder.buildRequest(context, config);

        assertNotNull(req);
        assertEquals("claude-3-5-sonnet", req.getModel());
        assertEquals(0.2, req.getTemperature());
        assertEquals(1024, req.getMaxTokens());
        assertEquals(2, req.getMessages().size());
        assertEquals("system", req.getMessages().get(0).getRole());
        assertEquals("Explain quantum computing.", req.getMessages().get(1).getContent());
    }

    @Test
    void testBuildRequestFallbackToDefaultModel() {
        AgentContext context = AgentContext.builder().build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("litellm-backend")
                .type("litellm")
                .properties(Map.of("defaultModel", "ollama/llama3.2"))
                .build();

        ChatCompletionRequest req = builder.buildRequest(context, config);

        assertNotNull(req);
        assertEquals("ollama/llama3.2", req.getModel());
        assertEquals(1, req.getMessages().size());
        assertEquals("Hello", req.getMessages().get(0).getContent());
    }
}
