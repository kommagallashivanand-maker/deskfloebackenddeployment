package com.p99soft.deskflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeskFlow API Documentation")
                        .version("1.0.0")
                        .description("API documentation for the DeskFlow ticketing system. " +
                                     "Login via POST /api/v1/auth/login to obtain a JWT, " +
                                     "then click 'Authorize' and enter: Bearer <token>"))
                // Register the Bearer scheme globally
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here. Obtain it from POST /api/v1/auth/login")))
                // Apply globally so all endpoints show the lock icon in Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
