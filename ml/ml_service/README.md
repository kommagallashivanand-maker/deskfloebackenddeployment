# DeskFlow ML Service

A FastAPI-based Machine Learning service scaffold for the DeskFlow platform.

## Overview

This service provides the foundational infrastructure for the DeskFlow ML Platform. It includes health and version endpoints, environment-based configuration, structured logging, and request tracing using correlation IDs.

## Features

- FastAPI application scaffold
- Health check endpoint (`/health`)
- Version endpoint (`/version`)
- Configuration via environment variables
- Structured application logging
- Request logging with Correlation ID middleware

## Folder Structure

```text
ml_service/
├── app/
│   ├── api/
│   │   └── health.py              # Health and version endpoints
│   ├── core/
│   │   ├── config.py              # Environment configuration
│   │   └── logging.py             # Logging configuration
│   ├── middleware/
│   │   └── correlation.py         # Correlation ID middleware
│   └── main.py                    # FastAPI application entry point
├── .env.example                   # Environment variables template
├── requirements.txt               # Project dependencies
└── README.md                      # Project documentation
```

## Setup & Installation

### Prerequisites

- Python 3.10 or higher

### Installation

1. Navigate to the project directory:

```bash
cd ml/ml_service
```

2. Create a virtual environment:

```bash
python -m venv .venv
```

3. Activate the virtual environment:

- **PowerShell (Windows)**

```powershell
.venv\Scripts\Activate.ps1
```

- **Command Prompt (Windows)**

```cmd
.venv\Scripts\activate.bat
```

- **Bash/zsh (Linux/macOS)**

```bash
source .venv/bin/activate
```

4. Install the required dependencies:

```bash
pip install -r requirements.txt
```

## Configuration

Create a `.env` file in the project root using `.env.example` as a template.

```ini
SERVICE_NAME=DeskFlow ML Service
SERVICE_VERSION=1.0.0

HOST=0.0.0.0
PORT=8000

LOG_LEVEL=INFO
```

## How to Run

Start the development server:

```bash
uvicorn app.main:app --reload
```

The service will be available at:

```text
http://127.0.0.1:8000
```

## Available Endpoints

### Health Check

```http
GET /health
```

Response

```json
{
  "status": "healthy"
}
```

### Version

```http
GET /version
```

Response

```json
{
  "service": "DeskFlow ML Service",
  "version": "1.0.0"
}
```

## Logging

The service uses structured logging to record application events and incoming requests.

For every request, the middleware logs:

- Correlation ID
- HTTP method
- Request path
- Response status code
- Request processing time

Example log output:

```text
2026-07-17 07:52:26 | INFO | [28c56e5c-3738-4619-af32-840c3b13d02b] Incoming GET /health
2026-07-17 07:52:26 | INFO | [28c56e5c-3738-4619-af32-840c3b13d02b] Completed GET /health -> 200 (6.53 ms)
```

The Correlation ID is also returned in the `X-Correlation-ID` response header to enable request tracing across services.