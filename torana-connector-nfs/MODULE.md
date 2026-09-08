# torana-connector-nfs — NFS/Block Storage Connector

## Responsibility

Implements a `BackendConnector` for **NFS (Network File System)**, **CIFS/SMB**, and **block storage** (mounted volumes). Enables AI agents to access files on enterprise shared drives, NAS devices, and SAN-mounted volumes.

Since Java NIO file I/O is inherently blocking, this connector uses **Java 21 Virtual Threads** (Project Loom) to wrap blocking file channel operations inside lightweight threads — ensuring they never block the Reactor event loop.

## Key Classes

### `handler/`

| Class | Description |
|-------|-------------|
| `NfsBackendConnector` | Implements `BackendConnector`. `type() = "nfs"`. Routes to `FileReadHandler` or `FileListHandler` based on operation. |
| `FileReadHandler` | Reads file content from the mount path. Opens `FileChannel` on a virtual thread, reads in configurable chunk sizes, returns `Flux<AgentResponse.Chunk>` via `Flux.create()` with backpressure. |
| `FileListHandler` | Lists directory contents (files matching a glob pattern). Returns JSON array of file metadata (name, size, lastModified, contentType). |
| `NfsPathValidator` | Validates the requested path: must be under configured `root-path`, no `..` traversal, no symlink escapes. Throws `ConnectorException` for path traversal attempts. |
| `NfsOperationGuard` | Enforces `allowed-operations` list: `READ_FILE`, `LIST_DIRECTORY`. Blocks `WRITE_FILE`, `DELETE_FILE` by default. |

### `buffer/`

| Class | Description |
|-------|-------------|
| `ChunkedFileReader` | Reads a `java.nio.file.Path` in configurable chunks (`chunk-size-bytes`, default 64KB) using a `FileChannel`. Emits each chunk as `AgentResponse.Chunk`. Runs on virtual thread executor. |
| `VirtualThreadExecutor` | `Executor` backed by `Executors.newVirtualThreadPerTaskExecutor()`. Used exclusively for blocking file I/O. |
| `ContentTypeDetector` | Detects MIME type from file extension or magic bytes (Apache Tika, optional). Used to set `Content-Type` in response. |

## Virtual Thread Integration Pattern

```java
// FileReadHandler.java (pseudocode — no implementation here)
return Flux.create(sink -> {
    virtualThreadExecutor.execute(() -> {
        try (FileChannel channel = FileChannel.open(path, READ)) {
            ByteBuffer buf = ByteBuffer.allocate(chunkSizeBytes);
            while (channel.read(buf) > 0) {
                buf.flip();
                sink.next(AgentResponse.Chunk.of(buf.array().clone()));
                buf.clear();
                if (sink.isCancelled()) break;
            }
            sink.complete();
        } catch (IOException e) {
            sink.error(new ConnectorException("File read failed", e));
        }
    });
});
```

## YAML Configuration

```yaml
torana:
  connectors:
    nfs-documents:
      type: nfs
      root-path: /mnt/enterprise-nas/documents
      security:
        allowed-operations: [READ_FILE, LIST_DIRECTORY]
        allowed-extensions: [.pdf, .docx, .txt, .md, .csv]
        max-file-size: 50MB
      chunk-size-bytes: 65536         # 64KB per chunk
      timeout: 60s
      resilience-profile: default

    smb-reports:
      type: nfs
      root-path: /mnt/smb/reports
      security:
        allowed-operations: [READ_FILE, LIST_DIRECTORY]
        max-file-size: 200MB
```

## Development Phases

### Phase 2B — Core NFS Connector
- [ ] Implement `VirtualThreadExecutor`
- [ ] Implement `NfsPathValidator` (path traversal prevention)
- [ ] Implement `ChunkedFileReader` (virtual thread + FileChannel)
- [ ] Implement `FileReadHandler`
- [ ] Implement `FileListHandler`
- [ ] Implement `NfsBackendConnector`
- [ ] Unit test: path traversal attempts blocked; large file streams correctly in chunks

### Phase 3 — Content Type + Security
- [ ] Implement `ContentTypeDetector`
- [ ] Implement `NfsOperationGuard` (extension allow-list)
- [ ] Add file size limit enforcement before read
- [ ] Add write support (`WRITE_FILE` if allowed)

## Package Layout

```
com.phaselume.torana.connector.nfs
├── NfsBackendConnector.java
├── handler/
│   ├── FileReadHandler.java
│   ├── FileListHandler.java
│   ├── NfsPathValidator.java
│   └── NfsOperationGuard.java
└── buffer/
    ├── ChunkedFileReader.java
    ├── VirtualThreadExecutor.java
    └── ContentTypeDetector.java
```
