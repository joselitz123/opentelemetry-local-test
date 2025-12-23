package otel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;

@Configuration
public class JmsConfig {

    @Value("${app.azure.servicebus.namespace:}")
    private String serviceBusNamespace;

    @Value("${app.azure.servicebus.entity-name:demo-queue}")
    private String entityName;

    @Value("${app.azure.servicebus.entity-type:queue}")
    private String entityType;

    @Value("${app.azure.tenant-id:}")
    private String tenantId;

    @Value("${app.azure.client-id:}")
    private String clientId;

    @Value("${app.azure.client-secret:}")
    private String clientSecret;

    @Value("${app.mode:demo}")
    private String appMode;

    private static EmbeddedActiveMQ embeddedServer;

    @Bean
    public ConnectionFactory connectionFactory() {
        if ("production".equals(appMode)) {
            return createAzureServiceBusConnectionFactory();
        } else {
            return createDemoConnectionFactory();
        }
    }

    private ConnectionFactory createAzureServiceBusConnectionFactory() {
        if (serviceBusNamespace == null || serviceBusNamespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Azure Service Bus namespace is required for production mode");
        }
        throw new UnsupportedOperationException("Add Azure dependencies to enable production mode");
    }

    private ConnectionFactory createDemoConnectionFactory() {
        try {
            // Start embedded Artemis server if not already running
            if (embeddedServer == null) {
                embeddedServer = new EmbeddedActiveMQ();
                
                ConfigurationImpl config = new ConfigurationImpl();
                config.setName("demo-broker");
                config.setSecurityEnabled(false);
                config.setPersistenceEnabled(false);
                
                // Configure in-VM transport
                config.addAcceptorConfiguration("invm", "vm://0");
                
                embeddedServer.setConfiguration(config);
                embeddedServer.start();
                
                System.out.println("🔧 Embedded Artemis broker started for demo mode on vm://0");
            }
            
            // Create connection factory connecting to the embedded server
            return new ActiveMQConnectionFactory("vm://0");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create demo JMS connection factory", e);
        }
    }

    @Bean
    public JmsListenerContainerFactory<?> messageListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        if ("topic".equals(entityType)) {
            template.setPubSubDomain(true);
        }
        return template;
    }

    public String getEntityName() { return entityName; }
    public String getEntityType() { return entityType; }
    public boolean isProductionMode() { return "production".equals(appMode); }
}
