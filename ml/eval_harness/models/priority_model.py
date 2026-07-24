"""
Priority Model (DF-027)

Real inference using the trained RandomForest pipeline with REAL feature extraction
from ml/priority_feature_extraction (DF-026).
"""

import os
import sys
from pathlib import Path
import joblib
import pandas as pd

# Resolve paths relative to this file
_current_dir = Path(__file__).resolve().parent.parent.parent
_model_path = _current_dir / "train_priority_model" / "models" / "priority_model_latest.joblib"
_feature_extraction_dir = _current_dir / "priority_feature_extraction"

# Add priority_feature_extraction to path so we can import its modules
if str(_feature_extraction_dir) not in sys.path:
    sys.path.insert(0, str(_feature_extraction_dir))

# Import REAL feature extraction pipeline from DF-026
try:
    from app.models.ticket import TicketIn, AuthContext
    from app.feature_extraction.feature_extractor import extract_features
except ImportError as e:
    raise ImportError(
        f"Failed to import priority feature extraction modules from {_feature_extraction_dir}. "
        f"Ensure ml/priority_feature_extraction is set up correctly. Error: {e}"
    ) from e

# Load model once at module level
if not _model_path.exists():
    raise FileNotFoundError(
        f"Priority model not found at {_model_path}. "
        f"Train the model first by running ml/train_priority_model/train.py"
    )

try:
    _pipeline = joblib.load(_model_path)
except Exception as e:
    raise RuntimeError(
        f"Failed to load priority model from {_model_path}: {e}"
    ) from e

def predict(
    subject: str,
    description: str,
    category: str = None,
    requester_role: str = None
) -> tuple[str, float]:
    """
    Predicts the priority of a ticket using REAL feature extraction from DF-026.
    
    This uses the exact same feature extraction pipeline (YAKE/spaCy keywords,
    VADER sentiment) that was used to generate the training data, ensuring
    consistent feature distributions between training and inference.
    
    Args:
        subject: The ticket subject.
        description: The ticket description.
        category: The ticket category (used as feature).
        requester_role: The role of the requester (used as feature if provided).
        
    Returns:
        A tuple of (predicted_priority, confidence)
    """
    # Create TicketIn and AuthContext objects for the REAL feature extractor
    ticket = TicketIn(
        subject=subject or "",
        description=description or "",
        category=category
    )
    auth = AuthContext(
        user_id="eval_harness",
        role=requester_role
    )
    
    # Extract features using REAL DF-026 pipeline
    feature_output = extract_features(ticket, auth, top_k=3)
    
    # Create feature DataFrame matching training format
    features = pd.DataFrame([{
        'keywords': feature_output.keywords,
        'category': feature_output.category,
        'sentiment_label': feature_output.sentiment.label,
        'requester_role': feature_output.requester_role,
        'sentiment_score': feature_output.sentiment.score,
        'description_length': feature_output.description_length
    }])
    
    # Get prediction
    predicted_label = _pipeline.predict(features)[0]
    
    # Get confidence (probability of the predicted class)
    probas = _pipeline.predict_proba(features)[0]
    predicted_class_idx = list(_pipeline.classes_).index(predicted_label)
    confidence = float(probas[predicted_class_idx])
    
    return predicted_label, confidence
