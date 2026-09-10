package com.phaselume.torana.connector.litellm.client;

import com.phaselume.torana.core.model.ConnectorConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches configured {@link WebClient} instances for LiteLLM backends.
 */
public class LiteLLMWebClient {

    private final WebClient.Builder webClientBuilder;
    private final Map<String, WebClient> clientCache = new ConcurrentHashMap<>();

    public LiteLLMWebClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder != null ? webClientBuilder : WebClient.builder();
    }

    public LiteLLMWebClient() {
        this(WebClient.builder());
    }

    /**
     * Retrieves or instantiates a WebClient for the given LiteLLM connector configuration.
     */
    public WebClient getClient(ConnectorConfig config) {
        String key = config != null && config.getId() != null ? config.getId() : "default-litellm";
        return clientCache.computeIfAbsent(key, k -> {
            String baseUrl = (config != null && config.getEndpoint() != null)
                    ? config.getEndpoint()
                    : (config != null ? config.getProperty("baseUrl", "http://localhost:4000") : "http://localhost:4000");

            String apiKey = config != null ? config.getProperty("apiKey", null) : null;

            WebClient.Builder builder = webClientBuilder.clone()
                    .baseUrl(baseUrl);

            if (apiKey != null && !apiKey.isBlank()) {
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }

            return builder.build();
        });
    }
}
