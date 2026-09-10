package com.phaselume.torana.connector.litellm;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class LiteLLMConnectorTest {

    private WireMockServer wireMockServer;
    private LiteLLMConnector connector;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        connector = new LiteLLMConnector();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testExecuteStreamingChatCompletion() {
        String sseResponse = """
                data: {"id":"chat-1","choices":[{"delta":{"content":"Deep"}}]}
                
                data: {"id":"chat-1","choices":[{"delta":{"content":"Mind"}}]}
                
                data: {"id":"chat-1","choices":[{"finish_reason":"stop"}]}
                
                data: [DONE]
                """;

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer sk-mock-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseResponse)));

        String promptJson = "{\"prompt\":\"Who created you?\"}";
        AgentRequest request = AgentRequest.builder()
                .cachedBody(promptJson.getBytes(StandardCharsets.UTF_8))
                .build();

        AgentContext context = AgentContext.builder().request(request).build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("litellm-service")
                .type("litellm")
                .endpoint("http://localhost:" + wireMockServer.port())
                .timeout(Duration.ofSeconds(5))
                .properties(Map.of(
                        "apiKey", "sk-mock-key",
                        "stream", true
                ))
                .build();

        StepVerifier.create(connector.execute(context, config))
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertEquals("Deep", chunk.getTextDelta());
                    assertFalse(chunk.isLast());
                })
                .assertNext(chunk -> {
                    assertEquals("Mind", chunk.getTextDelta());
                    assertFalse(chunk.isLast());
                })
                .assertNext(chunk -> {
                    assertTrue(chunk.isLast());
                    assertEquals("stop", chunk.getFinishReason());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteNonStreamingChatCompletion() {
        String jsonResponse = """
                {
                    "id": "chat-nonstream-1",
                    "object": "chat.completion",
                    "created": 1700000000,
                    "model": "gpt-4o",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "Hello there!"
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 5,
                        "total_tokens": 15
                    }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        AgentContext context = AgentContext.builder().build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("litellm-service")
                .type("litellm")
                .endpoint("http://localhost:" + wireMockServer.port())
                .timeout(Duration.ofSeconds(5))
                .properties(Map.of(
                        "stream", false
                ))
                .build();

        StepVerifier.create(connector.execute(context, config))
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertEquals("Hello there!", chunk.getTextDelta());
                    assertTrue(chunk.isLast());
                    assertEquals("stop", chunk.getFinishReason());
                    assertEquals(15, chunk.getTokenCount());
                })
                .verifyComplete();
    }
}
