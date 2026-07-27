# DeskFlow Evaluation Harness

This module implements the evaluation harness for DeskFlow model predictions. It compares model outputs against the curated golden test set `data/golden_set.csv`.

## Folder Structure
- `data/`: Contains the hand-curated `golden_set.csv` test set.
- `models/`: Real model wrappers for baseline classification, embedding-based classification, and priority models.
- `reports/`: Evaluation outputs (not tracked in Git, except `reports/.gitkeep`).
  - `latest.json`: The results of the most recent evaluation run.
  - `history/`: Historical evaluation records stamped by execution time.
- `tests/`: Unit tests for evaluation metrics and model smoke tests.
- `metrics.py`: Metrics calculations (precision, recall, F1, and confusion matrix).
- `eval.py`: Evaluation runner orchestration script.

## Setup Instructions

Run the setup batch file to create the virtual environment and install dependencies:
```bash
setup.bat
```

## Running Tests

To execute unit tests with verbose output:
```bash
.venv\Scripts\pytest tests/ -v
```

This includes:
- `tests/test_metrics.py`: Unit tests for metric calculations
- `tests/test_models.py`: Smoke tests verifying real models load and produce valid predictions

## Running Evaluation

To execute the evaluation harness and generate report files:
```bash
.venv\Scripts\python eval.py
```
This script runs all three real models on the golden test set, computes precision, recall, and class-wise F1 scores, writes JSON reports, and validates against CI quality thresholds.

## CI Quality Thresholds

The evaluation harness enforces minimum quality thresholds to catch model regressions. These thresholds are set at ~15% below the baseline real-model performance to allow for normal variance while catching genuine regressions.

**Baseline Numbers** (golden set eval with `en_core_web_sm` installed — required for priority):
- **category_baseline** (TF-IDF + LogisticRegression): macro-F1 = 0.9209 (2026-07-23)
- **category_embeddings** (SentenceTransformers + LogisticRegression): macro-F1 = 0.8163 (2026-07-23)
- **priority_model v1.1.0** (RandomForest + REAL DF-026 feature extraction): macro-Precision = 0.5372, macro-Recall = 0.6955 (2026-07-24)

**Note**: Priority eval uses the same DF-026 pipeline as production (YAKE + spaCy keywords, VADER sentiment). The spaCy model `en_core_web_sm` is listed in `requirements.txt` and installed in CI via `python -m spacy download en_core_web_sm`. Without it, keyword extraction silently falls back to YAKE/bigrams only and priority metrics are not comparable to these baselines.

**CI Thresholds** (15% below baseline):
- **category_baseline**: macro-F1 >= 0.78
- **category_embeddings**: macro-F1 >= 0.69
- **priority_model**: macro-Precision >= 0.46, macro-Recall >= 0.59

If any model falls below its threshold, `eval.py` exits with code 1 and prints a clear failure message identifying which model/metric failed.

### Environment consistency (historical)

- **v1.0.0** priority baseline (0.4846 / 0.6428) and **v1.1.0** (0.5372 / 0.6955) were measured on developer machines with spaCy loaded. Until the spaCy parity fix, GitHub Actions installed `spacy` but not `en_core_web_sm`, so CI priority results were **not comparable** to documented baselines (e.g. macro-Recall ~0.37 vs ~0.70).
- Before commit `1ba540a`, the eval harness used **inline simplified** keyword extraction, not the DF-026 service pipeline — those older numbers are invalid for production comparison.

## Models

### category_baseline.py
Loads the trained TF-IDF + Logistic Regression pipeline from `ml/baseline_classifier/models/baseline_v1.pkl`. Expects `title` and `body` fields and returns category prediction with confidence.

### category_embeddings.py
Loads the trained sentence-transformers + Logistic Regression model from `ml/embedding_classifier/models/embedding_v1.pkl`. Uses `all-MiniLM-L6-v2` embeddings for text encoding.

### priority_model.py
Loads the trained RandomForest pipeline from `ml/train_priority_model/models/priority_model_latest.joblib` (currently v1.1.0). Calls the REAL DF-026 feature extraction pipeline from `ml/priority_feature_extraction` (YAKE + spaCy keywords, VADER sentiment) so eval features match training and production.
