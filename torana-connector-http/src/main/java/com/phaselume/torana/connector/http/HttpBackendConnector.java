package com.phaselume.torana.connector.http;

import com.phaselume.torana.connector.http.auth.HttpConnectorAuthInjector;
import com.phaselume.torana.connector.http.proxy.HttpConnectorWebClientFactory;
import com.phaselume.torana.connector.http.proxy.RequestForwardingStrategy;
import com.phaselume.torana.connector.http.proxy.ResponseMappingStrategy;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;

/**
 * Generic reactive HTTP microservice reverse proxy connector.
 */
public class HttpBackendConnector implements BackendConnector {

    private static final Logger log = LoggerFactory.getLogger(HttpBackendConnector.class);

    private final HttpConnectorWebClientFactory clientFactory;
    private final RequestForwardingStrategy forwardingStrategy;
    private final ResponseMappingStrategy mappingStrategy;
    private final HttpConnectorAuthInjector authInjector;
    private final ResilienceDecorator resilienceDecorator;

    public HttpBackendConnector(HttpConnectorWebClientFactory clientFactory,
                                RequestForwardingStrategy forwardingStrategy,
                                ResponseMappingStrategy mappingStrategy,
                                HttpConnectorAuthInjector authInjector,
                                ResilienceDecorator resilienceDecorator) {
        this.clientFactory = clientFactory != null ? clientFactory : new HttpConnectorWebClientFactory();
        this.forwardingStrategy = forwardingStrategy != null ? forwardingStrategy : new RequestForwardingStrategy();
        this.mappingStrategy = mappingStrategy != null ? mappingStrategy : new ResponseMappingStrategy();
        this.authInjector = authInjector != null ? authInjector : new HttpConnectorAuthInjector();
        this.resilienceDecorator = resilienceDecorator;
    }

    public HttpBackendConnector() {
        this(new HttpConnectorWebClientFactory(), new RequestForwardingStrategy(), new ResponseMappingStrategy(), new HttpConnectorAuthInjector(), null);
    }

    @Override
    public String type() {
        return "http";
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        return config != null && "http".equalsIgnoreCase(config.getType());
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        if (config == null) {
            return Flux.error(new IllegalArgumentException("ConnectorConfig must not be null"));
        }

        return executeRequest(context, config);
    }

    private Flux<AgentResponse.Chunk> executeRequest(AgentContext context, ConnectorConfig config) {
        WebClient client = clientFactory.getClient(config);
        URI targetUri = forwardingStrategy.resolveTargetUri(context, config);
        HttpMethod method = forwardingStrategy.resolveMethod(context);
        HttpHeaders headers = forwardingStrategy.prepareHeaders(context, config);

        // Inject authentication
        authInjector.injectAuth(headers, config);

        WebClient.RequestBodySpec requestSpec = client.method(method)
                .uri(targetUri)
                .headers(h -> h.addAll(headers));

        AgentRequest req = context != null ? context.getRequest() : null;
        WebClient.RequestHeadersSpec<?> headersSpec = requestSpec;

        if (req != null && req.getCachedBody() != null && req.getCachedBody().length > 0) {
            headersSpec = requestSpec.body(BodyInserters.fromValue(req.getCachedBody()));
        } else if (req != null && req.getBody() != null) {
            headersSpec = requestSpec.body(req.getBody(), DataBuffer.class);
        }

        Duration timeout = config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(30);

        return headersSpec.exchangeToFlux(clientResponse -> {
            log.debug("HTTP connector [{}] received status: {}", config.getId(), clientResponse.statusCode());
            return mappingStrategy.mapResponse(clientResponse);
        }).timeout(timeout);
    }
}
