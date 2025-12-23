# JMS Listener OpenTelemetry Integration - TODO

## Project Status

### ✅ Completed Tasks

1. **Verified OrderMessageListener Architecture**
   - Confirmed that OrderMessageListener does NOT require Azure Service Bus
   - The application uses embedded Artemis (in-memory JMS) in demo mode
   - `app.mode=demo` in application.properties ensures Artemis is used, not Azure

2. **Identified and Fixed Root Issue**
   - Problem: OrderMessageListener had TWO competing listeners for the same destination
     - `receiveMessage(Map<String, Object>)` - expects Map messages
     - `receiveRawMessage(String)` - expects String messages
   - This caused Spring JMS routing ambiguity, preventing messages from being delivered
   - Solution: Removed the duplicate `receiveRawMessage` method from OrderMessageListener.java

3. **Rebuilt Application**
   - Successfully ran `gradle assemble`
   - Application JAR is ready at: `./build/libs/opentelemetry-local-test.jar`

4. **Tested the Fixed JMS Message Flow** ✅ COMPLETED
   
   **Test Results:**
   - ✅ Application starts successfully on port 8080
   - ✅ Configuration endpoint returns correct DEMO mode
   - ✅ Standard messages are sent and received correctly
   - ✅ Order messages trigger specialized processing logic
   - ✅ All business logic completes successfully
   
   **Test Executed:**
   ```bash
   # Test 1: Standard message
   curl -X POST "http://localhost:8080/messages/send?message=Hello%20World"
   # Result: ✅ Message received and processed
   
   # Test 2: Order message
   curl -X POST "http://localhost:8080/messages/send?message=New%20order%20for%20product%20123"
   # Result: ✅ Order processing triggered (validation, inventory, payment, database update)
   ```

   **Log Output Confirmed:**
   ```
   📨 JmsListener received message!
      Message ID: 0be739e9-dc68-4695-ba1c-3dd5f06e8e4b
      Content: Hello World
      Timestamp: 1766487224475
      Mode: DEMO
   🔄 Standard message processed
   ✅ Business logic completed for message: 0be739e9-dc68-4695-ba1c-3dd5f06e8e4b
   
   📨 JmsListener received message!
      Message ID: a4b4b2ab-e34b-47c2-8960-967ecd6c7b87
      Content: New order for product 123
      Timestamp: 1766487340648
      Mode: DEMO
   🎯 This is an order message! Processing order...
   📦 Processing Order: a4b4b2ab-e34b-47c2-8960-967ecd6c7b87
      ✓ Validating order...
      ✓ Checking inventory...
      ✓ Processing payment...
      ✓ Updating database...
      ✓ Order a4b4b2ab-e34b-47c2-8960-967ecd6c7b87 completed successfully
   ✅ Business logic completed for message: a4b4b2ab-e34b-47c2-8960-967ecd6c7b87
   ```

### ✅ Completed Tasks

5. **Enabled OpenTelemetry Tracing** ✅ COMPLETED

   **Test Results:**
   - ✅ OpenTelemetry agent v2.23.0 successfully attached to application
   - ✅ Application starts with agent: `Picked up JAVA_TOOL_OPTIONS: -javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar`
   - ✅ Distributed tracing is active - traceparent headers automatically added to JMS messages
   - ✅ Standard message test: Sent and received with trace context
   - ✅ Order message test: Sent and received with full order processing + trace context
   - ✅ Application PID: 94570 (running with OpenTelemetry agent)
   
   **Test Executed:**
   ```bash
   # Application started with OpenTelemetry agent
   nohup bash start-with-otel.sh > /tmp/startup.log 2>&1 &
   
   # Test 1: Standard message with tracing
   curl -X POST "http://localhost:8080/messages/send?message=OpenTelemetry%20test%20message"
   # Result: ✅ Message received with traceparent header
   # traceparent=00-a5c1bcaadea651531a5e162ea4de030f-164820beb4bd3a93-01
   
   # Test 2: Order message with tracing
   curl -X POST "http://localhost:8080/messages/send?message=New%20order%20for%20product%20OTel-123"
   # Result: ✅ Order processed with traceparent header
   # traceparent=00-6d0e579f41c02b79666c7273ce82c6b0-225b5a52216a3b8a-01
   ```

   **OpenTelemetry Agent Logs:**
   ```
   Picked up JAVA_TOOL_OPTIONS: -javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar
   [otel.javaagent 2025-12-23 11:19:22:307 +0000] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.23.0
   ```

### 🔄 Pending Tasks

6. **Fix Grafana Cloud Authentication**

   **Current State:** OpenTelemetry agent is working correctly and generating traces, but export to Grafana Cloud is failing with 401 Unauthorized errors.
   
   **Error Logs:**
   ```
   Failed to export spans. Server responded with HTTP status code 401
   Failed to export metrics. Server responded with HTTP status code 401
   Failed to export logs. Server responded with HTTP status code 401
   ```
   
   **To Fix:**
   
   a) **Get a Fresh Grafana Cloud Token:**
   - Go to your Grafana Cloud instance
   - Navigate to your API tokens section
   - Generate a new token with appropriate permissions
   
   b) **Update .env File:**
   ```bash
   # Edit .env and replace the token
   OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer YOUR_NEW_TOKEN_HERE
   ```
   
   c) **Restart Application:**
   ```bash
   # Stop current instance
   pkill -f "opentelemetry-local-test.jar"
   sleep 2
   
   # Start with new configuration
   nohup bash start-with-otel.sh > /tmp/startup.log 2>&1 &
   sleep 15
   
   # Verify no 401 errors in logs
   tail -100 /tmp/app-otel.log | grep -i "401"
   ```
   
   d) **Verify Traces in Grafana Cloud:**
   - Go to your Grafana Cloud instance
   - Navigate to Explore → Traces
   - Look for service name: `opentelemetry-demo`
   - Search for traces with operation names like:
     - `HTTP POST /messages/send`
     - `JmsTemplate.send`
     - `OrderMessageListener.receiveMessage`
     - `OrderMessageListener.processOrder`
   - Verify the trace flow shows: REST API → JMS Send → JMS Receive → Business Logic

## Files Modified

### OrderMessageListener.java
**Change**: Removed duplicate `receiveRawMessage(String)` method that was causing message routing conflicts.

**Before**: Had two listeners on same destination
**After**: Only has `receiveMessage(Map<String, Object>)` which matches the producer's message type

## Key Learnings

1. **JMS Provider Agnostic**: The `@JmsListener` annotation works with any JMS implementation (Artemis, Azure Service Bus, ActiveMQ, etc.)
2. **Message Type Matching**: Spring JMS requires message types to match between producer and consumer
3. **Single Listener per Destination**: Having multiple listeners on the same destination with different message types causes routing issues
4. **Demo Mode Configuration**: The `app.mode` property in `application.properties` controls which JMS provider is used
5. **OpenTelemetry Agent**: The agent must be attached via `-javaagent` flag to collect traces, metrics, and logs

## Configuration Details

### Current Configuration (Demo Mode)
- **JMS Provider**: Embedded Artemis (in-memory)
- **Destination Type**: Queue
- **Destination Name**: `demo-queue`
- **Transport**: `vm://0` (in-VM)
- **Message Type**: `Map<String, Object>`

### Properties Reference
```properties
# application.properties
app.mode=demo                                    # Uses Artemis (not Azure)
app.azure.servicebus.entity-name=demo-queue      # Destination name
app.azure.servicebus.entity-type=queue           # Queue or topic
spring.jms.listener.auto-startup=true           # Auto-start JMS listener
```

## Testing Summary

### ✅ Completed Tests

- [x] Application starts without errors on port 8080
- [x] `/messages/config` endpoint returns correct configuration (DEMO mode)
- [x] Message sent via `/messages/send` is received by OrderMessageListener
- [x] Console logs show: "📨 JmsListener received message!"
- [x] Message details (ID, content, timestamp) are logged correctly
- [x] Business logic processing completes: "✅ Business logic completed"
- [x] Order messages (containing "order") trigger order processing logic
  - ✓ Order validation
  - ✓ Inventory checking
  - ✓ Payment processing
  - ✓ Database update simulation
  - ✓ Order completion logging

### ⏳ Pending Tests (Requires OpenTelemetry Agent)

- [ ] OpenTelemetry agent is attached to application
- [ ] OpenTelemetry traces appear in Grafana Cloud
- [ ] Traces show complete flow: REST → JMS Send → JMS Receive → Processing
- [ ] Metrics are collected (response times, throughput, error rates)
- [ ] Logs include trace context linking them to traces

## OpenTelemetry Trace Structure

When OTEL is enabled, you should see traces with this structure:

```
HTTP POST /messages/send (Root Span)
├── JMS send operation (Child Span)
├── JMS receive operation (Child Span)
│   └── receiveMessage (Child Span)
│       └── Business logic processing (Child Span)
│           ├── Order validation (if order message)
│           ├── Inventory check
│           ├── Payment processing
│           └── Database update
└── HTTP response (Root Span)
```

## Potential Issues & Solutions

### Issue: Port 8080 already in use
**Solution**: Stop other running instances:
```bash
pkill -f "opentelemetry-local-test.jar"
# or
lsof -ti:8080 | xargs kill -9
```

### Issue: Messages not received
**Check**:
1. Application logs for JMS errors
2. Verify `app.mode=demo` in application.properties
3. Ensure only one listener method exists for the destination
4. Check if embedded Artemis started: look for "🔧 Embedded Artemis broker started" in logs

### Issue: No OTEL traces
**Check**:
1. OTEL agent jar path is correct: `/workspaces/dependencies/opentelemetry-javaagent.jar`
2. All OTEL environment variables are set
3. Grafana Cloud credentials are valid
4. OTEL endpoint is correct
5. Application is started with `-javaagent` flag
6. Check logs for OTEL initialization messages

### Issue: OTEL agent not found
**Solution**: Download the agent:
```bash
mkdir -p /workspaces/dependencies && \
cd /workspaces/dependencies && \
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

## Next Steps Priority

### To Enable OpenTelemetry Tracing:

**Setup Required:**
1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and add your actual Grafana Cloud credentials:
   - `OTEL_EXPORTER_OTLP_ENDPOINT` - Your Grafana Cloud OTLP endpoint
   - `OTEL_EXPORTER_OTLP_HEADERS` - `Authorization: Bearer <your-grafana-cloud-token>`

3. Run the application with OpenTelemetry:
   ```bash
   ./start-with-otel.sh
   ```

   The script will:
   - Stop any existing instances
   - Load credentials from `.env` file (not committed to git)
   - Start application with OpenTelemetry agent attached
   - Log output to `/tmp/app-otel.log`

4. Test the application:
   ```bash
   # Check configuration
   curl http://localhost:8080/messages/config
   
   # Send a test message
   curl -X POST "http://localhost:8080/messages/send?message=OTel%20test"
   
   # Check logs for OTEL agent initialization
   tail -50 /tmp/app-otel.log | grep -i "opentelemetry"
   ```

5. View traces in Grafana Cloud:
   - Go to your Grafana Cloud instance
   - Look for service name: `opentelemetry-demo`
   - Search for traces from the test message
   - Verify the complete flow: HTTP → JMS Send → JMS Receive → Business Logic

**Notes:**
- The `.env` file is already in `.gitignore` so credentials won't be committed
- The `start-with-otel.sh` script handles all OTEL configuration automatically
- If you don't have Grafana Cloud credentials yet, the JMS functionality works without OTEL (as verified earlier)

### Current Application Status:
- **JMS Functionality**: ✅ Working correctly (tested earlier)
- **OpenTelemetry Agent**: ✅ Attached and working (v2.23.0, PID 94570)
- **Distributed Tracing**: ✅ Active - trace context propagating through JMS
- **Grafana Cloud Export**: ❌ Failing with 401 Unauthorized (token issue)
- **Current PID**: 94570 (started at 11:19 with OpenTelemetry agent)

## Related Documentation

- `README.md` - Full project documentation and setup instructions
- `README_AZURE_JMS.md` - Azure Service Bus specific documentation (for future reference)
- `application.properties` - All configuration options
- `JmsConfig.java` - JMS configuration implementation
- `MessageProducerController.java` - REST API for sending messages
- `OrderMessageListener.java` - JMS message consumer with business logic

---
**Last Updated**: 2025-12-23 11:24
**Status**: ✅ JMS Message Flow Verified & Working - OpenTelemetry Agent Attached
**Summary**: The JMS integration is fully functional with OpenTelemetry tracing enabled. Messages flow correctly from REST API through JMS to the listener with distributed trace context. Both standard and order processing logic work as expected. The OpenTelemetry agent is successfully attached and generating traces, but export to Grafana Cloud requires a valid authentication token (currently failing with 401 errors).
