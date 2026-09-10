package com.phaselume.torana.connector.http.proxy;

import com.phaselume.torana.core.model.ConnectorConfig;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches configured {@link WebClient} instances per connector configuration.
 */
public class HttpConnectorWebClientFactory {

    private final WebClient.Builder webClientBuilder;
    private final Map<String, WebClient> clientCache = new ConcurrentHashMap<>();

    public HttpConnectorWebClientFactory(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder != null ? webClientBuilder : WebClient.builder();
    }

    public HttpConnectorWebClientFactory() {
        this(WebClient.builder());
    }

    /**
     * Gets or creates a WebClient for the given connector configuration.
     */
    public WebClient getClient(ConnectorConfig config) {
        String key = config.getId() != null ? config.getId() : (config.getEndpoint() != null ? config.getEndpoint() : "default-http");
        return clientCache.computeIfAbsent(key, k -> {
            WebClient.Builder builder = webClientBuilder.clone();
            if (config.getEndpoint() != null && !config.getEndpoint().isBlank()) {
                builder.baseUrl(config.getEndpoint());
            }
            return builder.build();
        });
    }
}
