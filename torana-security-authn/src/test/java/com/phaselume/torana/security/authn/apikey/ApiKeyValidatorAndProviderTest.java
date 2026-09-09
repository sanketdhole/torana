package com.phaselume.torana.security.authn.apikey;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.ApiKeyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyValidatorAndProviderTest {

    private ApiKeyValidator validator;
    private ApiKeyAuthenticationProvider provider;
    private ApiKeyConfig config;

    @BeforeEach
    void setUp() {
        validator = new ApiKeyValidator();
        config = ApiKeyConfig.builder()
                .header("X-Api-Key")
                .allowQueryParam(true)
                .queryParamName("api_key")
                .build();
        provider = new ApiKeyAuthenticationProvider(validator, config);
    }

    @Test
    void testValidApiKeyHeaderAuthentication() {
        String rawKey = "sec_test_api_key_1234567890";
        ApiKeyMetadata metadata = ApiKeyMetadata.builder()
                .name("Service Backend Alpha")
                .principalId("svc-alpha")
                .tenantId("tenant-finance")
                .roles(Set.of("service", "tools:execute"))
                .scopes(Set.of("read", "write"))
                .active(true)
                .build();

        validator.registerKey(rawKey, metadata);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resource")
                .header("X-Api-Key", rawKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("svc-alpha", auth.getPrincipalId());
                    assertEquals("tenant-finance", auth.getTenantId());
                    assertEquals("apikey", auth.getAuthMethod());
                    assertTrue(auth.hasRole("service"));
                    assertTrue(auth.hasScope("write"));
                })
                .verifyComplete();
    }

    @Test
    void testValidApiKeyQueryParamAuthentication() {
        String rawKey = "sec_query_param_key_999";
        ApiKeyMetadata metadata = ApiKeyMetadata.builder()
                .name("Query User")
                .principalId("query-user")
                .active(true)
                .build();

        validator.registerKey(rawKey, metadata);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resource?api_key=" + rawKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("query-user", auth.getPrincipalId());
                })
                .verifyComplete();
    }

    @Test
    void testInactiveApiKeyFails() {
        String rawKey = "inactive_key_987";
        ApiKeyMetadata metadata = ApiKeyMetadata.builder()
                .name("Inactive App")
                .principalId("inactive-app")
                .active(false)
                .build();

        validator.registerKey(rawKey, metadata);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resource")
                .header("X-Api-Key", rawKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .expectErrorMatches(err -> err instanceof IllegalArgumentException && err.getMessage().contains("inactive"))
                .verify();
    }
}
