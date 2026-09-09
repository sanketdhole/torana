package com.phaselume.torana.pipeline.step.transform;

import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Pipeline step that injects enriched headers (trace ID, tenant ID, principal info, static headers)
 * into the AgentRequest.
 */
@Component
public class HeaderEnrichStep implements PipelineStep {

    public static final String STEP_TYPE = "header-enrich";

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

        Map<String, String> headersConfig = stepDef.getParam("headers", null);
        if (headersConfig == null || headersConfig.isEmpty()) {
            return Mono.just(context);
        }

        HttpHeaders updatedHeaders = new HttpHeaders();
        if (context.getRequest().getHeaders() != null) {
            updatedHeaders.putAll(context.getRequest().getHeaders());
        }

        for (Map.Entry<String, String> entry : headersConfig.entrySet()) {
            String headerName = entry.getKey();
            String headerVal = resolvePlaceholder(entry.getValue(), context);
            if (headerVal != null) {
                updatedHeaders.set(headerName, headerVal);
            }
        }

        AgentRequest updatedRequest = context.getRequest().toBuilder()
                .headers(updatedHeaders)
                .build();

        return Mono.just(context.withRequest(updatedRequest));
    }

    private String resolvePlaceholder(String template, AgentContext context) {
        if (template == null) {
            return null;
        }

        String result = template;
        if (context.getTenantId() != null) {
            result = result.replace("${context.tenantId}", context.getTenantId());
            result = result.replace("${tenantId}", context.getTenantId());
        }
        if (context.getTraceId() != null) {
            result = result.replace("${context.traceId}", context.getTraceId());
            result = result.replace("${traceId}", context.getTraceId());
        }
        if (context.getSpanId() != null) {
            result = result.replace("${context.spanId}", context.getSpanId());
        }
        if (context.getAuthentication() != null) {
            if (context.getAuthentication().getPrincipalId() != null) {
                result = result.replace("${principal.id}", context.getAuthentication().getPrincipalId());
                result = result.replace("${principal.principalId}", context.getAuthentication().getPrincipalId());
            }
            if (context.getAuthentication().getName() != null) {
                result = result.replace("${principal.name}", context.getAuthentication().getName());
            }
        }
        if (context.getMatchedRoute() != null && context.getMatchedRoute().getId() != null) {
            result = result.replace("${route.id}", context.getMatchedRoute().getId());
        }

        return result;
    }
}
