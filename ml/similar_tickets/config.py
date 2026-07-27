"""
Configuration for Similar Tickets.

Loads all configurable values from environment variables.
"""

from pathlib import Path
from dotenv import load_dotenv
import os

BASE_DIR = Path(__file__).resolve().parent

load_dotenv(BASE_DIR / ".env")

# ------------------------------------------------------------------
# PostgreSQL
# ------------------------------------------------------------------

DB_HOST = os.getenv("DB_HOST")
DB_PORT = int(os.getenv("DB_PORT", "5432"))
DB_NAME = os.getenv("DB_NAME")
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")

DATABASE_URL = (
    f"postgresql://{DB_USER}:{DB_PASSWORD}"
    f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)

# ------------------------------------------------------------------
# Dataset
# ------------------------------------------------------------------

INPUT_CSV_PATH = BASE_DIR.parent.parent / "data" / "tickets_v1.csv"

# ------------------------------------------------------------------
# Embedding Model
# ------------------------------------------------------------------

MODEL_NAME = os.getenv(
    "MODEL_NAME",
    "sentence-transformers/all-MiniLM-L6-v2",
)

DEVICE = os.getenv("DEVICE", "cpu")

BATCH_SIZE = int(os.getenv("BATCH_SIZE", "32"))

NORMALIZE_EMBEDDINGS = (
    os.getenv("NORMALIZE_EMBEDDINGS", "true").lower() == "true"
)