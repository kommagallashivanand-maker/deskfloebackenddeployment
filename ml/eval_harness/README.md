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

**Baseline Numbers** (observed 2026-07-23, git commit a5cd9c9):
- **category_baseline** (TF-IDF + LogisticRegression): macro-F1 = 0.9209
- **category_embeddings** (SentenceTransformers + LogisticRegression): macro-F1 = 0.8163  
- **priority_model** (RandomForest + extracted features): macro-Precision = 0.5232, macro-Recall = 0.6694

**CI Thresholds** (15% below baseline):
- **category_baseline**: macro-F1 >= 0.78
- **category_embeddings**: macro-F1 >= 0.69
- **priority_model**: macro-Precision >= 0.44, macro-Recall >= 0.57

If any model falls below its threshold, `eval.py` exits with code 1 and prints a clear failure message identifying which model/metric failed.

## Models

### category_baseline.py
Loads the trained TF-IDF + Logistic Regression pipeline from `ml/baseline_classifier/models/baseline_v1.pkl`. Expects `title` and `body` fields and returns category prediction with confidence.

### category_embeddings.py
Loads the trained sentence-transformers + Logistic Regression model from `ml/embedding_classifier/models/embedding_v1.pkl`. Uses `all-MiniLM-L6-v2` embeddings for text encoding.

### priority_model.py
Loads the trained RandomForest pipeline from `ml/train_priority_model/models/priority_model_latest.joblib`. Includes simplified feature extraction (keywords, sentiment, description length) inline for inference.
