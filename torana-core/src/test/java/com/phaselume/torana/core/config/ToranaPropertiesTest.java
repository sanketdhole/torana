package com.phaselume.torana.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ToranaPropertiesTest {

    @Test
    void testToranaPropertiesDefaults() {
        ToranaProperties props = new ToranaProperties();

        assertTrue(props.isEnabled());
        assertTrue(props.getProtocols().getMcp().isEnabled());
        assertEquals("/mcp/v1", props.getProtocols().getMcp().getPath());
        assertTrue(props.getSecurity().isEnabled());
        assertEquals("http://localhost:8181/v1/data/torana/authz", props.getSecurity().getAuthz().getOpaUrl());
        assertTrue(props.getRatelimit().isEnabled());
        assertEquals("redis://localhost:6379", props.getRatelimit().getRedisUrl());
    }

    @Test
    void testCustomRouteAndPipelineDefinitions() {
        RouteDefinition route = RouteDefinition.builder()
                .id("llm-chat-route")
                .path("/v1/chat/completions")
                .methods(Set.of(HttpMethod.POST))
                .protocols(Set.of("http", "websocket"))
                .pipelineRef("llm-pipeline")
                .timeout(Duration.ofSeconds(45))
                .build();

        StepDefinition ragStep = StepDefinition.builder()
                .type("rag-retrieval")
                .params(Map.of("topK", 5, "threshold", 0.8))
                .build();

        StepDefinition llmStep = StepDefinition.builder()
                .type("llm-call")
                .params(Map.of("model", "gpt-4o", "temperature", 0.7))
                .build();

        PipelineDefinition pipeline = PipelineDefinition.builder()
                .name("llm-pipeline")
                .timeout(Duration.ofSeconds(60))
                .steps(List.of(ragStep, llmStep))
                .build();

        ConnectorDefinition connector = ConnectorDefinition.builder()
                .id("litellm-backend")
                .type("litellm")
                .endpoint("http://litellm:4000")
                .resilienceProfileRef("default-resilience")
                .properties(Map.of("defaultModel", "gpt-4o"))
                .build();

        ToranaProperties props = ToranaProperties.builder()
                .routing(ToranaProperties.RoutingProperties.builder().routes(List.of(route)).build())
                .pipeline(ToranaProperties.PipelineProperties.builder().pipelines(List.of(pipeline)).build())
                .connectors(ToranaProperties.ConnectorProperties.builder().backends(List.of(connector)).build())
                .build();

        assertEquals(1, props.getRouting().getRoutes().size());
        assertEquals("llm-chat-route", props.getRouting().getRoutes().get(0).getId());
        assertEquals(1, props.getPipeline().getPipelines().size());
        assertEquals(2, props.getPipeline().getPipelines().get(0).getSteps().size());
        assertEquals("rag-retrieval", props.getPipeline().getPipelines().get(0).getSteps().get(0).getType());
        assertEquals(5, props.getPipeline().getPipelines().get(0).getSteps().get(0).getParam("topK", 0));
        assertEquals("litellm", props.getConnectors().getBackends().get(0).getType());
    }
}
