"""
Category Embeddings Classifier (DF-023)

Real inference using sentence embeddings + Logistic Regression.
"""

import sys
from pathlib import Path

# Add repo root to path FIRST, before any ml.common imports
_repo_root = Path(__file__).resolve().parent.parent.parent.parent
if str(_repo_root) not in sys.path:
    sys.path.insert(0, str(_repo_root))

import joblib
import pandas as pd
from sentence_transformers import SentenceTransformer

# Use centralized path resolution (Docker-safe via DESKFLOW_REPO_ROOT env var)
from ml.common.paths import (
    get_embedding_classifier_model_path,
    get_embedding_classifier_dir,
)

# Resolve paths using centralized module
_model_path = get_embedding_classifier_model_path()
_embedding_dir = get_embedding_classifier_dir()

# Add embedding_classifier to path so pickle can find its utils module
if str(_embedding_dir) not in sys.path:
    sys.path.insert(0, str(_embedding_dir))

# Load model artifact once at module level
if not _model_path.exists():
    raise FileNotFoundError(
        f"Embedding classifier model not found at {_model_path}. "
        f"Train the model first by running ml/embedding_classifier/train.py"
    )

try:
    _model_artifact = joblib.load(_model_path)
    _classifier = _model_artifact["classifier"]
    _embedding_model_name = _model_artifact["embedding_model_name"]
    _classes = _model_artifact.get("classes", None)
except Exception as e:
    raise RuntimeError(
        f"Failed to load embedding classifier model from {_model_path}: {e}"
    ) from e

# Load SentenceTransformer encoder
try:
    _encoder = SentenceTransformer(_embedding_model_name)
except Exception as e:
    raise RuntimeError(
        f"Failed to load SentenceTransformer model '{_embedding_model_name}': {e}"
    ) from e

def predict(
    subject: str,
    description: str,
    category: str = None,
    requester_role: str = None
) -> tuple[str, float]:
    """
    Predicts the category of a ticket using sentence embeddings.
    
    Args:
        subject: The ticket subject.
        description: The ticket description.
        category: Ignored (not needed for category prediction).
        requester_role: Ignored (not needed for category prediction).
        
    Returns:
        A tuple of (predicted_category, confidence)
    """
    # Combine title and body as done in training
    combined_text = f"{subject or ''}\n\n{description or ''}".strip()
    
    # Encode the text
    embedding = _encoder.encode([combined_text], convert_to_numpy=True)
    
    # Get prediction
    predicted_label = _classifier.predict(embedding)[0]
    
    # Get confidence (probability of the predicted class)
    probas = _classifier.predict_proba(embedding)[0]
    predicted_class_idx = list(_classifier.classes_).index(predicted_label)
    confidence = float(probas[predicted_class_idx])
    
    return predicted_label, confidence
