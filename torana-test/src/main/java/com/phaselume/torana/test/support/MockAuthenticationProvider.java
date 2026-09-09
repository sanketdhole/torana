package com.phaselume.torana.test.support;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock implementation of AuthenticationProvider for unit and integration testing.
 */
public class MockAuthenticationProvider implements AuthenticationProvider {

    private final String type;
    private ToranaAuthentication stubbedAuthentication;
    private boolean shouldFail = false;
    private final AtomicInteger invocationCount = new AtomicInteger(0);

    public MockAuthenticationProvider() {
        this("mock-jwt");
    }

    public MockAuthenticationProvider(String type) {
        this.type = type;
    }

    public MockAuthenticationProvider returns(ToranaAuthentication authentication) {
        this.stubbedAuthentication = authentication;
        this.shouldFail = false;
        return this;
    }

    public MockAuthenticationProvider fails() {
        this.shouldFail = true;
        return this;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        invocationCount.incrementAndGet();
        if (shouldFail) {
            return Mono.empty();
        }
        return stubbedAuthentication != null ? Mono.just(stubbedAuthentication) : Mono.empty();
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    public void reset() {
        this.invocationCount.set(0);
        this.stubbedAuthentication = null;
        this.shouldFail = false;
    }
}
