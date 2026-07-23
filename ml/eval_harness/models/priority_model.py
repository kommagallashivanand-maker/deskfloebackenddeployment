"""
Priority Model (DF-027)

Real inference using the trained RandomForest pipeline with feature extraction.
"""

import os
import re
import sys
from pathlib import Path
import joblib
import pandas as pd

# Resolve model path relative to this file
_current_dir = Path(__file__).resolve().parent.parent.parent
_model_path = _current_dir / "train_priority_model" / "models" / "priority_model_latest.joblib"

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

# Simple feature extraction for inference
# (Simplified version of DF-026 priority feature extraction logic)

def _extract_keywords_simple(subject: str, description: str, top: int = 3) -> str:
    """
    Simplified keyword extraction for inference.
    Extracts meaningful tokens from subject and description.
    """
    text = f"{subject or ''} {description or ''}".lower()
    
    # Remove punctuation and split
    text = re.sub(r'[^\w\s]', ' ', text)
    tokens = text.split()
    
    # Simple stopwords
    stopwords = {
        'the', 'and', 'for', 'with', 'from', 'that', 'this', 'a', 'an', 'of', 
        'in', 'on', 'to', 'is', 'are', 'was', 'be', 'been', 'have', 'has', 
        'had', 'do', 'does', 'did', 'will', 'would', 'could', 'should', 'i',
        'my', 'me', 'we', 'our', 'you', 'your', 'it', 'its'
    }
    
    # Filter meaningful tokens (length > 2 or known acronyms)
    meaningful = []
    for token in tokens:
        if len(token) > 2 and token not in stopwords:
            meaningful.append(token)
        elif len(token) <= 2 and token in {'ip', 'ad', 'vpn', 'api', 'sql', 'dns'}:
            meaningful.append(token)
    
    # Return top N unique tokens, comma-separated
    unique_keywords = []
    seen = set()
    for kw in meaningful:
        if kw not in seen:
            unique_keywords.append(kw)
            seen.add(kw)
            if len(unique_keywords) >= top:
                break
    
    return ", ".join(unique_keywords) if unique_keywords else "none"

def _analyze_sentiment_simple(text: str) -> tuple[float, str]:
    """
    Simplified sentiment analysis for inference.
    Looks for negative/urgent keywords and assigns a score.
    """
    if not text:
        return 0.0, "Neutral"
    
    lower = text.lower()
    
    # Negative signals
    negative_patterns = [
        r'\bcannot\b', r'\bcan\'?t\b', r'\bnot\s+\w+ing\b', r'\bnot\s+able\b',
        r'\bunable\b', r'\bfail(ed|ing|ure)?\b', r'\bdown\b', r'\bbroken\b',
        r'\berror\b', r'\bcrash(ed|ing)?\b', r'\bblocked?\b', r'\bno\s+access\b',
        r'\blost\s+access\b'
    ]
    
    # Urgency amplifiers
    urgency_patterns = [
        r'\burgent\b', r'\basap\b', r'\bimmediately\b', r'\bcritical\b',
        r'\bemergency\b', r'\bdeadline\b', r'\bmeeting\s+in\b', r'\bminutes?\b'
    ]
    
    negative_hits = sum(1 for pattern in negative_patterns if re.search(pattern, lower))
    urgency_hits = sum(1 for pattern in urgency_patterns if re.search(pattern, lower))
    
    # Calculate compound score
    if negative_hits == 0 and urgency_hits == 0:
        score = 0.0
    else:
        score = max(-1.0, -0.15 * negative_hits - 0.10 * urgency_hits)
    
    # Determine label
    if score <= -0.35:
        label = "Negative"
    elif score >= 0.35:
        label = "Positive"
    else:
        label = "Neutral"
    
    return score, label

def predict(
    subject: str,
    description: str,
    category: str = None,
    requester_role: str = None
) -> tuple[str, float]:
    """
    Predicts the priority of a ticket using extracted features.
    
    Args:
        subject: The ticket subject.
        description: The ticket description.
        category: The ticket category (used as feature).
        requester_role: The role of the requester (used as feature if provided).
        
    Returns:
        A tuple of (predicted_priority, confidence)
    """
    # Extract features
    keywords = _extract_keywords_simple(subject, description, top=3)
    sentiment_score, sentiment_label = _analyze_sentiment_simple(f"{subject} {description}")
    description_length = len(description or "")
    
    # Normalize category (fallback to "General Inquiry" if not provided)
    category_normalized = category if category else "General Inquiry"
    
    # Normalize requester_role (fallback to "unknown" if not provided)
    requester_role_normalized = requester_role if requester_role else "unknown"
    
    # Create feature DataFrame matching training format
    # The pipeline expects: keywords, category, sentiment_label, requester_role (optional), 
    # sentiment_score, description_length
    features = pd.DataFrame([{
        'keywords': keywords,
        'category': category_normalized,
        'sentiment_label': sentiment_label,
        'requester_role': requester_role_normalized,
        'sentiment_score': sentiment_score,
        'description_length': description_length
    }])
    
    # Get prediction
    predicted_label = _pipeline.predict(features)[0]
    
    # Get confidence (probability of the predicted class)
    probas = _pipeline.predict_proba(features)[0]
    predicted_class_idx = list(_pipeline.classes_).index(predicted_label)
    confidence = float(probas[predicted_class_idx])
    
    return predicted_label, confidence
