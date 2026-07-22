# Priority Feature Extraction

FastAPI service to extract keywords, category, requester role, and sentiment from ticket data.

## Setup and Installation

This project utilizes `spaCy` and its `en_core_web_sm` model for language parsing and keyword extraction. 

### Windows (Automated Rebuild)
To rebuild the virtual environment and install all dependencies cleanly, double-click or run:
```bash
setup.bat
```

### Manual Setup (All Platforms)
To manually initialize a clean virtual environment and install dependencies:
```bash
# 1. Create a virtual environment
python -m venv .venv

# 2. Activate the environment
# On Windows:
.venv\Scripts\activate
# On macOS/Linux:
source .venv/bin/activate

# 3. Install packages and models
pip install -r requirements.txt
```

## Running Locally

```bash
uvicorn app.main:app --reload --port 8000
```

## Running Tests

```bash
pytest tests/ -v
```

