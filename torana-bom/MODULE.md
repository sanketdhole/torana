# torana-bom — Bill of Materials

## Responsibility

Centralizes all dependency version management for every Torana module and for enterprise adopters. Publishes a Maven BOM (Bill of Materials) POM so consumers can import `com.phaselume.torana:torana-bom` once and get consistent, curated versions of every Torana and third-party dependency — with no need to specify versions individually.

This module contains **no Java source code** — only the Gradle `java-platform` configuration.

## What It Manages

| Category | Key Libraries |
|----------|--------------|
| Spring Platform | `spring-boot-dependencies`, `spring-cloud-dependencies` |
| Reactive | `reactor-core`, `reactor-netty` |
| Resilience | `resilience4j-*` (all modules) |
| Security | `nimbus-jose-jwt`, `spring-security-saml2-service-provider` |
| Secrets | `vault-java-driver`, `spring-vault-core` |
| Data | `r2dbc-spi`, `r2dbc-postgresql`, `r2dbc-pool`, `spring-data-mongodb-reactive` |
| Cloud Storage | `aws-sdk-s3`, `aws-sdk-sts`, `aws-sdk-netty-nio-client` |
| gRPC | `grpc-netty-shaded`, `grpc-stub`, `grpc-protobuf`, `protobuf-java-util` |
| Observability | `micrometer-core`, `opentelemetry-api/sdk/exporter-otlp` |
| Messaging | `reactor-kafka` |
| Testing | `testcontainers-bom`, `wiremock-standalone` |

## Dependency Rule

> All other Torana modules import this BOM in their `dependencyManagement` block. Enterprise adopters who use `torana-starter` get this transitively. Direct adopters of individual modules can import the BOM separately.

## Development Phases

### Phase 0 — Foundation (current)
- [ ] Define initial version catalog in `gradle/libs.versions.toml`
- [ ] Create Gradle `java-platform` BOM module
- [ ] Align all Spring Boot 4 / Spring Cloud 2025 versions
- [ ] Publish to local Maven repository for cross-module resolution

### Phase 1 — Stabilize
- [ ] Lock down Resilience4j, Nimbus, Redis client versions
- [ ] Validate all versions against Spring Boot 4 BOM conflicts
- [ ] Add version constraints for gRPC + protobuf

### Phase 2 — Release
- [ ] Set up Maven Central publishing (`maven-publish` plugin + signing)
- [ ] Automate version bump via CI

## Files in This Module

```
torana-bom/
└── build.gradle     ← java-platform constraints block
```

## Notes

- Never add implementation logic here.
- When upgrading a library, update **only** this BOM — all modules pick up the change automatically.
- Spring Boot and Spring Cloud BOM versions must be kept in lock-step; check compatibility matrix at [start.spring.io](https://start.spring.io).
