"""
Configuration settings for the Embedding Pipeline.

This file contains all configurable values used by the embedding
pipeline. Keeping them here avoids hardcoding values throughout
the project and makes future changes easier.
"""

from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
OUTPUT_DIR = BASE_DIR / "output"

INPUT_CSV_PATH = DATA_DIR / "tickets_v1.csv"
OUTPUT_EMBEDDINGS_FILE = OUTPUT_DIR / "embeddings.json"
OUTPUT_METADATA_FILE = OUTPUT_DIR / "metadata.json"

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
BATCH_SIZE = 32
DEVICE = "cpu"
NORMALIZE_EMBEDDINGS = True

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
INDEX_FILE = OUTPUT_DIR / "index.faiss"