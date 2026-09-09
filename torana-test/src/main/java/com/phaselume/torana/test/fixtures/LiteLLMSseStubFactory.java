package com.phaselume.torana.test.fixtures;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Helper to register streaming Server-Sent Events (SSE) WireMock stubs for LiteLLM / OpenAI chat completions.
 */
public final class LiteLLMSseStubFactory {

    private LiteLLMSseStubFactory() {}

    /**
     * Stubs a streaming chat completion response on the given WireMock server with SSE chunks.
     */
    public static void stubChatCompletionSse(WireMockServer wireMock, String urlPattern, List<String> textDeltas) {
        StringBuilder sseBody = new StringBuilder();
        for (int i = 0; i < textDeltas.size(); i++) {
            String delta = textDeltas.get(i);
            String chunkJson = String.format("""
                    {"id":"chatcmpl-test","choices":[{"index":0,"delta":{"content":"%s"},"finish_reason":null}]}
                    """, delta.replace("\"", "\\\""));
            sseBody.append("data: ").append(chunkJson.trim()).append("\n\n");
        }

        // Final [DONE] message
        sseBody.append("data: [DONE]\n\n");

        wireMock.stubFor(post(urlMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withHeader("Cache-Control", "no-cache")
                        .withBody(sseBody.toString())));
    }

    /**
     * Stubs a simple buffered JSON chat completion response.
     */
    public static void stubChatCompletionJson(WireMockServer wireMock, String urlPattern, String responseContent) {
        String json = String.format("""
                {"id":"chatcmpl-test","choices":[{"index":0,"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}]}
                """, responseContent.replace("\"", "\\\""));

        wireMock.stubFor(post(urlMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));
    }
}
