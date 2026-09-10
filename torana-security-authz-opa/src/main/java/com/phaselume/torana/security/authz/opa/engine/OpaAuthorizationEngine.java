package com.phaselume.torana.security.authz.opa.engine;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AuthzDecision;
import com.phaselume.torana.core.spi.AuthorizationEngine;
import com.phaselume.torana.security.authz.opa.cache.OpaDecisionCache;
import com.phaselume.torana.security.authz.opa.client.OpaClient;
import com.phaselume.torana.security.authz.opa.client.OpaInputBuilder;
import com.phaselume.torana.security.authz.opa.filter.ObligationProcessor;
import com.phaselume.torana.security.authz.opa.model.OpaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Authorization engine evaluating requests against Open Policy Agent (OPA).
 * Implements the {@link AuthorizationEngine} SPI.
 */
public class OpaAuthorizationEngine implements AuthorizationEngine {

    private static final Logger log = LoggerFactory.getLogger(OpaAuthorizationEngine.class);

    private final OpaClient opaClient;
    private final OpaInputBuilder inputBuilder;
    private final OpaDecisionCache decisionCache;
    private final ObligationProcessor obligationProcessor;
    private final AuthzProperties properties;

    public OpaAuthorizationEngine(OpaClient opaClient,
                                  OpaInputBuilder inputBuilder,
                                  OpaDecisionCache decisionCache,
                                  ObligationProcessor obligationProcessor,
                                  AuthzProperties properties) {
        this.opaClient = opaClient != null ? opaClient : new OpaClient();
        this.inputBuilder = inputBuilder != null ? inputBuilder : new OpaInputBuilder();
        this.decisionCache = decisionCache != null ? decisionCache : new OpaDecisionCache();
        this.obligationProcessor = obligationProcessor != null ? obligationProcessor : new ObligationProcessor();
        this.properties = properties != null ? properties : new AuthzProperties();
    }

    @Override
    public Mono<AuthzDecision> authorize(AgentContext context) {
        if (!properties.isEnabled()) {
            return Mono.just(AuthzDecision.allow());
        }

        Map<String, Object> input = inputBuilder.buildInput(context);
        String policyPath = properties.getPolicyPackage();

        // 1. Check Decision Cache
        return decisionCache.get(policyPath, input)
                .switchIfEmpty(
                        // 2. Cache Miss: Query OPA Client
                        opaClient.evaluate(input)
                                .flatMap(response -> {
                                    // Store successful / definitive evaluations in cache
                                    return decisionCache.put(policyPath, input, response)
                                            .thenReturn(response);
                                })
                )
                .map(this::toAuthzDecision);
    }

    private AuthzDecision toAuthzDecision(OpaResponse response) {
        if (response == null || !response.isAllowed()) {
            String reason = (response != null && response.getDenyReason() != null)
                    ? response.getDenyReason()
                    : "Access denied by policy";
            return AuthzDecision.deny(reason);
        }

        AgentContext.Obligations obligations = obligationProcessor.process(response.getObligations());
        return AuthzDecision.builder()
                .allowed(true)
                .policyName(properties.getPolicyPackage())
                .obligations(obligations)
                .build();
    }
}
