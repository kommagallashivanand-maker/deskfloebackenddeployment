import os
from pathlib import Path

# Base directories
BASE_DIR = Path(__file__).resolve().parent  # ml/embedding_classifier/

# Reproducibility — must match baseline to guarantee identical train/test split
RANDOM_STATE = 42
TEST_SIZE = 0.2

# Embedding model configuration
EMBEDDING_MODEL_NAME = "all-MiniLM-L6-v2"
EMBEDDING_BATCH_SIZE = 64

# Classifier configuration
CLASSIFIER_MAX_ITER = 1000
CLASSIFIER_SOLVER = "lbfgs"

# Model versioning
MODEL_VERSION = "embedding_v1"

# Directory names/paths
MODEL_DIR = BASE_DIR / "models"
REPORT_DIR = BASE_DIR / "reports"

# Dataset path (same dataset as baseline — DF-021)
DATASET_PATH = BASE_DIR.parent / "dataset_generator" / "datasets" / "tickets_v1.csv"
