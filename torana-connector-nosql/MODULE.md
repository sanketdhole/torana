# torana-connector-nosql — NoSQL Connector (MongoDB + Cassandra)

## Responsibility

Implements `BackendConnector` for **NoSQL databases** using reactive Spring Data drivers. Initially focused on **MongoDB** (most common for AI/document workloads); Cassandra support added in Phase 3.

Enables AI agents to query document stores, vector databases (MongoDB Atlas Vector Search), and wide-column stores through the Torana pipeline with the same zero-trust, parameterized query guarantees as the SQL connector.

## Key Classes

### `mongodb/`

| Class | Description |
|-------|-------------|
| `MongoBackendConnector` | Implements `BackendConnector`. `type() = "mongodb"`. Executes MongoDB queries via `ReactiveMongoTemplate`. Returns results as `Flux<AgentResponse.Chunk>` (one chunk per document, or batched). |
| `MongoQueryBuilder` | Builds `Query` + `Criteria` from structured `AgentContext` query parameters. Prevents NoSQL injection by using typed field binding, never string concatenation. |
| `MongoOperationGuard` | Enforces allowed operations: `find`, `aggregate`, `count`. Blocks `insert`, `update`, `delete`, `drop` unless explicitly allowed. |
| `MongoResultSerializer` | Serializes `Document` BSON → JSON `AgentResponse.Chunk`. Handles `ObjectId`, `Date`, `Decimal128`, nested docs. |
| `MongoVaultCredentialProvider` | Fetches MongoDB credentials from Vault (`database/creds/mongo-role`). |

### `cassandra/`

| Class | Description |
|-------|-------------|
| `CassandraBackendConnector` | Implements `BackendConnector`. `type() = "cassandra"`. Executes CQL via `ReactiveCassandraTemplate`. |
| `CqlOperationGuard` | Enforces allowed CQL operations: SELECT only by default. |
| `CassandraResultSerializer` | Serializes Cassandra `Row` → JSON chunk. |

## YAML Configuration

```yaml
torana:
  connectors:
    mongodb-docs:
      type: mongodb
      uri: mongodb://mongodb.internal.corp.com:27017/enterprisedocs
      credentials-from-vault: true
      vault-ref: database/creds/mongo-readonly
      security:
        allowed-operations: [find, aggregate, count]
        allowed-collections: [documents, metadata, embeddings]
      timeout: 5s
      resilience-profile: default

    cassandra-events:
      type: cassandra
      contact-points: cassandra.internal.corp.com
      port: 9042
      keyspace: events
      credentials-from-vault: false
      username: ${CASSANDRA_USER}
      password: ${CASSANDRA_PASSWORD}
      security:
        allowed-operations: [SELECT]
        allowed-tables: [agent_events, audit_log]
```

## Development Phases

### Phase 2A — MongoDB
- [ ] Implement `MongoBackendConnector`
- [ ] Implement `MongoQueryBuilder` (structured query params → `Criteria`)
- [ ] Implement `MongoOperationGuard`
- [ ] Implement `MongoResultSerializer`
- [ ] Integration test (Testcontainers MongoDB)

### Phase 3A — Vault + Vector Search
- [ ] Implement `MongoVaultCredentialProvider`
- [ ] Add MongoDB Atlas Vector Search support (KNN query via `$vectorSearch` aggregation)

### Phase 3B — Cassandra
- [ ] Implement `CassandraBackendConnector`
- [ ] Implement `CqlOperationGuard` + `CassandraResultSerializer`
- [ ] Integration test (Testcontainers Cassandra)

## Package Layout

```
com.phaselume.torana.connector.nosql
├── mongodb/
│   ├── MongoBackendConnector.java
│   ├── MongoQueryBuilder.java
│   ├── MongoOperationGuard.java
│   ├── MongoResultSerializer.java
│   └── MongoVaultCredentialProvider.java
└── cassandra/
    ├── CassandraBackendConnector.java
    ├── CqlOperationGuard.java
    └── CassandraResultSerializer.java
```
