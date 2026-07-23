import csv
import json
import os
import subprocess
import sys
from datetime import datetime, timezone

# Ensure we can import from models and current directory
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from models import category_baseline, category_embeddings, priority_model
from metrics import f1_per_class, precision_recall_per_class

def get_git_commit() -> str:
    """Gets the short git commit hash of HEAD via subprocess."""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"Warning: Could not determine git commit: {e}", file=sys.stderr)
        return "unknown"

def check_thresholds(aggregate_metrics: dict) -> bool:
    """
    Validates metrics against thresholds.
    
    TODO: Set real threshold values once real trained models exist (DF-022, DF-023, DF-027).
    Currently, all thresholds are set to 0.0 to verify structural correctness.
    
    Args:
        aggregate_metrics: The calculated aggregate metrics dict.
        
    Returns:
        True if all metrics meet or exceed the thresholds, False otherwise.
    """
    # Check category baseline F1 scores
    for cls, f1 in aggregate_metrics.get("category_baseline_f1", {}).items():
        if f1 < 0.0:
            print(f"Error: category_baseline_f1 for class '{cls}' ({f1}) is below threshold 0.0", file=sys.stderr)
            return False
            
    # Check category embeddings F1 scores
    for cls, f1 in aggregate_metrics.get("category_embeddings_f1", {}).items():
        if f1 < 0.0:
            print(f"Error: category_embeddings_f1 for class '{cls}' ({f1}) is below threshold 0.0", file=sys.stderr)
            return False
            
    # Check priority precision & recall scores
    for cls, metrics in aggregate_metrics.get("priority_precision_recall", {}).items():
        precision = metrics.get("precision", 0.0)
        recall = metrics.get("recall", 0.0)
        if precision < 0.0:
            print(f"Error: priority precision for class '{cls}' ({precision}) is below threshold 0.0", file=sys.stderr)
            return False
        if recall < 0.0:
            print(f"Error: priority recall for class '{cls}' ({recall}) is below threshold 0.0", file=sys.stderr)
            return False
            
    return True

def main():
    # File paths
    base_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(base_dir, "data", "golden_set.csv")
    reports_dir = os.path.join(base_dir, "reports")
    history_dir = os.path.join(reports_dir, "history")
    
    # Create directories if they do not exist
    os.makedirs(history_dir, exist_ok=True)
    
    if not os.path.exists(csv_path):
        print(f"Error: Golden set file not found at {csv_path}", file=sys.stderr)
        sys.exit(1)
        
    print(f"Loading golden set from {csv_path}...")
    rows = []
    with open(csv_path, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
            
    print(f"Loaded {len(rows)} records. Running model predictions...")
    predictions = []
    
    for row in rows:
        subject = row["subject"]
        description = row["description"]
        category_label = row["category_label"]
        requester_role = row["requester_role"]
        priority_label = row["priority_label"]
        source_ticket_id = row["source_ticket_id"]
        
        # 1. Category Baseline Model
        pred_cat_base, conf_cat_base = category_baseline.predict(
            subject=subject,
            description=description
        )
        predictions.append({
            "ticket_id": source_ticket_id,
            "model": "category_baseline",
            "true_label": category_label,
            "predicted_label": pred_cat_base,
            "confidence": conf_cat_base
        })
        
        # 2. Category Embeddings Model
        pred_cat_embed, conf_cat_embed = category_embeddings.predict(
            subject=subject,
            description=description
        )
        predictions.append({
            "ticket_id": source_ticket_id,
            "model": "category_embeddings",
            "true_label": category_label,
            "predicted_label": pred_cat_embed,
            "confidence": conf_cat_embed
        })
        
        # 3. Priority Model
        pred_prio, conf_prio = priority_model.predict(
            subject=subject,
            description=description,
            category=category_label,
            requester_role=requester_role
        )
        predictions.append({
            "ticket_id": source_ticket_id,
            "model": "priority_model",
            "true_label": priority_label,
            "predicted_label": pred_prio,
            "confidence": conf_prio
        })
        
    print("Evaluating metrics...")
    baseline_results = [
        (p["true_label"], p["predicted_label"], p["confidence"])
        for p in predictions if p["model"] == "category_baseline"
    ]
    embeddings_results = [
        (p["true_label"], p["predicted_label"], p["confidence"])
        for p in predictions if p["model"] == "category_embeddings"
    ]
    priority_results = [
        (p["true_label"], p["predicted_label"], p["confidence"])
        for p in predictions if p["model"] == "priority_model"
    ]
    
    category_baseline_f1 = f1_per_class(baseline_results)
    category_embeddings_f1 = f1_per_class(embeddings_results)
    priority_precision_recall = precision_recall_per_class(priority_results)
    
    aggregate_metrics = {
        "category_baseline_f1": category_baseline_f1,
        "category_embeddings_f1": category_embeddings_f1,
        "priority_precision_recall": priority_precision_recall
    }
    
    # Metadata
    timestamp = datetime.now(timezone.utc).isoformat()
    git_commit = get_git_commit()
    
    report_data = {
        "timestamp": timestamp,
        "git_commit": git_commit,
        "predictions": predictions,
        "aggregate_metrics": aggregate_metrics
    }
    
    # Paths for output
    latest_path = os.path.join(reports_dir, "latest.json")
    
    # Windows-safe timestamp filename (replacing colons with dashes)
    timestamp_safe = timestamp.replace(":", "-")
    history_path = os.path.join(history_dir, f"{timestamp_safe}.json")
    
    print(f"Saving reports...")
    with open(latest_path, mode="w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=2)
        
    with open(history_path, mode="w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=2)
        
    print(f"Reports generated successfully:\n- {latest_path}\n- {history_path}")
    
    # Threshold check
    success = check_thresholds(aggregate_metrics)
    if not success:
        print("Evaluation failed validation thresholds.", file=sys.stderr)
        sys.exit(1)
        
    print("Evaluation completed successfully.")
    sys.exit(0)

if __name__ == "__main__":
    main()
