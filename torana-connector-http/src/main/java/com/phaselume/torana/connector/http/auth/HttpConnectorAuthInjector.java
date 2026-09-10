package com.phaselume.torana.connector.http.auth;

import com.phaselume.torana.core.model.ConnectorConfig;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Injects authentication credentials into outgoing HTTP connector requests.
 */
public class HttpConnectorAuthInjector {

    /**
     * Injects authentication headers into the provided {@link HttpHeaders} according to connector configuration.
     */
    public void injectAuth(HttpHeaders headers, ConnectorConfig config) {
        if (headers == null || config == null || config.getProperties() == null) {
            return;
        }

        Object authObj = config.getProperties().get("auth");
        if (!(authObj instanceof Map<?, ?> authMap)) {
            // Check top-level properties if auth block is not nested
            String apiKey = config.getProperty("apiKey", null);
            if (apiKey != null) {
                String headerName = config.getProperty("apiKeyHeader", "X-Api-Key");
                headers.set(headerName, apiKey);
            }
            String bearerToken = config.getProperty("bearerToken", null);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }
            return;
        }

        String type = getString(authMap, "type", "none");
        switch (type.toLowerCase()) {
            case "bearer", "vault-bearer" -> {
                String token = getString(authMap, "token", null);
                if (token != null) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            }
            case "api-key", "apikey" -> {
                String key = getString(authMap, "key", getString(authMap, "value", null));
                String header = getString(authMap, "header", "X-Api-Key");
                if (key != null) {
                    headers.set(header, key);
                }
            }
            case "basic", "vault-basic" -> {
                String user = getString(authMap, "username", "");
                String pass = getString(authMap, "password", "");
                String creds = user + ":" + pass;
                String base64 = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + base64);
            }
        }
    }

    private static String getString(Map<?, ?> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
