import datetime
import json
import logging
import sys
from pathlib import Path
import sklearn
import joblib
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import FunctionTransformer

# Add current folder to sys.path to resolve local imports cleanly when run from anywhere
current_dir = Path(__file__).resolve().parent
if str(current_dir) not in sys.path:
    sys.path.append(str(current_dir))

import config
from utils import setup_logging, combine_title_body, load_and_validate_dataset
from evaluate import evaluate_pipeline

def train_model(cfg=None):
    """
    Exposes a training entry point that accepts configuration and produces a versioned model artifact.
    If cfg is None, defaults to the global config module.
    """
    if cfg is None:
        cfg = config

    logger = setup_logging()
    logger.info("Training started")

    # Ensure output directories exist
    model_dir = Path(cfg.MODEL_DIR)
    model_dir.mkdir(parents=True, exist_ok=True)
    report_dir = Path(cfg.REPORT_DIR)
    report_dir.mkdir(parents=True, exist_ok=True)

    # 1. Load and validate dataset
    try:
        df = load_and_validate_dataset(cfg.DATASET_PATH)
    except Exception as e:
        logger.error(f"Failed to load dataset: {str(e)}")
        raise e

    # 2. Extract features and target
    X = df[['title', 'body']]
    y = df['category']

    # Get class list sorted to be deterministic
    classes = sorted(y.unique().tolist())

    # 3. Train/Test Split (stratified and reproducible)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=cfg.TEST_SIZE,
        random_state=cfg.RANDOM_STATE,
        stratify=y,
        shuffle=True
    )

    # 4. Define scikit-learn Pipeline
    pipeline = Pipeline([
        ('text_combiner', FunctionTransformer(combine_title_body, validate=False)),
        ('tfidf', TfidfVectorizer(
            lowercase=True,
            stop_words='english',
            ngram_range=cfg.NGRAM_RANGE,
            max_features=cfg.MAX_FEATURES,
            sublinear_tf=True
        )),
        ('clf', LogisticRegression(
            max_iter=1000,
            random_state=cfg.RANDOM_STATE,
            solver='lbfgs'
        ))
    ])

    # 5. Fit Pipeline
    pipeline.fit(X_train, y_train)
    logger.info("Training completed")

    # 6. Evaluate and save reports
    metrics_data = evaluate_pipeline(
        pipeline, X_train, X_test, y_train, y_test,
        classes=classes,
        report_dir=report_dir,
        model_version=cfg.MODEL_VERSION
    )

    # 7. Model Serialization
    model_file_name = f"{cfg.MODEL_VERSION}.pkl"
    model_save_path = model_dir / model_file_name
    
    logger.info(f"Saving model to {model_save_path}")
    try:
        joblib.dump(pipeline, model_save_path)
    except Exception as e:
        logger.error(f"Model save failure: {str(e)}")
        raise RuntimeError(f"Model save failure: {str(e)}") from e

    # 8. Save Metadata
    metadata = {
        "model_version": cfg.MODEL_VERSION,
        "training_date": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "dataset_version": Path(cfg.DATASET_PATH).stem,
        "random_seed": cfg.RANDOM_STATE,
        "train_size": len(X_train),
        "test_size": len(X_test),
        "classes": classes,
        "accuracy": metrics_data["accuracy"],
        "macro_f1": metrics_data["macro_f1"],
        "weighted_f1": metrics_data["weighted_f1"],
        "sklearn_version": sklearn.__version__
    }

    metadata_file_name = f"{cfg.MODEL_VERSION}_metadata.json"
    metadata_save_path = model_dir / metadata_file_name
    logger.info(f"Saving metadata to {metadata_save_path}")
    try:
        with open(metadata_save_path, "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=4)
    except Exception as e:
        logger.error(f"Metadata save failure: {str(e)}")
        raise RuntimeError(f"Metadata save failure: {str(e)}") from e

    logger.info("Done")
    return pipeline, metadata

if __name__ == "__main__":
    train_model()
