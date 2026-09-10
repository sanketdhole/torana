# ==============================================================================
# Torana Enterprise Agent Gateway - Production Multi-Stage Dockerfile
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package
# ------------------------------------------------------------------------------
FROM gradle:8.7-jdk17 AS builder

WORKDIR /workspace

# Copy build manifests and download dependencies
COPY gradle ./gradle
COPY gradlew settings.gradle build.gradle ./
COPY torana-bom/build.gradle ./torana-bom/
COPY torana-core/build.gradle ./torana-core/
COPY torana-protocol-mcp/build.gradle ./torana-protocol-mcp/
COPY torana-protocol-websocket/build.gradle ./torana-protocol-websocket/
COPY torana-protocol-grpc/build.gradle ./torana-protocol-grpc/
COPY torana-protocol-rest/build.gradle ./torana-protocol-rest/
COPY torana-security-authn/build.gradle ./torana-security-authn/
COPY torana-security-authz-opa/build.gradle ./torana-security-authz-opa/
COPY torana-security-vault/build.gradle ./torana-security-vault/
COPY torana-routing/build.gradle ./torana-routing/
COPY torana-pipeline/build.gradle ./torana-pipeline/
COPY torana-resilience/build.gradle ./torana-resilience/
COPY torana-ratelimit-redis/build.gradle ./torana-ratelimit-redis/
COPY torana-connector-http/build.gradle ./torana-connector-http/
COPY torana-connector-litellm/build.gradle ./torana-connector-litellm/
COPY torana-connector-s3/build.gradle ./torana-connector-s3/
COPY torana-connector-jdbc/build.gradle ./torana-connector-jdbc/
COPY torana-connector-nosql/build.gradle ./torana-connector-nosql/
COPY torana-connector-nfs/build.gradle ./torana-connector-nfs/
COPY torana-observability/build.gradle ./torana-observability/
COPY torana-test/build.gradle ./torana-test/

# Copy all source files
COPY . .

# Build application artifacts (skip tests in container build for reproducibility)
RUN ./gradlew assemble -x test --no-daemon

# ------------------------------------------------------------------------------
# Stage 2: Runtime Container
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

# Create dedicated non-root system user and group
RUN groupadd -r -g 10001 torana && \
    useradd -r -u 10001 -g torana -d /app -s /sbin/nologin -c "Torana Gateway User" torana

WORKDIR /app

# Create config and data directories with proper permissions
RUN mkdir -p /app/config /app/logs && \
    chown -R torana:torana /app

# Copy application artifacts
# Note: For production gateway standalone executable jar:
# COPY --from=builder --chown=torana:torana /workspace/torana-starter/build/libs/*.jar /app/torana-gateway.jar

USER torana:torana

# Expose Gateway Ports:
# 8080: Gateway HTTP/REST/MCP traffic
# 8081: Spring Actuator Health & Prometheus Metrics
# 9090: High-performance gRPC Adapter
EXPOSE 8080 8081 9090

# JVM container flags optimized for high-throughput reactive workloads
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ParallelRefProcEnabled -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/torana-gateway.jar --spring.config.additional-location=optional:file:/app/config/"]
