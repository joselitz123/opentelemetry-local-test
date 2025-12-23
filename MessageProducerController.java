package otel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for sending messages to JMS queue/topic
 * This is for demo purposes to test the JmsListener functionality
 */
@RestController
@RequestMapping("/messages")
public class MessageProducerController {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerController.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private JmsConfig jmsConfig;

    /**
     * Send a simple text message to the queue/topic
     */
    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestParam String message) {
        String messageId = UUID.randomUUID().toString();
        
        try {
            // Create a message object with metadata
            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("id", messageId);
            messagePayload.put("content", message);
            messagePayload.put("timestamp", System.currentTimeMillis());
            messagePayload.put("mode", jmsConfig.isProductionMode() ? "PRODUCTION" : "DEMO");
            
            // Send the message
            String destination = jmsConfig.getEntityName();
            jmsTemplate.convertAndSend(destination, messagePayload);
            
            logger.info("Message sent to {}: {}", destination, messagePayload);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("messageId", messageId);
            response.put("destination", destination);
            response.put("payload", messagePayload);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to send message", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("mode", jmsConfig.isProductionMode() ? "PRODUCTION" : "DEMO");
            
            return errorResponse;
        }
    }

    /**
     * Send multiple messages for batch testing
     */
    @PostMapping("/send-batch")
    public Map<String, Object> sendBatchMessages(@RequestParam int count) {
        Map<String, Object> response = new HashMap<>();
        response.put("mode", jmsConfig.isProductionMode() ? "PRODUCTION" : "DEMO");
        response.put("destination", jmsConfig.getEntityName());
        
        try {
            for (int i = 1; i <= count; i++) {
                Map<String, Object> messagePayload = new HashMap<>();
                messagePayload.put("id", UUID.randomUUID().toString());
                messagePayload.put("content", "Demo message " + i);
                messagePayload.put("timestamp", System.currentTimeMillis());
                messagePayload.put("batch", i);
                
                jmsTemplate.convertAndSend(jmsConfig.getEntityName(), messagePayload);
                logger.info("Sent batch message {}: {}", i, messagePayload);
            }
            
            response.put("status", "success");
            response.put("count", count);
            response.put("destination", jmsConfig.getEntityName());
            
        } catch (Exception e) {
            logger.error("Failed to send batch messages", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    /**
     * Get configuration info about the current JMS setup
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("mode", jmsConfig.isProductionMode() ? "PRODUCTION (Azure Service Bus)" : "DEMO (ActiveMQ Artemis)");
        config.put("destination", jmsConfig.getEntityName());
        config.put("entityType", jmsConfig.getEntityType());
        config.put("description", jmsConfig.isProductionMode() ? 
            "Using Azure Service Bus with token authentication" : 
            "Using in-memory JMS for demo - no setup required!");
        
        return config;
    }
}
