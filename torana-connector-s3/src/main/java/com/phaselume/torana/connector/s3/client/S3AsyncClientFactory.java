package com.phaselume.torana.connector.s3.client;

import com.phaselume.torana.connector.s3.model.S3ConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching S3AsyncClient instances per connector.
 */
public class S3AsyncClientFactory {

    private static final Logger log = LoggerFactory.getLogger(S3AsyncClientFactory.class);

    private final Map<String, S3AsyncClient> clientCache = new ConcurrentHashMap<>();

    public S3AsyncClient getClient(String connectorId, S3ConnectorConfig config) {
        String key = (connectorId != null && !connectorId.isBlank()) ? connectorId : "default-s3";
        return clientCache.computeIfAbsent(key, k -> createClient(config));
    }

    public S3AsyncClient createClient(S3ConnectorConfig config) {
        if (config == null) {
            config = new S3ConnectorConfig();
        }

        String regionStr = config.getRegion() != null ? config.getRegion() : "us-east-1";
        Region region = Region.of(regionStr);

        AwsCredentialsProvider credentialsProvider = resolveCredentials(config);

        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .region(region)
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(config.isForcePathStyle())
                        .build());

        if (config.getEndpointOverride() != null && !config.getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(config.getEndpointOverride()));
        }

        log.info("Initialized S3AsyncClient for region {} (endpoint override: {})", region, config.getEndpointOverride());
        return builder.build();
    }

    private AwsCredentialsProvider resolveCredentials(S3ConnectorConfig config) {
        if (config.getAccessKeyId() != null && config.getSecretAccessKey() != null) {
            if (config.getSessionToken() != null && !config.getSessionToken().isBlank()) {
                return StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey(), config.getSessionToken())
                );
            }
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey())
            );
        }

        try {
            return DefaultCredentialsProvider.create();
        } catch (Exception e) {
            log.warn("Default AWS credentials provider failed, using anonymous credentials: {}", e.getMessage());
            return AnonymousCredentialsProvider.create();
        }
    }
}
