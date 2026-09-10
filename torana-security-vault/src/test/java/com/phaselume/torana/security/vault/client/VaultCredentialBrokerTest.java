package com.phaselume.torana.security.vault.client;

import com.phaselume.torana.core.model.BackendCredentials;
import com.phaselume.torana.security.vault.config.VaultProperties;
import com.phaselume.torana.security.vault.model.VaultCredentialMapping;
import com.phaselume.torana.security.vault.model.VaultSecretResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class VaultCredentialBrokerTest {

    private VaultReactiveClient vaultClient;
    private VaultProperties properties;
    private VaultCredentialBroker broker;

    @BeforeEach
    void setUp() {
        vaultClient = Mockito.mock(VaultReactiveClient.class);
        properties = new VaultProperties();
        broker = new VaultCredentialBroker(vaultClient, properties);
    }

    @Test
    void testBrokerDatabaseCredentials() {
        VaultCredentialMapping mapping = VaultCredentialMapping.builder()
                .backend("postgres-prod")
                .vaultPath("database/creds/readonly")
                .credentialType("database")
                .usernameField("username")
                .passwordField("password")
                .build();
        broker.addMapping(mapping);

        VaultSecretResponse response = new VaultSecretResponse();
        response.setLeaseDuration(60);
        response.setData(Map.of("username", "v-user-123", "password", "v-pass-456"));

        when(vaultClient.readSecret("database/creds/readonly")).thenReturn(Mono.just(response));

        StepVerifier.create(broker.broker(null, "postgres-prod"))
                .assertNext(creds -> {
                    assertEquals("BASIC", creds.getType());
                    assertEquals("v-user-123", creds.getUsername());
                    assertEquals("v-pass-456", creds.getPassword());
                    assertNotNull(creds.getExpiresAt());
                })
                .verifyComplete();
    }

    @Test
    void testBrokerAwsStsCredentials() {
        VaultCredentialMapping mapping = VaultCredentialMapping.builder()
                .backend("s3-docs")
                .vaultPath("aws/creds/s3-role")
                .credentialType("aws-sts")
                .build();
        broker.addMapping(mapping);

        VaultSecretResponse response = new VaultSecretResponse();
        response.setLeaseDuration(900);
        response.setData(Map.of(
                "access_key", "ASIAEXAMPLEKEY",
                "secret_key", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "security_token", "FwoGZXIvYXdzEBAaD..."
        ));

        when(vaultClient.readSecret("aws/creds/s3-role")).thenReturn(Mono.just(response));

        StepVerifier.create(broker.broker(null, "s3-docs"))
                .assertNext(creds -> {
                    assertEquals("AWS_STS", creds.getType());
                    assertEquals("ASIAEXAMPLEKEY", creds.getAccessKeyId());
                    assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", creds.getSecretAccessKey());
                    assertEquals("FwoGZXIvYXdzEBAaD...", creds.getSessionToken());
                })
                .verifyComplete();
    }

    @Test
    void testBrokerKvBearerToken() {
        VaultCredentialMapping mapping = VaultCredentialMapping.builder()
                .backend("hr-api")
                .vaultPath("secret/data/hr-token")
                .credentialType("kv")
                .kvField("api_key")
                .build();
        broker.addMapping(mapping);

        VaultSecretResponse response = new VaultSecretResponse();
        response.setData(Map.of("data", Map.of("api_key", "sec-token-789")));

        when(vaultClient.readSecret("secret/data/hr-token")).thenReturn(Mono.just(response));

        StepVerifier.create(broker.broker(null, "hr-api"))
                .assertNext(creds -> {
                    assertEquals("BEARER", creds.getType());
                    assertEquals("sec-token-789", creds.getToken());
                })
                .verifyComplete();
    }
}
