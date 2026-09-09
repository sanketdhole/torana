package com.phaselume.torana.security.authn.outbound;

import com.phaselume.torana.core.model.ToranaAuthentication;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Spring WebFlux {@link ExchangeFilterFunction} that decorates outbound WebClient requests
 * with credentials determined by {@link OutboundAuthPolicyEngine}.
 */
public class OutboundAuthExchangeFilter implements ExchangeFilterFunction {

    private final OutboundAuthPolicyEngine policyEngine;
    private final String defaultServiceId;

    public OutboundAuthExchangeFilter(OutboundAuthPolicyEngine policyEngine, String defaultServiceId) {
        this.policyEngine = policyEngine != null ? policyEngine : new OutboundAuthPolicyEngine();
        this.defaultServiceId = defaultServiceId;
    }

    public OutboundAuthExchangeFilter(OutboundAuthPolicyEngine policyEngine) {
        this(policyEngine, null);
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String serviceId = request.attribute("TORANA_SERVICE_ID")
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(defaultServiceId != null ? defaultServiceId : request.url().getHost());

        return Mono.deferContextual(ctx -> {
            ToranaAuthentication auth = ctx.getOrDefault(ToranaAuthentication.class, null);

            ClientRequest.Builder requestBuilder = ClientRequest.from(request);
            requestBuilder.headers(headers -> policyEngine.applyOutboundAuth(headers, serviceId, auth));

            return next.exchange(requestBuilder.build());
        });
    }
}
