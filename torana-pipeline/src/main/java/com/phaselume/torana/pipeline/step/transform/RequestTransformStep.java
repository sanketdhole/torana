package com.phaselume.torana.pipeline.step.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Pipeline step that transforms the inbound AgentRequest body or headers using SpEL or template parameters.
 */
@Component
public class RequestTransformStep implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(RequestTransformStep.class);
    public static final String STEP_TYPE = "request-transform";

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String type() {
        return STEP_TYPE;
    }

    @Override
    public Mono<AgentContext> execute(AgentContext context) {
        StepDefinition stepDef = context.getAttribute(PipelineConstants.ATTR_CURRENT_STEP);
        if (stepDef == null || context.getRequest() == null) {
            return Mono.just(context);
        }

        AgentRequest request = context.getRequest();
        AgentRequest.AgentRequestBuilder requestBuilder = request.toBuilder();

        // 1. Direct body replacement if configured
        Object directBody = stepDef.getParam("body", null);
        if (directBody != null) {
            byte[] bodyBytes = directBody instanceof String str
                    ? str.getBytes(StandardCharsets.UTF_8)
                    : serialize(directBody);
            requestBuilder.cachedBody(bodyBytes);
        }

        // 2. SpEL Expression evaluation
        String expressionStr = stepDef.getParam("expression", null);
        if (expressionStr != null && !expressionStr.isBlank()) {
            try {
                StandardEvaluationContext evalContext = new StandardEvaluationContext();
                evalContext.setVariable("context", context);
                evalContext.setVariable("request", request);
                evalContext.setVariable("tenantId", context.getTenantId());
                evalContext.setVariable("principal", context.getAuthentication() != null
                        ? context.getAuthentication().getPrincipalId() : null);

                if (request.getCachedBody() != null) {
                    try {
                        Object parsedBody = objectMapper.readValue(request.getCachedBody(), Object.class);
                        evalContext.setVariable("input", parsedBody);
                    } catch (Exception e) {
                        evalContext.setVariable("input", new String(request.getCachedBody(), StandardCharsets.UTF_8));
                    }
                }

                Expression expr = expressionParser.parseExpression(expressionStr);
                Object result = expr.getValue(evalContext);
                if (result != null) {
                    byte[] transformedBytes = result instanceof String str
                            ? str.getBytes(StandardCharsets.UTF_8)
                            : serialize(result);
                    requestBuilder.cachedBody(transformedBytes);
                }
            } catch (Exception e) {
                log.error("Failed to evaluate SpEL expression '{}' in RequestTransformStep: {}",
                        expressionStr, e.getMessage());
                return Mono.error(e);
            }
        }

        // 3. Header replacements if configured
        Map<String, String> headersToReplace = stepDef.getParam("headers", null);
        if (headersToReplace != null && !headersToReplace.isEmpty()) {
            HttpHeaders updatedHeaders = new HttpHeaders();
            if (request.getHeaders() != null) {
                updatedHeaders.putAll(request.getHeaders());
            }
            headersToReplace.forEach(updatedHeaders::set);
            requestBuilder.headers(updatedHeaders);
        }

        return Mono.just(context.withRequest(requestBuilder.build()));
    }

    private byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            return String.valueOf(obj).getBytes(StandardCharsets.UTF_8);
        }
    }
}
