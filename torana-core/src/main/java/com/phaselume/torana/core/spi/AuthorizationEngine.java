package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AuthzDecision;
import reactor.core.publisher.Mono;

/**
 * SPI for authorization engines (e.g. OPA sidecar, local Rego, ABAC engines).
 */
public interface AuthorizationEngine {

    /**
     * Evaluate fine-grained policy rules for a fully-authenticated context.
     * Returns an AuthzDecision with allow/deny and obligations (row filters, column masks).
     */
    Mono<AuthzDecision> authorize(AgentContext context);
}
