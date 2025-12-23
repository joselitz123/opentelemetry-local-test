# Azure Service Bus JmsListener Implementation

This project implements a **Spring JmsListener library for Azure Service Bus** with token-based authentication. It supports both **demo mode** (in-memory JMS) and **production mode** (Azure Service Bus with access token authentication).

## 🚀 Complete Demo Walkthrough

### STEP 1: Build and Launch (30 seconds)

```bash
# Build the project
gradle assemble

# Start the application
java -jar ./build/libs/opentelemetry-local-test.jar
```

**Expected console output:**
```
🔧 Embedded Artemis broker started for demo mode on vm://0
Tomcat started on port(s): 8080
JMS message listener container started
```

### STEP 2: Verify It's Running

Open a **new terminal** and test:
```bash
# Check configuration
curl http://localhost:8080/messages/config
```

**Should return:**
```json
{
  "mode": "DEMO (ActiveMQ Artemis)",
  "destination": "demo-queue",
  "entityType": "queue",
  "description": "Using in-memory JMS for demo - no setup required!"
}
```

### STEP 3: Send Demo Messages

Try these in sequence and watch the console logs:

**Basic message:**
```bash
curl -X POST "http://localhost:8080/messages/send?message=Hello%20JMS%20World"
```

**Order message (shows business logic):**
```bash
curl -X POST "http://localhost:8080/messages/send?message=Processing%20Order%2012345"
```

**Batch test:**
```bash
curl -X POST "http://localhost:8080/messages/send-batch?count=3"
```

### STEP 4: Watch the Magic ✨

In your **application terminal**, you'll see logs like:
```
📨 JmsListener received message!
   Message ID: 2279f6fa-afeb-4984-a6a1-6f65390af14a
   Content: Hello JMS World
   Timestamp: 1766420483843
   Mode: DEMO
🔄 Standard message processed
✅ Business logic completed
```

## 🏗️ Production Mode Switch

### Step 1: Enable Azure Dependencies

Edit `build.gradle.kts` and uncomment:
```kotlin
// For production Azure Service Bus
implementation("com.azure:azure-servicebus-jms:1.0.0")
implementation("com.azure:azure-identity:1.10.0")
```

### Step 2: Configure Azure

Edit `application.properties`:
```properties
app.mode=production
app.azure.servicebus.namespace=your-namespace
app.azure.servicebus.entity-name=demo-queue
app.azure.tenant-id=your-tenant-id
app.azure.client-id=your-client-id
app.azure.client-secret=your-client-secret
```

### Step 3: Rebuild and Run
```bash
gradle clean assemble
java -jar ./build/libs/opentelemetry-local-test.jar
```

## 🔑 Token Authentication Explained

The demo uses this authentication flow for production:

```java
// 1. Create credential with Azure AD
ClientSecretCredential credential = new ClientSecretCredentialBuilder()
    .clientId(clientId)
    .clientSecret(clientSecret) 
    .tenantId(tenantId)
    .build();

// 2. Create connection factory
ServiceBusJmsConnectionFactory factory = new ServiceBusJmsConnectionFactory(
    connectionString, credential, settings
);

// 3. Use like regular JMS
jmsTemplate.convertAndSend("demo-queue", message);
```

## 🎯 API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/messages/send?message=...` | Send single message |
| POST | `/messages/send-batch?count=N` | Send N test messages |
| GET | `/messages/config` | View JMS configuration |

## 📊 Demo Real-World Example

```bash
# Send order
curl -X POST "http://localhost:8080/messages/send?message=Order#12345:5xWidgets"

# Console shows:
# 📨 JmsListener received message!
#    Content: Order#12345:5xWidgets
# 🎯 This is an order message! Processing order...
# 📦 Processing Order: 12345
#   ✓ Validating order...
#   ✓ Checking inventory...
#   ✓ Processing payment...
#   ✓ Updating database...
#   ✓ Order 12345 completed successfully
```

## 🐛 Troubleshooting

**Demo not working?**
1. The terminal shows "Embedded Artemis broker started"
2. No errors during startup
3. curl commands return JSON (not connection refused)

**Switching modes?**
```bash
# Update app.mode in application.properties
# For demo: app.mode=demo
# For production: app.mode=production

# Then rebuild and restart
gradle assemble
java -jar ./build/libs/opentelemetry-local-test.jar
```

---

## 📚 Original OpenTelemetry Documentation

**[Below is the original OpenTelemetry setup documentation and instruction set, preserved as part of the demo framework]**

<parameter name="original_documentation">
Below is the original OpenTelemetry instrumentation setup and instructions:<parameter1>
# Inspiration
To have first hand experience with instrumetation using OTel, this repo is created to quickly setup a running java app and instrument it from scratch. This is inspired from the [Getting Started by Example](https://opentelemetry.io/docs/languages/java/getting-started/) that was made by OTel.

# Setting Up and instrumenting a Rolling Dice app
Follow the instructions below:
1. Fork the project
2. Launch your VS Code
3. Install Github Codespaces
4. In VS Code press `Ctrl + Shift +P` to launch Command Palette and then search for "Codespaces: Create New Codespace.." then select the repo you forked earlier
5. Run the following command:
    1. Build: `gradle assemble`
    2. Run App: `java -jar ./build/libs/opentelemetry-local-test.jar`
6. To test the app do a port forwarding via port 8080 in your VS Code and then try to access the endpoint locally via [http://localhost:8080/rolldice](http://localhost:8080/rolldice) and after you determined that the app is working, stop the app from running to proceed with the instrumentation.
7. Create a free tier account in [Grafana Cloud](https://grafana.com/products/cloud/) and then create your own instance.
8. From your personal grafana instance, click the **Details** button to generate under Open Telemetry the variable tokens necessary for the step 10.
9. Download the Open Telemetry Agent by running the below command. **Note** this downloads the jar file outside the directory of the project under directory `/workspaces/dependencies`.

`mkdir /workspaces/dependencies && cd /workspaces/dependencies && curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar && cd /workspaces/opentelemetry-local-test`

10. Run the below command create the global variables that will be used by Open Telemetry agent during startup. **Note**: Don't forget to put proper values to a few variables below.

`export JAVA_TOOL_OPTIONS="-javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar" \`

`OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf" \`

`OTEL_EXPORTER_OTLP_HEADERS="<otel_auth_header_generated_from_grafana_cloud>" \`

`OTEL_METRICS_EXPORTER="otlp" \`

`OTEL_RESOURCE_ATTRIBUTES="<key-value_pair_of_attributes_separated_by_comma>" \`

`OTEL_TRACES_EXPORTER="otlp" \`

`OTEL_EXPORTER_OTLP_ENDPOINT="<otel_endpoint_generated_from_grafana_cloud>" \`

`OTEL_SERVICE_NAME="<service_name>" \`

`OTEL_METRIC_EXPORT_INTERVAL=15000 \`

`OTEL_LOGS_EXPORTER="otlp" `

10. Run one the app but this time with the instrumentation already enabled.

`java -jar ./build/libs/opentelemetry-local-test.jar`

11. Test the app by calling the endpoint
[http://localhost:8080/rolldice](http://localhost:8080/rolldice) you can also specify the player of the dice for example [http://localhost:8080/rolldice?player=joe](http://localhost:8080/rolldice?player=joe) <parameter2>nstrumentation using OTel, this repo is created to quickly setup a running java app and instrument it from scratch. This is inspired from the [Getting Started by Example](https://opentelemetry.io/docs/languages/java/getting-started/) that was made by OTel.

# Setting Up and instrumenting a Rolling Dice app</parameter2>
Follow the instructions below:
1. Fork the project
2. Launch your VS Code
3. Install Github Codespaces
4. In VS Code press `Ctrl + Shift +P` to launch Command Palette and then search for "Codespaces: Create New Codespace.." then select the repo you forked earlier
5. Run the following command:
    1. Build: `gradle assemble`
    2. Run App: `java -jar ./build/libs/opentelemetry-local-test.jar`
6. To test the app do a port forwarding via port 8080 in your VS Code and then try to access the endpoint locally via [http://localhost:8080/rolldice](http://localhost:8080/rolldice) and after you determined that the app is working, stop the app from running to proceed with the instrumentation.
7. Create a free tier account in [Grafana Cloud](https://grafana.com/products/cloud/) and then create your own instance.
8. From your personal grafana instance, click the **Details** button to generate under Open Telemetry the variable tokens necessary for the step 10.
9. Download the Open Telemetry Agent by running the below command. **Note** this downloads the jar file outside the directory of the project under directory `/workspaces/dependencies`.

`mkdir /workspaces/dependencies && cd /workspaces/dependencies && curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar && cd /workspaces/opentelemetry-local-test`

10. Run the below command create the global variables that will be used by Open Telemetry agent during startup. **Note**: Don't forget to put proper values to a few variables below.

`export JAVA_TOOL_OPTIONS="-javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar" \`

`OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf" \`

`OTEL_EXPORTER_OTLP_HEADERS="<otel_auth_header_generated_from_grafana_cloud>" \`

`OTEL_METRICS_EXPORTER="otlp" \`

`OTEL_RESOURCE_ATTRIBUTES="<key-value_pair_of_attributes_separated_by_comma>" \`

`OTEL_TRACES_EXPORTER="otlp" \`

`OTEL_EXPORTER_OTLP_ENDPOINT="<otel_endpoint_generated_from_grafana_cloud>" \`

`OTEL_SERVICE_NAME="<service_name>" \`

`OTEL_METRIC_EXPORT_INTERVAL=15000 \`

`OTEL_LOGS_EXPORTER="otlp" `

10. Run one the app but this time with the instrumentation already enabled.

`java -jar ./build/libs/opentelemetry-local-test.jar`

11. Test the app by calling the endpoint
[http://localhost:8080/rolldice](http://localhost:8080/rolldice) you can also specify the player of the dice for example [http://localhost:8080/rolldice?player=joe](http://localhost:8080/rolldice?player=joe)
</parameter>
