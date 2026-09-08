package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

/**
 * Result of an authorization evaluation (e.g. from OPA or internal policy engines).
 */
@Value
@Builder(toBuilder = true)
public class AuthzDecision {

    boolean allowed;
    String denyReason;
    String policyName;

    @Builder.Default
    AgentContext.Obligations obligations = AgentContext.Obligations.empty();

    @Builder.Default
    Map<String, Object> metadata = Collections.emptyMap();

    public static AuthzDecision allow() {
        return AuthzDecision.builder()
                .allowed(true)
                .build();
    }

    public static AuthzDecision allow(AgentContext.Obligations obligations) {
        return AuthzDecision.builder()
                .allowed(true)
                .obligations(obligations)
                .build();
    }

    public static AuthzDecision deny(String reason) {
        return AuthzDecision.builder()
                .allowed(false)
                .denyReason(reason)
                .build();
    }
}
