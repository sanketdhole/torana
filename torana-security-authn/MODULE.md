# torana-security-authn — Authentication Providers

## Responsibility

Implements all **authentication (AuthN)** mechanisms. This module's job is to extract credentials from an incoming request and validate them, producing a `ToranaAuthentication` object that flows through the rest of the security chain.

Authentication in Torana is a **composable chain** of `AuthenticationProvider` implementations. The chain is assembled at startup based on `torana.security.authn.providers[]` YAML list — no code changes are needed to add, remove, or reorder providers.

## Provider Chain Modes

| Mode | Behavior |
|------|----------|
| `first-match` | Returns the result of the first provider that succeeds. Remaining providers are skipped. |
| `all-required` | All declared providers must succeed (step-up auth). Useful for MFA scenarios. |
| `any-of` | At least one provider must succeed. |

## Providers

### JWT/OIDC Provider (`jwt/`)

Multi-issuer JWT Resource Server.

| Class | Description |
|-------|-------------|
| `JwtOidcAuthenticationProvider` | Implements `AuthenticationProvider`. Extracts `Authorization: Bearer` header. Validates JWT signature against JWKS. Supports multiple issuers (each with its own JWKS URI). |
| `MultiIssuerJwtDecoder` | Wraps Spring's `ReactiveJwtDecoder`. Determines which issuer's JWKS to use by reading the `iss` claim. JWKS responses cached in Redis. |
| `JwtClaimsExtractor` | Extracts scopes (from `scp`, `scope`, or `roles` claim — configurable), tenant ID (from configurable claim name), and principal name. |
| `JwksRedisCache` | Caches JWKS JSON in Redis with configurable TTL. Prevents JWKS fetch on every request. Falls back to live fetch on cache miss or signature validation failure. |

```yaml
torana:
  security:
    authn:
      providers:
        - type: jwt-oidc
          issuers:
            - uri: https://keycloak.corp.com/realms/internal
              audience: torana-gateway
              jwks-cache-ttl: 300s
              scope-claim: scp
              tenant-claim: tenant_id
            - uri: https://login.microsoftonline.com/{tenant}/v2.0
              audience: api://torana
              scope-claim: roles
```

### API Key Provider (`apikey/`)

Validates opaque API keys stored in Redis.

| Class | Description |
|-------|-------------|
| `ApiKeyAuthenticationProvider` | Implements `AuthenticationProvider`. Extracts key from configurable header (default: `X-Api-Key`). Looks up key hash in Redis. Returns `ToranaAuthentication` with scopes from Redis metadata. |
| `ApiKeyValidator` | Computes SHA-256 of the raw key, looks up in Redis hash `torana:apikey:{hash}`. Redis value is JSON: `{name, scopes[], tenantId, expiresAt, active}`. |
| `ApiKeyRotationSupport` | Supports key rotation: two keys can be valid simultaneously during rotation (old key + new key). Provides `void invalidate(String keyHash)` for admin use. |

```yaml
torana:
  security:
    authn:
      providers:
        - type: api-key
          header: X-Api-Key
          redis-prefix: torana:apikey:
          allow-query-param: false    # never allow ?apiKey= for security
```

### mTLS Provider (`mtls/`)

Validates client certificates and extracts principal from certificate DN.

| Class | Description |
|-------|-------------|
| `MtlsAuthenticationProvider` | Implements `AuthenticationProvider`. Extracts client certificate from the TLS session (`SslInfo` from the exchange). Validates cert against trusted CA. Extracts principal from `CN` or `SAN`. |
| `ClientCertificateValidator` | Verifies: cert is signed by trusted CA, not expired, not in CRL (Certificate Revocation List), `extendedKeyUsage` includes `clientAuth`. |
| `CertificateDnExtractor` | Extracts principal name from certificate DN: configurable field (CN, OU, emailAddress). Maps DN fields to scopes via configurable rules. |
| `CrlChecker` | Downloads and caches CRL (Certificate Revocation List) from distribution points in the cert. Redis-cached with TTL = CRL `nextUpdate` time. |

```yaml
torana:
  security:
    authn:
      providers:
        - type: mtls
          trust-store: classpath:enterprise-ca.p12
          trust-store-password: ${MTLS_TRUSTSTORE_PASSWORD}
          principal-field: CN
          crl-check-enabled: true
          crl-cache-ttl: 3600s
```

### SAML2 Provider (`saml/`) — Phase 3

Validates SAML2 assertions from enterprise identity providers (e.g., ADFS, Okta).

| Class | Description |
|-------|-------------|
| `SamlAuthenticationProvider` | Implements `AuthenticationProvider`. Validates SAML assertion signature, audience, and NotOnOrAfter. |
| `SamlAssertionParser` | Parses SAML XML assertion, extracts attributes mapped to scopes. |

```yaml
torana:
  security:
    authn:
      providers:
        - type: saml2
          idp-metadata-uri: https://adfs.corp.com/FederationMetadata/2007-06/FederationMetadata.xml
          sp-entity-id: urn:torana:gateway
          attribute-mapping:
            groups: torana-scopes
```

### Auth Chain (`chain/`)

| Class | Description |
|-------|-------------|
| `AuthenticationProviderChain` | Ordered composite of all active `AuthenticationProvider` instances. Evaluates based on `mode` (first-match / all-required / any-of). Returns `Mono<ToranaAuthentication>` or `Mono.error(AuthenticationException)`. |
| `AnonymousAuthenticationProvider` | Returns an anonymous `ToranaAuthentication` for routes with `auth.allow-anonymous: true`. Always last in the chain. |

### Security Config (`config/`)

| Class | Description |
|-------|-------------|
| `ToranaSecurityWebFilterChain` | Builds the `SecurityWebFilterChain`. Registers the `AuthenticationWebFilter` with the `AuthenticationProviderChain`. Sets up `ServerHttpSecurity` with stateless session management. |
| `AuthenticationWebFilter` | Spring Security `WebFilter` that invokes `AuthenticationProviderChain` for every request. Populates `SecurityContext` with the resulting `ToranaAuthentication`. |

## Development Phases

### Phase 1A — JWT/OIDC (implement first)
- [ ] Implement `JwtOidcAuthenticationProvider` (single issuer first)
- [ ] Implement `JwksRedisCache`
- [ ] Implement `JwtClaimsExtractor`
- [ ] Implement `AuthenticationProviderChain` (first-match only)
- [ ] Implement `ToranaSecurityWebFilterChain`
- [ ] Test: valid JWT → `ToranaAuthentication`; invalid JWT → 401; expired JWT → 401

### Phase 1B — API Key
- [ ] Implement `ApiKeyAuthenticationProvider`
- [ ] Implement `ApiKeyValidator` with Redis lookup
- [ ] Implement `ApiKeyRotationSupport`
- [ ] Test: valid API key → auth; unknown key → 401; expired key → 401

### Phase 1C — Multi-issuer JWT
- [ ] Implement `MultiIssuerJwtDecoder`
- [ ] Test: two issuers, each with their own JWKS — both validate correctly

### Phase 2 — mTLS
- [ ] Implement `MtlsAuthenticationProvider`
- [ ] Implement `ClientCertificateValidator`
- [ ] Implement `CertificateDnExtractor`
- [ ] Implement `CrlChecker` with Redis CRL cache
- [ ] Test: valid client cert → auth; revoked cert → 401; expired cert → 401

### Phase 3 — SAML2
- [ ] Implement `SamlAuthenticationProvider`
- [ ] Implement `SamlAssertionParser`
- [ ] Add `all-required` and `any-of` chain modes to `AuthenticationProviderChain`

## Package Layout

```
com.phaselume.torana.security.authn
├── jwt/
│   ├── JwtOidcAuthenticationProvider.java
│   ├── MultiIssuerJwtDecoder.java
│   ├── JwtClaimsExtractor.java
│   └── JwksRedisCache.java
├── apikey/
│   ├── ApiKeyAuthenticationProvider.java
│   ├── ApiKeyValidator.java
│   └── ApiKeyRotationSupport.java
├── mtls/
│   ├── MtlsAuthenticationProvider.java
│   ├── ClientCertificateValidator.java
│   ├── CertificateDnExtractor.java
│   └── CrlChecker.java
├── saml/
│   ├── SamlAuthenticationProvider.java      (Phase 3)
│   └── SamlAssertionParser.java             (Phase 3)
├── chain/
│   ├── AuthenticationProviderChain.java
│   └── AnonymousAuthenticationProvider.java
└── config/
    ├── ToranaSecurityWebFilterChain.java
    └── AuthenticationWebFilter.java
```
