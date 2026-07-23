# Priority Model — Training Pipeline

> **Module:** `ml/train_priority_model/`  
> **Model:** `RandomForestClassifier` with balanced class weights  
> **Version:** `v1.0.0`  
> **Target:** `priority` (Low · Medium · High · Urgent)

---

## Model Results — v1.0.0

> Results from a **Stratified 5-Fold Cross-Validation** run on 2,010 tickets.  
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

### Cross-Validation Classification Report

```
              precision    recall  f1-score   support

         Low       0.69      0.75      0.71       559
      Medium       0.54      0.45      0.49       658
        High       0.59      0.64      0.61       614
      Urgent       0.51      0.50      0.50       179

    accuracy                           0.60      2010
   macro avg       0.58      0.59      0.58      2010
weighted avg       0.59      0.60      0.59      2010
```

**Validation strategy:** `StratifiedKFold(n_splits=5, shuffle=True, random_state=42)`

---

### Per-Class Analysis

| Class | Precision | Recall | F1 | Notes |
|---|---|---|---|---|
| Low | 0.69 | **0.75** | 0.71 | Strongest class — well-separated signal |
| High | 0.59 | 0.64 | 0.61 | Solid recall; some confusion with Medium |
| Urgent | 0.51 | 0.50 | 0.50 | Minority class; recall is the key metric to improve |
| Medium | 0.54 | 0.45 | 0.49 | Hardest class — semantically adjacent to both Low and High |

#### Key observations

- **`Low` performs best (F1 0.71)** — likely benefits from distinctive keyword signals (e.g. general inquiries, low-urgency language).
- **`Urgent` recall is 0.50** — the model catches 1 in 2 urgent tickets. This is the primary target for future improvement. Techniques to explore: SMOTE oversampling, lower classification threshold for `Urgent`, or adding raw description text as a feature.
- **`Medium` is the weakest class (F1 0.49)** — expected, as "Medium" sits between High and Low and has no sharp definitional boundary, making it the most ambiguous label in the dataset.
- **Overall accuracy of 0.60** is a strong baseline for a 4-class problem with no free-text descriptions (keywords + metadata only). Category, sentiment, and description length together provide meaningful signal.

---

## Overview

This pipeline trains a support-ticket priority classifier using engineered features from `tickets_extracted_features.csv`. It addresses the natural class imbalance in the dataset by:

- Using `class_weight="balanced"` in the Random Forest.
- Evaluating with Stratified 5-Fold Cross-Validation so every fold maintains the original class proportions.

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
| `models/priority_model_v1.0.0.joblib` | Versioned, immutable snapshot of the trained pipeline |
| `models/priority_model_latest.joblib` | Convenience copy always pointing to the most recent version |

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
