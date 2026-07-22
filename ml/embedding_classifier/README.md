# DeskFlow — Embedding Ticket Category Classifier (DF-023)

An upgraded ticket classifier that replaces TF-IDF features with dense sentence
embeddings from [`all-MiniLM-L6-v2`](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
and trains a Logistic Regression head on top.  This is the direct successor to
the baseline classifier introduced in DF-022.

## Project Overview

The embedding classifier encodes each ticket's `title` and `body` into a
384-dimensional semantic vector using SentenceTransformers, then classifies it
into one of 10 support categories with Logistic Regression.

The same dataset (`tickets_v1.csv`), the same stratified 80/20 split
(`random_state=42`), and the same evaluation suite as DF-022 are used, making
the two models directly comparable.

### Key Features

- **Semantic understanding** — dense embeddings capture paraphrase and synonymy
  that TF-IDF misses.
- **Drop-in predict API** — identical `predict(title, body) → dict` interface
  as the baseline; no changes required in downstream services.
- **Reproducible** — fixed seeds and deterministic split guarantee identical
  results on every run.
- **Full evaluation suite** — classification report, confusion matrix CSV, JSON
  metrics, training summary, and a side-by-side comparison report with the
  baseline.
- **Model card** — `reports/model_card.md` documents architecture, dataset,
  metrics, recommendation, and future improvements.

---

## Folder Structure

```text
embedding_classifier/
├── config.py                   # Central configuration (paths, hyperparameters)
├── utils.py                    # Logging, dataset loader/validator, text combiner, encoder
├── train.py                    # Training orchestration script
├── predict.py                  # Prediction API and CLI interface
├── evaluate.py                 # Metrics calculation and report generation
├── compare.py                  # Side-by-side comparison with the baseline model
├── requirements.txt            # Package dependencies
├── .gitignore                  # Git ignore rules
├── models/                     # Saved model artefacts
│   ├── embedding_v1.pkl        # Serialised artefact dict (classifier + model name)
│   └── embedding_v1_metadata.json
├── reports/                    # Evaluation diagnostics
│   ├── classification_report.txt
│   ├── metrics.json
│   ├── training_summary.md
│   ├── confusion_matrix.csv
│   ├── comparison_report.md    # Baseline vs embedding comparison
│   ├── comparison_metrics.json
│   └── model_card.md           # Full model card (DF-023)
└── README.md
```

---

## Setup & Installation

Install the required dependencies:

```bash
pip install -r requirements.txt
```

> **Note** — `sentence-transformers` pulls in PyTorch.  If you only have a CPU,
> install the CPU-only variant first:
> ```bash
> pip install torch --index-url https://download.pytorch.org/whl/cpu
> pip install -r requirements.txt
> ```

The `all-MiniLM-L6-v2` model (~80 MB) is downloaded automatically from the
Hugging Face Hub on first use and cached locally.

---

## How to Run

### Prerequisites

The baseline classifier model must already be trained before running
`compare.py`:

```bash
python ml/baseline_classifier/train.py
```

### 1. Train the Embedding Model

```bash
python ml/embedding_classifier/train.py
```

Or from the workspace root:

```bash
python ml/embedding_classifier/train.py
```

#### Training Sequence

1. Loads `tickets_v1.csv` from the dataset generator.
2. Validates schema and filters empty records.
3. Performs a stratified 80/20 split (`random_state=42`) — identical to DF-022.
4. Encodes title+body with `all-MiniLM-L6-v2` (batch_size=64).
5. Fits `LogisticRegression` on the 384-dim training embeddings.
6. Evaluates on the held-out test set and writes reports to `reports/`.
7. Saves `models/embedding_v1.pkl` and `models/embedding_v1_metadata.json`.

### 2. Run the Comparison

```bash
python ml/embedding_classifier/compare.py
```

Evaluates both models on the exact same test set and writes:
- `reports/comparison_report.md` — human-readable side-by-side report.
- `reports/comparison_metrics.json` — machine-readable comparison metrics.

### 3. Make Predictions

#### CLI

```bash
python ml/embedding_classifier/predict.py \
  --title "Unable to reset password" \
  --body  "Password reset link expires immediately after clicking."
```

Expected output:

```text
Prediction: Account Access
Confidence: 82%
```

#### Python API

```python
from ml.embedding_classifier.predict import predict

result = predict(
    title="Having trouble billing credit card",
    body="The payment gateway returns credit card validation error code 43022."
)
print(result)
# Output: {'category': 'Billing', 'confidence': 0.87}
```

---

## Model Benchmark Summary (embedding_v1)

Evaluated on 402 unseen test samples from `tickets_v1.csv`:

- **Accuracy**: 77.61%
- **Macro F1-Score**: 0.7720
- **Weighted F1-Score**: 0.7725

### Per-Class F1 Performance

| Category | Precision | Recall | F1-Score | Support |
| :--- | :---: | :---: | :---: | :---: |
| **Feature Request** | 0.95 | 0.98 | **0.96** | 41 |
| **Notifications** | 0.90 | 0.95 | **0.93** | 40 |
| **Account Access** | 0.79 | 0.93 | **0.85** | 40 |
| **General Inquiry** | 0.84 | 0.78 | **0.81** | 40 |
| **Performance** | 0.76 | 0.85 | **0.80** | 41 |
| **Security** | 0.88 | 0.72 | **0.79** | 40 |
| **Data Management** | 0.72 | 0.85 | **0.78** | 40 |
| **Technical** | 0.75 | 0.53 | **0.62** | 40 |
| **Billing** | 0.62 | 0.62 | **0.62** | 40 |
| **Subscription** | 0.55 | 0.55 | **0.55** | 40 |

---

## Comparison with Baseline (baseline_v1)

| Model | Feature | Accuracy | Macro F1 |
| :--- | :--- | :---: | :---: |
| baseline_v1 | TF-IDF + Logistic Regression | 76.87% | 0.7636 |
| **embedding_v1** | **MiniLM Embeddings + Logistic Regression** | **77.61%** | **0.7720** |

The embedding model improves Macro-F1 by **+0.0084** (+1.10%) and wins
**7 out of 10** per-class comparisons.

Run `python compare.py` for the full side-by-side evaluation. See
`reports/comparison_report.md` for the detailed breakdown and
`reports/model_card.md` for the full model card.

---

## Model Summary

| Property | Value |
| :--- | :--- |
| Embedding model | `all-MiniLM-L6-v2` (384-dim) |
| Classifier | Logistic Regression (`lbfgs`, max_iter=1000) |
| Dataset | `tickets_v1.csv` — 2 010 samples, 10 classes |
| Train / Test | 1 608 / 402 (stratified, seed=42) |
| Accuracy | **77.61%** |
| Macro F1 | **0.7720** |
| Model artefact | `models/embedding_v1.pkl` |

---

## Files Reference

| File | Purpose |
| :--- | :--- |
| `config.py` | Paths, hyperparameters, model version |
| `utils.py` | Logging, data loading, text combiner, SentenceTransformer encoder |
| `train.py` | End-to-end training and artefact serialisation |
| `evaluate.py` | Metrics, reports, confusion matrix |
| `predict.py` | Prediction API (mirrors baseline interface) |
| `compare.py` | Cross-model evaluation on shared test set |
| `reports/model_card.md` | Architecture, metrics, recommendation, limitations |
