"""
Centralized Path Resolution for DeskFlow ML Components

This module provides a single source of truth for resolving paths to the
repository root and commonly-used ML component directories/artifacts.

Why this exists:
--------------
Previously, each ML component independently computed the repo root using
fragile relative path traversal (e.g., Path(__file__).resolve().parent.parent.parent).
This approach:
  1. Breaks silently when files move
  2. Assumes a fixed directory depth
  3. Fails in Docker containers where the file layout may differ from local dev

Solution:
--------
This module centralizes path resolution with Docker-friendly environment variable
overrides. Components import from here instead of computing paths inline.

Environment Variable Override:
-----------------------------
Set DESKFLOW_REPO_ROOT to the absolute path where the repository is located.
This is especially useful in Docker containers:

    # In Dockerfile
    ENV DESKFLOW_REPO_ROOT=/app

If DESKFLOW_REPO_ROOT is not set, the module falls back to computing the repo
root relative to this file's location (assumes ml/common/paths.py is always
two levels below the repo root).

Usage:
------
    from ml.common.paths import get_repo_root, get_priority_model_path
    
    repo_root = get_repo_root()
    model_path = get_priority_model_path()

Docker Example:
--------------
    FROM python:3.10-slim
    
    # Copy repo into container at /app
    COPY . /app
    WORKDIR /app
    
    # Set environment variable so Python code finds paths correctly
    ENV DESKFLOW_REPO_ROOT=/app
    
    # Now all ml/ components will resolve paths correctly
    CMD ["python", "ml/eval_harness/eval.py"]
"""

import os
from pathlib import Path
from typing import Optional


def get_repo_root() -> Path:
    """
    Get the absolute path to the DeskFlow-backend repository root.
    
    Resolution order:
    1. Check DESKFLOW_REPO_ROOT environment variable (Docker/deployment override)
    2. Fall back to relative resolution from this file's location
    
    Returns:
        Absolute Path to repository root (DeskFlow-backend/)
        
    Raises:
        RuntimeError: If the computed path doesn't appear to be the repo root
    """
    # Check environment variable first
    env_root = os.getenv("DESKFLOW_REPO_ROOT")
    if env_root:
        repo_root = Path(env_root).resolve()
        if not repo_root.is_dir():
            raise RuntimeError(
                f"DESKFLOW_REPO_ROOT is set to '{env_root}' but that path doesn't exist or isn't a directory"
            )
        return repo_root
    
    # Fall back to relative resolution
    # This file is at: ml/common/paths.py
    # Repo root is: ml/common/paths.py -> parent (common) -> parent (ml) -> parent (repo root)
    this_file = Path(__file__).resolve()
    repo_root = this_file.parent.parent.parent
    
    # Sanity check: verify this looks like the repo root by checking for expected directories
    if not (repo_root / "ml").is_dir():
        raise RuntimeError(
            f"Computed repo root at '{repo_root}' doesn't contain expected 'ml/' directory. "
            f"If running in Docker or non-standard layout, set DESKFLOW_REPO_ROOT environment variable."
        )
    
    return repo_root


# Commonly-used paths as helper functions
# These make it easy to get specific artifacts without knowing internal structure

def get_baseline_classifier_dir() -> Path:
    """Get path to baseline_classifier component directory."""
    return get_repo_root() / "ml" / "baseline_classifier"


def get_baseline_classifier_model_path(model_name: str = "baseline_v1.pkl") -> Path:
    """Get path to baseline classifier model artifact."""
    return get_baseline_classifier_dir() / "models" / model_name


def get_embedding_classifier_dir() -> Path:
    """Get path to embedding_classifier component directory."""
    return get_repo_root() / "ml" / "embedding_classifier"


def get_embedding_classifier_model_path(model_name: str = "embedding_v1.pkl") -> Path:
    """Get path to embedding classifier model artifact."""
    return get_embedding_classifier_dir() / "models" / model_name


def get_priority_model_path(model_name: str = "priority_model_latest.joblib") -> Path:
    """Get path to priority model artifact."""
    return get_repo_root() / "ml" / "train_priority_model" / "models" / model_name


def get_priority_feature_extraction_dir() -> Path:
    """Get path to priority_feature_extraction component directory."""
    return get_repo_root() / "ml" / "priority_feature_extraction"


def get_training_data_path(filename: str = "tickets_extracted_features.csv") -> Path:
    """
    Get path to training data file at repository root.
    
    Args:
        filename: Name of the training data file (default: tickets_extracted_features.csv)
        
    Returns:
        Path to training data file
    """
    return get_repo_root() / filename
