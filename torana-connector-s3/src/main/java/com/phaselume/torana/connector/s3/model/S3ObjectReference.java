package com.phaselume.torana.connector.s3.model;

import lombok.Builder;
import lombok.Value;

/**
 * Parsed S3 object reference from AgentContext/request.
 */
@Value
@Builder
public class S3ObjectReference {

    String bucket;
    String key;
    String versionId;
    String operation; // GET_OBJECT, PUT_OBJECT, LIST_OBJECTS, HEAD_OBJECT, GENERATE_PRESIGNED_URL
    String prefix;
    Integer maxKeys;

    public static S3ObjectReference of(String bucket, String key) {
        return S3ObjectReference.builder()
                .bucket(bucket)
                .key(key)
                .operation("GET_OBJECT")
                .build();
    }
}
