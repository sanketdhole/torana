package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.BackendCredentials;
import reactor.core.publisher.Mono;

/**
 * SPI for dynamic credential brokering (e.g. HashiCorp Vault, AWS STS).
 */
public interface CredentialBroker {

    /**
     * Broker dynamic or short-lived credentials for the given backend reference scoped to the user context.
     */
    Mono<BackendCredentials> broker(AgentContext context, String backendRef);
}
