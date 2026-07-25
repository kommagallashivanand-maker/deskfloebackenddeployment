# DeskFlow ML Service

A FastAPI-based Machine Learning service for the DeskFlow platform.

## Overview

This service provides the ML inference layer for the DeskFlow platform, including health and version endpoints, a file-based model registry, structured logging, and request tracing using correlation IDs.

## Features

- FastAPI application
- Health check endpoint (`/health`)
- Version endpoint (`/version`) — shows active model versions
- File-based model registry (`registry/registry.json`)
- Model loader with in-memory caching
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
├── registry/
│   ├── __init__.py
│   ├── registry.json              # Model registry configuration
│   ├── registry.py                # Registry reader and resolver
│   └── loader.py                  # Model loader with caching
├── .env.example                   # Environment variables template
├── requirements.txt               # Project dependencies
└── README.md                      # This file
```

---

## Setup & Installation

### Prerequisites

- Python 3.10 or higher

### Installation

1. Navigate to the project directory:

```bash
cd ml/ml_service
```

2. Create and activate a virtual environment:

```bash
python -m venv .venv
.venv\Scripts\activate        # Windows
source .venv/bin/activate     # Linux/macOS
```

3. Install dependencies:

```bash
pip install -r requirements.txt
```

4. Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

## How to Run

Start from the `ml/ml_service/` directory so that the `registry/` package resolves correctly:

```bash
uvicorn app.main:app --reload
```

The service will be available at `http://127.0.0.1:8000`.

---

## Available Endpoints

### Health Check

```http
GET /health
```

```json
{ "status": "healthy" }
```

### Version

```http
GET /version
```

```json
{
  "service": "DeskFlow ML Service",
  "version": "1.0.0",
  "models": {
    "category": "embedding_v1",
    "priority": "priority_v1"
  }
}
```

---

## Model Registry

The registry lives in `registry/registry.json`. It maps model family names to versioned artifact paths. The service never duplicates model files — it references their existing locations.

### registry.json structure

```json
{
    "category": {
        "active": "embedding_v1",
        "versions": {
            "baseline_v1": {
                "artifact": "../../baseline_classifier/models/baseline_v1.pkl",
                "metadata": "../../baseline_classifier/models/baseline_v1_metadata.json",
                "type": "sklearn_pipeline"
            },
            "embedding_v1": {
                "artifact": "../../embedding_classifier/models/embedding_v1.pkl",
                "metadata": "../../embedding_classifier/models/embedding_v1_metadata.json",
                "type": "embedding_artefact"
            }
        }
    }
}
```

### Artifact types

| Type | Description |
|:---|:---|
| `sklearn_pipeline` | A joblib-serialised sklearn `Pipeline` object |
| `embedding_artefact` | A joblib-serialised dict with keys `classifier`, `embedding_model_name`, `classes` |

### How to switch the active model version

Edit `registry/registry.json` and change the `"active"` field for the model family. No code changes required. Restart the service to apply.

```json
"category": {
    "active": "baseline_v1",   ← change this
    ...
}
```

### How to register a new model version

Add a new entry under `"versions"` for the relevant family and point `"artifact"` to the existing `.pkl` or `.joblib` file:

```json
"embedding_v2": {
    "artifact": "../../embedding_classifier/models/embedding_v2.pkl",
    "metadata": "../../embedding_classifier/models/embedding_v2_metadata.json",
    "type": "embedding_artefact"
}
```

Then set `"active": "embedding_v2"` and restart the service.

### How to add a new model family

Add a new top-level key to `registry.json`:

```json
"sentiment": {
    "active": "sentiment_v1",
    "versions": {
        "sentiment_v1": {
            "artifact": "../../sentiment_classifier/models/sentiment_v1.pkl",
            "metadata": null,
            "type": "sklearn_pipeline"
        }
    }
}
```

No loader code changes needed — the registry and loader are family-agnostic.

### Loading models in code

```python
from registry.loader import load_model

model = load_model("category")          # loads active version
result = model.predict("Can't login", "Password reset link is broken")
# → {"category": "Account Access", "confidence": 0.87}

# Load a specific version explicitly
baseline = load_model("category", version="baseline_v1")
```

---

## Logging

Every request is logged with its Correlation ID, method, path, status, and duration.

```text
2026-07-23 00:00:00 | INFO | [abc-123] Incoming GET /version
2026-07-23 00:00:00 | INFO | [abc-123] Completed GET /version -> 200 (4.21 ms)
```

The `X-Correlation-ID` header is echoed back in every response.
