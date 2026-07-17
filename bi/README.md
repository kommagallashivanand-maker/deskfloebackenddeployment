# DeskFlow BI Tooling

Metabase is deployed as the BI tool for DeskFlow using Docker Compose.

## Architecture

```text
DeskFlow Analytics Database
          │
          │ Read-only connection
          ▼
      Metabase
          │
          ▼
      Dashboards
```

Metabase uses a separate PostgreSQL database for storing its own metadata,
including users, dashboards, and configuration.

## Local Setup

### Prerequisites

- Docker Desktop
- Access to the DeskFlow repository

### Start the BI Tool

```
cd bi
docker compose up -d
```

Check the service status:

```
docker compose ps
```

Open Metabase at:

```
http://localhost:3000
```

### Stop the BI Tool

```
docker compose down
```

Metabase configuration is persisted using a Docker named volume, so users,
dashboards, and settings are preserved when the containers are recreated.

> Do not use `docker compose down -v` unless you intentionally want to delete
> the persisted Metabase configuration.

## Warehouse Access

Metabase should connect to the DeskFlow analytics database using a dedicated
read-only database user.

The BI user should have only the required read permissions:

- `CONNECT`
- `USAGE` on the required schema
- `SELECT` on required tables/views

No write or administrative permissions should be granted.

The actual warehouse credentials and connection details will be provided
through the approved team configuration/secrets process.

## Environment Configuration

Copy `.env.example` to `.env` for local configuration.

```
cp .env.example .env
```

The `.env` file is ignored by Git and must not be committed.

## Squad Access

1. Clone the repository.
2. Navigate to the `bi` directory.
3. Create `.env` from `.env.example`.
4. Run `docker compose up -d`.
5. Open `http://localhost:3000`.
6. Connect to the analytics database using the approved read-only credentials.