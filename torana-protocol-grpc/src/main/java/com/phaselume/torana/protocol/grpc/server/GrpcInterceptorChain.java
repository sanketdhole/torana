package com.phaselume.torana.protocol.grpc.server;

import com.phaselume.torana.protocol.grpc.codec.GrpcMetadataExtractor;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * gRPC ServerInterceptor for intercepting metadata, trace context, tenant ID, and auth tokens.
 */
public class GrpcInterceptorChain implements ServerInterceptor {

    public static final Context.Key<Metadata> METADATA_CONTEXT_KEY = Context.key("torana.grpc.metadata");
    public static final Context.Key<String> TENANT_ID_CONTEXT_KEY = Context.key("torana.grpc.tenant_id");
    public static final Context.Key<String> TRACE_PARENT_CONTEXT_KEY = Context.key("torana.grpc.trace_parent");

    private final GrpcMetadataExtractor metadataExtractor;

    public GrpcInterceptorChain() {
        this(new GrpcMetadataExtractor());
    }

    public GrpcInterceptorChain(GrpcMetadataExtractor metadataExtractor) {
        this.metadataExtractor = metadataExtractor != null ? metadataExtractor : new GrpcMetadataExtractor();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        Context context = Context.current().withValue(METADATA_CONTEXT_KEY, headers);

        String tenantId = headers.get(GrpcMetadataExtractor.TENANT_ID_KEY);
        if (tenantId != null) {
            context = context.withValue(TENANT_ID_CONTEXT_KEY, tenantId);
        }

        String traceparent = headers.get(GrpcMetadataExtractor.TRACEPARENT_KEY);
        if (traceparent != null) {
            context = context.withValue(TRACE_PARENT_CONTEXT_KEY, traceparent);
        }

        return Contexts.interceptCall(context, call, headers, next);
    }

    public GrpcMetadataExtractor getMetadataExtractor() {
        return metadataExtractor;
    }
}
