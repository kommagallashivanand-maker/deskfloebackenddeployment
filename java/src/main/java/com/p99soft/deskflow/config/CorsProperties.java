package com.p99soft.deskflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typed configuration binding for CORS settings defined in {@code application.yaml}.
 *
 * <pre>
 * cors:
 *   allowed-origins: http://localhost:3000,http://localhost:5173
 *   allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
 *   allowed-headers: "*"
 *   allow-credentials: true
 *   max-age: 3600
 * </pre>
 *
 * Override origins in production via the {@code CORS_ALLOWED_ORIGINS} environment variable.
 */
@Component
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {

    /** Comma-separated list of allowed origins. Override via {@code CORS_ALLOWED_ORIGINS}. */
    private List<String> allowedOrigins;

    /** HTTP methods allowed for cross-origin requests. */
    private List<String> allowedMethods;

    /** Request headers allowed in cross-origin requests. Use {@code *} to allow all. */
    private List<String> allowedHeaders;

    /** Whether to include credentials (cookies, auth headers) in CORS requests. */
    private boolean allowCredentials;

    /** How long (in seconds) the browser can cache CORS preflight responses. */
    private long maxAge;
}
