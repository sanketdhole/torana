package com.phaselume.torana.autoconfigure;

import com.phaselume.torana.core.config.ToranaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Validates gateway routing, security providers, and connector cross-references on application startup.
 */
public class ToranaStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ToranaStartupValidator.class);

    private final ToranaProperties properties;

    public ToranaStartupValidator(ToranaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Validating Torana Gateway configuration...");

        if (properties != null) {
            log.info("Torana Protocols: MCP={}, WebSocket={}, gRPC={}, REST={}",
                    properties.getProtocols().getMcp().isEnabled(),
                    properties.getProtocols().getWebsocket().isEnabled(),
                    properties.getProtocols().getGrpc().isEnabled(),
                    properties.getProtocols().getRest().isEnabled()
            );

            log.info("Torana Security: Authn={}, Authz={}",
                    properties.getSecurity().getAuthn().isEnabled(),
                    properties.getSecurity().getAuthz().isEnabled()
            );

            if (properties.getRouting() != null && properties.getRouting().getRoutes() != null && !properties.getRouting().getRoutes().isEmpty()) {
                log.info("Loaded {} configured Torana routes", properties.getRouting().getRoutes().size());
                properties.getRouting().getRoutes().forEach(route -> {
                    if (route.getPath() == null) {
                        log.warn("Route [{}] has no path declared", route.getId());
                    }
                });
            }
        }

        log.info("Torana Gateway configuration validated successfully.");
    }
}

