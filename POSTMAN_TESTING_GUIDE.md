# DeskFlow Authentication Testing Guide (Postman)

## Prerequisites

1. **Start the application**
   ```bash
   cd c:\Users\Dell\Downloads\DeskFlow-backend\java
   .\mvnw.cmd spring-boot:run
   ```

2. **Ensure database is running** (check your `application.yaml` for DB connection settings)

3. **Base URL**: `http://localhost:8080`

---

## Test Sequence

### 1. Register a New User (EMPLOYEE)

**Endpoint**: `POST /api/v1/auth/register`

**Headers**:
```
Content-Type: application/json
```

**Body** (raw JSON):
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123",
  "role": "EMPLOYEE"
}
```

**Expected Response** (201 Created):
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "EMPLOYEE",
  "message": "User registered successfully"
}
```

**What to verify**:
- ✅ Status code is 201
- ✅ Response contains userId (UUID format)
- ✅ Password is NOT in the response (security)
- ✅ Role is correctly set to EMPLOYEE

---

### 2. Register an AGENT

**Endpoint**: `POST /api/v1/auth/register`

**Body**:
```json
{
  "employeeCode": "AGT-001",
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@example.com",
  "password": "AgentPass456",
  "role": "AGENT"
}
```

**Expected Response** (201 Created):
```json
{
  "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "email": "jane.smith@example.com",
  "fullName": "Jane Smith",
  "role": "AGENT",
  "message": "User registered successfully"
}
```

---

### 3. Register an ADMIN

**Endpoint**: `POST /api/v1/auth/register`

**Body**:
```json
{
  "employeeCode": "ADM-001",
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com",
  "password": "AdminPass789",
  "role": "ADMIN"
}
```

**Expected Response** (201 Created):
```json
{
  "userId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "email": "admin@example.com",
  "fullName": "Admin User",
  "role": "ADMIN",
  "message": "User registered successfully"
}
```

---

### 4. Login with EMPLOYEE Account

**Endpoint**: `POST /api/v1/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

**Expected Response** (200 OK):
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "EMPLOYEE",
  "message": "Login successful"
}
```

**What to verify**:
- ✅ Status code is 200
- ✅ Correct user details returned
- ✅ Password is NOT in the response

---

### 5. Login with AGENT Account

**Endpoint**: `POST /api/v1/auth/login`

**Body**:
```json
{
  "email": "jane.smith@example.com",
  "password": "AgentPass456"
}
```

**Expected Response** (200 OK):
```json
{
  "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "email": "jane.smith@example.com",
  "fullName": "Jane Smith",
  "role": "AGENT",
  "message": "Login successful"
}
```

---

### 6. Login with ADMIN Account

**Endpoint**: `POST /api/v1/auth/login`

**Body**:
```json
{
  "email": "admin@example.com",
  "password": "AdminPass789"
}
```

**Expected Response** (200 OK):
```json
{
  "userId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "email": "admin@example.com",
  "fullName": "Admin User",
  "role": "ADMIN",
  "message": "Login successful"
}
```

---

## Error Cases to Test

### 7. Duplicate Email Registration

**Endpoint**: `POST /api/v1/auth/register`

**Body** (use already registered email):
```json
{
  "firstName": "Duplicate",
  "lastName": "User",
  "email": "john.doe@example.com",
  "password": "AnotherPass123",
  "role": "EMPLOYEE"
}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email is already registered: john.doe@example.com",
  "details": "uri=/api/v1/auth/register"
}
```

---

### 8. Invalid Email Format

**Endpoint**: `POST /api/v1/auth/register`

**Body**:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "not-an-email",
  "password": "ValidPass123",
  "role": "EMPLOYEE"
}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": "uri=/api/v1/auth/register",
  "errors": {
    "email": "Must be a valid email address"
  }
}
```

---

### 9. Password Too Short

**Endpoint**: `POST /api/v1/auth/register`

**Body**:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test@example.com",
  "password": "123",
  "role": "EMPLOYEE"
}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": "uri=/api/v1/auth/register",
  "errors": {
    "password": "Password must be at least 8 characters long"
  }
}
```

---

### 10. Missing Required Fields

**Endpoint**: `POST /api/v1/auth/register`

**Body**:
```json
{
  "email": "incomplete@example.com"
}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": "uri=/api/v1/auth/register",
  "errors": {
    "firstName": "First name is required",
    "lastName": "Last name is required",
    "password": "Password is required",
    "role": "Role is required"
  }
}
```

---

### 11. Wrong Password on Login

**Endpoint**: `POST /api/v1/auth/login`

**Body**:
```json
{
  "email": "john.doe@example.com",
  "password": "WrongPassword"
}
```

**Expected Response** (401 Unauthorized):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Bad credentials",
  "details": "uri=/api/v1/auth/login"
}
```

---

### 12. Non-Existent User Login

**Endpoint**: `POST /api/v1/auth/login`

**Body**:
```json
{
  "email": "nonexistent@example.com",
  "password": "SomePassword123"
}
```

**Expected Response** (401 Unauthorized):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Bad credentials",
  "details": "uri=/api/v1/auth/login"
}
```

---

### 13. Invalid Role Value

**Endpoint**: `POST /api/v1/auth/register`

**Body**:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test2@example.com",
  "password": "ValidPass123",
  "role": "SUPERUSER"
}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Malformed JSON request or invalid enum value: ...",
  "details": "uri=/api/v1/auth/register"
}
```

---

## Advanced Testing: Register with Team

### 14. Register User with Team Assignment

**Pre-requisite**: You need a valid team ID. Create one first or get from database.

**Body**:
```json
{
  "employeeCode": "EMP-002",
  "firstName": "Team",
  "lastName": "Member",
  "email": "team.member@example.com",
  "password": "TeamPass123",
  "role": "EMPLOYEE",
  "teamId": "put-valid-team-uuid-here"
}
```

**Expected Response** (201 Created):
```json
{
  "userId": "...",
  "email": "team.member@example.com",
  "fullName": "Team Member",
  "role": "EMPLOYEE",
  "message": "User registered successfully"
}
```

---

### 15. Register with Non-Existent Team

**Body**:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test3@example.com",
  "password": "ValidPass123",
  "role": "EMPLOYEE",
  "teamId": "00000000-0000-0000-0000-000000000000"
}
```

**Expected Response** (404 Not Found):
```json
{
  "timestamp": "2026-07-24T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Team not found with id: 00000000-0000-0000-0000-000000000000",
  "details": "uri=/api/v1/auth/register"
}
```

---

## Verification Checklist

### Security Checks
- [ ] Passwords are hashed in the database (never stored as plain text)
- [ ] Passwords are never returned in any API response
- [ ] Failed login returns 401 (not 404 — don't reveal if email exists)
- [ ] SQL injection attempts are blocked by JPA/Hibernate

### Database Checks

Run these queries in your PostgreSQL client to verify:

```sql
-- Check that password is BCrypt hashed (starts with $2a$ or $2b$)
SELECT email, password, role, status FROM users;

-- Verify roles are stored as strings matching the enum
SELECT role, COUNT(*) FROM users GROUP BY role;

-- Check the role constraint exists
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'chk_user_role';
```

### Expected Database Values
- `password` column should look like: `$2a$10$AbCdEfGhIjKlMnOpQrStUvWxYz...` (60 chars)
- `role` column should be: `EMPLOYEE`, `AGENT`, or `ADMIN` (exact case)
- `status` column should be: `ACTIVE` (default)

---

## Swagger UI Testing (Alternative)

Instead of Postman, you can also test via Swagger:

1. **Start the application**
2. **Open browser**: `http://localhost:8080/swagger-ui.html`
3. **Navigate to**: "Authentication" section
4. **Expand** `POST /api/v1/auth/register` and `POST /api/v1/auth/login`
5. **Click "Try it out"** and enter request bodies directly

---

## Next Steps

Once JWT is implemented, you will need to:
1. Copy the JWT token from login response
2. Add it to the `Authorization` header for protected endpoints:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

For now, all endpoints are open (`.anyRequest().permitAll()` in SecurityConfig).

---

## Troubleshooting

### Issue: 500 Internal Server Error on any endpoint
- **Check**: Application logs for stack trace
- **Check**: Database connection (application.yaml)
- **Check**: Flyway migrations ran successfully

### Issue: 401 on login with correct password
- **Check**: User exists in database
- **Check**: User status is 'ACTIVE' (not 'INACTIVE')
- **Check**: Email spelling is exact (case-sensitive in PostgreSQL)

### Issue: Cannot start application
- **Check**: Port 8080 is not already in use
- **Check**: PostgreSQL is running and accessible
- **Check**: Environment variables are set (DB_URL, DB_USERNAME, DB_PASSWORD)

---

## Environment Variables Required

Make sure these are set before starting the app:

```bash
DB_URL=jdbc:postgresql://localhost:5432/deskflow
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
AWS_S3_BUCKET=deskflow-attachments
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

Or create a `.env` file based on `.env.example`.

---

**Ready to test!** Start with test case #1 (Register EMPLOYEE) and work through the sequence.
