package com.incidentiq.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class AuthServiceApplication {

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartup() {
        log.info("========================================");
        log.info("Auth Service Started Successfully");
        log.info("========================================");

        // Log active profiles
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            log.info("Active Profiles: {}", String.join(", ", profiles));
        } else {
            log.info("Active Profiles: default (none specified)");
        }

        // Log database URL (sanitized - no password)
        String dbUrl = environment.getProperty("spring.datasource.url");
        log.info("Database URL: {}", dbUrl != null ? dbUrl : "NOT SET");

        // Log other key properties
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        log.info("Eureka URL: {}", eurekaUrl != null ? eurekaUrl : "NOT SET");

        // Check for critical environment variables
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            log.info("JWT Secret: Set via environment variable (length: {})", jwtSecret.length());
        } else {
            log.info("JWT Secret: Using default from configuration");
        }

        log.info("========================================");
    }
}
