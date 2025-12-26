# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Spring Boot OpenTelemetry demo project** demonstrating complete observability instrumentation for a Java application with two functional demos:

1. **REST API Demo** (`/rolldice`) - Simple dice rolling application
2. **JMS Demo** (`/messages/*`) - Complete JMS messaging system with dual-mode architecture (in-memory demo / Azure Service Bus production)

**Key Purpose**: Showcase OpenTelemetry auto-instrumentation for collecting traces, metrics, and logs from HTTP endpoints, JMS operations, and business logic.

## Build and Run Commands

### Build the Application
```bash
gradle assemble
```
Creates JAR at `./build/libs/opentelemetry-local-test.jar`

### Run Without OpenTelemetry (for quick testing)
```bash
java -jar ./build/libs/opentelemetry-local-test.jar
```

### Run With OpenTelemetry (Recommended)

The project supports two modes for OpenTelemetry observability:

#### Local Mode (Docker Stack) - Recommended for Development

Complete local observability stack with Docker Compose:
- **OpenTelemetry Collector** (port 4318) - Central telemetry pipeline
- **Prometheus** (port 9090) - Metrics storage with exemplar support
- **Grafana Tempo** (port 3200) - Distributed tracing backend
- **Grafana Loki** (port 3100) - Log aggregation
- **Grafana** (port 3000) - Unified visualization (admin/admin)

**Prerequisites:**
```bash
# Ensure Docker is running
docker info
```

**Start Local Stack:**
```bash
# 1. Ensure OpenTelemetry agent is downloaded
mkdir -p /workspaces/dependencies && \
cd /workspaces/dependencies && \
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar && \
cd /workspaces/opentelemetry-local-test

# 2. Create local environment file
cp .env.example .env.local
# .env.local is pre-configured for localhost, no changes needed

# 3. Start with local observability stack
./start-with-otel-local.sh
```

The script handles: starting Docker containers, waiting for health checks, loading `.env.local` variables, attaching the agent, and logging to `/tmp/app-otel-local.log`.

**Access Local Services:**
- Grafana Dashboard: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Tempo: http://localhost:3200
- Loki: http://localhost:3100

**Stop Local Stack:**
```bash
# Stop application
pkill -f "opentelemetry-local-test.jar"

# Stop Docker containers
cd docker && docker-compose down
```

#### Cloud Mode (Grafana Cloud) - For Production Monitoring

1. **Download OpenTelemetry Agent** (once):
```bash
mkdir -p /workspaces/dependencies && \
cd /workspaces/dependencies && \
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar && \
cd /workspaces/opentelemetry-local-test
```

2. **Configure Grafana Cloud Credentials**:
```bash
cp .env.example .env
# Edit .env with your actual Grafana Cloud OTLP endpoint and token
```

3. **Run with OpenTelemetry**:
```bash
./start-with-otel.sh
```

The script handles: stopping existing instances, loading `.env` variables, attaching the agent, and logging to `/tmp/app-otel.log`.

**Run in background**:
```bash
nohup bash start-with-otel.sh > /tmp/startup.log 2>&1 &
```

**Switching Modes:**
Simply run the appropriate startup script - no code changes required:
- Local: `./start-with-otel-local.sh` (uses `.env.local`)
- Cloud: `./start-with-otel.sh` (uses `.env`)

### Stop the Application
```bash
pkill -f "opentelemetry-local-test.jar"
```

### Test Endpoints
```bash
# REST API demo
curl http://localhost:8080/rolldice
curl http://localhost:8080/rolldice?player=joe

# JMS demo
curl http://localhost:8080/messages/config
curl -X POST "http://localhost:8080/messages/send?message=Hello%20World"
curl -X POST "http://localhost:8080/messages/send?message=Processing%20Order%2012345"
curl -X POST "http://localhost:8080/messages/send-batch?count=5"
```

## Development with VS Code Dev Containers

The project includes VS Code Dev Container configuration for a consistent, containerized development environment.

### Dev Container Features

- **Java 21** - Pre-configured Java development environment
- **Gradle 8** - Build tool pre-installed
- **Docker CLI + Host Socket Mount** - Docker access via host daemon

### Prerequisites

- Docker Desktop (or Docker daemon) running on the host
- VS Code with "Dev Containers" extension

### Getting Started in Dev Container

1. **Open in Dev Container:**
   - Open the project in VS Code
   - Press `Ctrl+Shift+P` > "Dev Containers: Reopen in Container"
   - VS Code will build the container and connect

2. **Verify Docker Access:**
   ```bash
   docker --version
   docker info
   ```
   The Docker CLI connects to the host's Docker daemon via socket mount.

3. **Run the Application:**
   ```bash
   # Build
   gradle assemble

   # Run with local observability stack
   ./start-with-otel-local.sh
   ```

### Important Notes

- The devcontainer mounts the host's Docker socket (`/var/run/docker.sock`)
- Docker Engine runs on the host, not inside the container
- The startup script (`start-with-otel-local.sh`) detects devcontainer environment and provides appropriate instructions
- If Docker is not available, rebuild the devcontainer after ensuring Docker Desktop is running

### Troubleshooting Dev Container

- If Docker commands fail: Ensure Docker Desktop is running on the host, then rebuild the container
- To rebuild: `Ctrl+Shift+P` > "Dev Containers: Rebuild Container"

## High-Level Architecture

### Dual-Mode JMS Configuration

The application supports two modes via `app.mode` in `application.properties`:

1. **Demo Mode** (`app.mode=demo`) - Default
   - Uses embedded ActiveMQ Artemis (in-memory JMS)
   - No external setup required
   - Broker started programmatically via `vm://0` transport in `JmsConfig.java:58-84`
   - Perfect for development and testing

2. **Production Mode** (`app.mode=production`)
   - Connects to Azure Service Bus
   - Requires Azure AD authentication credentials
   - Uncomment Azure dependencies in `build.gradle.kts:39-41`
   - `JmsConfig.java:51-56` throws `UnsupportedOperationException` until Azure deps are added

The `JmsConfig` class is the switch point - `connectionFactory()` bean returns either Artemis or Azure ConnectionFactory based on mode.

### Message Flow Architecture

```
HTTP Request → MessageProducerController → JmsTemplate → JMS Queue → OrderMessageListener → Business Logic
     ↓                ↓                         ↓                ↓                  ↓
   Traced           Traced                  Traced           Traced             Traced
```

**Producer** (`MessageProducerController.java:32-68`):
- Creates `Map<String, Object>` payload with id, content, timestamp, mode
- Sends via `JmsTemplate.convertAndSend()`

**Consumer** (`OrderMessageListener.java:43-67`):
- `@JmsListener` on destination from `${app.azure.servicebus.entity-name}`
- Receives `Message` (generic type), casts to `MapMessage`
- Extracts payload and delegates to `processBusinessLogic()`

**Business Logic** (`OrderMessageListener.java:72-101`):
- Standard messages: Basic logging
- Order messages (contains "order"): Specialized processing (validation, inventory, payment, database)

### Local Observability Stack Architecture

The local observability stack provides a complete development and testing environment:

**Telemetry Flow:**
```
Spring Boot App → OTLP Agent → OpenTelemetry Collector → ├─ Prometheus (metrics)
                                                      ├─ Tempo (traces)
                                                      └─ Loki (logs)
                                                              ↓
                                                         Grafana (visualization)
```

**Docker Services:**
- **OpenTelemetry Collector** - Central telemetry pipeline receiving OTLP on port 4318, routing to appropriate backends
- **Prometheus** - Metrics storage with exemplar support for trace-metric correlation
- **Grafana Tempo** - Distributed tracing backend with local filesystem storage
- **Grafana Loki** - Log aggregation with BoltDB storage
- **Grafana** - Unified visualization with pre-provisioned datasources and dashboard

**Key Configuration Files:**
- `docker/docker-compose.yml` - Orchestrates all 5 observability services
- `docker/otel-collector-config.yaml` - Central telemetry pipeline configuration
- `docker/prometheus.yml` - Metrics storage configuration
- `docker/tempo-config.yaml` - Traces storage configuration
- `docker/loki-config.yaml` - Logs storage configuration
- `docker/grafana/provisioning/datasources/datasources.yml` - Auto-configured datasources
- `docker/grafana/dashboards/opentelemetry-dashboard.json` - Pre-built dashboard
- `.env.local` - Local environment configuration (localhost:4318, no authentication)
- `start-with-otel-local.sh` - Startup script with health checks

**Testing Local Stack:**
```bash
# Generate traffic
curl http://localhost:8080/rolldice
curl http://localhost:8080/rolldice?player=test
curl -X POST "http://localhost:8080/messages/send?message=Local%20Stack%20Test"

# View in Grafana Dashboard
# http://localhost:3000 (admin/admin)
# Dashboards → OpenTelemetry Demo Dashboard
# Should see traces, metrics, and logs with full correlation
```

### OpenTelemetry Integration

The project uses **OpenTelemetry Java Agent v2.23.0** for **zero-code auto-instrumentation**:

- Agent attached via `-javaagent` JVM flag (set in `.env` as `JAVA_TOOL_OPTIONS`)
- Automatic context propagation across HTTP and JMS boundaries
- No manual instrumentation code required
- `start-with-otel.sh` script handles all environment setup

**Key OpenTelemetry Environment Variables** (from `.env.example`):
- `JAVA_TOOL_OPTIONS` - Agent attachment
- `OTEL_EXPORTER_OTLP_ENDPOINT` - Grafana Cloud endpoint
- `OTEL_EXPORTER_OTLP_HEADERS` - Authentication token
- `OTEL_SERVICE_NAME` - Service identifier (searchable in Grafana)
- `OTEL_TRACES_EXPORTER`, `OTEL_METRICS_EXPORTER`, `OTEL_LOGS_EXPORTER` - Enable all three signals

## Important Conventions and Patterns

### Message Type Consistency
- Producer sends `Map<String, Object>` via `JmsTemplate.convertAndSend()`
- Consumer receives `Message`, extracts from `MapMessage`
- Spring automatically handles the Map → MapMessage conversion
- Using generic `Message` type in listener enables proper OTEL metric capture

### Single Listener per Destination
- Only ONE `@JmsListener` method per destination
- Having multiple listeners with different message types causes routing ambiguity
- This was a bug that was fixed - previously had `receiveMessage(Map)` and `receiveRawMessage(String)` competing

### Configuration-Driven Behavior
- `app.mode=demo|production` controls entire JMS provider switch
- `app.azure.servicebus.entity-name` and `entity-type` define destination
- Properties injected via `@Value` in `JmsConfig`

### OpenTelemetry Credentials
- `.env` file is in `.gitignore` for security
- `.env.example` shows required format
- `start-with-otel.sh` sources `.env` with `set -a source .env set +a` pattern to handle quoted values correctly

## Key Files and Their Relationships

| File | Purpose | Key Details |
|------|---------|-------------|
| `DiceApplication.java` | Main Spring Boot entry point | Standard `@SpringBootApplication` |
| `JmsConfig.java` | JMS configuration switchpoint | Returns Artemis or Azure ConnectionFactory based on `app.mode` |
| `MessageProducerController.java` | REST API for sending messages | `/messages/send`, `/messages/send-batch`, `/messages/config` |
| `OrderMessageListener.java` | JMS consumer with business logic | `@JmsListener`, detects order messages for special processing |
| `RollController.java` | REST API demo controller | `/rolldice` endpoint |
| `application.properties` | Application configuration | Sets mode, destination names |
| `start-with-otel.sh` | Cloud mode startup script | Handles `.env` loading and agent attachment |
| `start-with-otel-local.sh` | Local mode startup script | Starts Docker stack, loads `.env.local` |
| `docker/docker-compose.yml` | Local observability orchestration | 5 services: Collector, Prometheus, Tempo, Loki, Grafana |
| `docker/otel-collector-config.yaml` | Telemetry pipeline configuration | Routes traces/metrics/logs to appropriate backends |
| `build.gradle.kts` | Gradle build configuration | Azure dependencies commented out for demo mode |

## JMS Metrics

The OpenTelemetry agent automatically captures these metrics (actual metric names):

- **Processing Duration**: `jms.message.process.seconds` (histogram)
  - `_count`, `_sum`, `_bucket`, `_max` aggregations

- **Publish Duration**: `jms.message.publish.seconds` (histogram)

Note: Spring Boot produces `jms.message.process.*` names (dot notation), not `jms_message_process_*` (underscore notation). See `JMS_METRICS_ACTUAL_NAMES.md` for details.

## Troubleshooting

### Port 8080 already in use
```bash
pkill -f "opentelemetry-local-test.jar"
```

### OpenTelemetry 401 Unauthorized errors
- Grafana Cloud token is invalid/expired
- Update `OTEL_EXPORTER_OTLP_HEADERS` in `.env` with fresh token
- Restart application

### No traces appearing in Grafana Cloud
- Verify `OTEL_SERVICE_NAME` matches what you're searching
- Check time range in dashboard
- Ensure OTLP endpoint is correct (should end in `:4318` for Grafana Cloud)
- Check `/tmp/app-otel.log` for errors

### JMS messages not received
- Check logs for listener errors
- Verify `app.mode=demo` unless Azure is configured
- Ensure only one listener method exists for the destination

### Local Observability Stack Issues

#### Docker Socket Permission Denied
**Symptom:** `permission denied while trying to connect to the docker API`

**Solution:**
```bash
# Fix socket group ownership
sudo chgrp docker /var/run/docker.sock

# Verify
docker info
```

**Root Cause:** Socket owned by wrong group (e.g., `root:800` instead of `root:docker`)

#### Docker API Version Mismatch
**Symptom:** `client version 1.52 is too new. Maximum supported API version is 1.43`

**Solution:** The `start-with-otel-local.sh` script now automatically detects and fixes this by setting `DOCKER_API_VERSION`.

**Manual fix:**
```bash
export DOCKER_API_VERSION=1.43
```

#### Collector Shows "Unhealthy" Status
**Symptom:** `docker compose ps` shows otel-collector as unhealthy

**Note:** This is often a display issue due to Docker API version mismatch. Verify health directly:
```bash
curl http://localhost:13133/-/healthy
```

#### Otel-Collector Logs Show "Unauthorized" for Loki
**Symptom:** `HTTP 401 "Unauthorized": no org id`

**Solution:** Ensure `loki-config.yaml` has `auth_enabled: false` set (for local development)

#### Tempo Container Keeps Restarting
**Symptom:** Tempo exits with `mkdir /tmp/tempo/wal: permission denied`

**Solution:** Remove the `wal` section from `tempo-config.yaml` (not needed for local development)
