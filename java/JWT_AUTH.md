# JWT Authentication Implementation Guide

This document describes the JWT authentication implementation in the DeskFlow backend application.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Implementation Details](#implementation-details)
4. [Configuration](#configuration)
5. [API Endpoints](#api-endpoints)
6. [Token Structure](#token-structure)
7. [Security Flow](#security-flow)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Overview

The DeskFlow backend implements **stateless JWT-based authentication** using Spring Security and the JJWT library.

### Key Features

- ✅ BCrypt password hashing
- ✅ Stateless sessions (no HTTP session storage)
- ✅ JWT tokens with role-based claims
- ✅ Token expiration (configurable via `JWT_EXPIRATION_MS`)
- ✅ Secure HMAC-SHA256 signing
- ✅ Centralized exception handling
- ✅ Clean separation of concerns (SOLID principles)

### Technology Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot |
| Security | Spring Security |
| JWT Library | JJWT |
| Password Encoding | BCrypt |
| Database | PostgreSQL |

---

## Architecture

### Component Diagram

```
┌─────────────────┐
│  AuthController │  ← REST endpoints (delegates to service)
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│   AuthService   │  ← Business logic (orchestrates auth flow)
└────────┬────────┘
         │
    ┌────┴────┐
    ↓         ↓
┌─────────┐  ┌──────────┐
│ JwtSvc  │  │  Spring  │
│         │  │ Security │
└─────────┘  └──────────┘
    ↓              ↓
   JWT       BCrypt + DB
```

### File Structure

```
com.p99soft.deskflow/
├── config/
│   ├── JwtProperties.java          # JWT config binding
│   └── SecurityConfig.java         # Spring Security config
├── controller/
│   └── AuthController.java         # /api/v1/auth/* endpoints
├── dto/
│   ├── LoginRequest.java           # Login payload
│   ├── LoginResponse.java          # JWT response
│   ├── RegisterRequest.java        # Registration payload
│   └── AuthResponse.java           # Registration response
├── entity/
│   └── User.java                   # User entity (Role enum)
├── enums/
│   └── Role.java                   # EMPLOYEE | AGENT | ADMIN
├── repository/
│   └── UserRepository.java         # findByEmail(), existsByEmail()
├── security/
│   ├── JwtService.java             # Token generation & validation
│   ├── JwtAuthenticationFilter.java # Filter for token validation
│   ├── UserDetailsImpl.java        # Spring Security adapter
│   └── UserDetailsServiceImpl.java # Loads user from DB
└── service/
    ├── AuthService.java            # Auth interface
    └── Impl/
        └── AuthServiceImpl.java    # Auth implementation
```

---

## Implementation Details

### 1. User Entity & Role Enum

**File:** `entity/User.java`

The `User` entity uses a `Role` enum (not plain String):

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private Role role;  // EMPLOYEE | AGENT | ADMIN
```

**File:** `enums/Role.java`

```java
public enum Role {
    EMPLOYEE,
    AGENT,
    ADMIN
}
```

**Database:** The `role` column in the `users` table stores enum values as strings (`VARCHAR(30)`).

---

### 2. JWT Configuration

**File:** `config/JwtProperties.java`

Binds properties from `application.yaml`:

```java
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;    // Hex-encoded HMAC key
    private long expiration;  // Milliseconds (default: 24h)
}
```

**File:** `src/main/resources/application.yaml`

```yaml
jwt:
  secret: ${JWT_SECRET:<default-256-bit-hex-secret>}
  expiration: ${JWT_EXPIRATION_MS:<default-expiration-ms>}
```

**Environment Variables (Production):**

```bash
JWT_SECRET=<your-256-bit-hex-secret>
JWT_EXPIRATION_MS=<token-validity-in-milliseconds>
```

---

### 3. JwtService (Token Operations)

**File:** `security/JwtService.java`

**Responsibilities:**
- Generate signed JWT from `User` entity
- Parse and validate tokens
- Extract claims (`email`, `userId`, `role`)

**Key Methods:**

```java
public String generateToken(User user)
public boolean isTokenValid(String token, UserDetails userDetails)
public String extractEmail(String token)
public UUID extractUserId(String token)
public String extractRole(String token)
```

**Design Notes:**
- **Single Responsibility:** Only handles JWT mechanics, no auth business logic
- **Stateless:** No database calls, no session storage
- Uses JJWT library for all crypto operations
- Signing key derived from hex secret in config

---

### 4. JwtAuthenticationFilter

**File:** `security/JwtAuthenticationFilter.java`

**Purpose:** Intercepts every HTTP request and validates the JWT in the `Authorization` header.

**Flow:**

```
1. Extract Bearer token from Authorization header
2. If no token → skip (public endpoint or will be rejected by SecurityFilterChain)
3. If token exists:
   a. Parse token → extract email
   b. Load UserDetails from database
   c. Validate token (signature + expiration + email match)
   d. If valid → set SecurityContext with authenticated principal
4. Continue filter chain
```

**Integration:** Registered in `SecurityConfig` to run **before** `UsernamePasswordAuthenticationFilter`.

---

### 5. AuthService & AuthServiceImpl

**File:** `service/AuthService.java` (Interface)

```java
AuthResponse register(RegisterRequest request);
LoginResponse login(LoginRequest request);
```

**File:** `service/Impl/AuthServiceImpl.java`

**`register()` Logic:**
1. Check if email already exists → throw `IllegalArgumentException`
2. Hash password using `BCryptPasswordEncoder`
3. Save user to database
4. Return `AuthResponse` (no JWT — user must log in)

**`login()` Logic:**
1. Delegate credential validation to `AuthenticationManager` (Spring Security)
2. If valid → extract `User` from `UserDetailsImpl`
3. Generate JWT via `JwtService.generateToken(user)`
4. Return `LoginResponse` with token + user metadata

**Dependencies Injected:**
- `UserRepository`
- `TeamRepository`
- `PasswordEncoder` (BCrypt)
- `AuthenticationManager` (Spring Security)
- `JwtService` (our custom service)

---

### 6. SecurityConfig

**File:** `config/SecurityConfig.java`

**Beans Configured:**

| Bean | Purpose |
|---|---|
| `PasswordEncoder` | BCrypt with default strength (10) |
| `AuthenticationProvider` | `DaoAuthenticationProvider` backed by `UserDetailsService` |
| `AuthenticationManager` | Exposed for programmatic auth in `AuthServiceImpl` |
| `SecurityFilterChain` | HTTP security rules + filter registration |

**Security Rules:**

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/auth/**",      // Public: register, login
        "/swagger-ui/**",        // Public: API docs
        "/v3/api-docs/**",       // Public: OpenAPI spec
        "/actuator/health"       // Public: health check
    ).permitAll()
    .anyRequest().authenticated()  // Everything else requires JWT
)
```

**Session Management:**

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

No HTTP session is created or used — purely JWT-based.

**Filter Chain:**

```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

JWT filter runs **before** default Spring Security filters.

---

### 7. DTOs

#### LoginRequest

```java
{
  "email": "<user@example.com>",
  "password": "<password>"
}
```

**Validation:**
- `@NotBlank` on email and password
- `@Email` on email

---

#### LoginResponse

```java
{
  "token": "<jwt-token>",
  "tokenType": "Bearer",
  "userId": "<uuid>",
  "email": "<user@example.com>",
  "fullName": "<First Last>",
  "role": "<EMPLOYEE|AGENT|ADMIN>"
}
```

**Fields:**
- `token` — the signed JWT
- `tokenType` — always `"Bearer"`
- `userId`, `email`, `fullName`, `role` — user metadata

---

#### RegisterRequest

```java
{
  "employeeCode": "<employee-code>",    // Optional
  "firstName": "<first-name>",
  "lastName": "<last-name>",
  "email": "<user@example.com>",
  "password": "<password>",
  "role": "<EMPLOYEE|AGENT|ADMIN>",     // Required
  "teamId": "<team-uuid>"               // Optional
}
```

---

#### AuthResponse (Register)

```java
{
  "userId": "<uuid>",
  "email": "<user@example.com>",
  "fullName": "<First Last>",
  "role": "<EMPLOYEE|AGENT|ADMIN>",
  "message": "User registered successfully"
}
```

**No JWT returned** — user must call `/login` to get a token.

---

## Configuration

### application.yaml

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
  
  security:
    # Spring Security auto-config is overridden by SecurityConfig

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION_MS}
```

### Environment Variables

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET` | 256-bit hex-encoded HMAC key | `<64-char-hex-string>` |
| `JWT_EXPIRATION_MS` | Token validity in milliseconds | `<expiration-in-ms>` |
| `DB_URL` | JDBC connection string | `jdbc:postgresql://<host>:<port>/<database>` |
| `DB_USERNAME` | Database username | `<your-db-username>` |
| `DB_PASSWORD` | Database password | `<your-db-password>` |

**Generate a secure JWT secret:**

```bash
# 256-bit random hex (64 characters)
openssl rand -hex 32
```

---

## API Endpoints

### Public Endpoints (No Auth Required)

#### 1. Register User

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "firstName": "<first-name>",
  "lastName": "<last-name>",
  "email": "<user@example.com>",
  "password": "<password>",
  "role": "<EMPLOYEE|AGENT|ADMIN>"
}
```

**Response (201 Created):**

```json
{
  "userId": "<uuid>",
  "email": "<user@example.com>",
  "fullName": "<First Last>",
  "role": "<EMPLOYEE|AGENT|ADMIN>",
  "message": "User registered successfully"
}
```

---

#### 2. Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "<user@example.com>",
  "password": "<password>"
}
```

**Response (200 OK):**

```json
{
  "token": "<jwt-token>",
  "tokenType": "Bearer",
  "userId": "<uuid>",
  "email": "<user@example.com>",
  "fullName": "<First Last>",
  "role": "<EMPLOYEE|AGENT|ADMIN>"
}
```

**Error (401 Unauthorized):**

```json
{
  "timestamp": "<timestamp>",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Bad credentials",
  "details": "uri=/api/v1/auth/login"
}
```

---

### Protected Endpoints (JWT Required)

All endpoints under `/api/v1/tickets`, `/api/v1/users`, etc. require a valid JWT.

**Example:**

```http
GET /api/v1/tickets
Authorization: Bearer <jwt-token>
```

**No token or invalid token → 401 Unauthorized**

---

## Token Structure

### JWT Payload (Claims)

```json
{
  "sub": "<user@example.com>",       // Subject (email)
  "userId": "<uuid>",                 // Custom claim
  "role": "<EMPLOYEE|AGENT|ADMIN>",   // Custom claim
  "iat": <unix-timestamp>,            // Issued at (Unix timestamp)
  "exp": <unix-timestamp>             // Expiration (Unix timestamp)
}
```

### JWT Header

```json
{
  "alg": "HS256",                     // HMAC-SHA256
  "typ": "JWT"
}
```

### Signature

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

**Decode a token:** Paste it into **[jwt.io](https://jwt.io)** to inspect the payload.

---

## Security Flow

### 1. Registration Flow

```
User → POST /register
  ↓
AuthController.register()
  ↓
AuthService.register()
  ↓
Check email exists? → Yes: throw IllegalArgumentException
  ↓ No
Hash password (BCrypt)
  ↓
Save User to DB
  ↓
Return AuthResponse (no JWT)
```

---

### 2. Login Flow

```
User → POST /login
  ↓
AuthController.login()
  ↓
AuthService.login()
  ↓
AuthenticationManager.authenticate()
  ↓
UserDetailsService.loadUserByUsername()
  ↓
Query DB → User found? → No: throw UsernameNotFoundException (401)
  ↓ Yes
BCrypt.matches(inputPassword, dbHash) → No: throw BadCredentialsException (401)
  ↓ Yes
Return UserDetailsImpl
  ↓
JwtService.generateToken(user)
  ↓
Return LoginResponse with JWT
```

---

### 3. Authenticated Request Flow

```
User → GET /tickets (with Authorization: Bearer <token>)
  ↓
JwtAuthenticationFilter.doFilterInternal()
  ↓
Extract token from header
  ↓
JwtService.extractEmail(token)
  ↓
UserDetailsService.loadUserByUsername(email)
  ↓
JwtService.isTokenValid(token, userDetails) → No: continue without auth (401)
  ↓ Yes
Set SecurityContext.authentication
  ↓
Continue filter chain
  ↓
TicketController.listTickets()
  ↓
@PreAuthorize or SecurityContext used to check permissions
  ↓
Return tickets (200 OK)
```

---

## Testing

### Postman Collection

### Quick Test Sequence

```bash
# 1. Register
POST /api/v1/auth/register
Body: { "firstName": "<first>", "lastName": "<last>", "email": "<user@example.com>", "password": "<password>", "role": "EMPLOYEE" }

# 2. Login
POST /api/v1/auth/login
Body: { "email": "<user@example.com>", "password": "<password>" }
Response: { "token": "<jwt-token>" }

# 3. Access protected endpoint
GET /api/v1/tickets
Header: Authorization: Bearer <jwt-token>
Response: 200 OK
```

---

## Troubleshooting

### Issue: "401 Unauthorized" on Login

**Cause:** Invalid email or password

**Solution:**
1. Check the database: `SELECT email, role FROM users WHERE email = '<user@example.com>';`
2. Verify password hash starts with `$2a$` or `$2b$` (BCrypt)
3. If password is plain text, hash it:
   ```sql
   UPDATE users 
   SET password = '<bcrypt-hash>'
   WHERE email = '<user@example.com>';
   ```

---

### Issue: "401 Unauthorized" on Protected Endpoint

**Cause:** Missing, invalid, or expired JWT

**Checklist:**
- [ ] Token is in the `Authorization` header
- [ ] Header format is exactly: `Bearer <token>` (with space after `Bearer`)
- [ ] Token has not expired (check `exp` claim at jwt.io)
- [ ] Token signature is valid (secret key matches)
- [ ] User still exists in the database

**Debug:**
```bash
# Check token expiration
curl -H "Authorization: Bearer <your-jwt-token>" http://localhost:<port>/actuator/health
```

---

### Issue: "Malformed JWT" or "Signature Verification Failed"

**Cause:** JWT secret changed or token tampered

**Solution:**
1. Ensure `JWT_SECRET` environment variable is consistent across restarts
2. Generate a new token by logging in again
3. Old tokens become invalid when secret changes

---

### Issue: Token Works in Postman But Not in Browser

**Cause:** CORS or browser-specific security policy

**Solution:**
Add CORS configuration to `SecurityConfig`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("<your-frontend-origin>"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Then in `SecurityFilterChain`:
```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

---

### Issue: "Table 'flyway_schema_history' has failed migration"

**Cause:** Flyway V4 migration failed (role constraint violation)

**Solution:**
```sql
DELETE FROM flyway_schema_history WHERE version = '4';
```

Then restart the application.

---

## Best Practices

### Security

- ✅ **Never log JWTs** — they're credentials
- ✅ **Use HTTPS in production** — tokens sent over HTTP can be intercepted
- ✅ **Rotate JWT secrets periodically** (invalidates all existing tokens)
- ✅ **Use environment variables** for secrets, never hardcode
- ✅ **Set short expiration times** (1-24 hours) and implement refresh tokens for long-lived sessions
- ✅ **Validate all inputs** — use `@Valid` on DTOs

### Development

- ✅ **Test with expired tokens** — set `jwt.expiration=<short-ms>` for testing
- ✅ **Use jwt.io** to decode tokens during debugging
- ✅ **Check logs** — `JwtAuthenticationFilter` logs warnings on invalid tokens
- ✅ **Handle exceptions** — `GlobalExceptionHandler` centralizes all error responses

### Database

- ✅ **Always use BCrypt** for passwords — never plain text
- ✅ **Email is unique** — enforced by DB constraint
- ✅ **Role is enum** — prevents invalid values
- ✅ **User status** — `ACTIVE` users can log in, others are rejected

---

## Dependencies

### pom.xml

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## Future Enhancements

### Planned Features

- [ ] **Refresh tokens** — long-lived tokens for mobile apps
- [ ] **Token revocation** — blacklist tokens before expiration
- [ ] **Multi-factor authentication (MFA)** — TOTP or SMS OTP
- [ ] **OAuth2 integration** — Google, Microsoft, etc.
- [ ] **Rate limiting** — prevent brute-force attacks on `/login`
- [ ] **Account lockout** — disable account after N failed login attempts
- [ ] **Password reset** — email-based reset flow
- [ ] **Audit log** — track all login attempts and security events

---

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JJWT Library](https://github.com/jwtk/jjwt)
- [JWT.io](https://jwt.io)
- [BCrypt Calculator](https://bcrypt-generator.com/)
- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)

---

## Changelog

| Date | Change |
|---|---|
| `<date>` | Initial JWT implementation |
| `<date>` | Added role-based claims to JWT |
| `<date>` | Configured stateless session management |
| `<date>` | Created comprehensive documentation |

---

**Questions?** Contact the backend team or open an issue on the project repository.
