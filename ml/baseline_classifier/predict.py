import argparse
import sys
from pathlib import Path
import joblib
import pandas as pd

# Add current folder to sys.path to resolve local imports cleanly when run from anywhere
current_dir = Path(__file__).resolve().parent
if str(current_dir) not in sys.path:
    sys.path.append(str(current_dir))

import config
# Import utils to ensure custom FunctionTransformer combine_title_body function is registered in namespace
import utils

# Cache the loaded pipeline
_pipeline = None

def get_pipeline():
    """Load and cache the trained pipeline."""
    global _pipeline
    if _pipeline is None:
        model_path = Path(config.MODEL_DIR) / f"{config.MODEL_VERSION}.pkl"
        if not model_path.exists():
            raise FileNotFoundError(
                f"Model file not found at {model_path}. "
                "Please run train.py first to train the model."
            )
        _pipeline = joblib.load(model_path)
    return _pipeline

def predict(title: str, body: str) -> dict:
    """
    Predict the ticket category and return the predicted class and confidence.
    Exposes a prediction function that accepts (title, body) and returns the predicted category
    and confidence if available.
    
    Args:
        title (str): Ticket title
        body (str): Ticket body
        
    Returns:
        dict: A dictionary containing:
            - 'category': The predicted category name
            - 'confidence': The prediction confidence probability (if available, otherwise None)
    """
    pipeline = get_pipeline()
    
    # Create input representation (as expected by utils.combine_title_body)
    X = pd.DataFrame([{"title": title, "body": body}])
    
    # Predict category
    category = pipeline.predict(X)[0]
    
    # Predict probability / confidence if supported
    confidence = None
    if hasattr(pipeline, "predict_proba"):
        probs = pipeline.predict_proba(X)[0]
        # Retrieve the classifier class list to identify category index
        clf = pipeline.named_steps['clf']
        classes = clf.classes_.tolist()
        if category in classes:
            idx = classes.index(category)
            confidence = float(probs[idx])
            
    return {
        "category": category,
        "confidence": confidence
    }

def main():
    parser = argparse.ArgumentParser(description="Predict support ticket category using the baseline classifier.")
    parser.add_argument(
        "--title", 
        type=str, 
        default="Unable to reset password",
        help="The title of the ticket."
    )
    parser.add_argument(
        "--body", 
        type=str, 
        default="Password reset link expires immediately after clicking.",
        help="The body content of the ticket."
    )
    
    args = parser.parse_args()
    
    try:
        res = predict(args.title, args.body)
        print(f"Prediction: {res['category']}")
        if res['confidence'] is not None:
            print(f"Confidence: {res['confidence']:.0%}")
    except Exception as e:
        print(f"Error during prediction: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
