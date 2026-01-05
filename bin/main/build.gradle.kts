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
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation(platform("io.opentelemetry:opentelemetry-bom:1.54.1")) // Make sure to use the latest version
  implementation("io.opentelemetry:opentelemetry-api")
}
