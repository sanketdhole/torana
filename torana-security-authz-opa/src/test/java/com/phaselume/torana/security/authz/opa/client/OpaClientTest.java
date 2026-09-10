package com.phaselume.torana.security.authz.opa.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties;
import com.phaselume.torana.security.authz.opa.model.OpaResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class OpaClientTest {

    private WireMockServer wireMockServer;
    private OpaClient opaClient;
    private AuthzProperties properties;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        properties = AuthzProperties.builder()
                .enabled(true)
                .opaUrl("http://localhost:" + wireMockServer.port() + "/v1/data/torana/v1")
                .policyPackage("torana.v1")
                .timeoutMs(1000)
                .failOpen(false)
                .build();

        opaClient = new OpaClient(properties);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testEvaluateAllowWithObligations() {
        String opaJsonResponse = """
                {
                    "decision_id": "test-decision-001",
                    "result": {
                        "allow": true,
                        "obligations": [
                            {
                                "type": "mask-field",
                                "params": { "field": "credit_card", "mask": "LAST4" }
                            }
                        ]
                    }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/v1/data/torana/v1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(opaJsonResponse)));

        StepVerifier.create(opaClient.evaluate(Map.of("principal", Map.of("name", "alice"))))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.isAllowed());
                    assertEquals("test-decision-001", response.getDecisionId());
                    assertEquals(1, response.getObligations().size());
                    assertEquals("mask-field", response.getObligations().get(0).getType());
                })
                .verifyComplete();
    }

    @Test
    void testEvaluateDenyWithReason() {
        String opaJsonResponse = """
                {
                    "decision_id": "test-decision-002",
                    "result": {
                        "allow": false,
                        "deny_reason": "Tenant isolation violation"
                    }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/v1/data/torana/v1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(opaJsonResponse)));

        StepVerifier.create(opaClient.evaluate(Map.of("principal", Map.of("name", "mallory"))))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.isAllowed());
                    assertEquals("Tenant isolation violation", response.getDenyReason());
                })
                .verifyComplete();
    }

    @Test
    void testEvaluateTimeoutFailClosed() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/data/torana/v1"))
                .willReturn(aResponse()
                        .withFixedDelay(2000)
                        .withBody("{}")));

        StepVerifier.create(opaClient.evaluate(Map.of("principal", Map.of("name", "bob"))))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.isAllowed());
                    assertTrue(response.getDenyReason().contains("unavailable") || response.getDenyReason().contains("Timeout"));
                })
                .verifyComplete();
    }

    @Test
    void testEvaluateFailOpenWhenConfigured() {
        properties.setFailOpen(true);
        OpaClient failOpenClient = new OpaClient(properties);

        wireMockServer.stubFor(post(urlEqualTo("/v1/data/torana/v1"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(failOpenClient.evaluate(Map.of("principal", Map.of("name", "charlie"))))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.isAllowed());
                })
                .verifyComplete();
    }
}
