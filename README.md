# OpenTelemetry Demo Project

This project demonstrates **OpenTelemetry instrumentation** applied to a complete Spring Boot application with two functional demos:

1. **REST API Demo** (`/rolldice`) - A rolling dice application
2. **JMS Demo** (`/messages/*`) - A JmsListener for Azure Service Bus with in-memory demo mode

**Both demos are instrumented with OpenTelemetry**, meaning traces, metrics, and logs are collected from ALL components including HTTP requests, JMS operations, and business logic processing.

## 🎯 What Gets Instrumented

When you enable OpenTelemetry, it automatically instruments:
- **HTTP Endpoints**: `/rolldice` and `/messages/*` REST API calls
- **JMS Operations**: Message sending, receiving, and processing
- **Business Logic**: Order processing in listener
- **Spring Components**: All internal application components
- **Database Operations**: (when configured)
- **Logs**: Application logs with trace context

## 🚀 Quick Start - OpenTelemetry Instrumented Demo

Follow these steps to set up and run application with OpenTelemetry instrumentation.

### 1. Fork and Setup

**Fork this project** to your GitHub account, then:

**Launch GitHub Codespaces:**
- Open your VS Code
- Press `Ctrl + Shift + P` to open Command Palette
- Search for "Codespaces: Create New Codespace"
- Select the repo you forked

### 2. Build Application
```bash
gradle assemble
```

### 3. Create Grafana Cloud Account
- Sign up for a [free tier account at Grafana Cloud](https://grafana.com/products/cloud/)
- Create your own instance

### 4. Get OpenTelemetry Connection Details
- From your Grafana instance, click **Details** button
- Copy the OTLP endpoint and authentication headers

### 5. Download OpenTelemetry Agent
```bash
mkdir /workspaces/dependencies && \
cd /workspaces/dependencies && \
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar && \
cd /workspaces/opentelemetry-local-test
```

### 6. Configure OpenTelemetry Credentials

The project uses a `start-with-otel.sh` script that automatically handles all OpenTelemetry configuration. You just need to provide your Grafana Cloud credentials:

```bash
# Copy example .env file
cp .env.example .env

# Edit .env and add your actual Grafana Cloud credentials
# You only need to update these TWO required fields:

# 1. OTLP Endpoint (from your Grafana Cloud instance)
# Example: https://otlp-gateway-prod-us-east-0.grafana.net/otlp
OTEL_EXPORTER_OTLP_ENDPOINT="https://your-grafana-cloud-otlp-endpoint"

# 2. Authentication Token (from your Grafana Cloud instance)
# Example: Authorization: Bearer glc_eyJvIjoi...
OTEL_EXPORTER_OTLP_HEADERS="Authorization: Bearer your-grafana-cloud-token"
```

All other OpenTelemetry settings are pre-configured in `.env.example` and will work with any Grafana Cloud instance.

### 7. Run Application with OpenTelemetry

The `start-with-otel.sh` script handles everything:

```bash
# Make script executable (if needed)
chmod +x start-with-otel.sh

# Run script
./start-with-otel.sh
```

**What script does automatically:**
1. Stops any existing application instances to avoid port conflicts
2. Loads all OpenTelemetry environment variables from `.env` file
3. Ensures OpenTelemetry Java agent is properly configured
4. Starts application with agent attached
5. Logs all output to `/tmp/app-otel.log` for debugging

**Running in background:**
If you want to run application in background (continue using terminal):

```bash
nohup bash start-with-otel.sh > /tmp/startup.log 2>&1 &
```

**Verifying application started:**
```bash
# Wait 15 seconds for startup
sleep 15

# Check if application is running
ps aux | grep "opentelemetry-local-test.jar" | grep -v grep

# Check application logs for OpenTelemetry agent
tail -50 /tmp/app-otel.log | grep -i "opentelemetry"

# Verify OpenTelemetry agent version (should show version 2.23.0 or later)
grep "opentelemetry-javaagent - version" /tmp/app-otel.log

# Test application is responding
curl http://localhost:8080/messages/config
```

**What to look for in logs:**
```
Picked up JAVA_TOOL_OPTIONS: -javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar
[otel.javaagent ...] INFO ... opentelemetry-javaagent - version: 2.23.0
Started DiceApplication in X seconds
```

**If you see 401 Unauthorized errors:**
This means your Grafana Cloud token is invalid or expired. Update token in `.env` and restart:
```bash
pkill -f "opentelemetry-local-test.jar"
sleep 2
./start-with-otel.sh
```

**Important notes:**
- The `.env` file is in `.gitignore` so your credentials won't be committed
- The script automatically includes `JAVA_TOOL_OPTIONS` if missing
- All environment variables are loaded from `.env` before starting application
- The script will show your configuration before starting (service name, endpoint, etc.)

The application starts with:
- **OpenTelemetry enabled** (collecting traces, metrics, logs)
- **JMS Demo Mode** (in-memory ActiveMQ Artemis, no Azure setup needed)
- **Both demos ready to test**

### 8. Test Both Demos (All Instrumented!)

**REST API Demo:**
```bash
# Roll dice (anonymous player) - generates OTel traces
curl http://localhost:8080/rolldice

# Roll dice with player name - generates OTel traces with player attribute
curl http://localhost:8080/rolldice?player=joe
```

**JMS Demo (also instrumented!):**
```bash
# Send a message - generates OTel traces for JMS operations
curl -X POST "http://localhost:8080/messages/send?message=Hello%20World"

# Send an order message - demonstrates business logic with OTel traces
curl -X POST "http://localhost:8080/messages/send?message=Processing%20Order%2012345"

# Send multiple messages - batch operations with OTel metrics
curl -X POST "http://localhost:8080/messages/send-batch?count=5"

# Check configuration
curl http://localhost:8080/messages/config
```

### 9. View Traces in Grafana Cloud

Go to your Grafana Cloud dashboard to explore the observability data:

- **Traces**: See the complete request flow from HTTP to JMS processing
- **Metrics**: Response times, error rates, throughput
- **Logs**: Application logs with trace context linking them to traces

You can observe:
- HTTP spans from `/rolldice` and `/messages/*` endpoints
- JMS spans from message sending and receiving
- Business logic spans from order processing
- Parent-child relationships showing how operations connect
- Metrics for both demos in a single dashboard

---

## 🏠 Local Observability Stack (Docker)

For local development and testing, you can run a complete observability stack using Docker Compose instead of sending data to Grafana Cloud.

### Architecture

The local stack consists of:
- **OpenTelemetry Collector** - Central telemetry pipeline (receives OTLP on port 4318)
- **Prometheus** - Metrics storage and querying (port 9090)
- **Grafana Tempo** - Distributed tracing backend (port 3200)
- **Grafana Loki** - Log aggregation (port 3100)
- **Grafana** - Unified visualization dashboard (port 3000)

### Prerequisites

1. **Docker** installed (v20.10+)
2. **Docker Compose** installed (v2.0+)
3. **Application JAR built** (`gradle assemble`)

### Quick Start

#### 1. Build the Application
```bash
gradle assemble
```

#### 2. Start Local Observability Stack

All configuration files are pre-created in the `docker/` directory. Simply run:

```bash
# Make script executable (if needed)
chmod +x start-with-otel-local.sh

# Start everything (Docker stack + application)
./start-with-otel-local.sh
```

**This script automatically:**
1. Checks if Docker is running
2. Starts the Docker Compose observability stack
3. Waits for services to be healthy
4. Loads environment variables from `.env.local`
5. Starts the application with OpenTelemetry agent attached
6. Logs output to `/tmp/app-otel-local.log`

**Stop the stack:**
```bash
# Stop application only
pkill -f "opentelemetry-local-test.jar"

# Stop Docker observability stack
cd docker && docker-compose down
```

### Access the Observability Stack

Once running, access each service:

- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
  - Pre-configured datasources for Prometheus, Tempo, Loki
  - Pre-loaded dashboard for OpenTelemetry metrics

- **Prometheus**: http://localhost:9090
  - Query metrics directly
  - Check targets and configuration

- **Tempo**: http://localhost:3200
  - Search and visualize traces

- **Loki**: http://localhost:3100
  - Query logs with LogQL

- **OpenTelemetry Collector**: http://localhost:4318
  - Receives telemetry from application (OTLP endpoint)

### Component Versions

The local observability stack uses these container images:
- **OpenTelemetry Collector**: 0.142.0
- **Prometheus**: 2.55.1
- **Grafana Tempo**: 2.9.0
- **Grafana Loki**: 3.6.3
- **Grafana**: 12.3.1

**Note:** These are the latest stable versions as of December 2025. To check for updates:
- OpenTelemetry: https://github.com/open-telemetry/opentelemetry-collector-contrib/releases
- Prometheus: https://github.com/prometheus/prometheus/releases
- Tempo: https://github.com/grafana/tempo/releases
- Loki: https://github.com/grafana/loki/releases
- Grafana: https://github.com/grafana/grafana/releases

### Testing the Local Stack

Generate some traffic to populate the observability stack:

```bash
# REST API demo - generates traces, metrics, logs
curl http://localhost:8080/rolldice
curl http://localhost:8080/rolldice?player=alice

# JMS demo - demonstrates messaging instrumentation
curl -X POST "http://localhost:8080/messages/send?message=Hello%20Local%20Stack"
curl -X POST "http://localhost:8080/messages/send-batch?count=10"
curl -X POST "http://localhost:8080/messages/send?message=Processing%20Order%2067890"
```

Then explore in Grafana:
1. Open http://localhost:3000
2. Navigate to **Dashboards → OpenTelemetry Demo Dashboard**
3. View traces, metrics, and logs with full correlation

### Switching Between Local and Cloud

The project supports two modes via environment file selection:

**Local Mode (Docker stack):**
```bash
./start-with-otel-local.sh  # Uses .env.local
```

**Cloud Mode (Grafana Cloud):**
```bash
./start-with-otel.sh        # Uses .env
```

Simply run the appropriate startup script. No code changes required!

### Troubleshooting Local Stack

**Docker won't start:**
- Ensure Docker daemon is running: `docker info`
- Check port conflicts (4318, 3000, 9090, 3200, 3100)

**Collector not receiving data:**
- Check collector health: `curl http://localhost:4318`
- View collector logs: `cd docker && docker-compose logs otel-collector`
- Verify application is using `.env.local`: Check startup script output

**No traces in Tempo:**
- Check Tempo is running: `curl http://localhost:3200`
- Verify collector Tempo exporter is configured
- Check application logs for OTLP errors: `tail -50 /tmp/app-otel-local.log`

**No metrics in Prometheus:**
- Check Prometheus targets: http://localhost:9090/targets
- Verify collector remote write configuration
- Check collector metrics: `curl http://localhost:8888/metrics`

**Grafana can't connect to datasources:**
- Check datasource settings in Grafana UI (Configuration → Data Sources)
- Verify all Docker containers are running: `cd docker && docker-compose ps`
- Check Docker network: `docker network inspect docker_observability`

**Stack won't stop:**
```bash
# Force stop all containers
cd docker && docker-compose down -v

# Remove volumes (deletes all data)
docker-compose down -v --volumes

# Remove orphaned containers
docker system prune -f
```

### Advantages of Local Stack

- **No Cloud Dependencies**: Works offline, no API limits
- **Faster Development**: Local data, low latency
- **Full Control**: Customize any component configuration
- **Cost-Free**: No Grafana Cloud subscription needed
- **Privacy**: Data never leaves your machine
- **Educational**: Learn each component in isolation

---

## 📊 Understanding the Demos

### REST API Demo (`/rolldice`)
A simple dice rolling REST endpoint that demonstrates:
- HTTP instrumentation
- Request parameters as span attributes
- Metrics collection (response times, error rates)
- Simple business logic tracing

### JMS Demo (`/messages/*`)
A complete JMS messaging system that demonstrates:
- JMS producer and consumer instrumentation
- Asynchronous message processing traces
- Business logic in message listeners
- Integration with Azure Service Bus (optional)

#### JMS REST API Endpoints

**Send Message:**
```bash
curl -X POST "http://localhost:8080/messages/send?message=YourMessageHere"
```
- Generates OTel traces for message sending
- JmsListener receives and processes the message
- Business logic is traced within listener

**Send Batch Messages:**
```bash
curl -X POST "http://localhost:8080/messages/send-batch?count=5"
```
- Demonstrates batch operations with multiple traces
- Shows concurrent message processing

**Check Configuration:**
```bash
curl http://localhost:8080/messages/config
```
- Returns current JMS configuration (demo/production mode)

#### Message Processing Flow

When a message is sent:
1. **Producer**: `/messages/send` receives HTTP request → traced
2. **JMS Send**: Message is sent to queue → traced
3. **Listener**: `OrderMessageListener` receives message → traced
4. **Business Logic**: Message is processed → traced
5. **Response**: HTTP response sent → traced

All steps are connected in a single distributed trace!

#### Order Processing Logic

The `OrderMessageListener` automatically processes incoming messages:

- **Standard Messages**: Logs receipt and processes
- **Order Messages**: Detects "order" keyword and runs specialized business logic:
  - Order validation (traced)
  - Inventory checking (traced)
  - Payment processing (traced)
  - Database updates (traced, when configured)
  - Completion logging (traced)

---

## 🔧 Production Mode - Azure Service Bus

To connect to Azure Service Bus instead of in-memory JMS:

### 1. Add Azure Dependencies
Uncomment these lines in `build.gradle.kts`:
```kotlin
implementation("com.azure:azure-servicebus-jms:1.0.0")
implementation("com.azure:azure-identity:1.10.0")
```

### 2. Configure Azure Properties
Edit `application.properties`:
```properties
app.mode=production
app.azure.servicebus.namespace=your-namespace.servicebus.windows.net
app.azure.tenant-id=your-tenant-id
app.azure.client-id=your-client-id
app.azure.client-secret=your-client-secret
```

### 3. Uncomment Azure Imports
In `JmsConfig.java`, uncomment the Azure-specific imports.

### 4. Rebuild and Run
```bash
gradle assemble
java -jar ./build/libs/opentelemetry-local-test.jar
```

The application will now use Azure Service Bus with token authentication, and all Azure operations will be traced by OpenTelemetry!

---

## 📁 Project Structure

### Core Application Files
- `DiceApplication.java` - Main Spring Boot application entry point
- `RollController.java` - REST API demo controller (`/rolldice`)
- `MessageProducerController.java` - JMS demo REST API (`/messages/*`)
- `OrderMessageListener.java` - JMS message listener with business logic
- `JmsConfig.java` - JMS configuration (demo/production modes)
- `application.properties` - Application configuration

### Build Files
- `build.gradle.kts` - Gradle build configuration
- `build/` - Compiled JAR file location

### Documentation
- `README.md` - This file
- `README_AZURE_JMS.md` - Additional Azure JMS documentation

---

## 🎓 Learning Resources

### OpenTelemetry
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Java Instrumentation](https://opentelemetry.io/docs/languages/java/)
- [Getting Started by Example](https://opentelemetry.io/docs/languages/java/getting-started/)
- [OTLP Specification](https://opentelemetry.io/docs/reference/specification/protocol/otlp/)

### Grafana Cloud
- [Grafana Cloud Documentation](https://grafana.com/docs/grafana-cloud/)
- [Grafana Cloud Free Tier](https://grafana.com/products/cloud/)

### Azure Service Bus JMS
- [Azure Service Bus JMS Documentation](https://learn.microsoft.com/azure/service-bus-messaging/service-bus-java-how-to-use-jms-api-overview)
- [Azure Identity Documentation](https://learn.microsoft.com/java/api/overview/azure/identity-readme)

### Spring JMS
- [Spring JMS Documentation](https://docs.spring.io/spring-framework/reference/integration/jms.html)

---

## 🐛 Troubleshooting

### Application won't start
- Check that port 8080 is not already in use
- Ensure Java 17+ is installed
- Verify JAR file exists: `./build/libs/opentelemetry-local-test.jar`

### OpenTelemetry not exporting
- Verify all environment variables are set correctly
- Check that OTLP endpoint and credentials from Grafana Cloud are accurate
- Review the agent jar path: `/workspaces/dependencies/opentelemetry-javaagent.jar`
- Check application logs for connection errors

### No traces appearing in Grafana
- Verify `OTEL_SERVICE_NAME` matches what you're searching in Grafana
- Check that the time range in Grafana dashboard includes your test timeframe
- Ensure OTLP headers include the correct authentication token
- Verify `OTEL_EXPORTER_OTLP_ENDPOINT` is correct

### JMS messages not received
- Check the console logs for JMS listener errors
- Verify application is in demo mode (default) unless Azure is configured
- Ensure message payload format is correct

### Azure Service Bus connection fails
- Verify all Azure properties are correctly configured
- Check that the Azure Service Bus namespace and credentials are valid
- Ensure required Azure dependencies are uncommented in build.gradle.kts
- Review Azure logs for authentication failures

### Codespaces port forwarding issues
- In VS Code, click "Ports" tab in the bottom panel
- Click "Add Port" and enter 8080
- Set visibility to "Public" if accessing from outside

---

## 📝 License

This project is open source and available for educational purposes.
