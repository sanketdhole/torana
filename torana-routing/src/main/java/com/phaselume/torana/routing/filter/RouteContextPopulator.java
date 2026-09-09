package com.phaselume.torana.routing.filter;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentContext;

/**
 * Populates RouteDefinition and route-specific metadata into AgentContext.
 */
public class RouteContextPopulator {

    public AgentContext populate(AgentContext context, RouteDefinition route) {
        if (context == null || route == null) {
            return context;
        }

        AgentContext updated = context.withMatchedRoute(route);
        if (route.getTimeout() != null) {
            updated = updated.withAttribute("torana.route.timeout", route.getTimeout());
        }
        if (route.getPipelineRef() != null) {
            updated = updated.withAttribute("torana.pipeline.ref", route.getPipelineRef());
        }
        if (route.getBackendRef() != null) {
            updated = updated.withAttribute("torana.backend.ref", route.getBackendRef());
        }
        if (route.getRateLimitRef() != null) {
            updated = updated.withAttribute("torana.ratelimit.ref", route.getRateLimitRef());
        }
        if (route.getAuthRef() != null) {
            updated = updated.withAttribute("torana.auth.ref", route.getAuthRef());
        }

        return updated;
    }
}
