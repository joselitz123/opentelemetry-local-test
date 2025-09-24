# Inspiration
To have first hand experience with instrumetation using OTel, this repo to quickly setup a running java app inspired from the [Getting Started by Example](https://opentelemetry.io/docs/languages/java/getting-started/) made by OTel.

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
[http://localhost:8080/rolldice](http://localhost:8080/rolldice) you can also specify the player of the dice for example [http://localhost:8080/rolldice?player=joe](http://localhost:8080/rolldice?player=joe)