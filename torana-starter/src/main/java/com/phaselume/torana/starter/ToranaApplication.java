package com.phaselume.torana.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Torana Enterprise AI Gateway.
 */
@SpringBootApplication(scanBasePackages = "com.phaselume.torana")
public class ToranaApplication {

    private static final Logger log = LoggerFactory.getLogger(ToranaApplication.class);

    public static void main(String[] args) {
        log.info("Starting Torana Enterprise AI Gateway...");
        SpringApplication.run(ToranaApplication.class, args);
        log.info("Torana Enterprise AI Gateway started successfully.");
    }
}
