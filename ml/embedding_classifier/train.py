import datetime
import json
import logging
import sys
from pathlib import Path

import joblib
import sklearn
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split

# Add current folder to sys.path to resolve local imports cleanly when run from anywhere
current_dir = Path(__file__).resolve().parent
if str(current_dir) not in sys.path:
    sys.path.append(str(current_dir))

import config
from utils import setup_logging, combine_title_body, load_and_validate_dataset, encode_texts
from evaluate import evaluate_model


def train_model(cfg=None):
    """
    Train the embedding-based ticket classifier and persist all artefacts.

    Steps:
        1. Load and validate the dataset.
        2. Perform the same stratified 80/20 split as the baseline (identical seed).
        3. Encode title+body text with SentenceTransformers.
        4. Fit a Logistic Regression classifier on the training embeddings.
        5. Evaluate on the test set and save reports.
        6. Serialise the classifier and SentenceTransformer model name.
        7. Write metadata JSON.

    Args:
        cfg: Configuration module. Defaults to the local config module.

    Returns:
        Tuple[dict, dict]: (model_artefact_dict, metadata_dict)
            model_artefact_dict contains keys 'classifier' and 'embedding_model_name'.
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

    # ------------------------------------------------------------------
    # 1. Load and validate dataset
    # ------------------------------------------------------------------
    try:
        df = load_and_validate_dataset(cfg.DATASET_PATH)
    except Exception as e:
        logger.error(f"Failed to load dataset: {str(e)}")
        raise

    # ------------------------------------------------------------------
    # 2. Extract features and target
    # ------------------------------------------------------------------
    X = df[["title", "body"]]
    y = df["category"]

    # Deterministic class list
    classes = sorted(y.unique().tolist())

    # ------------------------------------------------------------------
    # 3. Train/Test Split — identical parameters to baseline (DF-022)
    # ------------------------------------------------------------------
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=cfg.TEST_SIZE,
        random_state=cfg.RANDOM_STATE,
        stratify=y,
        shuffle=True,
    )
    logger.info(
        f"Split — train: {len(X_train)} samples, test: {len(X_test)} samples"
    )

    # ------------------------------------------------------------------
    # 4. Build combined text series for both splits
    # ------------------------------------------------------------------
    train_texts = combine_title_body(X_train)
    test_texts = combine_title_body(X_test)

    # ------------------------------------------------------------------
    # 5. Encode with SentenceTransformers
    # ------------------------------------------------------------------
    train_embeddings = encode_texts(
        train_texts,
        model_name=cfg.EMBEDDING_MODEL_NAME,
        batch_size=cfg.EMBEDDING_BATCH_SIZE,
    )
    test_embeddings = encode_texts(
        test_texts,
        model_name=cfg.EMBEDDING_MODEL_NAME,
        batch_size=cfg.EMBEDDING_BATCH_SIZE,
    )

    # ------------------------------------------------------------------
    # 6. Train Logistic Regression on embeddings
    # ------------------------------------------------------------------
    logger.info("Fitting Logistic Regression classifier on embeddings")
    clf = LogisticRegression(
        max_iter=cfg.CLASSIFIER_MAX_ITER,
        random_state=cfg.RANDOM_STATE,
        solver=cfg.CLASSIFIER_SOLVER,
    )
    clf.fit(train_embeddings, y_train)
    logger.info("Training completed")

    # ------------------------------------------------------------------
    # 7. Evaluate and save reports
    # ------------------------------------------------------------------
    metrics_data = evaluate_model(
        clf=clf,
        X_test_embeddings=test_embeddings,
        y_test=y_test,
        classes=classes,
        report_dir=report_dir,
        model_version=cfg.MODEL_VERSION,
        X_train_len=len(X_train),
    )

    # ------------------------------------------------------------------
    # 8. Serialise model artefact
    #    We persist the classifier and the embedding model name together so
    #    predict.py can reconstruct the full inference pipeline.
    # ------------------------------------------------------------------
    model_artefact = {
        "classifier": clf,
        "embedding_model_name": cfg.EMBEDDING_MODEL_NAME,
        "classes": classes,
    }

    model_file_name = f"{cfg.MODEL_VERSION}.pkl"
    model_save_path = model_dir / model_file_name
    logger.info(f"Saving model to {model_save_path}")
    try:
        joblib.dump(model_artefact, model_save_path)
    except Exception as e:
        logger.error(f"Model save failure: {str(e)}")
        raise RuntimeError(f"Model save failure: {str(e)}") from e

    # ------------------------------------------------------------------
    # 9. Save metadata JSON
    # ------------------------------------------------------------------
    metadata = {
        "model_version": cfg.MODEL_VERSION,
        "training_date": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "dataset_version": Path(cfg.DATASET_PATH).stem,
        "random_seed": cfg.RANDOM_STATE,
        "train_size": len(X_train),
        "test_size": len(X_test),
        "classes": classes,
        "embedding_model": cfg.EMBEDDING_MODEL_NAME,
        "classifier": "LogisticRegression",
        "accuracy": metrics_data["accuracy"],
        "macro_f1": metrics_data["macro_f1"],
        "weighted_f1": metrics_data["weighted_f1"],
        "sklearn_version": sklearn.__version__,
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
    return model_artefact, metadata


if __name__ == "__main__":
    train_model()
