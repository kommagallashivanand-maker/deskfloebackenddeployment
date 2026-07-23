import argparse
import sys
from pathlib import Path
from typing import Optional

import joblib
import numpy as np
import pandas as pd

# Add current folder to sys.path to resolve local imports cleanly when run from anywhere
current_dir = Path(__file__).resolve().parent
if str(current_dir) not in sys.path:
    sys.path.append(str(current_dir))

import config

# Module-level cache for the loaded artefact
_artefact = None
_sentence_model = None


def _get_artefact() -> dict:
    """Load and cache the model artefact dict from disk."""
    global _artefact
    if _artefact is None:
        model_path = Path(config.MODEL_DIR) / f"{config.MODEL_VERSION}.pkl"
        if not model_path.exists():
            raise FileNotFoundError(
                f"Model file not found at {model_path}. "
                "Please run train.py first to train the model."
            )
        _artefact = joblib.load(model_path)
    return _artefact


def _get_sentence_model(model_name: str):
    """Load and cache the SentenceTransformer model."""
    global _sentence_model
    if _sentence_model is None:
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise ImportError(
                "sentence-transformers is required. "
                "Install it with: pip install sentence-transformers"
            ) from exc
        _sentence_model = SentenceTransformer(model_name)
    return _sentence_model


def predict(title: str, body: str) -> dict:
    """
    Predict the ticket category and return the predicted class and confidence.

    Mirrors the same interface as the baseline classifier's predict() function
    so it can be used as a drop-in replacement in downstream services.

    Args:
        title: Ticket title text.
        body:  Ticket body text.

    Returns:
        dict with keys:
            - 'category'   (str):   Predicted category label.
            - 'confidence' (float): Probability of the predicted class, or None
                                    if the classifier does not support predict_proba.
    """
    artefact = _get_artefact()
    clf = artefact["classifier"]
    embedding_model_name: str = artefact["embedding_model_name"]
    classes: list = artefact["classes"]

    # Build combined text identical to training time
    combined_text: str = (
        str(title).strip() + " " + str(body).strip()
    )

    # Encode to embedding vector
    sentence_model = _get_sentence_model(embedding_model_name)
    embedding: np.ndarray = sentence_model.encode(
        [combined_text], convert_to_numpy=True
    )

    # Predict category
    category: str = clf.predict(embedding)[0]

    # Predict probability / confidence if supported
    confidence: Optional[float] = None
    if hasattr(clf, "predict_proba"):
        probs = clf.predict_proba(embedding)[0]
        if category in classes:
            idx = list(clf.classes_).index(category)
            confidence = float(probs[idx])

    return {
        "category": category,
        "confidence": confidence,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Predict support ticket category using the embedding classifier."
    )
    parser.add_argument(
        "--title",
        type=str,
        default="Unable to reset password",
        help="The title of the support ticket.",
    )
    parser.add_argument(
        "--body",
        type=str,
        default="Password reset link expires immediately after clicking.",
        help="The body content of the support ticket.",
    )

    args = parser.parse_args()

    try:
        res = predict(args.title, args.body)
        print(f"Prediction: {res['category']}")
        if res["confidence"] is not None:
            print(f"Confidence: {res['confidence']:.0%}")
    except Exception as e:
        print(f"Error during prediction: {str(e)}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
