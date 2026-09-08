# torana-connector-s3 — AWS S3 Connector

## Responsibility

Implements a `BackendConnector` for **AWS S3** using the non-blocking `S3AsyncClient` from AWS SDK v2 with Netty async HTTP client. Enables AI agents to read, list, and stream enterprise documents, model artifacts, and data files from S3 buckets through the Torana pipeline.

Credentials are sourced from **Vault's AWS Secrets Engine** (STS AssumeRole) — no static AWS access keys anywhere.

## Key Classes

### `client/`

| Class | Description |
|-------|-------------|
| `S3BackendConnector` | Implements `BackendConnector`. `type() = "s3"`. Routes to the correct operation handler based on `AgentContext.operation`: `GET_OBJECT`, `LIST_OBJECTS`, `PUT_OBJECT`, `GENERATE_PRESIGNED_URL`. Returns `Flux<AgentResponse.Chunk>`. |
| `S3AsyncClientFactory` | Creates `S3AsyncClient` instances with Vault STS credentials. Client is short-lived: created per-request when Vault STS TTL is short, or cached per credential lease. |
| `S3OperationGuard` | Enforces `allowed-operations` allow-list. Default: `[GET_OBJECT, LIST_OBJECTS]`. Blocks `DELETE_OBJECT`, `PUT_OBJECT` unless explicitly allowed. |

### `auth/`

| Class | Description |
|-------|-------------|
| `VaultAwsStsCredentialProvider` | Fetches AWS STS session credentials from Vault (`aws/creds/{role}` endpoint). Returns `AwsSessionCredentials` for the `S3AsyncClient`. |
| `AwsCredentialCache` | Caches `AwsSessionCredentials` per connector + Vault lease. Refreshes 60 seconds before expiry. |

### `model/`

| Class | Description |
|-------|-------------|
| `S3ConnectorConfig` | YAML-bound: `region`, `bucket`, `path-prefix`, `vault-ref`, `allowed-operations`, `max-object-size`, `presigned-url-ttl`. |
| `S3ObjectReference` | Parsed object reference from `AgentContext`: bucket (defaults to configured), key, version ID. |
| `S3ListResult` | Serialized LIST result: `objects[]` with key, size, lastModified, etag. |

## Operations

| Operation | Description | Returns |
|-----------|-------------|---------|
| `GET_OBJECT` | Stream object content | `Flux<AgentResponse.Chunk>` (binary or text chunks) |
| `LIST_OBJECTS` | List objects with prefix | Single chunk with JSON array |
| `PUT_OBJECT` | Write object (if allowed) | Single chunk with confirmation |
| `GENERATE_PRESIGNED_URL` | Generate time-limited URL | Single chunk with URL string |
| `HEAD_OBJECT` | Get object metadata | Single chunk with metadata JSON |

## YAML Configuration

```yaml
torana:
  connectors:
    s3-docs:
      type: s3
      region: us-east-1
      bucket: enterprise-ai-docs
      path-prefix: /documents/      # restrict connector to this prefix
      credentials-from-vault: true
      vault-ref: aws/creds/torana-s3-readonly
      security:
        allowed-operations: [GET_OBJECT, LIST_OBJECTS, HEAD_OBJECT]
        max-object-size: 100MB
      presigned-url-ttl: 3600s
      timeout: 30s
      resilience-profile: default

    s3-models:
      type: s3
      region: us-west-2
      bucket: ml-model-artifacts
      credentials-from-vault: true
      vault-ref: aws/creds/torana-s3-models
      security:
        allowed-operations: [GET_OBJECT, LIST_OBJECTS]
```

## Development Phases

### Phase 2B — Core S3 Connector
- [ ] Implement `S3AsyncClientFactory` (static credentials first for dev)
- [ ] Implement `S3OperationGuard`
- [ ] Implement `S3BackendConnector` with `GET_OBJECT` and `LIST_OBJECTS`
- [ ] Implement `S3ObjectReference` parser
- [ ] Integration test (Testcontainers LocalStack S3)

### Phase 3A — Vault STS
- [ ] Implement `VaultAwsStsCredentialProvider`
- [ ] Implement `AwsCredentialCache`
- [ ] Wire into `S3AsyncClientFactory`
- [ ] Test: Vault STS creds → S3 GET → stream response

### Phase 3B — Additional Operations
- [ ] Add `PUT_OBJECT` support (if allowed)
- [ ] Add `GENERATE_PRESIGNED_URL`
- [ ] Add `HEAD_OBJECT`
- [ ] Add path prefix enforcement

## Package Layout

```
com.phaselume.torana.connector.s3
├── S3BackendConnector.java
├── client/
│   ├── S3AsyncClientFactory.java
│   └── S3OperationGuard.java
├── auth/
│   ├── VaultAwsStsCredentialProvider.java
│   └── AwsCredentialCache.java
└── model/
    ├── S3ConnectorConfig.java
    ├── S3ObjectReference.java
    └── S3ListResult.java
```
