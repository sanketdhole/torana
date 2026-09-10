package com.phaselume.torana.connector.litellm;

import com.phaselume.torana.connector.litellm.client.LiteLLMRequestBuilder;
import com.phaselume.torana.connector.litellm.client.LiteLLMWebClient;
import com.phaselume.torana.connector.litellm.model.ChatCompletionRequest;
import com.phaselume.torana.connector.litellm.model.ChatCompletionResponse;
import com.phaselume.torana.connector.litellm.streaming.ChunkToAgentResponseMapper;
import com.phaselume.torana.connector.litellm.streaming.SseChunkParser;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Primary LLM BackendConnector interfacing with LiteLLM / OpenAI-compatible model backends.
 */
public class LiteLLMConnector implements BackendConnector {

    private static final Logger log = LoggerFactory.getLogger(LiteLLMConnector.class);

    private final LiteLLMWebClient clientFactory;
    private final LiteLLMRequestBuilder requestBuilder;
    private final SseChunkParser sseParser;
    private final ChunkToAgentResponseMapper responseMapper;
    private final ResilienceDecorator resilienceDecorator;

    public LiteLLMConnector(LiteLLMWebClient clientFactory,
                            LiteLLMRequestBuilder requestBuilder,
                            SseChunkParser sseParser,
                            ChunkToAgentResponseMapper responseMapper,
                            ResilienceDecorator resilienceDecorator) {
        this.clientFactory = clientFactory != null ? clientFactory : new LiteLLMWebClient();
        this.requestBuilder = requestBuilder != null ? requestBuilder : new LiteLLMRequestBuilder();
        this.sseParser = sseParser != null ? sseParser : new SseChunkParser();
        this.responseMapper = responseMapper != null ? responseMapper : new ChunkToAgentResponseMapper();
        this.resilienceDecorator = resilienceDecorator;
    }

    public LiteLLMConnector() {
        this(new LiteLLMWebClient(), new LiteLLMRequestBuilder(), new SseChunkParser(), new ChunkToAgentResponseMapper(), null);
    }

    @Override
    public String type() {
        return "litellm";
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        return config != null && "litellm".equalsIgnoreCase(config.getType());
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        if (config == null) {
            return Flux.error(new IllegalArgumentException("ConnectorConfig must not be null"));
        }

        ChatCompletionRequest chatRequest = requestBuilder.buildRequest(context, config);
        WebClient webClient = clientFactory.getClient(config);
        Duration timeout = config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60);

        if (chatRequest.isStream()) {
            return executeStreaming(webClient, chatRequest, timeout);
        } else {
            return executeNonStreaming(webClient, chatRequest, timeout);
        }
    }

    private Flux<AgentResponse.Chunk> executeStreaming(WebClient webClient, ChatCompletionRequest chatRequest, Duration timeout) {
        Flux<DataBuffer> dataBufferFlux = webClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(timeout);

        return responseMapper.map(sseParser.parse(dataBufferFlux));
    }

    private Flux<AgentResponse.Chunk> executeNonStreaming(WebClient webClient, ChatCompletionRequest chatRequest, Duration timeout) {
        return webClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(timeout)
                .flatMapMany(resp -> {
                    String text = "";
                    String finishReason = "stop";
                    if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                        ChatCompletionResponse.Choice choice = resp.getChoices().get(0);
                        if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                            text = choice.getMessage().getContent();
                        }
                        if (choice.getFinishReason() != null) {
                            finishReason = choice.getFinishReason();
                        }
                    }

                    Integer tokens = resp.getUsage() != null ? resp.getUsage().getTotalTokens() : null;
                    Map<String, Object> metadata = new HashMap<>();
                    if (resp.getModel() != null) metadata.put("model", resp.getModel());
                    if (resp.getId() != null) metadata.put("completion_id", resp.getId());

                    AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                            .index(0)
                            .data(text.getBytes(StandardCharsets.UTF_8))
                            .textDelta(text)
                            .last(true)
                            .finishReason(finishReason)
                            .tokenCount(tokens)
                            .metadata(Collections.unmodifiableMap(metadata))
                            .build();

                    return Flux.just(chunk);
                });
    }
}
