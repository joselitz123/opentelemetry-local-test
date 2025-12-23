package otel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JmsListener implementation for Azure Service Bus message processing
 * 
 * This demonstrates message consumption with access token authentication
 * The listener automatically adapts to demo or production mode
 */
@Component
public class OrderMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderMessageListener.class);

    /**
     * Main message listener for the configured destination
     * Uses the default container factory configured in JmsConfig
     * 
     * For demo mode: Uses in-memory JMS with Map messages
     * For production: Would connect to Azure Service Bus with token authentication
     */
    @JmsListener(destination = "${app.azure.servicebus.entity-name:demo-queue}")
    public void receiveMessage(Map<String, Object> messagePayload) {
        logger.info("📨 JmsListener received message!");
        logger.info("   Message ID: {}", messagePayload.get("id"));
        logger.info("   Content: {}", messagePayload.get("content"));
        logger.info("   Timestamp: {}", messagePayload.get("timestamp"));
        logger.info("   Mode: {}", messagePayload.get("mode"));
        
        // Simulate business logic processing
        processBusinessLogic(messagePayload);
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

    /**
     * Alternative listener for raw JMS messages - optional
     * This shows more advanced message processing capabilities
     */
    @JmsListener(destination = "${app.azure.servicebus.entity-name:demo-queue}")
    public void receiveRawMessage(String rawMessage) {
        logger.info("📝 Received raw text message: {}", rawMessage);
        
        if (rawMessage.toLowerCase().contains("order")) {
            logger.info("🎯 Detected order in raw message");
        }
    }
}
