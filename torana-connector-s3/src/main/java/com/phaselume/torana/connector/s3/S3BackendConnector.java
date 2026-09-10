package com.phaselume.torana.connector.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.s3.client.S3AsyncClientFactory;
import com.phaselume.torana.connector.s3.client.S3OperationGuard;
import com.phaselume.torana.connector.s3.model.S3ConnectorConfig;
import com.phaselume.torana.connector.s3.model.S3ListResult;
import com.phaselume.torana.connector.s3.model.S3ObjectReference;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reactive backend connector for AWS S3 and S3-compatible object storage.
 */
public class S3BackendConnector implements BackendConnector {

    private static final Logger log = LoggerFactory.getLogger(S3BackendConnector.class);

    private final S3AsyncClientFactory clientFactory;
    private final S3OperationGuard operationGuard;
    private final ResilienceDecorator resilienceDecorator;
    private final ObjectMapper objectMapper;

    public S3BackendConnector(S3AsyncClientFactory clientFactory,
                              S3OperationGuard operationGuard,
                              ResilienceDecorator resilienceDecorator,
                              ObjectMapper objectMapper) {
        this.clientFactory = clientFactory != null ? clientFactory : new S3AsyncClientFactory();
        this.operationGuard = operationGuard != null ? operationGuard : new S3OperationGuard();
        this.resilienceDecorator = resilienceDecorator;
        ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
        mapper.findAndRegisterModules();
        this.objectMapper = mapper;
    }

    public S3BackendConnector() {
        this(new S3AsyncClientFactory(), new S3OperationGuard(), null, new ObjectMapper());
    }

    @Override
    public String type() {
        return "s3";
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        return config != null && "s3".equalsIgnoreCase(config.getType());
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        if (config == null) {
            return Flux.error(new IllegalArgumentException("ConnectorConfig must not be null"));
        }

        S3ConnectorConfig s3Config = mapConfig(config);
        S3ObjectReference ref = parseReference(context, s3Config);

        operationGuard.validate(s3Config, ref);

        String profileName = s3Config.getResilienceProfile() != null ? s3Config.getResilienceProfile() : "default";

        if (resilienceDecorator != null) {
            return resilienceDecorator.decorate(profileName, null, () -> dispatchOperation(context, s3Config, ref, config.getId()));
        }

        return dispatchOperation(context, s3Config, ref, config.getId());
    }

    private Flux<AgentResponse.Chunk> dispatchOperation(AgentContext context, S3ConnectorConfig s3Config, S3ObjectReference ref, String connectorId) {
        S3AsyncClient client = clientFactory.getClient(connectorId, s3Config);
        String operation = ref.getOperation() != null ? ref.getOperation().toUpperCase() : "GET_OBJECT";

        switch (operation) {
            case "LIST_OBJECTS":
                return listObjects(client, s3Config, ref);
            case "PUT_OBJECT":
                return putObject(client, context, s3Config, ref);
            case "HEAD_OBJECT":
                return headObject(client, s3Config, ref);
            case "GET_OBJECT":
            default:
                return getObject(client, s3Config, ref);
        }
    }

    private Flux<AgentResponse.Chunk> getObject(S3AsyncClient client, S3ConnectorConfig s3Config, S3ObjectReference ref) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(ref.getBucket())
                .key(ref.getKey())
                .versionId(ref.getVersionId())
                .build();

        return Mono.fromFuture(() -> client.getObject(request, AsyncResponseTransformer.toPublisher()))
                .flatMapMany(responsePublisher -> Flux.from(responsePublisher)
                        .map(this::byteBufferToChunk)
                )
                .concatWith(Mono.just(AgentResponse.Chunk.last("stop")));
    }

    private Flux<AgentResponse.Chunk> listObjects(S3AsyncClient client, S3ConnectorConfig s3Config, S3ObjectReference ref) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                .bucket(ref.getBucket());

        if (ref.getPrefix() != null && !ref.getPrefix().isBlank()) {
            builder.prefix(ref.getPrefix());
        }
        if (ref.getMaxKeys() != null && ref.getMaxKeys() > 0) {
            builder.maxKeys(ref.getMaxKeys());
        }

        return Mono.fromFuture(() -> client.listObjectsV2(builder.build()))
                .flatMapMany(resp -> {
                    List<S3ListResult.S3Item> items = new ArrayList<>();
                    for (S3Object obj : resp.contents()) {
                        items.add(S3ListResult.S3Item.builder()
                                .key(obj.key())
                                .size(obj.size())
                                .lastModified(obj.lastModified())
                                .etag(obj.eTag())
                                .storageClass(obj.storageClassAsString())
                                .build());
                    }

                    S3ListResult listResult = S3ListResult.builder()
                            .bucket(resp.name())
                            .prefix(resp.prefix())
                            .objects(items)
                            .isTruncated(resp.isTruncated())
                            .nextContinuationToken(resp.nextContinuationToken())
                            .build();

                    try {
                        String json = objectMapper.writeValueAsString(listResult);
                        AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                                .textDelta(json)
                                .data(json.getBytes(StandardCharsets.UTF_8))
                                .last(true)
                                .finishReason("stop")
                                .metadata(Map.of("objectCount", items.size(), "bucket", resp.name()))
                                .build();
                        return Flux.just(chunk);
                    } catch (Exception e) {
                        return Flux.error(e);
                    }
                });
    }

    private Flux<AgentResponse.Chunk> putObject(S3AsyncClient client, AgentContext context, S3ConnectorConfig s3Config, S3ObjectReference ref) {
        AgentRequest req = context != null ? context.getRequest() : null;
        byte[] body = (req != null && req.getCachedBody() != null) ? req.getCachedBody() : new byte[0];

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(ref.getBucket())
                .key(ref.getKey())
                .contentLength((long) body.length)
                .build();

        return Mono.fromFuture(() -> client.putObject(putReq, AsyncRequestBody.fromBytes(body)))
                .map(resp -> {
                    String msg = String.format("{\"bucket\":\"%s\",\"key\":\"%s\",\"etag\":%s,\"status\":\"created\"}",
                            ref.getBucket(), ref.getKey(), resp.eTag());
                    return AgentResponse.Chunk.builder()
                            .textDelta(msg)
                            .data(msg.getBytes(StandardCharsets.UTF_8))
                            .last(true)
                            .finishReason("stop")
                            .metadata(Map.of("etag", resp.eTag() != null ? resp.eTag() : ""))
                            .build();
                })
                .flux();
    }

    private Flux<AgentResponse.Chunk> headObject(S3AsyncClient client, S3ConnectorConfig s3Config, S3ObjectReference ref) {
        HeadObjectRequest headReq = HeadObjectRequest.builder()
                .bucket(ref.getBucket())
                .key(ref.getKey())
                .build();

        return Mono.fromFuture(() -> client.headObject(headReq))
                .map(resp -> {
                    String msg = String.format("{\"bucket\":\"%s\",\"key\":\"%s\",\"contentLength\":%d,\"contentType\":\"%s\",\"etag\":%s}",
                            ref.getBucket(), ref.getKey(), resp.contentLength(), resp.contentType(), resp.eTag());
                    return AgentResponse.Chunk.builder()
                            .textDelta(msg)
                            .data(msg.getBytes(StandardCharsets.UTF_8))
                            .last(true)
                            .finishReason("stop")
                            .metadata(Map.of("contentLength", resp.contentLength(), "contentType", resp.contentType() != null ? resp.contentType() : ""))
                            .build();
                })
                .flux();
    }

    private AgentResponse.Chunk byteBufferToChunk(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return AgentResponse.Chunk.builder()
                .data(bytes)
                .textDelta(new String(bytes, StandardCharsets.UTF_8))
                .last(false)
                .build();
    }

    private S3ObjectReference parseReference(AgentContext context, S3ConnectorConfig config) {
        String bucket = config.getBucket();
        String key = null;
        String operation = "GET_OBJECT";
        String prefix = config.getPathPrefix();
        Integer maxKeys = null;

        if (context != null) {
            AgentRequest req = context.getRequest();
            if (req != null) {
                if (req.getPath() != null && !req.getPath().isBlank()) {
                    key = req.getPath().startsWith("/") ? req.getPath().substring(1) : req.getPath();
                }
                if (req.getQueryParams() != null) {
                    if (req.getQueryParams().containsKey("op")) {
                        operation = req.getQueryParams().getFirst("op");
                    }
                    if (req.getQueryParams().containsKey("key")) {
                        key = req.getQueryParams().getFirst("key");
                    }
                    if (req.getQueryParams().containsKey("prefix")) {
                        prefix = req.getQueryParams().getFirst("prefix");
                    }
                    if (req.getQueryParams().containsKey("bucket")) {
                        bucket = req.getQueryParams().getFirst("bucket");
                    }
                    if (req.getQueryParams().containsKey("maxKeys")) {
                        try {
                            maxKeys = Integer.parseInt(req.getQueryParams().getFirst("maxKeys"));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            Object opAttr = context.getAttribute("operation");
            if (opAttr != null) {
                operation = opAttr.toString();
            }
            Object keyAttr = context.getAttribute("key");
            if (keyAttr != null) {
                key = keyAttr.toString();
            }
            Object bucketAttr = context.getAttribute("bucket");
            if (bucketAttr != null) {
                bucket = bucketAttr.toString();
            }
        }

        if (key == null && (prefix != null || "LIST_OBJECTS".equalsIgnoreCase(operation))) {
            operation = "LIST_OBJECTS";
        }

        return S3ObjectReference.builder()
                .bucket(bucket != null ? bucket : "default-bucket")
                .key(key)
                .operation(operation)
                .prefix(prefix)
                .maxKeys(maxKeys)
                .build();
    }

    private S3ConnectorConfig mapConfig(ConnectorConfig config) {
        S3ConnectorConfig.S3ConnectorConfigBuilder builder = S3ConnectorConfig.builder();
        if (config.getEndpoint() != null) {
            builder.endpointOverride(config.getEndpoint().toString());
        }
        if (config.getTimeout() != null) {
            builder.timeout(config.getTimeout());
        }
        if (config.getProperties() != null) {
            Map<String, Object> props = config.getProperties();
            if (props.containsKey("bucket")) builder.bucket(props.get("bucket").toString());
            if (props.containsKey("region")) builder.region(props.get("region").toString());
            if (props.containsKey("pathPrefix")) builder.pathPrefix(props.get("pathPrefix").toString());
            if (props.containsKey("forcePathStyle")) builder.forcePathStyle(Boolean.parseBoolean(props.get("forcePathStyle").toString()));
            if (props.containsKey("accessKeyId")) builder.accessKeyId(props.get("accessKeyId").toString());
            if (props.containsKey("secretAccessKey")) builder.secretAccessKey(props.get("secretAccessKey").toString());
            if (props.containsKey("resilienceProfile")) builder.resilienceProfile(props.get("resilienceProfile").toString());
        }
        return builder.build();
    }
}
