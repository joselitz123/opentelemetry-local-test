package otel;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JmsListener implementation for Azure Service Bus message processing
 *
 * This demonstrates message consumption with access token authentication
 * The listener automatically adapts to demo or production mode
 *
 * MANUAL METRICS IMPLEMENTATION:
 * OpenTelemetry Java Agent does NOT instrument ActiveMQ Artemis JMS for metrics.
 * It only works with Azure Service Bus. Therefore, we use Micrometer to manually
 * capture the jms_message_process_* metrics matching the expected naming convention.
 */
@Component
public class OrderMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderMessageListener.class);

    @Autowired
    private MeterRegistry meterRegistry;

    // Metrics matching the expected naming convention from OTEL agent
    private final Counter messageProcessedCounter;
    private final Timer processingTimer;

    public OrderMessageListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Counter for total messages processed
        this.messageProcessedCounter = Counter.builder("jms.message.processed.total")
                .description("Total number of JMS messages processed")
                .tags("messaging_operation", "process")
                .register(meterRegistry);

        // Timer for message processing duration (produces _count, _sum, _max, and histogram buckets)
        this.processingTimer = Timer.builder("jms.message.process.seconds")
                .description("Time taken to process JMS messages")
                .tags("messaging_operation", "process")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    /**
     * Main message listener for the configured destination
     * Uses the default container factory configured in JmsConfig
     *
     * For demo mode: Uses in-memory JMS with Map messages
     * For production: Would connect to Azure Service Bus with token authentication
     *
     * MANUAL METRICS CAPTURE:
     * Captures jms_message_process_seconds_* metrics using Micrometer:
     * - jms_message_process_seconds_count
     * - jms_message_process_seconds_sum
     * - jms_message_process_seconds_max
     * - jms_message_process_seconds_bucket (histogram)
     *
     * With label: messaging_operation="process"
     */
    @JmsListener(destination = "${app.azure.servicebus.entity-name:demo-queue}")
    public void receiveMessage(Message message) throws JMSException {
        // Start timing the message processing
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Extract payload from MapMessage
            if (message instanceof MapMessage) {
                MapMessage mapMessage = (MapMessage) message;

                logger.info("📨 JmsListener received message!");
                logger.info("   Message ID: {}", message.getJMSMessageID());
                logger.info("   Content: {}", mapMessage.getString("content"));
                logger.info("   Timestamp: {}", mapMessage.getString("timestamp"));
                logger.info("   Mode: {}", mapMessage.getString("mode"));

                // Create a map for business logic processing
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("id", message.getJMSMessageID());
                payload.put("content", mapMessage.getString("content"));
                payload.put("timestamp", mapMessage.getString("timestamp"));
                payload.put("mode", mapMessage.getString("mode"));

                // Simulate business logic processing
                processBusinessLogic(payload);

                // Increment counter for successful processing
                messageProcessedCounter.increment();
            } else {
                logger.warn("Received non-MapMessage, skipping processing");
            }
        } finally {
            // Stop timing and record the duration
            sample.stop(processingTimer);
        }
    }

    /**
     * Process business logic from message payload
     */
    private void processBusinessLogic(Map<String, Object> payload) {
        String id = (String) payload.get("id");
        String content = (String) payload.get("content");

        // Example business logic
        if (content != null && content.toLowerCase().contains("order")) {
            logger.info("🎯 This is an order message! Processing order...");
            processOrder(id, payload);
        } else {
            logger.info("🔄 Standard message processed");
        }

        // Log processing completion
        logger.info("✅ Business logic completed for message: {}", id);
    }

    /**
     * Specialized order processing logic
     * Demonstrates real-world processing scenario
     */
    private void processOrder(String orderId, Map<String, Object> orderDetails) {
        logger.info("📦 Processing Order: {}", orderId);

        // Simulate order processing steps
        logger.info("  ✓ Validating order...");
        logger.info("  ✓ Checking inventory...");
        logger.info("  ✓ Processing payment...");
        logger.info("  ✓ Updating database...");
        logger.info("  ✓ Order {} completed successfully", orderId);
    }

}
