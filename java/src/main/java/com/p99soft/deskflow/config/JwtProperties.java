package com.p99soft.deskflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration binding for JWT settings defined in {@code application.yaml}.
 *
 * <pre>
 * jwt:
 *   secret: &lt;hex-encoded 256-bit secret&gt;
 *   expiration: 86400000   # milliseconds (24 h)
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Hex-encoded HMAC-SHA256 signing secret.
     * Must be at least 256 bits (64 hex characters).
     * Override via the {@code JWT_SECRET} environment variable in production.
     */
    private String secret;

    /**
     * Token validity in milliseconds.
     * Default is 86 400 000 ms = 24 hours.
     * Override via the {@code JWT_EXPIRATION_MS} environment variable.
     */
    private long expiration;
}
