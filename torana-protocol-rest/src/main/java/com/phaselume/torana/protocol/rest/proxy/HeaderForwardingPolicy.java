package com.phaselume.torana.protocol.rest.proxy;

import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manages header forwarding policies between clients and backend upstream services.
 * Drops hop-by-hop headers and enforces allow-list / block-list rules to prevent header leakage.
 */
public class HeaderForwardingPolicy {

    /**
     * Standard HTTP/1.1 and HTTP/2 hop-by-hop headers that should never be forwarded.
     */
    public static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
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

    private final Set<String> allowList;
    private final Set<String> blockList;
    private final boolean forwardTraceHeaders;

    public HeaderForwardingPolicy() {
        this(Collections.emptySet(), Collections.emptySet(), true);
    }

    public HeaderForwardingPolicy(Set<String> allowList, Set<String> blockList, boolean forwardTraceHeaders) {
        this.allowList = normalizeSet(allowList);
        this.blockList = normalizeSet(blockList);
        this.forwardTraceHeaders = forwardTraceHeaders;
    }

    /**
     * Filters and prepares inbound headers for upstream forwarding.
     */
    public HttpHeaders filterRequestHeaders(HttpHeaders incomingHeaders) {
        if (incomingHeaders == null) {
            return new HttpHeaders();
        }

        HttpHeaders forwarded = new HttpHeaders();
        incomingHeaders.forEach((name, values) -> {
            String lowerName = name.toLowerCase(Locale.ROOT);

            // Always strip hop-by-hop headers
            if (HOP_BY_HOP_HEADERS.contains(lowerName)) {
                return;
            }

            // Check block-list
            if (blockList.contains(lowerName)) {
                return;
            }

            // If allow-list is defined, header must be in allow-list or be a trace header if allowed
            if (!allowList.isEmpty() && !allowList.contains(lowerName)) {
                if (forwardTraceHeaders && isTraceHeader(lowerName)) {
                    forwarded.addAll(name, values);
                }
                return;
            }

            forwarded.addAll(name, values);
        });

        return forwarded;
    }

    /**
     * Filters response headers from upstream before sending downstream.
     */
    public HttpHeaders filterResponseHeaders(HttpHeaders upstreamHeaders) {
        if (upstreamHeaders == null) {
            return new HttpHeaders();
        }

        HttpHeaders downstream = new HttpHeaders();
        upstreamHeaders.forEach((name, values) -> {
            String lowerName = name.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(lowerName) || blockList.contains(lowerName)) {
                return;
            }
            downstream.addAll(name, values);
        });

        return downstream;
    }

    private static boolean isTraceHeader(String lowerName) {
        return lowerName.startsWith("x-trace-")
                || lowerName.startsWith("x-b3-")
                || lowerName.equals("traceparent")
                || lowerName.equals("tracestate")
                || lowerName.startsWith("x-torana-");
    }

    private static Set<String> normalizeSet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>(source.size());
        for (String item : source) {
            if (item != null) {
                normalized.add(item.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
