# JWT Authentication & Authorization Guide

This document describes the complete JWT authentication and role-based authorization implementation in the DeskFlow backend.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Role Matrix](#role-matrix)
4. [Implementation Details](#implementation-details)
5. [Configuration](#configuration)
6. [API Endpoints](#api-endpoints)
7. [Token Structure](#token-structure)
8. [Security Flow](#security-flow)
9. [CORS Configuration](#cors-configuration)
10. [Testing](#testing)
11. [Troubleshooting](#troubleshooting)
12. [Best Practices](#best-practices)
13. [Dependencies](#dependencies)
14. [Future Enhancements](#future-enhancements)

---

## Overview

The DeskFlow backend implements **stateless JWT-based authentication and role-based authorization** using Spring Security and the JJWT library.

### Key Features

- ✅ BCrypt password hashing
- ✅ Stateless sessions (no HTTP session storage)
- ✅ JWT tokens with `userId`, `email`, and `role` claims
- ✅ Token expiration (configurable via `JWT_EXPIRATION_MS`)
- ✅ Secure HMAC-SHA256 signing
- ✅ Role-based access control (RBAC) — EMPLOYEE, AGENT, ADMIN
- ✅ Admin-provisioned user registration (self-registration disabled)
- ✅ Structured JSON error responses for 401 and 403
- ✅ CORS support with configurable allowed origins
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
┌──────────────────┐     ┌──────────────────────────┐
│  AuthController  │     │  TicketController         │
│  POST /login     │     │  POST /tickets  (EMPLOYEE)│
│  POST /register  │     │  GET  /tickets  (ALL)     │
│  (ADMIN only)    │     │  PUT  /tickets  (AGENT/   │
└────────┬─────────┘     │                  ADMIN)   │
         │               └──────────────────────────┘
         ↓
┌─────────────────┐
│   AuthService   │  ← orchestrates auth flow
└────────┬────────┘
         │
    ┌────┴────┐
    ↓         ↓
┌──────────┐  ┌────────────────┐
│JwtService│  │Spring Security │
│(tokens)  │  │(AuthManager +  │
│          │  │ BCrypt)        │
└──────────┘  └────────────────┘

Every request passes through:
JwtAuthenticationFilter → SecurityFilterChain → Controller
```

### File Structure

```
com.p99soft.deskflow/
├── config/
│   ├── CorsProperties.java             # CORS config binding
│   ├── JwtProperties.java              # JWT config binding
│   ├── OpenApiConfig.java              # Swagger Bearer auth scheme
│   └── SecurityConfig.java            # RBAC + CORS + filter chain
├── controller/
│   ├── AuthController.java            # /api/v1/auth/*
│   ├── TicketController.java          # /api/v1/tickets
│   └── TicketCommentController.java   # /api/v1/tickets/{id}/comments
├── dto/
│   ├── LoginRequest.java              # Login payload
│   ├── LoginResponse.java             # JWT response
│   ├── RegisterRequest.java           # Registration payload
│   └── AuthResponse.java              # Registration response
├── entity/
│   └── User.java                      # User entity (Role enum)
├── enums/
│   └── Role.java                      # EMPLOYEE | AGENT | ADMIN
├── repository/
│   └── UserRepository.java            # findByEmail(), existsByEmail()
├── security/
│   ├── JwtService.java                # Token generation & validation
│   ├── JwtAuthenticationFilter.java   # Validates JWT on every request
│   ├── JwtAuthenticationEntryPoint.java # 401 JSON response
│   ├── JwtAccessDeniedHandler.java    # 403 JSON response
│   ├── UserDetailsImpl.java           # Spring Security adapter
│   └── UserDetailsServiceImpl.java    # Loads user from DB
└── service/
    ├── AuthService.java               # Auth interface
    └── Impl/
        └── AuthServiceImpl.java       # Auth implementation
```

---

## Role Matrix

| Endpoint | EMPLOYEE | AGENT | ADMIN |
|---|:---:|:---:|:---:|
| `POST /api/v1/auth/login` | ✅ | ✅ | ✅ |
| `POST /api/v1/auth/register` | ❌ 403 | ❌ 403 | ✅ |
| `POST /api/v1/tickets` | ✅ | ❌ 403 | ❌ 403 |
| `GET /api/v1/tickets` | ✅ | ✅ | ✅ |
| `GET /api/v1/tickets/{id}` | ✅ | ✅ | ✅ |
| `PUT /api/v1/tickets/{id}` | ❌ 403 | ✅ | ✅ |
| `POST /api/v1/tickets/{id}/comments` | ✅ | ✅ | ✅ |
| `GET /api/v1/tickets/{id}/comments` | ✅ | ✅ | ✅ |
| `GET /api/v1/tickets/{id}/activities` | ✅ | ✅ | ✅ |
| No token on any protected endpoint | 401 | 401 | 401 |

### Role Responsibilities

| Role | Description |
|---|---|
| `EMPLOYEE` | Raises support tickets, views tickets, adds comments |
| `AGENT` | Handles and resolves tickets (update/assign), adds comments |
| `ADMIN` | Manages users (register), views all data, updates tickets |

---

## Implementation Details

### 1. User Entity & Role Enum

**File:** `entity/User.java`

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private Role role;  // EMPLOYEE | AGENT | ADMIN
```

**File:** `enums/Role.java`

```java
public enum Role {
    EMPLOYEE,  // raises tickets
    AGENT,     // resolves tickets
    ADMIN      // manages users and system
}
```

---

### 2. JWT Configuration

**File:** `config/JwtProperties.java`

Binds `jwt.*` from `application.yaml`:

```java
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;    // Hex-encoded HMAC-SHA256 key (min 64 hex chars)
    private long expiration;  // Token validity in milliseconds
}
```

**File:** `src/main/resources/application.yaml`

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION_MS}
```

---

### 3. JwtService

**File:** `security/JwtService.java`

Single responsibility: JWT generation, validation, and claim extraction only.

```java
public String generateToken(User user)          // builds signed JWT
public boolean isTokenValid(String token, UserDetails userDetails)
public String extractEmail(String token)         // reads sub claim
public UUID   extractUserId(String token)        // reads userId claim
public String extractRole(String token)          // reads role claim
```

Token payload includes:
- `sub` — user email
- `userId` — UUID
- `role` — EMPLOYEE / AGENT / ADMIN
- `iat` — issued at
- `exp` — expiration

---

### 4. JwtAuthenticationFilter

**File:** `security/JwtAuthenticationFilter.java`

Runs on every request before Spring's default filter:

```
1. Read Authorization header
2. If missing or not "Bearer " → skip (let SecurityFilterChain handle it)
3. Extract and parse JWT
4. Load UserDetails from DB by email
5. Validate token (signature + expiry + email match)
6. If valid → populate SecurityContext
7. Continue filter chain
```

---

### 5. JwtAuthenticationEntryPoint

**File:** `security/JwtAuthenticationEntryPoint.java`

Returns structured `401 Unauthorized` JSON when:
- No token is present on a protected endpoint
- Token is expired, malformed, or has invalid signature

```json
{
  "timestamp": "<timestamp>",
  "status": 401,
  "error": "Unauthorized",
  "message": "Access denied: Authorization header is missing",
  "details": "uri=<request-uri>"
}
```

---

### 6. JwtAccessDeniedHandler

**File:** `security/JwtAccessDeniedHandler.java`

Returns structured `403 Forbidden` JSON when an authenticated user lacks the required role:

```json
{
  "timestamp": "<timestamp>",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: you do not have permission to perform this action. ADMIN role required.",
  "details": "uri=<request-uri>"
}
```

---

### 7. SecurityConfig

**File:** `config/SecurityConfig.java`

**Beans Configured:**

| Bean | Purpose |
|---|---|
| `PasswordEncoder` | BCrypt with default strength 10 |
| `AuthenticationProvider` | `DaoAuthenticationProvider` + BCrypt |
| `AuthenticationManager` | Exposed for programmatic auth in `AuthServiceImpl` |
| `CorsConfigurationSource` | Reads allowed origins from `CorsProperties` |
| `SecurityFilterChain` | Full RBAC rules + CORS + exception handlers |

**Authorization Rules (URL level):**

```java
// Public
POST  /api/v1/auth/login       → permitAll()
      /swagger-ui/**           → permitAll()
      /v3/api-docs/**          → permitAll()
      /actuator/health         → permitAll()

// Role-restricted
POST  /api/v1/auth/register    → hasRole("ADMIN")
POST  /api/v1/tickets          → hasRole("EMPLOYEE")
PUT   /api/v1/tickets/**       → hasAnyRole("AGENT", "ADMIN")
GET   /api/v1/tickets/**       → hasAnyRole("EMPLOYEE", "AGENT", "ADMIN")

// Catch-all
*                              → authenticated()
```

**Method-level security** (`@PreAuthorize`) mirrors URL rules on each controller method as a second enforcement layer.

---

### 8. AuthService & AuthServiceImpl

**`register()` — ADMIN calls this:**
1. Verify email not already registered
2. Hash password with BCrypt
3. Save user to DB
4. Return `AuthResponse` (no JWT — user must log in separately)

**`login()` — all roles:**
1. Authenticate via `AuthenticationManager`
2. Spring Security validates credentials + BCrypt hash
3. `JwtService.generateToken(user)` creates signed JWT
4. Return `LoginResponse` with token + user metadata

---

### 9. DTOs

#### LoginRequest
```json
{
  "email": "<user@example.com>",
  "password": "<password>"
}
```

#### LoginResponse
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

#### RegisterRequest (ADMIN only)
```json
{
  "employeeCode": "<employee-code>",
  "firstName": "<first-name>",
  "lastName": "<last-name>",
  "email": "<user@example.com>",
  "password": "<password>",
  "role": "<EMPLOYEE|AGENT|ADMIN>",
  "teamId": "<team-uuid>"
}
```

#### AuthResponse (register success)
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

## Configuration

### application.yaml

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION_MS}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
  allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
  max-age: 3600
```

### Environment Variables

| Variable | Description |
|---|---|
| `JWT_SECRET` | 256-bit hex-encoded HMAC key (min 64 hex chars) |
| `JWT_EXPIRATION_MS` | Token validity in milliseconds |
| `DB_URL` | JDBC connection string |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins |

**Generate a secure JWT secret:**

```bash
openssl rand -hex 32
```

---

## API Endpoints

### Public

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "<user@example.com>",
  "password": "<password>"
}
```
**Response `200 OK`:** `LoginResponse` with JWT token

---

### ADMIN Only

#### Register User
```http
POST /api/v1/auth/register
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "firstName": "<first-name>",
  "lastName": "<last-name>",
  "email": "<user@example.com>",
  "password": "<password>",
  "role": "<EMPLOYEE|AGENT|ADMIN>"
}
```
**Response `201 Created`:** `AuthResponse`
**`401`** — no/invalid token | **`403`** — not ADMIN

---

### EMPLOYEE Only

#### Create Ticket
```http
POST /api/v1/tickets
Authorization: Bearer <employee-jwt-token>
Content-Type: application/json

{
  "title": "<ticket-title>",
  "description": "<ticket-description>",
  "priority": "<LOW|MEDIUM|HIGH|URGENT>",
  "categoryId": "<category-uuid>",
  "createdBy": "<user-uuid>"
}
```
**Response `201 Created`:** `TicketResponse`
**`403`** — AGENT and ADMIN cannot create tickets

---

### AGENT and ADMIN

#### Update Ticket
```http
PUT /api/v1/tickets/{id}
Authorization: Bearer <agent-or-admin-jwt-token>
Content-Type: application/json
```
**Response `200 OK`:** `TicketResponse`
**`403`** — EMPLOYEE cannot update tickets

---

### All Authenticated Users

#### List Tickets
```http
GET /api/v1/tickets
Authorization: Bearer <jwt-token>
```

#### Get Ticket
```http
GET /api/v1/tickets/{id}
Authorization: Bearer <jwt-token>
```

#### Add Comment
```http
POST /api/v1/tickets/{id}/comments
Authorization: Bearer <jwt-token>
```

#### Get Comments
```http
GET /api/v1/tickets/{id}/comments
Authorization: Bearer <jwt-token>
```

#### Get Activity Timeline
```http
GET /api/v1/tickets/{id}/activities
Authorization: Bearer <jwt-token>
```

---

## Token Structure

### JWT Payload

```json
{
  "sub": "<user@example.com>",
  "userId": "<uuid>",
  "role": "<EMPLOYEE|AGENT|ADMIN>",
  "iat": "<unix-issued-at>",
  "exp": "<unix-expiration>"
}
```

**Decode tokens at:** [jwt.io](https://jwt.io)

---

## Security Flow

### Login Flow

```
POST /login
  ↓
AuthenticationManager.authenticate(email, password)
  ↓
UserDetailsService.loadUserByUsername(email)
  ↓
DB lookup → not found → 401
  ↓ found
BCrypt.matches(input, hash) → mismatch → 401
  ↓ match
JwtService.generateToken(user)
  ↓
Return LoginResponse { token, tokenType, userId, email, fullName, role }
```

### Authenticated Request Flow

```
Request with Authorization: Bearer <token>
  ↓
JwtAuthenticationFilter
  ↓
Extract + validate token
  ↓
Invalid/expired → JwtAuthenticationEntryPoint → 401 JSON
  ↓ valid
Populate SecurityContext
  ↓
SecurityFilterChain URL rules → wrong role → JwtAccessDeniedHandler → 403 JSON
  ↓ authorized
@PreAuthorize on method → wrong role → 403 JSON
  ↓ authorized
Controller → Service → Response
```

---

## CORS Configuration

**File:** `config/CorsProperties.java`

Binds `cors.*` from `application.yaml`:

```java
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private boolean allowCredentials;
    private long maxAge;
}
```

**Production setup:**

```bash
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

CORS is registered in `SecurityConfig` via `CorsConfigurationSource` bean and applied with `.cors(cors -> cors.configurationSource(...))`.

---

## Testing

### Postman Quick Reference

| Step | Method | Endpoint | Auth | Expected |
|---|---|---|---|---|
| Login as ADMIN | POST | `/api/v1/auth/login` | none | `200` + token |
| Register EMPLOYEE | POST | `/api/v1/auth/register` | ADMIN token | `201` |
| EMPLOYEE registers (blocked) | POST | `/api/v1/auth/register` | EMPLOYEE token | `403` |
| EMPLOYEE creates ticket | POST | `/api/v1/tickets` | EMPLOYEE token | `201` |
| AGENT creates ticket (blocked) | POST | `/api/v1/tickets` | AGENT token | `403` |
| ADMIN creates ticket (blocked) | POST | `/api/v1/tickets` | ADMIN token | `403` |
| AGENT updates ticket | PUT | `/api/v1/tickets/{id}` | AGENT token | `200` |
| EMPLOYEE updates ticket (blocked) | PUT | `/api/v1/tickets/{id}` | EMPLOYEE token | `403` |
| Anyone views tickets | GET | `/api/v1/tickets` | any token | `200` |
| No token on protected endpoint | GET | `/api/v1/tickets` | none | `401` |
| Expired/malformed token | GET | `/api/v1/tickets` | bad token | `401` |

### Auto-Save Tokens (Post-response script)

```javascript
// Run on login responses to save tokens as collection variables
const role = pm.response.json().role;
const token = pm.response.json().token;
if (role === "ADMIN")    pm.collectionVariables.set("adminToken", token);
if (role === "AGENT")    pm.collectionVariables.set("agentToken", token);
if (role === "EMPLOYEE") pm.collectionVariables.set("employeeToken", token);
```

---

## Troubleshooting

### 401 on Login
- Wrong email or password
- User status is not `ACTIVE`
- Password stored as plain text — needs BCrypt hash

```sql
-- Check user exists and is active
SELECT email, role, status FROM users WHERE email = '<user@example.com>';

-- Update to BCrypt hash if plain text
UPDATE users SET password = '<bcrypt-hash>' WHERE email = '<user@example.com>';
```

### 401 on Protected Endpoint
- Missing `Authorization` header
- Header format wrong — must be `Bearer <token>` (space required)
- Token expired — check `exp` claim at jwt.io
- Token signature invalid — `JWT_SECRET` changed

### 403 on Endpoint
- Authenticated but wrong role — check the [Role Matrix](#role-matrix)
- Verify the role stored in DB: `SELECT email, role FROM users;`

### CORS Error in Browser
- Add frontend origin to `CORS_ALLOWED_ORIGINS` env var
- Ensure `OPTIONS` is in `cors.allowed-methods`

### Flyway Failed Migration
```sql
DELETE FROM flyway_schema_history WHERE version = '4' AND success = false;
```

---

## Best Practices

- ✅ Never log JWTs — they are credentials
- ✅ Use HTTPS in production
- ✅ Store `JWT_SECRET` in a secrets manager, never in code
- ✅ Keep token expiration short (1–24 hours)
- ✅ Defense in depth — URL rules + `@PreAuthorize` both enforced
- ✅ Return `401` for missing/invalid tokens, `403` for wrong role
- ✅ Admin-provisioned registration — no self-registration

---

## Dependencies

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

- [ ] Refresh tokens — avoid repeated logins on expiry
- [ ] Token revocation / blacklist
- [ ] Rate limiting on `/login` — prevent brute force
- [ ] Account lockout after N failed attempts
- [ ] Password reset via email
- [ ] MFA — TOTP or SMS OTP
- [ ] OAuth2 — Google/Microsoft SSO

---

**Questions?** Contact the backend team or open an issue on the project repository.
