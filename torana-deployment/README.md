# torana-deployment — Containerization, Local Stack & Kubernetes Helm

This module provides complete, enterprise-grade deployment deliverables for **Torana Gateway**:

1. **Multi-Stage Dockerfile** — Hardened, non-root OpenJDK 17 runtime image with JVM cgroup limits.
2. **Full Local Dev Stack (Docker Compose)** — Spins up Torana Gateway along with Redis 7, OPA, OpenTelemetry Collector, Prometheus, Grafana, and Mock Backend.
3. **Production Kubernetes Helm 3 Chart** — High-availability deployment with Horizontal Pod Autoscaling (HPA), Ingress TLS, structured ConfigMaps, and Actuator health probes.

---

## 1. Directory Structure

```
torana-deployment/
├── docker/
│   └── Dockerfile
├── docker-compose/
│   ├── docker-compose.yml
│   └── configs/
│       ├── application.yml
│       ├── prometheus.yml
│       ├── otel-collector-config.yml
│       └── torana-base-policy.rego
├── helm/
│   └── torana/
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
│           ├── _helpers.tpl
│           ├── deployment.yaml
│           ├── service.yaml
│           ├── configmap.yaml
│           ├── ingress.yaml
│           ├── hpa.yaml
│           └── serviceaccount.yaml
└── README.md
```

---

## 2. Docker Container Build

Build the optimized container image:

```bash
docker build -t phaselume/torana-gateway:latest -f torana-deployment/docker/Dockerfile .
```

### Key Security & Performance Features:
- **Non-Root Execution**: Runs as UID `10001` (`torana` user).
- **JVM Container Support**: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`.
- **Minimal Surface**: Based on `eclipse-temurin:17-jre-jammy`.

---

## 3. Local Docker Compose Stack

Spin up the entire gateway ecosystem:

```bash
cd torana-deployment/docker-compose
docker-compose up -d
```

### Services & Port Mappings:

| Service | Port | Description |
|---------|------|-------------|
| `torana-gateway` | `8080` | Gateway Traffic (MCP / REST / SSE) |
| `torana-gateway` (Management) | `8081` | Actuator Health & Prometheus Metrics |
| `torana-gateway` (gRPC) | `9090` | High-throughput gRPC Adapter |
| `redis` | `6379` | Distributed sliding-window rate limiting & audit stream |
| `opa` | `8181` | Open Policy Agent zero-trust authorization |
| `otel-collector` | `4317 / 4318` | OpenTelemetry OTLP Collector |
| `prometheus` | `9091` | Metrics scraping & time series DB |
| `grafana` | `3000` | Observability dashboards (`admin` / `torana`) |
| `mock-backend` | `8089` | Upstream mock service |

---

## 4. Kubernetes Deployment via Helm 3

Deploy to Kubernetes:

```bash
# Dry run template rendering
helm template torana torana-deployment/helm/torana/ -f torana-deployment/helm/torana/values.yaml

# Install or upgrade chart
helm upgrade --install torana torana-deployment/helm/torana/ \
  --namespace torana \
  --create-namespace \
  --values torana-deployment/helm/torana/values.yaml
```

### Scaling & High Availability:
- **HPA**: Automatically scales between 3 and 10 pods based on CPU/Memory thresholds.
- **Health Checks**: Liveness probe on `/actuator/health/liveness`, Readiness probe on `/actuator/health/readiness`.
- **Rolling Updates**: Zero downtime deploys with Kubernetes rolling update strategy and config hash annotations.
