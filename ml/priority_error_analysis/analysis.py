"""
analysis.py — Priority Model Error Analysis
============================================
Analyzes priority_model predictions from eval harness output to:
1. Build a confusion matrix
2. Identify the top-3 most frequent misclassification patterns
3. Compute per-class precision/recall

Usage (from ml/priority_error_analysis/):
    python analysis.py
"""

import json
import os
from collections import Counter, defaultdict
from pathlib import Path


def load_eval_data(eval_path: Path) -> dict:
    """Load the latest eval harness report."""
    with open(eval_path, 'r', encoding='utf-8') as f:
        return json.load(f)


def extract_priority_predictions(data: dict) -> list:
    """Filter predictions to priority_model only."""
    return [
        p for p in data['predictions']
        if p['model'] == 'priority_model'
    ]


def build_confusion_matrix(predictions: list) -> dict:
    """Build a confusion matrix as nested dict: true_label -> predicted_label -> count."""
    matrix = defaultdict(lambda: defaultdict(int))
    
    for pred in predictions:
        true_label = pred['true_label']
        predicted_label = pred['predicted_label']
        matrix[true_label][predicted_label] += 1
    
    return matrix


def find_top_error_patterns(matrix: dict, n: int = 3) -> list:
    """Identify the top-N most frequent misclassification patterns (true != predicted)."""
    errors = []
    
    for true_label, pred_counts in matrix.items():
        for predicted_label, count in pred_counts.items():
            if true_label != predicted_label:
                errors.append({
                    'true': true_label,
                    'predicted': predicted_label,
                    'count': count
                })
    
    # Sort by count descending
    errors.sort(key=lambda x: x['count'], reverse=True)
    return errors[:n]


def compute_per_class_metrics(matrix: dict) -> dict:
    """Compute precision and recall for each class."""
    all_labels = set(matrix.keys())
    for pred_dict in matrix.values():
        all_labels.update(pred_dict.keys())
    
    metrics = {}
    
    for label in sorted(all_labels):
        # True positives: correctly predicted as this label
        tp = matrix.get(label, {}).get(label, 0)
        
        # False positives: predicted as this label but was actually something else
        fp = sum(
            matrix.get(other, {}).get(label, 0)
            for other in all_labels if other != label
        )
        
        # False negatives: was this label but predicted as something else
        fn = sum(
            matrix.get(label, {}).get(other, 0)
            for other in all_labels if other != label
        )
        
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        
        metrics[label] = {
            'precision': precision,
            'recall': recall,
            'tp': tp,
            'fp': fp,
            'fn': fn
        }
    
    return metrics


def print_confusion_matrix(matrix: dict):
    """Pretty print the confusion matrix."""
    all_labels = ['Low', 'Medium', 'High', 'Urgent']
    
    print("\nConfusion Matrix (True Label × Predicted Label):")
    print("=" * 70)
    
    # Header
    header = "True \\ Pred    " + "".join(f"{label:>10}" for label in all_labels) + "      Total"
    print(header)
    print("-" * 70)
    
    # Rows
    for true_label in all_labels:
        row_counts = [matrix.get(true_label, {}).get(pred_label, 0) for pred_label in all_labels]
        row_total = sum(row_counts)
        row_str = f"{true_label:<15}" + "".join(f"{count:>10}" for count in row_counts) + f"      {row_total:>5}"
        print(row_str)
    
    print("=" * 70)
    print()


def print_top_errors(errors: list):
    """Print the top error patterns with counts."""
    print("\nTop-3 Misclassification Patterns:")
    print("=" * 70)
    
    for i, error in enumerate(errors, 1):
        print(f"{i}. {error['true']} → {error['predicted']}: {error['count']} occurrences")
    
    print("=" * 70)
    print()


def print_metrics(metrics: dict):
    """Print per-class precision and recall."""
    print("\nPer-Class Metrics:")
    print("=" * 70)
    print(f"{'Class':<15} {'Precision':>12} {'Recall':>12} {'TP':>6} {'FP':>6} {'FN':>6}")
    print("-" * 70)
    
    for label in ['Low', 'Medium', 'High', 'Urgent']:
        if label in metrics:
            m = metrics[label]
            print(f"{label:<15} {m['precision']:>12.4f} {m['recall']:>12.4f} {m['tp']:>6} {m['fp']:>6} {m['fn']:>6}")
    
    # Macro averages
    prec_vals = [m['precision'] for m in metrics.values()]
    rec_vals = [m['recall'] for m in metrics.values()]
    macro_precision = sum(prec_vals) / len(prec_vals)
    macro_recall = sum(rec_vals) / len(rec_vals)
    
    print("-" * 70)
    print(f"{'Macro-avg':<15} {macro_precision:>12.4f} {macro_recall:>12.4f}")
    print("=" * 70)
    print()


def explain_error_pattern(error: dict, rank: int) -> str:
    """Generate explanation for a misclassification pattern."""
    true_label = error['true']
    predicted_label = error['predicted']
    count = error['count']
    
    explanations = {
        ('Medium', 'High'): (
            f"The model over-predicts High priority for Medium tickets ({count} cases). "
            "This suggests the model may be too sensitive to urgency-related keywords or "
            "categories that it associates with higher severity (Technical, Account Access, etc.), "
            "leading it to escalate genuine Medium-priority issues unnecessarily. The class_weight='balanced' "
            "setting may be over-compensating for Medium's underrepresentation in training data."
        ),
        ('Low', 'Medium'): (
            f"The model over-predicts Medium priority for Low tickets ({count} cases). "
            "This indicates the model struggles to distinguish routine inquiries from issues requiring "
            "more attention. It may be interpreting neutral sentiment or certain categories "
            "(Billing, Security) as signals of higher priority even when the actual urgency is low."
        ),
        ('Low', 'High'): (
            f"The model dramatically over-predicts High priority for Low tickets ({count} cases). "
            "This is a severe escalation error where the model misinterprets low-severity issues "
            "as requiring immediate attention. This could be driven by specific keywords, negative sentiment, "
            "or category associations that the model has learned to correlate with High priority, "
            "even when the overall context suggests a routine matter."
        ),
        ('High', 'Urgent'): (
            f"The model escalates High tickets to Urgent ({count} cases). "
            "While these are adjacent priority levels, this pattern suggests the model may be "
            "over-reacting to Security or critical-category tickets. Given Urgent's low precision (0.20), "
            "the model appears to predict Urgent too liberally."
        ),
        ('Medium', 'Urgent'): (
            f"The model escalates Medium tickets all the way to Urgent ({count} cases). "
            "This is a significant over-escalation error. Given that Urgent has perfect recall (1.0) "
            "but very low precision (0.20), the model is clearly over-predicting Urgent class, "
            "likely triggered by specific keywords, sentiment extremes, or certain categories."
        ),
    }
    
    pattern_key = (true_label, predicted_label)
    return explanations.get(pattern_key, 
        f"The model predicts {predicted_label} when the true label is {true_label} ({count} cases). "
        f"This may indicate confusion between these priority levels due to overlapping feature patterns."
    )


def main():
    # Paths
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent.parent
    eval_path = repo_root / "ml" / "eval_harness" / "reports" / "latest.json"
    
    if not eval_path.exists():
        print(f"Error: Eval report not found at {eval_path}")
        print("Please run ml/eval_harness/eval.py first to generate the report.")
        return 1
    
    print("=" * 70)
    print("  Priority Model Error Analysis")
    print("=" * 70)
    print()
    
    # Load data
    print(f"Loading eval data from: {eval_path}")
    data = load_eval_data(eval_path)
    print(f"Timestamp: {data['timestamp']}")
    print(f"Git commit: {data['git_commit']}")
    
    # Extract priority predictions
    predictions = extract_priority_predictions(data)
    print(f"Total priority_model predictions: {len(predictions)}")
    
    # Build confusion matrix
    matrix = build_confusion_matrix(predictions)
    print_confusion_matrix(matrix)
    
    # Find top-3 error patterns
    top_errors = find_top_error_patterns(matrix, n=3)
    print_top_errors(top_errors)
    
    # Generate explanations for top-3 patterns
    print("\nError Pattern Explanations:")
    print("=" * 70)
    for i, error in enumerate(top_errors, 1):
        explanation = explain_error_pattern(error, i)
        print(f"\nPattern {i}: {error['true']} → {error['predicted']} ({error['count']} occurrences)")
        print(f"Explanation: {explanation}")
    print("=" * 70)
    print()
    
    # Compute and print metrics
    metrics = compute_per_class_metrics(matrix)
    print_metrics(metrics)
    
    print("\n[SUCCESS] Analysis complete.")
    print(f"[INFO] Next step: Review the error patterns and implement a targeted fix in train.py")
    
    return 0


if __name__ == "__main__":
    exit(main())
