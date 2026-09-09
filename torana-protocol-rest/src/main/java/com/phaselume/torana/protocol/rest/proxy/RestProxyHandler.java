package com.phaselume.torana.protocol.rest.proxy;

import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles HTTP reverse proxying of an AgentRequest to an upstream target URL using WebClient.
 */
public class RestProxyHandler {

    private final WebClient webClient;
    private final HeaderForwardingPolicy headerPolicy;

    public RestProxyHandler(WebClient.Builder webClientBuilder, HeaderForwardingPolicy headerPolicy) {
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();
        this.headerPolicy = headerPolicy != null ? headerPolicy : new HeaderForwardingPolicy();
    }

    /**
     * Forwards the normalized AgentRequest to the specified target base URL.
     */
    public Mono<AgentResponse> forward(AgentRequest request, String targetBaseUrl, String rewrittenPath) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("AgentRequest cannot be null"));
        }
        if (targetBaseUrl == null || targetBaseUrl.isBlank()) {
            return Mono.error(new IllegalArgumentException("targetBaseUrl cannot be null or empty"));
        }

        String targetPath = rewrittenPath != null ? rewrittenPath : request.getPath();
        URI targetUri = buildTargetUri(targetBaseUrl, targetPath, request);
        HttpMethod method = request.getMethod() != null ? request.getMethod() : HttpMethod.GET;
        HttpHeaders forwardedHeaders = headerPolicy.filterRequestHeaders(request.getHeaders());

        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(targetUri)
                .headers(h -> h.putAll(forwardedHeaders));

        // Insert request body if available
        WebClient.RequestHeadersSpec<?> headersSpec;
        if (request.getCachedBody() != null && request.getCachedBody().length > 0) {
            headersSpec = spec.body(BodyInserters.fromValue(request.getCachedBody()));
        } else if (request.getBody() != null) {
            headersSpec = spec.body(request.getBody(), DataBuffer.class);
        } else {
            headersSpec = spec;
        }

        return headersSpec.exchangeToMono(this::buildAgentResponse);
    }

    private URI buildTargetUri(String baseUrl, String path, AgentRequest request) {
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanPath = path != null ? (path.startsWith("/") ? path : "/" + path) : "";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(cleanBase + cleanPath);
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            builder.queryParams(request.getQueryParams());
        }
        return builder.build(true).toUri();
    }

    private Mono<AgentResponse> buildAgentResponse(ClientResponse clientResponse) {
        HttpHeaders responseHeaders = headerPolicy.filterResponseHeaders(clientResponse.headers().asHttpHeaders());
        MediaType contentType = clientResponse.headers().contentType().orElse(MediaType.APPLICATION_JSON);

        boolean isStreaming = MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)
                || MediaType.APPLICATION_NDJSON.isCompatibleWith(contentType);

        if (isStreaming) {
            AtomicInteger chunkCounter = new AtomicInteger(0);
            Flux<AgentResponse.Chunk> chunkFlux = clientResponse.bodyToFlux(DataBuffer.class)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return AgentResponse.Chunk.builder()
                                .index(chunkCounter.getAndIncrement())
                                .data(bytes)
                                .textDelta(new String(bytes))
                                .last(false)
                                .build();
                    });

            return Mono.just(AgentResponse.builder()
                    .status(clientResponse.statusCode())
                    .headers(responseHeaders)
                    .stream(chunkFlux)
                    .build());
        }

        return clientResponse.bodyToMono(byte[].class)
                .defaultIfEmpty(new byte[0])
                .map(bytes -> AgentResponse.builder()
                        .status(clientResponse.statusCode())
                        .headers(responseHeaders)
                        .bufferedBody(Mono.just(bytes))
                        .build());
    }
}
