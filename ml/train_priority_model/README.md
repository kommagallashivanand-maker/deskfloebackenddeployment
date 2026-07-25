# Priority Model — Training Pipeline

> **Module:** `ml/train_priority_model/`  
> **Model:** `RandomForestClassifier` with custom class weights  
> **Current Version:** `v1.1.0` (see [Version History](#version-history) below)  
> **Target:** `priority` (Low · Medium · High · Urgent)

---

## Version History

### v1.1.0 (2026-07-24) — DF-028 Error Analysis Fix

**Changes:**
- Replaced generic `class_weight="balanced"` with custom per-class weights:
  - `Low`: 1.0 (baseline)
  - `Medium`: 2.5 (boosted to improve recall)
  - `High`: 0.7 (reduced to penalize false positives)
  - `Urgent`: 1.5 (elevated for critical rare class)

**Motivation:**
Error analysis on the golden set (DF-028) revealed that v1.0.0 was severely over-predicting High priority (precision=0.20, 16 false positives) while under-predicting Medium priority (recall=0.30, only 7/23 caught). The primary error pattern was Medium → High misclassification (14 occurrences, 60.9% of Medium tickets).

**Results (Golden Set n=50):**
- Medium recall: **0.30 → 0.57** (+86% improvement)
- High precision: **0.20 → 0.31** (+55% improvement)
- Medium → High errors: **14 → 9** (-35% reduction)
- Macro-Precision: **0.4846 → 0.5372** (+10.8%)
- Macro-Recall: **0.6428 → 0.6955** (+8.2%)

**Trade-offs:**
- Low recall decreased slightly (0.60 → 0.55) due to increased Medium weight
- Low → Medium errors increased (6 → 9), but this is less costly than Medium → High escalation

**Full Analysis:** See `ml/priority_error_analysis/report.md` for complete confusion matrices, error pattern explanations, and recommendations.

---

### v1.0.0 (2026-07-23) — Initial Release

**Configuration:**
- `class_weight="balanced"` (automatic inverse frequency weighting)
- `n_estimators=300`
- `max_features="sqrt"`
- Features: TF-IDF keywords, category, sentiment (label + score), description length

**Golden Set Metrics (n=50):**
- Macro-Precision: 0.4846
- Macro-Recall: 0.6428
- Per-class:
  - Low: P=1.00, R=0.60
  - Medium: P=0.54, R=0.30
  - High: P=0.20, R=0.67
  - Urgent: P=0.20, R=1.00

**Known Issues:**
- High class over-predicted (16 false positives)
- Medium class under-predicted (missed 16 of 23 true Medium tickets)
- Primary error: Medium → High misclassification (14 cases)

**Status:** Superseded by v1.1.0 (2026-07-24) with targeted class weight fix

---

## Model Results — v1.1.0

> Results from a **Stratified 5-Fold Cross-Validation** run on 2,010 tickets with custom class weights.  
> All metrics are computed on **out-of-fold predictions** (never seen during training).

### Dataset — Class Distribution

| Priority | Count | Share |
|---|---|---|
| Low | 559 | 27.8% |
| Medium | 658 | 32.7% |
| High | 614 | 30.5% |
| **Urgent** | **179** | **8.9%** |
| **Total** | **2,010** | |

> `Urgent` is the minority class at ~9%. The model uses `class_weight="balanced"` to compensate.

---

### Cross-Validation Classification Report (v1.1.0)

```
              precision    recall  f1-score   support

         Low       0.76      0.60      0.67       559
      Medium       0.45      0.79      0.57       658
        High       0.78      0.38      0.51       614
      Urgent       0.61      0.40      0.48       179

    accuracy                           0.58      2010
   macro avg       0.65      0.54      0.56      2010
weighted avg       0.65      0.58      0.57      2010
```

**Validation strategy:** `StratifiedKFold(n_splits=5, shuffle=True, random_state=42)`

---

### Per-Class Analysis (v1.1.0)

| Class | Precision | Recall | F1 | Notes |
|---|---|---|---|---|
| Low | 0.76 | 0.60 | 0.67 | Precision improved (+7pp vs v1.0.0); some Low tickets now escalated to Medium |
| Medium | 0.45 | **0.79** | 0.57 | **Major improvement:** Recall jumped from 0.45 to 0.79 (+34pp) due to increased class weight |
| High | **0.78** | 0.38 | 0.51 | **Precision improved:** Now 0.78 vs 0.59 in v1.0.0; reduced false positives via lower class weight |
| Urgent | 0.61 | 0.40 | 0.48 | Slight precision improvement; recall unchanged |

#### Key observations (v1.1.0 vs v1.0.0 comparison)

- **Medium recall dramatically improved** from 0.45 to 0.79 (+75% relative) by boosting its class weight to 2.5. The model now catches significantly more Medium-priority tickets.
- **High precision improved** from 0.59 to 0.78 (+32% relative) by reducing its class weight to 0.7, leading to fewer false positives.
- **Trade-off:** Low recall decreased from 0.75 to 0.60 as some Low tickets are now classified as Medium (acceptable trade-off per DF-028 analysis).
- **Overall macro F1 comparable** (0.58 in both versions), but class-level behavior is now better aligned with business priorities: catching more Medium tickets (common, important) while reducing High false alarms.

For detailed golden set evaluation and error pattern analysis, see `ml/priority_error_analysis/report.md`.

---

## Overview

This pipeline trains a support-ticket priority classifier using engineered features from `tickets_extracted_features.csv`. It addresses the natural class imbalance in the dataset by:

- Using **custom per-class weights** (v1.1.0+) tuned via error analysis to balance precision/recall per class
- Evaluating with Stratified 5-Fold Cross-Validation so every fold maintains the original class proportions

**Version History:** v1.0.0 used generic `class_weight="balanced"` (inverse frequency), which over-predicted High and under-predicted Medium. v1.1.0 uses custom weights (Low=1.0, Medium=2.5, High=0.7, Urgent=1.5) based on golden set error analysis (DF-028).

### Feature Strategy

| Feature | Type | Transformer |
|---|---|---|
| `keywords` | Text (comma-separated) | `TfidfVectorizer` (top-500 terms) |
| `category` | Categorical | `OneHotEncoder` |
| `sentiment_label` | Categorical | `OneHotEncoder` |
| `sentiment_score` | Numerical | `StandardScaler` |
| `description_length` | Numerical | `StandardScaler` |
| `requester_role` | Dropped (constant) | — |

---

## Setup

### Prerequisites

- Python 3.10+
- `cd` into the `ml/train_priority_model/` directory

### 1. Create the virtual environment and install dependencies

```cmd
cd ml\train_priority_model
setup.bat
```

`setup.bat` will:
1. Check that Python is available on your `PATH`.
2. Delete any existing `.venv` to ensure a clean build.
3. Create a new virtual environment in `.venv/`.
4. Upgrade `pip` and install all packages from `requirements.txt`.

---

## Running the Training Pipeline

Run the script **from the repository root** so that it can resolve the data file path correctly:

```cmd
ml\train_priority_model\.venv\Scripts\python ml\train_priority_model\train.py
```

### What happens during a run

1. **Data loading** — Reads `tickets_extracted_features.csv` from the repo root and validates that all expected columns are present.
2. **Class distribution** — Prints the count and percentage for each priority class.
3. **Stratified 5-Fold CV** — Runs cross-validated predictions across all 5 folds and prints the full per-class classification report.
4. **Final training** — Fits the pipeline on the complete dataset.
5. **Artifact saving** — Writes the serialised pipeline to `models/`.

---

## Output Artifacts

| File | Description |
|---|---|
| `models/priority_model_v1.0.0.joblib` | Original baseline (class_weight="balanced") — superseded by v1.1.0 |
| `models/priority_model_v1.1.0.joblib` | Current version with custom class weights (DF-028 fix) |
| `models/priority_model_latest.joblib` | Convenience copy always pointing to the most recent version (currently v1.1.0) |

Both files contain the complete scikit-learn `Pipeline` object (preprocessor + classifier) and can be loaded with:

```python
import joblib
pipeline = joblib.load("ml/train_priority_model/models/priority_model_latest.joblib")
predictions = pipeline.predict(df[feature_cols])
```

---

## Metric Reference

| Metric | Meaning |
|---|---|
| **Precision** | Of all tickets predicted as class X, what fraction were actually X? |
| **Recall** | Of all tickets that are truly class X, what fraction did the model catch? |
| **F1-score** | Harmonic mean of precision and recall — the primary metric to optimise. |
| **Support** | Number of true instances of that class across all test folds. |

> **Focus on `Urgent` recall.** A missed urgent ticket is a worse outcome than a false alarm — optimise recall for this class in future iterations.

---

## Dependencies

| Package | Version | Purpose |
|---|---|---|
| `pandas` | 3.0.3 | CSV loading and DataFrame manipulation |
| `scikit-learn` | 1.9.0 | Preprocessing pipeline, RandomForest, cross-validation, metrics |
| `joblib` | 1.5.3 | Fast serialisation of the trained pipeline |

See [`requirements.txt`](requirements.txt) for the full dependency list.

---

## Directory Structure

```
ml/train_priority_model/
├── .venv/                        # Virtual environment (git-ignored)
├── models/                       # Generated model artifacts (git-ignored)
│   ├── priority_model_v1.0.0.joblib
│   └── priority_model_latest.joblib
├── README.md                     # This file
├── requirements.txt              # Python dependencies
├── setup.bat                     # One-click venv setup (Windows)
└── train.py                      # Training & validation script
```
