"""
Category Baseline Classifier (DF-022)

Real inference using the trained TF-IDF + Logistic Regression pipeline.
"""

import os
import sys
from pathlib import Path
import joblib
import pandas as pd

# Resolve paths relative to this file
_current_dir = Path(__file__).resolve().parent.parent.parent
_model_path = _current_dir / "baseline_classifier" / "models" / "baseline_v1.pkl"
_baseline_dir = _current_dir / "baseline_classifier"

# Add baseline_classifier to path so pickle can find its utils module
if str(_baseline_dir) not in sys.path:
    sys.path.insert(0, str(_baseline_dir))

# Load model once at module level
if not _model_path.exists():
    raise FileNotFoundError(
        f"Baseline classifier model not found at {_model_path}. "
        f"Train the model first by running ml/baseline_classifier/train.py"
    )

try:
    _pipeline = joblib.load(_model_path)
except Exception as e:
    raise RuntimeError(
        f"Failed to load baseline classifier model from {_model_path}: {e}"
    ) from e

def predict(
    subject: str,
    description: str,
    category: str = None,
    requester_role: str = None
) -> tuple[str, float]:
    """
    Predicts the category of a ticket using the baseline TF-IDF + LogisticRegression model.
    
    Args:
        subject: The ticket subject.
        description: The ticket description.
        category: Ignored (not needed for category prediction).
        requester_role: Ignored (not needed for category baseline).
        
    Returns:
        A tuple of (predicted_category, confidence)
    """
    # Create input DataFrame with title and body columns as expected by the pipeline
    X_input = pd.DataFrame([{
        'title': subject or "",
        'body': description or ""
    }])
    
    # Get prediction
    predicted_label = _pipeline.predict(X_input)[0]
    
    # Get confidence (probability of the predicted class)
    probas = _pipeline.predict_proba(X_input)[0]
    predicted_class_idx = list(_pipeline.classes_).index(predicted_label)
    confidence = float(probas[predicted_class_idx])
    
    return predicted_label, confidence
