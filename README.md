# DeskFlow: Internal Helpdesk

DeskFlow is an internal helpdesk and ticketing platform designed to streamline support operations. The platform triages, routes, and assists agents to resolve tickets against SLAs in record time.

---

## Database Entity-Relationship Diagram (ERD)

Below is the entity-relationship model representing the database schema. It consists of eight core tables: `teams`, `categories`, `users`, `tickets`, `ticket_attachments`, `ticket_activities`, `ticket_comments`, and `sla_policies`.

```mermaid
erDiagram
    TEAMS ||--o{ USERS : "has members"
    CATEGORIES ||--o{ TICKETS : "classifies"
    USERS ||--o{ TICKETS : "creates (created_by)"
    USERS ||--o{ TICKETS : "handles (assigned_to)"
    TICKETS ||--o{ TICKET_ATTACHMENTS : "has attachments"
    TICKETS ||--o{ TICKET_ACTIVITIES : "logs history"
    TICKETS ||--o{ TICKET_COMMENTS : "has conversation"
    USERS ||--o{ TICKET_ACTIVITIES : "performs action (actor_id)"
    USERS ||--o{ TICKET_COMMENTS : "posts (user_id)"
    TICKET_COMMENTS ||--o{ TICKET_COMMENTS : "replies to (parent_comment_id)"

    TEAMS {
        uuid id PK
        varchar name UK
        text description
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    SLA_POLICIES {
        uuid id PK
        varchar priority UK
        int response_time_hours
        int resolution_time_hours
        timestamp created_at
        timestamp updated_at
    }

    CATEGORIES {
        uuid id PK
        varchar name UK
        text description
        timestamp created_at
        timestamp updated_at
    }

    USERS {
        uuid id PK
        varchar employee_code UK
        varchar first_name
        varchar last_name
        varchar email UK
        varchar password
        varchar role
        varchar status
        uuid team_id FK
        timestamp created_at
        timestamp updated_at
    }

    TICKETS {
        uuid id PK
        varchar ticket_number UK
        varchar title
        text description
        varchar priority
        varchar status
        uuid created_by FK
        uuid assigned_to FK
        uuid category_id FK
        timestamp first_responded_at
        int reopen_count
        timestamp created_at
        timestamp updated_at
        timestamp resolved_at
        timestamp closed_at
    }

    TICKET_ATTACHMENTS {
        uuid id PK
        uuid ticket_id FK
        varchar file_name
        varchar file_url
        varchar file_type
        bigint file_size
        timestamp created_at
    }

    TICKET_ACTIVITIES {
        uuid id PK
        uuid ticket_id FK
        uuid actor_id FK
        varchar activity_type
        varchar old_value
        varchar new_value
        timestamp created_at
    }

    TICKET_COMMENTS {
        uuid id PK
        uuid ticket_id FK
        uuid user_id FK
        uuid parent_comment_id FK
        text content
        timestamp created_at
        timestamp updated_at
    }
```

### Table Details
1. **Teams (`teams`)**: Organizes users into specific departments (e.g., Support, Engineering).
2. **SLA Policies (`sla_policies`)**: Configures response and resolution target times per priority level.
3. **Categories (`categories`)**: Categorizes tickets to enable correct routing (e.g., Network, Hardware).
4. **Users (`users`)**: Represents all registered employees, agents, and admins.
5. **Tickets (`tickets`)**: Tracks support issues, priority, state machine transitions, assignees, and resolution timestamps.
6. **Ticket Attachments (`ticket_attachments`)**: Stores AWS S3 file attachment metadata linked to tickets.
7. **Ticket Activities (`ticket_activities`)**: Stores state transition audit logs and historical changes.
8. **Ticket Comments (`ticket_comments`)**: Stores threaded conversation comments, replies, and user mentions.

---

## Getting Started

### Prerequisites
* **Java 21**
* **PostgreSQL** running locally on port `5432` with a database named `deskflow` (username: `postgres`, password: `postgres`).

### Running Locally
To run the backend service and execute Flyway database migrations:

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```

2. Run the application using the Maven Wrapper:
   ```bash
   # On Windows
   .\mvnw.cmd spring-boot:run

   # On Linux/macOS
   ./mvnw spring-boot:run
   ```

---

## API Documentation & Endpoint Reference

### Ticket Core & Attachments APIs (`/api/v1/tickets`)

| Method | Endpoint | Description | Query Parameters / Content-Type |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/tickets` | Create a new ticket with optional file attachments (AWS S3) | `multipart/form-data` (`ticket` JSON string + optional `files` array) |
| **GET** | `/api/v1/tickets/{id}` | Get detailed information of a ticket by UUID (returns 60-min pre-signed S3 URLs) | `id` (Path variable) |
| **PUT** | `/api/v1/tickets/{id}` | Update an existing ticket (enforces state machine matrix & `reopenCount`) | `id` (Path variable), JSON Body |
| **GET** | `/api/v1/tickets` | List tickets with pagination, timeline sorting, multi-field search, and filters | `page`, `size`, `search`, `status`, `priority`, `category`, `assignee`, `sortBy`, `sortDir` |

---

### Ticket Comments & Audit Activity APIs (`/api/v1/tickets`)

| Method | Endpoint | Description | Query Parameters / Content-Type |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/tickets/{ticketId}/comments` | Post a top-level comment or threaded reply (parses `@user` mentions) | `ticketId` (Path variable), `application/json` |
| **GET** | `/api/v1/tickets/{ticketId}/comments` | Retrieve threaded comments and nested replies for a ticket | `ticketId` (Path variable) |
| **GET** | `/api/v1/tickets/{ticketId}/activities` | Retrieve chronological state transition and audit activity timeline | `ticketId` (Path variable) |

---

### Interactive Swagger UI

The project features fully integrated OpenAPI 3/Swagger documentation conforming to enterprise developer standards. When the application is running locally, you can access:

* **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) — used to view, explore, and trigger REST API endpoints directly from your browser.
* **Raw OpenAPI JSON Specs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) — raw OpenAPI 3.0 specification definition.
