package com.phaselume.torana.connector.http.auth;

import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpConnectorAuthInjectorTest {

    private final HttpConnectorAuthInjector injector = new HttpConnectorAuthInjector();

    @Test
    void testInjectBearerAuth() {
        HttpHeaders headers = new HttpHeaders();
        ConnectorConfig config = ConnectorConfig.builder()
                .properties(Map.of(
                        "auth", Map.of(
                                "type", "bearer",
                                "token", "secret-bearer-token-123"
                        )
                ))
                .build();

        injector.injectAuth(headers, config);
        assertEquals("Bearer secret-bearer-token-123", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void testInjectApiKeyAuth() {
        HttpHeaders headers = new HttpHeaders();
        ConnectorConfig config = ConnectorConfig.builder()
                .properties(Map.of(
                        "auth", Map.of(
                                "type", "api-key",
                                "header", "X-Service-Key",
                                "key", "key-999-xyz"
                        )
                ))
                .build();

        injector.injectAuth(headers, config);
        assertEquals("key-999-xyz", headers.getFirst("X-Service-Key"));
    }

    @Test
    void testInjectBasicAuth() {
        HttpHeaders headers = new HttpHeaders();
        ConnectorConfig config = ConnectorConfig.builder()
                .properties(Map.of(
                        "auth", Map.of(
                                "type", "basic",
                                "username", "admin",
                                "password", "secretpass"
                        )
                ))
                .build();

        injector.injectAuth(headers, config);
        String expected = "Basic " + Base64.getEncoder().encodeToString("admin:secretpass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, headers.getFirst(HttpHeaders.AUTHORIZATION));
    }
}
