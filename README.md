# OpenTelemetry & JMS Demo Project

This project demonstrates **two complete demo applications** in a single Spring Boot application:

1. **OpenTelemetry Demo** - A rolling dice application instrumented with OpenTelemetry for observability
2. **JMS Demo** - A JmsListener implementation for Azure Service Bus with access token authentication support (includes demo mode for immediate testing)

Both demos run simultaneously on port 8080, making it easy to explore different observability and messaging concepts.

## 🎯 Features

### OpenTelemetry Demo (`/rolldice`)
- Simple REST API for rolling dice
- OpenTelemetry instrumentation ready
- Traces, metrics, and logs export to Grafana Cloud
- Perfect for learning OpenTelemetry from scratch

### JMS Demo (`/messages/*`)
- JmsListener library implementation for Azure Service Bus
- Token authentication support using Azure Identity (ClientSecretCredential)
- **Demo Mode**: Works immediately with in-memory ActiveMQ Artemis (no setup required!)
- **Production Mode**: Ready for Azure Service Bus integration
- REST API endpoints for sending/receiving messages
- Order processing business logic examples

## 🚀 Quick Start (Demo Mode)

Both demos work out-of-the-box with no external setup required!

### 1. Build the Project
```bash
gradle assemble
```

### 2. Run the Application
```bash
java -jar ./build/libs/opentelemetry-local-test.jar
```

The application starts with **Demo Mode** enabled by default, using in-memory JMS.

### 3. Test Both Demos

#### **OpenTelemetry Demo - Roll the Dice**
```bash
# Roll dice (anonymous player)
curl http://localhost:8080/rolldice

# Roll dice with player name
curl http://localhost:8080/rolldice?player=joe
```

#### **JMS Demo - Send and Receive Messages**
```bash
# Send a message (watch the console - the JmsListener will receive it!)
curl -X POST "http://localhost:8080/messages/send?message=Hello%20World"

# Send an order message (demonstrates business logic)
curl -X POST "http://localhost:8080/messages/send?message=Processing%20Order%2012345"

# Send multiple messages
curl -X POST "http://localhost:8080/messages/send-batch?count=5"

# Check current configuration
curl http://localhost:8080/messages/config
```

That's it! Both demos are now running and you can explore them further.

---

## 📊 OpenTelemetry Demo - Full Setup

This section provides complete instructions for setting up OpenTelemetry instrumentation with Grafana Cloud.

### Inspiration
This demo is inspired by the [Getting Started by Example](https://opentelemetry.io/docs/languages/java/getting-started/) guide from OpenTelemetry.

### Step-by-Step Setup

1. **Fork this project** to your GitHub account

2. **Launch GitHub Codespaces**
   - Open your VS Code
   - Press `Ctrl + Shift + P` to open Command Palette
   - Search for "Codespaces: Create New Codespace"
   - Select the repo you forked

3. **Test the Application**
   ```bash
   # Build
   gradle assemble
   
   # Run
   java -jar ./build/libs/opentelemetry-local-test.jar
   ```

4. **Verify the App Works**
   - Do port forwarding via port 8080 in VS Code
   - Access [http://localhost:8080/rolldice](http://localhost:8080/rolldice)
   - Test with player: [http://localhost:8080/rolldice?player=joe](http://localhost:8080/rolldice?player=joe)
   - Stop the app once verified

5. **Create Grafana Cloud Account**
   - Sign up for a [free tier account at Grafana Cloud](https://grafana.com/products/cloud/)
   - Create your own instance

6. **Get OpenTelemetry Connection Details**
   - From your Grafana instance, click the **Details** button
   - Copy the OTLP endpoint and authentication headers for the next steps

7. **Download the OpenTelemetry Agent**
   ```bash
   mkdir /workspaces/dependencies && \
   cd /workspaces/dependencies && \
   curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar && \
   cd /workspaces/opentelemetry-local-test
   ```

8. **Set Environment Variables**
   Replace the placeholder values with your actual Grafana Cloud details:
   ```bash
   export JAVA_TOOL_OPTIONS="-javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar"
   export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
   export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer <your-grafana-cloud-token>"
   export OTEL_METRICS_EXPORTER="otlp"
   export OTEL_RESOURCE_ATTRIBUTES="service.name=dice-demo,deployment.environment=production"
   export OTEL_TRACES_EXPORTER="otlp"
   export OTEL_EXPORTER_OTLP_ENDPOINT="<your-grafana-cloud-otel-endpoint>"
   export OTEL_SERVICE_NAME="dice-demo"
   export OTEL_METRIC_EXPORT_INTERVAL=15000
   export OTEL_LOGS_EXPORTER="otlp"
   ```

9. **Run with OpenTelemetry Instrumentation**
   ```bash
   java -jar ./build/libs/opentelemetry-local-test.jar
   ```

10. **Test and View in Grafana**
    - Call the endpoint: [http://localhost:8080/rolldice](http://localhost:8080/rolldice)
    - Call with player: [http://localhost:8080/rolldice?player=joe](http://localhost:8080/rolldice?player=joe)
    - Go to your Grafana Cloud dashboard to view traces, metrics, and logs!

---

## 📨 JMS Demo - Detailed Guide

This demo showcases a complete JMS messaging system with both demo and production modes.

### Demo Mode (Default)
- Uses in-memory ActiveMQ Artemis broker
- No external setup required
- Perfect for testing and development
- JmsListener automatically receives messages

### REST API Endpoints

#### Send Message
```bash
curl -X POST "http://localhost:8080/messages/send?message=YourMessageHere"
```
**Response:** JSON with message ID, destination, and payload details

#### Send Batch Messages
```bash
curl -X POST "http://localhost:8080/messages/send-batch?count=5"
```
**Response:** JSON with batch count and status

#### Check Configuration
```bash
curl http://localhost:8080/messages/config
```
**Response:** Current JMS configuration (demo/production mode, destination, entity type)

### Message Processing
The `OrderMessageListener` automatically processes incoming messages:

- **Standard Messages**: Logs receipt and processes
- **Order Messages**: Detects "order" keyword and runs specialized business logic:
  - Order validation
  - Inventory checking
  - Payment processing
  - Database updates
  - Completion logging

### Production Mode - Azure Service Bus

To connect to Azure Service Bus instead of in-memory JMS:

#### 1. Add Azure Dependencies
Uncomment these lines in `build.gradle.kts`:
```kotlin
implementation("com.azure:azure-servicebus-jms:1.0.0")
implementation("com.azure:azure-identity:1.10.0")
```

#### 2. Configure Azure Properties
Edit `application.properties`:
```properties
app.mode=production
app.azure.servicebus.namespace=your-namespace.servicebus.windows.net
app.azure.tenant-id=your-tenant-id
app.azure.client-id=your-client-id
app.azure.client-secret=your-client-secret
```

#### 3. Uncomment Azure Imports
In `JmsConfig.java`, uncomment the Azure-specific imports when switching to production mode.

#### 4. Rebuild and Run
```bash
gradle assemble
java -jar ./build/libs/opentelemetry-local-test.jar
```

The application will now use Azure Service Bus with token authentication via `ClientSecretCredential`.

---

## 📁 Project Structure

### Core Application Files
- `DiceApplication.java` - Main Spring Boot application entry point
- `RollController.java` - OpenTelemetry demo controller (`/rolldice`)
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

### JMS messages not received
- Check the console logs for JMS listener errors
- Verify the application is in demo mode (default) unless Azure is configured

### OpenTelemetry not exporting
- Verify environment variables are set correctly
- Check that the OTLP endpoint and credentials from Grafana Cloud are accurate
- Review the agent jar path is correct

### Azure Service Bus connection fails
- Verify all Azure properties are correctly configured
- Check that the Azure Service Bus namespace and credentials are valid
- Ensure the required Azure dependencies are uncommented in build.gradle.kts

---

## 📝 License

This project is open source and available for educational purposes.
