package com.phaselume.torana.connector.s3.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Serialized representation of S3 LIST_OBJECTS results.
 */
@Value
@Builder
public class S3ListResult {

    String bucket;
    String prefix;
    List<S3Item> objects;
    boolean isTruncated;
    String nextContinuationToken;

    @Value
    @Builder
    public static class S3Item {
        String key;
        long size;
        Instant lastModified;
        String etag;
        String storageClass;
    }
}
