#!/bin/bash

# Stop any existing instances
pkill -f "opentelemetry-local-test.jar"
sleep 2

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    # Source the .env file to properly handle quoted values
    set -a
    source .env
    set +a
else
    echo "Warning: .env file not found. Using default/example values."
    echo "Please copy .env.example to .env and add your Grafana Cloud credentials."
    echo ""
    
    # Set default/example values (these won't work without real credentials)
    export JAVA_TOOL_OPTIONS="-javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar"
    export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
    export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer your-grafana-cloud-token"
    export OTEL_METRICS_EXPORTER="otlp"
    export OTEL_RESOURCE_ATTRIBUTES="service.name=opentelemetry-demo,deployment.environment=demo"
    export OTEL_TRACES_EXPORTER="otlp"
    export OTEL_EXPORTER_OTLP_ENDPOINT="https://your-grafana-cloud-instance:4318"
    export OTEL_SERVICE_NAME="opentelemetry-demo"
    export OTEL_METRIC_EXPORT_INTERVAL=15000
    export OTEL_LOGS_EXPORTER="otlp"
fi

# Ensure JAVA_TOOL_OPTIONS includes the agent
if [[ ! "$JAVA_TOOL_OPTIONS" =~ "opentelemetry-javaagent" ]]; then
    export JAVA_TOOL_OPTIONS="-javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar $JAVA_TOOL_OPTIONS"
fi

echo "Starting application with OpenTelemetry agent..."
echo "Service: $OTEL_SERVICE_NAME"
echo "OTEL Endpoint: $OTEL_EXPORTER_OTLP_ENDPOINT"
echo "JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
echo ""

# Start the application
java -jar ./build/libs/opentelemetry-local-test.jar 2>&1 | tee /tmp/app-otel.log
