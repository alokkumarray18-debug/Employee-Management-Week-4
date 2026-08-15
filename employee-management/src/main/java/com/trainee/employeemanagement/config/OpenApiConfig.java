package com.trainee.employeemanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is served at /swagger-ui.html and the raw OpenAPI JSON at
 * /v3/api-docs once the app is running.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee & Department Management API")
                        .description("Week 4 Trainee Assignment - Advanced REST API with JPA relationships, " +
                                "DTOs, pagination, transactions and external API integration.")
                        .version("v1.0")
                        .contact(new Contact().name("Trainee").email("trainee@example.com"))
                        .license(new License().name("Internal Training Use")));
    }
}
