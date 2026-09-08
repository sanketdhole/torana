# torana-connector-jdbc — Reactive SQL Connector (R2DBC)

## Responsibility

Implements a `BackendConnector` for **SQL databases** using **R2DBC** (Reactive Relational Database Connectivity) — the non-blocking, reactive API for relational databases. This connector enables AI agents to safely query enterprise SQL databases through the Torana pipeline with:

- **Zero-trust query execution**: declared allow-list of permitted SQL operations (SELECT only by default)
- **Parameterized queries**: all agent-provided values are bound as parameters — never string-concatenated
- **Vault-managed credentials**: database username/password fetched from Vault per-request
- **Result serialization**: SQL `ResultSet` rows serialized as JSON for pipeline consumption

## Supported Databases

| Database | R2DBC Driver |
|----------|-------------|
| PostgreSQL | `org.postgresql:r2dbc-postgresql` |
| MySQL / MariaDB | `dev.miku:r2dbc-mysql` |
| MS SQL Server | `io.r2dbc:r2dbc-mssql` |
| Oracle | `com.oracle.database.r2dbc:oracle-r2dbc` |
| H2 (testing) | `io.r2dbc:r2dbc-h2` |

## Key Classes

### `r2dbc/`

| Class | Description |
|-------|-------------|
| `R2dbcBackendConnector` | Implements `BackendConnector`. `type() = "r2dbc"`. Fetches credentials from Vault (`CredentialBroker`), builds a connection from `R2dbcConnectionFactory`, executes the query, serializes results as JSON `AgentResponse.Chunk`s. |
| `R2dbcConnectionFactory` | Per-connector reactive `ConnectionFactory` backed by `ConnectionPool`. Configured with Vault-provided credentials (refreshed when lease expires). |
| `R2dbcConnectionPool` | `r2dbc-pool` backed pool with configurable `maxSize`, `acquireTimeout`, `validationQuery`. |

### `query/`

| Class | Description |
|-------|-------------|
| `SqlQueryExtractor` | Extracts the SQL query and bind parameters from `AgentContext`. The query is specified in the step definition or in the request body (structured format). |
| `SqlOperationGuard` | Enforces the `allowed-operations` allow-list. Parses the SQL statement (using JSqlParser or simple prefix check) and rejects any statement type not in the allow-list. Throws `ConnectorException` for disallowed operations. Default allow-list: `[SELECT]`. |
| `ResultSetSerializer` | Converts `io.r2dbc.spi.Result` rows → `Flux<AgentResponse.Chunk>` where each chunk is a JSON object `{"columns": [...], "rows": [...]}`. Handles `NULL` values, type mapping (UUID, JSONB, etc.). |

### `security/`

| Class | Description |
|-------|-------------|
| `VaultDatabaseCredentialProvider` | Calls `CredentialBroker.broker(ctx, "postgres-prod")` → gets username + password from Vault with short TTL. Creates a fresh `ConnectionConfiguration` per credential fetch. |
| `SchemaAllowList` | Enforces which tables / schemas the connector is permitted to access. Blocks queries referencing tables outside the allow-list. |

## YAML Configuration

```yaml
torana:
  connectors:
    postgres-prod:
      type: r2dbc
      driver: postgresql
      host: db.internal.corp.com
      port: 5432
      database: enterprisedb
      credentials-from-vault: true
      vault-ref: database/creds/torana-readonly
      pool:
        max-size: 20
        min-idle: 2
        max-idle-time: 300s
        acquire-timeout: 2s
        validation-query: SELECT 1
      security:
        allowed-operations: [SELECT]     # no INSERT/UPDATE/DELETE/DDL
        allowed-schemas: [public, reporting]
        allowed-tables: [products, orders, customers]
      timeout: 10s
      resilience-profile: default

    analytics-mysql:
      type: r2dbc
      driver: mysql
      host: analytics-db.internal.corp.com
      port: 3306
      database: analytics
      username: ${MYSQL_USER}
      password: ${MYSQL_PASSWORD}
      security:
        allowed-operations: [SELECT]
```

## Query Format (in step params)

```yaml
pipelines:
  product-lookup-pipeline:
    steps:
      - type: request-transform
        expression: "{ productId: input.body.productId }"
      - type: tool-call         # dispatches to JDBC connector via tool registry
        tool: get-product
        # Tool is mapped to connector:
        # GET product WHERE id = :productId  (parameterized)
```

## Development Phases

### Phase 2A — Core JDBC Connector
- [ ] Implement `R2dbcConnectionFactory` + `R2dbcConnectionPool`
- [ ] Implement `SqlOperationGuard` (simple prefix-based check)
- [ ] Implement `ResultSetSerializer`
- [ ] Implement `R2dbcBackendConnector`
- [ ] Integration test (Testcontainers PostgreSQL): SELECT query → JSON rows

### Phase 3A — Vault Credentials
- [ ] Implement `VaultDatabaseCredentialProvider`
- [ ] Dynamic connection per Vault credential lease
- [ ] Test: Vault-issued creds → DB connection → query executes → creds expire

### Phase 3B — Security Hardening
- [ ] Implement `SchemaAllowList` (table-level filtering)
- [ ] Add JSqlParser for full SQL AST-based operation guard
- [ ] Test: INSERT rejected; query to disallowed table rejected

## Package Layout

```
com.phaselume.torana.connector.jdbc
├── R2dbcBackendConnector.java
├── r2dbc/
│   ├── R2dbcConnectionFactory.java
│   └── R2dbcConnectionPool.java
├── query/
│   ├── SqlQueryExtractor.java
│   ├── SqlOperationGuard.java
│   └── ResultSetSerializer.java
└── security/
    ├── VaultDatabaseCredentialProvider.java
    └── SchemaAllowList.java
```
