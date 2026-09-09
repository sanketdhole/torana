package com.phaselume.torana.test.containers;

import org.testcontainers.vault.VaultContainer;

/**
 * Pre-configured HashiCorp Vault Testcontainer for credential brokering tests.
 */
public class ToranaVaultContainer {

    private static final String DEFAULT_TOKEN = "test-root-token";
    private final VaultContainer<?> container;

    public ToranaVaultContainer() {
        this.container = new VaultContainer<>("hashicorp/vault:1.15")
                .withVaultToken(DEFAULT_TOKEN);
    }

    public void start() {
        if (!container.isRunning()) {
            container.start();
        }
    }

    public void stop() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    public String getVaultAddress() {
        return String.format("http://%s:%d", container.getHost(), container.getFirstMappedPort());
    }

    public String getVaultToken() {
        return DEFAULT_TOKEN;
    }

    public VaultContainer<?> getContainer() {
        return container;
    }
}
