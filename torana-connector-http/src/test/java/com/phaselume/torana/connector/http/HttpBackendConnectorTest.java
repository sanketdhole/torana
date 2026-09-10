package com.phaselume.torana.connector.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class HttpBackendConnectorTest {

    private WireMockServer wireMockServer;
    private HttpBackendConnector connector;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        connector = new HttpBackendConnector();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testExecuteGetRequest() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/health"))
                .withHeader("X-Tenant-Id", equalTo("tenant-1"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\"}")));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "tenant-1");

        AgentRequest request = AgentRequest.builder()
                .method(HttpMethod.GET)
                .path("/api/v1/health")
                .headers(headers)
                .build();

        AgentContext context = AgentContext.builder().request(request).build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("health-service")
                .type("http")
                .endpoint("http://localhost:" + wireMockServer.port())
                .timeout(Duration.ofSeconds(5))
                .properties(Map.of(
                        "auth", Map.of("type", "bearer", "token", "test-token")
                ))
                .build();

        StepVerifier.create(connector.execute(context, config))
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertFalse(chunk.isLast());
                    String body = new String(chunk.getData(), StandardCharsets.UTF_8);
                    assertEquals("{\"status\":\"UP\"}", body);
                })
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertTrue(chunk.isLast());
                    assertEquals("stop", chunk.getFinishReason());
                })
                .verifyComplete();
    }

    @Test
    void testExecutePostRequestWithBody() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/echo"))
                .withRequestBody(equalTo("{\"message\":\"hello\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"echo\":\"hello\"}")));

        byte[] bodyBytes = "{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8);

        AgentRequest request = AgentRequest.builder()
                .method(HttpMethod.POST)
                .path("/api/v1/echo")
                .cachedBody(bodyBytes)
                .build();

        AgentContext context = AgentContext.builder().request(request).build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("echo-service")
                .type("http")
                .endpoint("http://localhost:" + wireMockServer.port())
                .timeout(Duration.ofSeconds(5))
                .build();

        StepVerifier.create(connector.execute(context, config))
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertFalse(chunk.isLast());
                    String body = new String(chunk.getData(), StandardCharsets.UTF_8);
                    assertEquals("{\"echo\":\"hello\"}", body);
                })
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertTrue(chunk.isLast());
                })
                .verifyComplete();
    }
}
