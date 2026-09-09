package com.phaselume.torana.pipeline.step.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Pipeline step that sets a static or transformed AgentResponse and optionally short-circuits the pipeline.
 */
@Component
public class ResponseTransformStep implements PipelineStep {

    public static final String STEP_TYPE = "response-transform";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String type() {
        return STEP_TYPE;
    }

    @Override
    public Mono<AgentContext> execute(AgentContext context) {
        StepDefinition stepDef = context.getAttribute(PipelineConstants.ATTR_CURRENT_STEP);
        if (stepDef == null) {
            return Mono.just(context);
        }

        // 1. Static response configuration
        Map<String, Object> staticResponseConfig = stepDef.getParam("static-response", null);
        if (staticResponseConfig != null) {
            int statusCode = 200;
            if (staticResponseConfig.containsKey("status")) {
                Object statusObj = staticResponseConfig.get("status");
                if (statusObj instanceof Number num) {
                    statusCode = num.intValue();
                } else if (statusObj instanceof String str) {
                    statusCode = Integer.parseInt(str);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            if (staticResponseConfig.containsKey("headers") && staticResponseConfig.get("headers") instanceof Map<?, ?> map) {
                map.forEach((k, v) -> headers.set(String.valueOf(k), String.valueOf(v)));
            }

            byte[] bodyBytes = new byte[0];
            if (staticResponseConfig.containsKey("body")) {
                Object bodyObj = staticResponseConfig.get("body");
                bodyBytes = bodyObj instanceof String str
                        ? str.getBytes(StandardCharsets.UTF_8)
                        : serialize(bodyObj);
            }

            AgentResponse response = AgentResponse.builder()
                    .status(HttpStatusCode.valueOf(statusCode))
                    .headers(headers)
                    .bufferedBody(Mono.just(bodyBytes))
                    .build();

            AgentContext updatedContext = context
                    .withAttribute(PipelineConstants.ATTR_RESPONSE, response)
                    .withAttribute(PipelineConstants.ATTR_SHORT_CIRCUIT, true);

            return Mono.just(updatedContext);
        }

        return Mono.just(context);
    }

    private byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            return String.valueOf(obj).getBytes(StandardCharsets.UTF_8);
        }
    }
}
