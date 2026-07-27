package com.p99soft.deskflow.security;

import com.p99soft.deskflow.config.JwtProperties;
import com.p99soft.deskflow.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Stateless service responsible solely for JWT operations:
 * generation, parsing, and validation. No authentication business logic here.
 *
 * <h3>Token claims</h3>
 * <ul>
 *   <li>{@code sub}    — user email (standard JWT subject)</li>
 *   <li>{@code userId} — user UUID</li>
 *   <li>{@code role}   — EMPLOYEE / AGENT / ADMIN</li>
 *   <li>{@code iat}    — issued-at timestamp</li>
 *   <li>{@code exp}    — expiration timestamp</li>
 * </ul>
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE    = "role";

    private final long      expiration;
    private final SecretKey signingKey;   // computed once at startup — not per-call

    public JwtService(JwtProperties jwtProperties) {
        this.expiration = jwtProperties.getExpiration();
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(jwtProperties.getSecret()));
    }

    // ------------------------------------------------------------------ //
    // Token generation
    // ------------------------------------------------------------------ //

    /**
     * Builds and signs a JWT for the given authenticated {@link User}.
     *
     * @param user the authenticated user
     * @return compact, URL-safe JWT string
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ------------------------------------------------------------------ //
    // Token validation
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} if the token is well-formed, correctly signed,
     * not expired, and the subject matches the provided {@link UserDetails}.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String subject = extractEmail(token);
            return subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ //
    // Claim extraction
    // ------------------------------------------------------------------ //

    /** Returns the {@code sub} claim (user email). */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns the {@code userId} claim as a {@link UUID}. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    /** Returns the {@code role} claim string. */
    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    // ------------------------------------------------------------------ //
    // Internal helpers
    // ------------------------------------------------------------------ //

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
