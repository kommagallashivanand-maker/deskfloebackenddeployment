package com.p99soft.deskflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration for the DeskFlow backend.
 *
 * <p>How to authenticate in Swagger UI:</p>
 * <ol>
 *   <li>Call {@code POST /api/v1/auth/login} and copy the {@code token} value.</li>
 *   <li>Click the <b>Authorize</b> button at the top of the page.</li>
 *   <li>Enter {@code Bearer <token>} and click Authorize.</li>
 *   <li>All subsequent requests will include the token automatically.</li>
 * </ol>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .tags(buildTags());
    }

    private Info buildInfo() {
        return new Info()
                .title("DeskFlow API")
                .version("1.0.0")
                .description("""
                        REST API for the DeskFlow internal helpdesk platform.

                        **Authentication:**
                        1. Call `POST /api/v1/auth/login` with your email and password.
                        2. Copy the `token` from the response.
                        3. Click the **Authorize** button and enter: `Bearer <token>`.

                        **Role-based access:**
                        | Endpoint | EMPLOYEE | AGENT | ADMIN |
                        |---|:---:|:---:|:---:|
                        | POST /auth/login | ✅ | ✅ | ✅ |
                        | POST /auth/register | ❌ | ❌ | ✅ |
                        | POST /tickets | ✅ | ❌ | ❌ |
                        | GET  /tickets | ✅ own only | ✅ all | ✅ all |
                        | PUT  /tickets/{id} | ❌ | ✅ | ✅ |
                        | GET  /categories | ✅ | ✅ | ✅ |
                        """)
                .contact(new Contact()
                        .name("DeskFlow Backend Team")
                        .email("backend@p99soft.com"));
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtained from POST /api/v1/auth/login. " +
                                     "Enter the token value only — the 'Bearer ' prefix is added automatically."));
    }

    private List<Tag> buildTags() {
        return List.of(
                new Tag().name("Authentication")
                         .description("Login and admin-provisioned user registration"),
                new Tag().name("Tickets")
                         .description("Create, view, and update support tickets. " +
                                      "EMPLOYEE creates tickets and sees only their own. " +
                                      "AGENT updates and resolves tickets."),
                new Tag().name("Ticket Comments & Activity Timeline")
                         .description("Threaded comments, @mentions, and audit activity logs for tickets"),
                new Tag().name("Categories")
                         .description("Fetch available ticket categories for dropdown population")
        );
    }
}
