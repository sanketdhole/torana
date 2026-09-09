package com.phaselume.torana.protocol.grpc.codec;

import io.grpc.Metadata;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * Extracts standard metadata keys from incoming gRPC Metadata into HTTP headers and attributes.
 */
public class GrpcMetadataExtractor {

    public static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> API_KEY =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TRACEPARENT_KEY =
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TENANT_ID_KEY =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * Extracts gRPC metadata keys into Spring HttpHeaders.
     */
    public HttpHeaders extractHeaders(Metadata metadata) {
        HttpHeaders headers = new HttpHeaders();
        if (metadata == null) {
            return headers;
        }

        for (String keyName : metadata.keys()) {
            if (keyName.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                continue;
            }
            Metadata.Key<String> key = Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER);
            Iterable<String> values = metadata.getAll(key);
            if (values != null) {
                for (String val : values) {
                    headers.add(keyName, val);
                }
            }
        }
        return headers;
    }

    /**
     * Extracts attributes such as tenantId and traceId into a key-value Map.
     */
    public Map<String, Object> extractAttributes(Metadata metadata) {
        Map<String, Object> attributes = new HashMap<>();
        if (metadata == null) {
            return attributes;
        }

        String tenantId = metadata.get(TENANT_ID_KEY);
        if (tenantId != null) {
            attributes.put("torana.tenant.id", tenantId);
        }

        String traceparent = metadata.get(TRACEPARENT_KEY);
        if (traceparent != null) {
            attributes.put("torana.trace.parent", traceparent);
        }

        return attributes;
    }
}
