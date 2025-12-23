plugins {
  id("java")
  id("org.springframework.boot") version "3.0.6"
  id("io.spring.dependency-management") version "1.1.0"
}

sourceSets {
  main {
    java.setSrcDirs(setOf("."))
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Spring Boot starters
  implementation("org.springframework.boot:spring-boot-starter-web")
  
  // Spring JMS support
  implementation("org.springframework:spring-jms:6.0.11")
  
  // Spring Boot Artemis for demo mode (in-memory JMS)
  implementation("org.springframework.boot:spring-boot-starter-artemis")
  // Required for embedded Artemis server with vm:// transport
  implementation("org.apache.activemq:artemis-server")
  
  // JMS API
  implementation("jakarta.jms:jakarta.jms-api:3.1.0")
  
  // ActiveMQ core
  implementation("org.apache.activemq:activemq-client:5.18.0")
  
  // Logging
  implementation("org.slf4j:slf4j-api")
  implementation("ch.qos.logback:logback-classic:1.4.11")
  
  // For production Azure Service Bus (commented for demo)
  // implementation("com.azure:azure-servicebus-jms:1.0.0")
  // implementation("com.azure:azure-identity:1.10.0")
}
