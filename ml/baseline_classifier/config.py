import os
from pathlib import Path

# Base directories
BASE_DIR = Path(__file__).resolve().parent  # ml/baseline_classifier/

# Reproducibility
RANDOM_STATE = 42
TEST_SIZE = 0.2

# TF-IDF Feature configuration
MAX_FEATURES = 10000
NGRAM_RANGE = (1, 2)

# Model configuration
MODEL_VERSION = "baseline_v1"

# Directory names/paths
MODEL_DIR = BASE_DIR / "models"
REPORT_DIR = BASE_DIR / "reports"

# Dataset path (read from dataset_generator)
DATASET_PATH = BASE_DIR.parent / "dataset_generator" / "datasets" / "tickets_v1.csv"
