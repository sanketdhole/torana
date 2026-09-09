package com.phaselume.torana.routing.reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * Listens for RouteChangeEvent and writes structured audit logs.
 */
public class RouteChangeAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(RouteChangeAuditLogger.class);

    @EventListener(RouteChangeEvent.class)
    public void onRouteChanged(RouteChangeEvent event) {
        log.info("Torana routes reloaded successfully at {}. Old count: {}, New count: {}",
                event.getEventTimestamp(),
                event.getPreviousRoutes().size(),
                event.getCurrentRoutes().size());
    }
}
