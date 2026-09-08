# torana-connector-http — Generic HTTP Microservice Connector

## Responsibility

Implements a **general-purpose reactive HTTP reverse proxy** as a `BackendConnector`. This connector enables any enterprise HTTP/REST microservice — internal APIs, third-party services, SAP, Salesforce, ServiceNow, etc. — to be connected to the Torana pipeline without writing any Java code.

Configurable via YAML: base URL, auth injection (Vault-sourced Bearer token or Basic auth), TLS (including mTLS with Vault-issued client cert), path rewrite, header forwarding, and timeout.

## Key Classes

### `proxy/`

| Class | Description |
|-------|-------------|
| `HttpBackendConnector` | Implements `BackendConnector`. `type() = "http"`. Builds a `WebClient` request from `AgentContext` and the `ConnectorConfig`. Returns `Flux<AgentResponse.Chunk>` (streaming-passthrough or buffered). |
| `HttpConnectorWebClientFactory` | Creates and caches one `WebClient` per named connector. Configures: base URL, connection pool, timeout, TLS context (from `DynamicSslContextFactory` when mTLS configured). |
| `RequestForwardingStrategy` | Determines what to forward from the inbound request to the upstream: method, path (with rewrite), headers (allow-list), body. |
| `ResponseMappingStrategy` | Maps upstream HTTP response to `Flux<AgentResponse.Chunk>`: status code, response headers, body streaming. |

### `auth/`

| Class | Description |
|-------|-------------|
| `HttpConnectorAuthInjector` | Injects authentication into the outgoing HTTP request based on config: `Bearer` token from Vault, `Basic` credentials from Vault, `X-Api-Key` from Vault, or no auth. |
| `VaultTokenRefresher` | Refreshes the Vault-sourced Bearer token before it expires. Caches the token per connector instance with TTL = credential lease duration minus buffer. |

### `transform/`

| Class | Description |
|-------|-------------|
| `HttpRequestTransformer` | Applies: path rewrite rules, header injection/removal, body transformation (optional). |
| `HttpResponseTransformer` | Normalizes upstream response: adds Torana trace headers, applies content-type based streaming detection. |

## YAML Configuration

```yaml
torana:
  connectors:
    hr-api:
      type: http
      base-url: https://hr.internal.corp.com
      timeout: 5s
      resilience-profile: default
      auth:
        type: vault-bearer           # vault-bearer | vault-basic | api-key | none
        vault-ref: secret/data/torana/hr-api-token
        vault-field: token
      tls:
        enabled: true
        verify-hostname: true
        # mTLS: use Vault PKI for client cert
        client-cert-from-vault: pki/issue/torana-client
      headers:
        forward:
          - X-Tenant-Id
          - X-Trace-Id
        inject:
          X-Gateway: torana
        block:
          - Authorization   # don't forward user's JWT to backend
      path-rewrite:
        strip-prefix: /api/v1/hr

    salesforce-api:
      type: http
      base-url: https://myorg.salesforce.com
      timeout: 10s
      auth:
        type: vault-bearer
        vault-ref: secret/data/salesforce/oauth-token
```

## Development Phases

### Phase 2A — Core HTTP Connector
- [ ] Implement `HttpBackendConnector` with WebClient-based forwarding
- [ ] Implement `HttpConnectorWebClientFactory` (one WebClient per connector)
- [ ] Implement `RequestForwardingStrategy` (method, path, headers, body)
- [ ] Implement `ResponseMappingStrategy` (status, headers, streaming body)
- [ ] Integration test (WireMock): HTTP connector → WireMock backend → response

### Phase 2B — Auth Injection
- [ ] Implement `HttpConnectorAuthInjector` (Bearer token from Vault)
- [ ] Implement `VaultTokenRefresher`
- [ ] Test: Vault token injected into Authorization header

### Phase 3 — mTLS
- [ ] Wire `DynamicSslContextFactory` from `torana-security-vault` into `HttpConnectorWebClientFactory`
- [ ] Test: mTLS client cert presented to WireMock TLS server

## Package Layout

```
com.phaselume.torana.connector.http
├── HttpBackendConnector.java
├── proxy/
│   ├── HttpConnectorWebClientFactory.java
│   ├── RequestForwardingStrategy.java
│   └── ResponseMappingStrategy.java
├── auth/
│   ├── HttpConnectorAuthInjector.java
│   └── VaultTokenRefresher.java
└── transform/
    ├── HttpRequestTransformer.java
    └── HttpResponseTransformer.java
```
