package com.incidentiq.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Authentication for IncidentIQ")
                        .version("1.0.0"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                        .url("/api/auth")
                        .description("API Gateway"));
    }
}
