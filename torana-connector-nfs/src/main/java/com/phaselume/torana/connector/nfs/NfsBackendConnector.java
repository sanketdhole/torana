package com.phaselume.torana.connector.nfs;

import com.phaselume.torana.connector.nfs.handler.FileListHandler;
import com.phaselume.torana.connector.nfs.handler.FileReadHandler;
import com.phaselume.torana.connector.nfs.handler.NfsOperationGuard;
import com.phaselume.torana.connector.nfs.handler.NfsPathValidator;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Reactive backend connector for NFS, CIFS/SMB, and mounted block storage filesystems.
 */
public class NfsBackendConnector implements BackendConnector {

    private static final Logger log = LoggerFactory.getLogger(NfsBackendConnector.class);

    private final NfsPathValidator pathValidator;
    private final NfsOperationGuard operationGuard;
    private final FileReadHandler fileReadHandler;
    private final FileListHandler fileListHandler;
    private final ResilienceDecorator resilienceDecorator;

    public NfsBackendConnector(NfsPathValidator pathValidator,
                               NfsOperationGuard operationGuard,
                               FileReadHandler fileReadHandler,
                               FileListHandler fileListHandler,
                               ResilienceDecorator resilienceDecorator) {
        this.pathValidator = pathValidator != null ? pathValidator : new NfsPathValidator();
        this.operationGuard = operationGuard != null ? operationGuard : new NfsOperationGuard();
        this.fileReadHandler = fileReadHandler != null ? fileReadHandler : new FileReadHandler();
        this.fileListHandler = fileListHandler != null ? fileListHandler : new FileListHandler();
        this.resilienceDecorator = resilienceDecorator;
    }

    public NfsBackendConnector() {
        this(new NfsPathValidator(), new NfsOperationGuard(), new FileReadHandler(), new FileListHandler(), null);
    }

    @Override
    public String type() {
        return "nfs";
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        if (config == null || config.getType() == null) return false;
        String t = config.getType().toLowerCase();
        return "nfs".equals(t) || "file".equals(t) || "nas".equals(t) || "smb".equals(t);
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        if (config == null) {
            return Flux.error(new IllegalArgumentException("ConnectorConfig must not be null"));
        }

        String rootPath = extractProperty(config.getProperties(), "rootPath", "root-path", null);
        if (rootPath == null || rootPath.isBlank()) {
            return Flux.error(new ConnectorException("nfs", "Missing required 'rootPath' property in connector configuration"));
        }

        String relativePath = extractRelativePath(context);
        String operation = extractOperation(context);

        Path resolvedPath = pathValidator.validateAndResolve(rootPath, relativePath);

        // Auto-detect directory listing if target is a directory and operation not explicitly READ_FILE
        if (Files.isDirectory(resolvedPath) && !"READ_FILE".equalsIgnoreCase(operation)) {
            operation = "LIST_DIRECTORY";
        }

        List<String> allowedOps = extractList(config.getProperties(), "allowedOperations", List.of("READ_FILE", "LIST_DIRECTORY"));
        List<String> allowedExts = extractList(config.getProperties(), "allowedExtensions", List.of());

        operationGuard.validate(operation, resolvedPath, allowedOps, allowedExts);

        int chunkSize = parseInt(extractProperty(config.getProperties(), "chunkSizeBytes", "chunk-size-bytes", "65536"), 64 * 1024);
        long maxFileSize = parseLong(extractProperty(config.getProperties(), "maxFileSize", "max-file-size", "52428800"), 50 * 1024 * 1024L);

        String profileName = extractProperty(config.getProperties(), "resilienceProfile", "resilience-profile", "default");
        String finalOp = operation;

        if (resilienceDecorator != null) {
            return resilienceDecorator.decorate(profileName, null, () -> dispatch(finalOp, resolvedPath, chunkSize, maxFileSize));
        }

        return dispatch(finalOp, resolvedPath, chunkSize, maxFileSize);
    }

    private Flux<AgentResponse.Chunk> dispatch(String operation, Path path, int chunkSize, long maxFileSize) {
        if ("LIST_DIRECTORY".equalsIgnoreCase(operation)) {
            return fileListHandler.listDirectory(path);
        } else {
            return fileReadHandler.readFile(path, chunkSize, maxFileSize);
        }
    }

    private String extractRelativePath(AgentContext context) {
        if (context == null) return "";
        Object pathAttr = context.getAttribute("path");
        if (pathAttr != null) return pathAttr.toString();

        AgentRequest req = context.getRequest();
        if (req != null) {
            if (req.getQueryParams() != null && req.getQueryParams().containsKey("path")) {
                return req.getQueryParams().getFirst("path");
            }
            if (req.getPath() != null) {
                return req.getPath();
            }
        }
        return "";
    }

    private String extractOperation(AgentContext context) {
        if (context == null) return "READ_FILE";
        Object opAttr = context.getAttribute("operation");
        if (opAttr != null) return opAttr.toString();

        AgentRequest req = context.getRequest();
        if (req != null && req.getQueryParams() != null && req.getQueryParams().containsKey("operation")) {
            return req.getQueryParams().getFirst("operation");
        }
        return "READ_FILE";
    }

    private String extractProperty(Map<String, Object> props, String key1, String key2, String fallback) {
        if (props == null) return fallback;
        if (props.containsKey(key1)) return props.get(key1).toString();
        if (props.containsKey(key2)) return props.get(key2).toString();
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> props, String key, List<String> fallback) {
        if (props == null || !props.containsKey(key)) return fallback;
        Object val = props.get(key);
        if (val instanceof List) {
            return (List<String>) val;
        }
        return fallback;
    }

    private int parseInt(String val, int fallback) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLong(String val, long fallback) {
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return fallback;
        }
    }
}
