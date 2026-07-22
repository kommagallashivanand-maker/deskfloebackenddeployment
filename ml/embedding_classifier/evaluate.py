import json
import logging
from pathlib import Path
from typing import Any, Dict, List

import numpy as np
import pandas as pd
from sklearn.metrics import classification_report, confusion_matrix

logger = logging.getLogger("embedding_classifier")


def evaluate_model(
    clf,
    X_test_embeddings: np.ndarray,
    y_test: pd.Series,
    classes: List[str],
    report_dir,
    model_version: str,
    X_train_len: int,
) -> Dict[str, Any]:
    """
    Evaluate the trained classifier on the test set and persist all reports.

    Saves the following artefacts under `report_dir`:
        - classification_report.txt
        - metrics.json
        - confusion_matrix.csv
        - training_summary.md

    Args:
        clf:                  Fitted sklearn classifier (operates on embeddings).
        X_test_embeddings:    numpy array of shape (n_test, embedding_dim).
        y_test:               Ground-truth labels for the test set.
        classes:              Sorted list of class names.
        report_dir:           Directory where reports will be written.
        model_version:        Version string used in report headers.
        X_train_len:          Number of training samples (for summary stats).

    Returns:
        Dictionary with scalar metrics: accuracy, macro_f1, weighted_f1 and
        per-class breakdown under 'metrics_by_class'.
    """
    logger.info("Evaluation started")

    report_path = Path(report_dir)
    report_path.mkdir(parents=True, exist_ok=True)

    # Predict on test set
    y_pred = clf.predict(X_test_embeddings)

    # ------------------------------------------------------------------
    # 1. Classification report (text)
    # ------------------------------------------------------------------
    report_str = classification_report(y_test, y_pred, target_names=classes)
    report_txt_file = report_path / "classification_report.txt"
    with open(report_txt_file, "w", encoding="utf-8") as f:
        f.write(report_str)
    logger.info(f"Saved classification report to {report_txt_file}")

    # ------------------------------------------------------------------
    # 2. Metrics JSON
    # ------------------------------------------------------------------
    report_dict = classification_report(
        y_test, y_pred, target_names=classes, output_dict=True
    )

    accuracy: float = report_dict.get("accuracy")
    macro_f1: float = report_dict.get("macro avg", {}).get("f1-score")
    weighted_f1: float = report_dict.get("weighted avg", {}).get("f1-score")

    metrics_data: Dict[str, Any] = {
        "model_version": model_version,
        "accuracy": accuracy,
        "macro_f1": macro_f1,
        "weighted_f1": weighted_f1,
        "metrics_by_class": {
            cls: {
                "precision": report_dict[cls]["precision"],
                "recall": report_dict[cls]["recall"],
                "f1-score": report_dict[cls]["f1-score"],
                "support": report_dict[cls]["support"],
            }
            for cls in classes
            if cls in report_dict
        },
    }

    metrics_json_file = report_path / "metrics.json"
    with open(metrics_json_file, "w", encoding="utf-8") as f:
        json.dump(metrics_data, f, indent=4)
    logger.info(f"Saved metrics JSON to {metrics_json_file}")

    # ------------------------------------------------------------------
    # 3. Confusion matrix CSV
    # ------------------------------------------------------------------
    cm = confusion_matrix(y_test, y_pred, labels=classes)
    cm_df = pd.DataFrame(cm, index=classes, columns=classes)
    cm_csv_file = report_path / "confusion_matrix.csv"
    cm_df.to_csv(cm_csv_file)
    logger.info(f"Saved confusion matrix to {cm_csv_file}")

    # ------------------------------------------------------------------
    # 4. Training summary markdown
    # ------------------------------------------------------------------
    total_samples = X_train_len + len(y_test)
    summary_md_file = report_path / "training_summary.md"
    summary_content = (
        f"# Training Summary - {model_version}\n\n"
        f"- **Dataset**: {total_samples} samples, {len(classes)} classes\n"
        f"- **Train**: {X_train_len}\n"
        f"- **Test**: {len(y_test)}\n"
        f"- **Macro F1**: {macro_f1:.4f}\n"
        f"- **Accuracy**: {accuracy:.2%}\n"
        f"- **Model**: SentenceTransformer Embeddings + Logistic Regression\n"
        f"- **Version**: {model_version}\n"
    )
    with open(summary_md_file, "w", encoding="utf-8") as f:
        f.write(summary_content)
    logger.info(f"Saved training summary to {summary_md_file}")

    logger.info(
        f"Evaluation completed — Accuracy: {accuracy:.4f}  Macro-F1: {macro_f1:.4f}"
    )
    return metrics_data
