package otel;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MapMessage;
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
 * 
 * IMPORTANT: This listener receives a generic Message object to allow
 * OpenTelemetry Java Agent to automatically instrument and capture metrics
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
     * 
     * NOTE: Using generic Message type (not Map) to allow OTEL agent instrumentation
     * to automatically capture JMS metrics like:
     * - jms_message_process_active_active
     * - jms_message_process_active_duration_seconds
     * - jms_message_process_max_seconds
     * - jms_message_process_seconds_bucket
     * - jms_message_process_seconds_count
     * - jms_message_process_seconds_sum
     */
    @JmsListener(destination = "${app.azure.servicebus.entity-name:demo-queue}")
    public void receiveMessage(Message message) throws JMSException {
        // Extract payload from MapMessage
        if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            
            logger.info("📨 JmsListener received message!");
            logger.info("   Message ID: {}", message.getJMSMessageID());
            logger.info("   Content: {}", mapMessage.getString("content"));
            logger.info("   Timestamp: {}", mapMessage.getString("timestamp"));
            logger.info("   Mode: {}", mapMessage.getString("mode"));
            
            // Create a map for business logic processing
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("id", message.getJMSMessageID());
            payload.put("content", mapMessage.getString("content"));
            payload.put("timestamp", mapMessage.getString("timestamp"));
            payload.put("mode", mapMessage.getString("mode"));
            
            // Simulate business logic processing
            processBusinessLogic(payload);
        } else {
            logger.warn("Received non-MapMessage, skipping processing");
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
