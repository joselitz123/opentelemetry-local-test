#!/bin/bash

# Script to start Spring Boot application with local OpenTelemetry observability stack
# This script ensures Docker Compose observability stack is running before starting app

set -e

# ANSI colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== OpenTelemetry Local Stack Startup ===${NC}"

# 1. Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker is running${NC}"

# 2. Check for required directories
DOCKER_DIR="$(dirname "$0")/docker"
if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}Error: Docker configuration directory not found at $DOCKER_DIR${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker config directory found${NC}"

# 3. Check if docker-compose.yml exists
if [ ! -f "$DOCKER_DIR/docker-compose.yml" ]; then
    echo -e "${RED}Error: docker-compose.yml not found in $DOCKER_DIR${NC}"
    echo -e "${YELLOW}Please create the docker-compose.yml and other configuration files first.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ docker-compose.yml found${NC}"

# 4. Start Docker Compose observability stack
echo -e "\n${YELLOW}Starting Docker Compose observability stack...${NC}"
cd "$DOCKER_DIR"
# Try docker compose (V2) first, fallback to docker-compose (V1)
if docker compose version &>/dev/null; then
    docker compose up -d
elif docker-compose --version &>/dev/null; then
    docker-compose up -d
else
    echo -e "${RED}Error: Neither 'docker compose' nor 'docker-compose' command found.${NC}"
    echo -e "${YELLOW}Please install Docker Compose and try again.${NC}"
    exit 1
fi

# Wait for services to be healthy
echo -e "${YELLOW}Waiting for services to be ready...${NC}"
sleep 10

# Check if collector is accepting connections
if ! curl -s http://localhost:4318 > /dev/null 2>&1; then
    echo -e "${RED}Error: OpenTelemetry Collector is not responding on http://localhost:4318${NC}"
    echo -e "${YELLOW}Check 'docker compose logs otel-collector' for errors${NC}"
    exit 1
fi
echo -e "${GREEN}✓ OpenTelemetry Collector is ready${NC}"

# Check if Grafana is ready
if ! curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo -e "${RED}Error: Grafana is not responding on http://localhost:3000${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Grafana is ready${NC}"

cd - > /dev/null

# 5. Stop any existing application instances
echo -e "\n${YELLOW}Stopping existing application instances...${NC}"
pkill -f "opentelemetry-local-test.jar" || true
sleep 2

# 6. Load environment variables from .env.local
echo -e "\n${YELLOW}Loading environment variables from .env.local...${NC}"
if [ -f .env.local ]; then
    set -a
    source .env.local
    set +a
    echo -e "${GREEN}✓ Environment variables loaded from .env.local${NC}"
else
    echo -e "${RED}Error: .env.local file not found.${NC}"
    echo -e "${YELLOW}Please copy .env.example to .env.local and update with local configuration.${NC}"
    exit 1
fi

# 7. Ensure JAVA_TOOL_OPTIONS includes the agent
if [[ ! "$JAVA_TOOL_OPTIONS" =~ "opentelemetry-javaagent" ]]; then
    export JAVA_TOOL_OPTIONS="-javaagent:/workspaces/dependencies/opentelemetry-javaagent.jar $JAVA_TOOL_OPTIONS"
fi

# 8. Verify OpenTelemetry agent exists
if [ ! -f "/workspaces/dependencies/opentelemetry-javaagent.jar" ]; then
    echo -e "${RED}Error: OpenTelemetry agent not found at /workspaces/dependencies/opentelemetry-javaagent.jar${NC}"
    echo -e "${YELLOW}Download it with:${NC}"
    echo "  mkdir -p /workspaces/dependencies && cd /workspaces/dependencies &&"
    echo "  curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
    exit 1
fi

# 9. Display configuration
echo -e "\n${GREEN}=== Configuration ===${NC}"
echo "Service:       $OTEL_SERVICE_NAME"
echo "Environment:   local"
echo "OTLP Endpoint: $OTEL_EXPORTER_OTLP_ENDPOINT"
echo "JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
echo ""

# 10. Start the application
echo -e "${GREEN}Starting application with OpenTelemetry agent...${NC}"
echo -e "${YELLOW}Application logs: /tmp/app-otel-local.log${NC}"
echo ""

java -jar ./build/libs/opentelemetry-local-test.jar 2>&1 | tee /tmp/app-otel-local.log
