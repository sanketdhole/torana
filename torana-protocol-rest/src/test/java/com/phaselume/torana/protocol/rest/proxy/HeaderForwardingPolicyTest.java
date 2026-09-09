package com.phaselume.torana.protocol.rest.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderForwardingPolicyTest {

    @Test
    void testHopByHopHeadersStripped() {
        HeaderForwardingPolicy policy = new HeaderForwardingPolicy();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Transfer-Encoding", "chunked");
        headers.add("Connection", "keep-alive");
        headers.add("Host", "gateway.example.com");
        headers.add("X-Custom-Header", "allowed-value");

        HttpHeaders filtered = policy.filterRequestHeaders(headers);

        assertFalse(filtered.containsHeader("Transfer-Encoding"));
        assertFalse(filtered.containsHeader("Connection"));
        assertFalse(filtered.containsHeader("Host"));
        assertTrue(filtered.containsHeader("X-Custom-Header"));
        assertEquals("allowed-value", filtered.getFirst("X-Custom-Header"));
    }

    @Test
    void testAllowListEnforced() {
        HeaderForwardingPolicy policy = new HeaderForwardingPolicy(
                Set.of("authorization", "x-tenant-id"),
                Set.of(),
                true);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer token123");
        headers.add("X-Tenant-Id", "tenant-alpha");
        headers.add("X-Unwanted-Header", "should-be-dropped");
        headers.add("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        HttpHeaders filtered = policy.filterRequestHeaders(headers);

        assertTrue(filtered.containsHeader("Authorization"));
        assertTrue(filtered.containsHeader("X-Tenant-Id"));
        assertTrue(filtered.containsHeader("traceparent")); // trace headers forwarded by default
        assertFalse(filtered.containsHeader("X-Unwanted-Header"));
    }

    @Test
    void testBlockListEnforced() {
        HeaderForwardingPolicy policy = new HeaderForwardingPolicy(
                Set.of(),
                Set.of("x-internal-secret"),
                true);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Internal-Secret", "secret123");
        headers.add("Accept", "application/json");

        HttpHeaders filtered = policy.filterRequestHeaders(headers);

        assertFalse(filtered.containsHeader("X-Internal-Secret"));
        assertTrue(filtered.containsHeader("Accept"));
    }
}
