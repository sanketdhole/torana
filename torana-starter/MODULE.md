# torana-starter — Convenience Spring Boot Starter

## Responsibility

The **one-dependency entry point** for enterprise adopters. Pulling `com.phaselume.torana:torana-starter` as a single Maven/Gradle dependency gives a project the entire Torana framework — all modules, all auto-configurations, sensible defaults — ready to run with just a `application.yaml` and a `policy.rego`.

This module contains **no logic** — only dependency declarations and auto-configuration registration for the complete out-of-the-box experience.

## What It Includes by Default

| Module | Included |
|--------|----------|
| `torana-core` | ✅ Always |
| `torana-autoconfigure` | ✅ Always |
| `torana-protocol-mcp` | ✅ Default |
| `torana-protocol-websocket` | ✅ Default |
| `torana-protocol-rest` | ✅ Default |
| `torana-security-authn` | ✅ Default |
| `torana-security-authz-opa` | ✅ Default |
| `torana-routing` | ✅ Default |
| `torana-pipeline` | ✅ Default |
| `torana-resilience` | ✅ Default |
| `torana-ratelimit-redis` | ✅ Default |
| `torana-connector-litellm` | ✅ Default |
| `torana-connector-http` | ✅ Default |
| `torana-streaming` | ✅ Default |
| `torana-observability` | ✅ Default |
| `torana-protocol-grpc` | ⚙️ Optional — add dep separately |
| `torana-security-vault` | ⚙️ Optional |
| `torana-connector-jdbc` | ⚙️ Optional |
| `torana-connector-nosql` | ⚙️ Optional |
| `torana-connector-s3` | ⚙️ Optional |
| `torana-connector-nfs` | ⚙️ Optional |

## Adoption Guide

### Minimal `build.gradle` for enterprise adopters

```groovy
dependencies {
    implementation 'com.phaselume.torana:torana-starter:0.1.0'
    // Add optional modules as needed:
    // implementation 'com.phaselume.torana:torana-connector-jdbc:0.1.0'
    // implementation 'com.phaselume.torana:torana-security-vault:0.1.0'
}
```

### Minimal `application.yaml`

See the implementation plan's "Getting Started" example — two files are enough.

## Development Phases

### Phase 1E — Initial Starter
- [ ] Declare all Phase 1 module dependencies
- [ ] Register `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [ ] Verify `./gradlew bootRun` with only `application.yaml` + one OIDC issuer starts correctly

### Phase 5 — Maven Central Publication
- [ ] Configure `maven-publish` plugin with POM metadata
- [ ] Sign artifacts with GPG
- [ ] Publish to Maven Central via Sonatype OSSRH

## Package Layout

```
torana-starter/
├── build.gradle         ← dependency declarations only
├── MODULE.md            ← this file
└── src/main/resources/
    └── META-INF/
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
