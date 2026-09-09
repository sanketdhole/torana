package com.phaselume.torana.pipeline.step.llm;

import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.registry.BackendConnectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Pipeline step that resolves a BackendConnector reference and dispatches the execution to the backend connector.
 */
@Component
public class LlmCallStep implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(LlmCallStep.class);
    public static final String STEP_TYPE = "llm-call";

    private final BackendConnectorRegistry connectorRegistry;

    public LlmCallStep(BackendConnectorRegistry connectorRegistry) {
        this.connectorRegistry = connectorRegistry;
    }

    @Override
    public String type() {
        return STEP_TYPE;
    }

    @Override
    public Mono<AgentContext> execute(AgentContext context) {
        StepDefinition stepDef = context.getAttribute(PipelineConstants.ATTR_CURRENT_STEP);

        // Resolve backend-ref from step params or fallback to matched route backendRef
        String backendRef = stepDef != null ? stepDef.getParam("backend-ref", null) : null;
        if (backendRef == null && context.getMatchedRoute() != null) {
            backendRef = context.getMatchedRoute().getBackendRef();
        }

        if (backendRef == null) {
            return Mono.error(new ConnectorException("No backend-ref specified in step definition or route"));
        }

        BackendConnectorRegistry.ResolvedConnector resolved;
        try {
            resolved = connectorRegistry.resolve(backendRef);
        } catch (Exception e) {
            return Mono.error(e);
        }

        // Apply any step parameter overrides to ConnectorConfig
        ConnectorConfig config = resolved.config();
        if (stepDef != null && stepDef.getParams() != null && !stepDef.getParams().isEmpty()) {
            Map<String, Object> mergedProps = new HashMap<>(config.getProperties());
            mergedProps.putAll(stepDef.getParams());
            config = config.toBuilder().properties(mergedProps).build();
        }

        log.debug("Invoking backend connector '{}' (type: '{}') for context '{}'",
                config.getId(), resolved.connector().type(), context.getId());

        Flux<AgentResponse.Chunk> chunkFlux = resolved.connector().execute(context, config);

        AgentResponse response = AgentResponse.builder()
                .status(HttpStatus.OK)
                .stream(chunkFlux)
                .build();

        AgentContext updatedContext = context
                .withAttribute(PipelineConstants.ATTR_RESPONSE, response)
                .withAttribute(PipelineConstants.ATTR_RESPONSE_STREAM, chunkFlux);

        return Mono.just(updatedContext);
    }
}
