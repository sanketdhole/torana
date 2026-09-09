package com.phaselume.torana.pipeline.step.audit;

import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AuditEvent;
import com.phaselume.torana.core.spi.AuditSink;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Pipeline step that computes SHA-256 payload digests and emits structured AuditEvents to registered AuditSinks.
 */
@Component
public class AuditStep implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(AuditStep.class);
    public static final String STEP_TYPE = "audit";

    private final List<AuditSink> auditSinks;

    public AuditStep() {
        this(List.of());
    }

    @Autowired(required = false)
    public AuditStep(List<AuditSink> auditSinks) {
        this.auditSinks = auditSinks != null ? auditSinks : List.of();
    }

    @Override
    public String type() {
        return STEP_TYPE;
    }

    @Override
    public Mono<AgentContext> execute(AgentContext context) {
        StepDefinition stepDef = context.getAttribute(PipelineConstants.ATTR_CURRENT_STEP);

        boolean includePayloadHash = stepDef == null || stepDef.getParam("include-payload-hash", true);
        String action = stepDef != null ? stepDef.getParam("action", "PIPELINE_EXECUTE") : "PIPELINE_EXECUTE";

        String payloadHash = null;
        if (includePayloadHash && context.getRequest() != null && context.getRequest().getCachedBody() != null) {
            payloadHash = sha256Hex(context.getRequest().getCachedBody());
        }

        long latencyMillis = Duration.between(context.getCreatedAt(), Instant.now()).toMillis();

        AuditEvent event = AuditEvent.builder()
                .tenantId(context.getTenantId())
                .principalId(context.getAuthentication() != null ? context.getAuthentication().getPrincipalId() : null)
                .routeId(context.getMatchedRoute() != null ? context.getMatchedRoute().getId() : null)
                .action(action)
                .status("SUCCESS")
                .httpStatus(200)
                .latencyMillis(latencyMillis)
                .payloadHashSha256(payloadHash)
                .traceId(context.getTraceId())
                .spanId(context.getSpanId())
                .build();

        log.debug("Emitting audit event '{}' for route '{}'", event.getEventId(), event.getRouteId());

        if (auditSinks.isEmpty()) {
            return Mono.just(context);
        }

        return Flux.fromIterable(auditSinks)
                .flatMap(sink -> sink.publish(event).onErrorResume(e -> {
                    log.warn("AuditSink '{}' failed to publish event: {}", sink.type(), e.getMessage());
                    return Mono.empty();
                }))
                .then(Mono.just(context));
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
