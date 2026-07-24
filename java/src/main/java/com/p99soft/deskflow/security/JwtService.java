package com.p99soft.deskflow.security;

import com.p99soft.deskflow.config.JwtProperties;
import com.p99soft.deskflow.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Stateless service responsible solely for JWT operations:
 * generation, parsing, and validation.
 *
 * <p>This class has no knowledge of authentication business logic —
 * it only works with tokens and claims (Single Responsibility).</p>
 *
 * <p>Token structure (claims):</p>
 * <ul>
 *   <li>{@code sub}    — user email (standard JWT subject)</li>
 *   <li>{@code userId} — UUID of the user</li>
 *   <li>{@code role}   — user role (EMPLOYEE / AGENT / ADMIN)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE    = "role";

    private final JwtProperties jwtProperties;

    // ------------------------------------------------------------------ //
    // Token generation
    // ------------------------------------------------------------------ //

    /**
     * Generates a signed JWT for the given {@link User}.
     *
     * @param user the authenticated user entity
     * @return compact, URL-safe JWT string
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    // ------------------------------------------------------------------ //
    // Token validation
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} if the token is structurally valid, signed correctly,
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

    /** Extracts the {@code sub} (email) claim from the token. */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extracts the {@code userId} claim from the token. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    /** Extracts the {@code role} claim from the token. */
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
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = HexFormat.of().parseHex(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
