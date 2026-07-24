"""Display aggregate metrics from latest eval run."""
import json
from pathlib import Path

report_path = Path(__file__).parent / "reports" / "latest.json"
data = json.load(open(report_path))
metrics = data["aggregate_metrics"]

print("\n" + "="*60)
print("REAL MODEL EVALUATION METRICS - BASELINE NUMBERS")
print("="*60)

# Category Baseline
print("\n=== CATEGORY BASELINE (TF-IDF + LogisticRegression) ===")
baseline_f1s = list(metrics["category_baseline_f1"].values())
macro_f1_baseline = sum(baseline_f1s) / len(baseline_f1s)
print(f"Macro-F1: {macro_f1_baseline:.4f}")
print("\nPer-class F1 scores:")
for cls, score in sorted(metrics["category_baseline_f1"].items()):
    print(f"  {cls:20s}: {score:.4f}")

# Category Embeddings
print("\n=== CATEGORY EMBEDDINGS (Sentence-Transformers + LogisticRegression) ===")
embed_f1s = list(metrics["category_embeddings_f1"].values())
macro_f1_embed = sum(embed_f1s) / len(embed_f1s)
print(f"Macro-F1: {macro_f1_embed:.4f}")
print("\nPer-class F1 scores:")
for cls, score in sorted(metrics["category_embeddings_f1"].items()):
    print(f"  {cls:20s}: {score:.4f}")

# Priority Model
print("\n=== PRIORITY MODEL (RandomForest with extracted features) ===")
pr = metrics["priority_precision_recall"]
prec_vals = [v["precision"] for v in pr.values()]
rec_vals = [v["recall"] for v in pr.values()]
macro_prec = sum(prec_vals) / len(prec_vals)
macro_rec = sum(rec_vals) / len(rec_vals)
print(f"Macro-Precision: {macro_prec:.4f}")
print(f"Macro-Recall:    {macro_rec:.4f}")
print("\nPer-class precision/recall:")
for priority in ["Urgent", "High", "Medium", "Low"]:
    if priority in pr:
        p = pr[priority]["precision"]
        r = pr[priority]["recall"]
        print(f"  {priority:10s}: Precision={p:.4f}, Recall={r:.4f}")

print("\n" + "="*60)
print("RECOMMENDED CI THRESHOLDS (15% below baseline)")
print("="*60)

threshold_baseline = macro_f1_baseline * 0.85
threshold_embed = macro_f1_embed * 0.85
threshold_priority_prec = macro_prec * 0.85
threshold_priority_rec = macro_rec * 0.85

print(f"\ncategory_baseline:    macro-F1 >= {threshold_baseline:.2f}")
print(f"category_embeddings:  macro-F1 >= {threshold_embed:.2f}")
print(f"priority_model:       macro-Precision >= {threshold_priority_prec:.2f}")
print(f"priority_model:       macro-Recall    >= {threshold_priority_rec:.2f}")
print()
print("Note: priority_model baseline corrected 2026-07-24 after")
print("      fixing feature extraction to use REAL DF-026 pipeline.")
print()
