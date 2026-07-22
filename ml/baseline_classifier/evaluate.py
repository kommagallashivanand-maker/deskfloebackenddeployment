import json
import logging
from pathlib import Path
import pandas as pd
from sklearn.metrics import classification_report, confusion_matrix

logger = logging.getLogger("baseline_classifier")

def evaluate_pipeline(pipeline, X_train, X_test, y_train, y_test, classes, report_dir, model_version):
    """
    Perform full evaluation of the trained pipeline and save all reports.
    """
    logger.info("Evaluation started")
    
    # Ensure report directory exists
    report_path = Path(report_dir)
    report_path.mkdir(parents=True, exist_ok=True)
    
    # Predict on test set
    y_pred = pipeline.predict(X_test)
    
    # 1. Generate text classification report
    report_str = classification_report(y_test, y_pred, target_names=classes)
    report_txt_file = report_path / "classification_report.txt"
    with open(report_txt_file, "w", encoding="utf-8") as f:
        f.write(report_str)
    logger.info(f"Saved classification report to {report_txt_file}")
    
    # 2. Generate metrics dict for JSON
    report_dict = classification_report(y_test, y_pred, target_names=classes, output_dict=True)
    
    accuracy = report_dict.get("accuracy")
    macro_f1 = report_dict.get("macro avg", {}).get("f1-score")
    weighted_f1 = report_dict.get("weighted avg", {}).get("f1-score")
    
    metrics_data = {
        "model_version": model_version,
        "accuracy": accuracy,
        "macro_f1": macro_f1,
        "weighted_f1": weighted_f1,
        "metrics_by_class": {
            cls: {
                "precision": report_dict[cls]["precision"],
                "recall": report_dict[cls]["recall"],
                "f1-score": report_dict[cls]["f1-score"],
                "support": report_dict[cls]["support"]
            }
            for cls in classes if cls in report_dict
        }
    }
    
    metrics_json_file = report_path / "metrics.json"
    with open(metrics_json_file, "w", encoding="utf-8") as f:
        json.dump(metrics_data, f, indent=4)
    logger.info(f"Saved metrics JSON to {metrics_json_file}")
    
    # 3. Generate Confusion Matrix
    cm = confusion_matrix(y_test, y_pred, labels=classes)
    cm_df = pd.DataFrame(cm, index=classes, columns=classes)
    cm_csv_file = report_path / "confusion_matrix.csv"
    cm_df.to_csv(cm_csv_file)
    logger.info(f"Saved confusion matrix to {cm_csv_file}")
    
    # 4. Generate training_summary.md
    total_samples = len(X_train) + len(X_test)
    train_size = len(X_train)
    test_size = len(X_test)
    num_classes = len(classes)
    
    summary_md_file = report_path / "training_summary.md"
    summary_content = f"""# Training Summary - {model_version}

- **Dataset**: {total_samples} samples, {num_classes} classes
- **Train**: {train_size}
- **Test**: {test_size}
- **Macro F1**: {macro_f1:.4f}
- **Accuracy**: {accuracy:.2%}
- **Model**: TF-IDF + Logistic Regression
- **Version**: {model_version}
"""
    with open(summary_md_file, "w", encoding="utf-8") as f:
        f.write(summary_content)
    logger.info(f"Saved training summary markdown to {summary_md_file}")
    
    logger.info("Evaluation completed")
    return metrics_data
