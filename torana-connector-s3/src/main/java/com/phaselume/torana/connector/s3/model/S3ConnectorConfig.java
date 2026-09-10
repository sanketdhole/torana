package com.phaselume.torana.connector.s3.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for S3 connector instances.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3ConnectorConfig {

    private String region;
    private String bucket;
    private String pathPrefix;
    private String endpointOverride;
    private boolean forcePathStyle;

    @Builder.Default
    private boolean credentialsFromVault = false;
    private String vaultRef;

    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;

    @Builder.Default
    private List<String> allowedOperations = new ArrayList<>(List.of("GET_OBJECT", "LIST_OBJECTS", "HEAD_OBJECT"));

    @Builder.Default
    private long maxObjectSizeBytes = 100 * 1024 * 1024L; // 100MB

    @Builder.Default
    private Duration presignedUrlTtl = Duration.ofHours(1);

    @Builder.Default
    private Duration timeout = Duration.ofSeconds(30);

    private String resilienceProfile;
}
