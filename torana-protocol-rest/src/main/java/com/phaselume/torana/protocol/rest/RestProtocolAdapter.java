package com.phaselume.torana.protocol.rest;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.ProtocolAdapter;
import com.phaselume.torana.protocol.rest.filter.ResponseNormalizationFilter;
import com.phaselume.torana.protocol.rest.proxy.PathRewriteRule;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ProtocolAdapter SPI implementation for HTTP/REST reverse proxy and gateway endpoints.
 */
@Component
public class RestProtocolAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_NAME = "rest";

    private final RestProtocolProperties properties;
    private final List<PathRewriteRule> rewriteRules;
    private final ResponseNormalizationFilter normalizationFilter;

    public RestProtocolAdapter(RestProtocolProperties properties) {
        this.properties = properties != null ? properties : new RestProtocolProperties();
        this.rewriteRules = initRewriteRules(this.properties);
        this.normalizationFilter = new ResponseNormalizationFilter();
    }

    private List<PathRewriteRule> initRewriteRules(RestProtocolProperties props) {
        List<PathRewriteRule> rules = new ArrayList<>();
        if (props.isStripPathPrefix() && props.getPath() != null) {
            rules.add(PathRewriteRule.stripPrefix(props.getPath()));
        }
        if (props.getPathRewrites() != null) {
            for (RestProtocolProperties.PathRewriteConfig cfg : props.getPathRewrites()) {
                if (cfg.getFrom() != null && cfg.getTo() != null) {
                    rules.add(new PathRewriteRule(cfg.getFrom(), cfg.getTo(), cfg.isRegex()));
                }
            }
        }
        return Collections.unmodifiableList(rules);
    }

    @Override
    public String protocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public RouterFunction<ServerResponse> routerFunction() {
        String pathPattern = properties.getPath() != null ? properties.getPath() : "/api/v1/**";
        return RouterFunctions.route(
                RequestPredicates.path(pathPattern),
                request -> ServerResponse.ok().bodyValue("Torana REST Gateway Endpoint")
        );
    }

    @Override
    public Mono<AgentRequest> decode(ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        ServerHttpRequest httpRequest = exchange.getRequest();
        String requestId = httpRequest.getHeaders().getFirst("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String rawPath = httpRequest.getPath().value();
        String effectivePath = applyPathRewrites(rawPath);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("torana.protocol.raw_path", rawPath);
        attributes.put("torana.protocol.effective_path", effectivePath);

        final String finalRequestId = requestId;

        // Read and buffer request body into cached byte array if present
        return DataBufferUtils.join(httpRequest.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .map(bodyBytes -> AgentRequest.builder()
                        .id(finalRequestId)
                        .protocol(PROTOCOL_NAME)
                        .method(httpRequest.getMethod())
                        .path(effectivePath)
                        .headers(HttpHeaders.readOnlyHttpHeaders(httpRequest.getHeaders()))
                        .queryParams(httpRequest.getQueryParams())
                        .cachedBody(bodyBytes)
                        .rawExchange(exchange)
                        .attributes(attributes)
                        .timestamp(Instant.now())
                        .build());
    }

    @Override
    public Mono<Void> encode(AgentContext context, Flux<AgentResponse.Chunk> responseStream, ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        ServerHttpResponse httpResponse = exchange.getResponse();
        Instant startTime = context != null && context.getCreatedAt() != null ? context.getCreatedAt() : Instant.now();

        // Apply normalization headers
        normalizationFilter.normalize(exchange, context, startTime);

        if (responseStream == null) {
            httpResponse.setStatusCode(HttpStatus.OK);
            return httpResponse.setComplete();
        }

        // Write streaming chunks
        Flux<DataBuffer> bufferFlux = responseStream.map(chunk -> {
            byte[] data = chunk.getData() != null ? chunk.getData() : new byte[0];
            return httpResponse.bufferFactory().wrap(data);
        });

        return httpResponse.writeWith(bufferFlux);
    }

    /**
     * Applies path rewriting rules to the incoming raw path.
     */
    public String applyPathRewrites(String path) {
        if (path == null) {
            return "";
        }
        String rewritten = path;
        for (PathRewriteRule rule : rewriteRules) {
            if (rule.matches(rewritten)) {
                rewritten = rule.rewrite(rewritten);
            }
        }
        return rewritten;
    }

    public List<PathRewriteRule> getRewriteRules() {
        return rewriteRules;
    }

    public RestProtocolProperties getProperties() {
        return properties;
    }
}
