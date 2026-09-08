# torana-security-vault — HashiCorp Vault Credential Broker

## Responsibility

Implements the `CredentialBroker` SPI using **HashiCorp Vault** as the secrets engine. This module ensures that **no static credentials exist** anywhere in the gateway — all backend credentials (database passwords, API tokens, AWS STS session credentials, TLS private keys) are fetched from Vault dynamically at request time with a short TTL.

This module embodies the **zero-trust, zero-static-credential** principle: even if an attacker compromises a running gateway node, they cannot extract long-lived credentials because none are stored in memory beyond the request scope.

## Core Concept: Dynamic Secrets

Vault's dynamic secrets engines generate unique, short-lived credentials for every caller:
- **Database engine** → unique Postgres/MySQL user + password, TTL 60s
- **AWS secrets engine** → STS AssumeRole session credentials, TTL 900s
- **PKI engine** → signed client TLS certificate, TTL 1h
- **KV v2 engine** → static secrets (fallback; less preferred)

## Key Classes

### `client/`

| Class | Description |
|-------|-------------|
| `VaultReactiveClient` | Thin reactive `WebClient` wrapper around Vault's HTTP API (`GET /v1/{secret_path}`). Handles Vault token renewal and 403/404 error mapping. |
| `VaultCredentialBroker` | Implements `CredentialBroker` SPI. Given a backend reference name, looks up the configured `vault-path` mapping and fetches credentials from Vault. Returns `Mono<BackendCredentials>`. |
| `VaultCredentialCache` | Per-request credential cache using a `Mono.cache()` operator. Credentials fetched once per request context are reused within the same request. NOT shared across requests. |
| `VaultTokenRenewer` | Background task (Reactor `Flux.interval`) that renews the gateway's own Vault token before it expires. Token stored in memory only. |

### `auth/`

Vault itself requires authentication. The gateway authenticates to Vault using one of:

| Class | Auth Method | When to Use |
|-------|------------|-------------|
| `KubernetesVaultAuthenticator` | Kubernetes Service Account JWT | Production (k8s) |
| `AppRoleVaultAuthenticator` | AppRole (role-id + secret-id) | Non-k8s production |
| `TokenVaultAuthenticator` | Static Vault token | Development only |
| `VaultAuthenticatorChain` | Tries authenticators in order until one succeeds | Multi-environment |

```yaml
torana:
  security:
    credential-broker:
      engine: vault
      vault:
        uri: https://vault.corp.com
        auth-method: kubernetes
        role: torana-gw
        kubernetes:
          service-account-token-path: /var/run/secrets/kubernetes.io/serviceaccount/token
          kubernetes-host: https://kubernetes.default.svc
```

### `pki/`

Vault PKI engine support: fetches short-lived TLS client certificates for mTLS backend connections.

| Class | Description |
|-------|-------------|
| `VaultPkiCertificateProvider` | Calls Vault PKI `POST /v1/{pki_mount}/issue/{role}`. Returns signed cert + private key. Caches in memory for the cert's validity period minus 10 minutes. |
| `DynamicSslContextFactory` | Builds a `SslContext` from Vault-issued cert/key pair. Used by `HttpBackendConnector` and `MtlsAuthenticationProvider`. |

### `model/`

| Class | Description |
|-------|-------------|
| `VaultSecretResponse` | Deserialized Vault API response: `data` map, `lease_id`, `lease_duration`, `renewable`. |
| `VaultCredentialMapping` | YAML-bound mapping: `backendRef` → `vaultPath`, `credentialType` (database/aws/pki/kv). |

## Credential Types Supported

| Type | Vault Engine | `BackendCredentials` output |
|------|-------------|---------------------------|
| `database` | Database Secrets | `type=BASIC`, username + password |
| `aws-sts` | AWS Secrets | `type=AWS_STS`, accessKey + secretKey + sessionToken |
| `pki-cert` | PKI | `type=TLS_CERT`, cert PEM + key PEM |
| `kv` | KV v2 | `type=BEARER` or `type=BASIC` depending on secret shape |

## YAML Configuration

```yaml
torana:
  security:
    credential-broker:
      engine: vault
      vault:
        uri: https://vault.corp.com
        auth-method: kubernetes
        role: torana-gw
        token-renew-threshold: 30s   # renew when this much TTL remains
      mappings:
        - backend: postgres-prod
          vault-path: database/creds/torana-readonly
          credential-type: database
        - backend: s3-docs
          vault-path: aws/creds/torana-s3-role
          credential-type: aws-sts
        - backend: hr-api
          vault-path: secret/data/torana/hr-api-token
          credential-type: kv
          kv-field: token
        - backend: internal-mtls
          vault-path: pki/issue/torana-client
          credential-type: pki-cert
          common-name: torana-gateway
```

## Development Phases

### Phase 3A — Core Vault Integration
- [ ] Implement `VaultReactiveClient` (HTTP API wrapper)
- [ ] Implement `KubernetesVaultAuthenticator`
- [ ] Implement `AppRoleVaultAuthenticator`
- [ ] Implement `VaultCredentialBroker` (KV + database credential types)
- [ ] Implement `VaultTokenRenewer`
- [ ] Integration test (Testcontainers Vault): fetch database creds → validate short TTL

### Phase 3B — AWS STS + PKI
- [ ] Implement AWS STS credential type
- [ ] Implement `VaultPkiCertificateProvider`
- [ ] Implement `DynamicSslContextFactory`
- [ ] Test: AWS STS creds injected into S3 connector

### Phase 3C — Hardening
- [ ] Add Vault unavailability fallback (circuit breaker via `torana-resilience`)
- [ ] Add `VaultHealthIndicator` to Spring Actuator
- [ ] Implement `TokenVaultAuthenticator` for local dev
- [ ] Implement `VaultAuthenticatorChain`

## Package Layout

```
com.phaselume.torana.security.vault
├── client/
│   ├── VaultReactiveClient.java
│   ├── VaultCredentialBroker.java
│   ├── VaultCredentialCache.java
│   └── VaultTokenRenewer.java
├── auth/
│   ├── KubernetesVaultAuthenticator.java
│   ├── AppRoleVaultAuthenticator.java
│   ├── TokenVaultAuthenticator.java
│   └── VaultAuthenticatorChain.java
├── pki/
│   ├── VaultPkiCertificateProvider.java
│   └── DynamicSslContextFactory.java
└── model/
    ├── VaultSecretResponse.java
    └── VaultCredentialMapping.java
```
