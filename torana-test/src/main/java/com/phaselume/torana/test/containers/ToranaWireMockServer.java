package com.phaselume.torana.test.containers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Pre-configured WireMock server for mocking upstream AI providers (LiteLLM, OpenAI) and enterprise HTTP APIs.
 */
public class ToranaWireMockServer {

    private final WireMockServer server;

    public ToranaWireMockServer() {
        this(0); // Dynamic free port
    }

    public ToranaWireMockServer(int port) {
        this.server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
    }

    public void start() {
        if (!server.isRunning()) {
            server.start();
        }
    }

    public void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public int port() {
        return server.port();
    }

    public WireMockServer getWireMockServer() {
        return server;
    }

    public void resetAll() {
        server.resetAll();
    }
}
