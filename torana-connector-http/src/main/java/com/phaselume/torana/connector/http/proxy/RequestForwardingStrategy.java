package com.phaselume.torana.connector.http.proxy;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Prepares HTTP request forwarding parameters (URI, method, headers) for upstream invocation.
 */
public class RequestForwardingStrategy {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade",
            "host"
    );

    /**
     * Resolves the full upstream target URI, applying path rewrites and query params.
     */
    public URI resolveTargetUri(AgentContext context, ConnectorConfig config) {
        String baseUrl = config.getEndpoint() != null ? config.getEndpoint() : config.getProperty("baseUrl", "http://localhost");
        baseUrl = baseUrl.replaceAll("/+$", "");

        String rawPath = "";
        AgentRequest request = context != null ? context.getRequest() : null;
        if (request != null && request.getPath() != null) {
            rawPath = request.getPath();
        }

        String rewrittenPath = applyPathRewrites(rawPath, config);
        if (!rewrittenPath.startsWith("/")) {
            rewrittenPath = "/" + rewrittenPath;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + rewrittenPath);

        if (request != null && request.getQueryParams() != null) {
            builder.queryParams(request.getQueryParams());
        }

        return builder.build(true).toUri();
    }

    /**
     * Resolves the HTTP method to use for upstream request.
     */
    public HttpMethod resolveMethod(AgentContext context) {
        if (context != null && context.getRequest() != null && context.getRequest().getMethod() != null) {
            return context.getRequest().getMethod();
        }
        return HttpMethod.POST;
    }

    /**
     * Filters and prepares headers for the upstream request.
     */
    public HttpHeaders prepareHeaders(AgentContext context, ConnectorConfig config) {
        HttpHeaders targetHeaders = new HttpHeaders();
        if (context == null || context.getRequest() == null || context.getRequest().getHeaders() == null) {
            return targetHeaders;
        }

        HttpHeaders inbound = context.getRequest().getHeaders();
        Set<String> blockList = getHeaderBlockList(config);
        Set<String> allowList = getHeaderAllowList(config);

        inbound.forEach((key, values) -> {
            String lower = key.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(lower) || blockList.contains(lower)) {
                return;
            }
            if (!allowList.isEmpty() && !allowList.contains(lower)) {
                return;
            }
            targetHeaders.addAll(key, values);
        });

        // Inject custom headers from config
        Object headersObj = config.getProperties().get("headers");
        if (headersObj instanceof Map<?, ?> map) {
            Object injectObj = map.get("inject");
            if (injectObj instanceof Map<?, ?> injectMap) {
                injectMap.forEach((k, v) -> {
                    if (k != null && v != null) {
                        targetHeaders.set(k.toString(), v.toString());
                    }
                });
            }
        }

        return targetHeaders;
    }

    private String applyPathRewrites(String path, ConnectorConfig config) {
        if (path == null) return "";

        Object rewriteObj = config.getProperties().get("path-rewrite");
        if (!(rewriteObj instanceof Map<?, ?> map)) {
            return path;
        }

        String stripPrefix = (String) map.get("strip-prefix");
        if (stripPrefix != null && !stripPrefix.isBlank() && path.startsWith(stripPrefix)) {
            path = path.substring(stripPrefix.length());
        }

        String prependPrefix = (String) map.get("prepend-prefix");
        if (prependPrefix != null && !prependPrefix.isBlank()) {
            path = prependPrefix.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
        }

        return path;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getHeaderBlockList(ConnectorConfig config) {
        Set<String> set = new HashSet<>();
        Object headersObj = config.getProperties().get("headers");
        if (headersObj instanceof Map<?, ?> map) {
            Object blockObj = map.get("block");
            if (blockObj instanceof Collection<?> list) {
                for (Object item : list) {
                    if (item != null) set.add(item.toString().toLowerCase(Locale.ROOT));
                }
            }
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getHeaderAllowList(ConnectorConfig config) {
        Set<String> set = new HashSet<>();
        Object headersObj = config.getProperties().get("headers");
        if (headersObj instanceof Map<?, ?> map) {
            Object forwardObj = map.get("forward");
            if (forwardObj instanceof Collection<?> list) {
                for (Object item : list) {
                    if (item != null) set.add(item.toString().toLowerCase(Locale.ROOT));
                }
            }
        }
        return set;
    }
}
