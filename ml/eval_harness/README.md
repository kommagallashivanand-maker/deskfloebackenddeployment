# DeskFlow Evaluation Harness

This module implements the evaluation harness for DeskFlow model predictions. It compares model outputs against the curated golden test set `data/golden_set.csv`.

## Folder Structure
- `data/`: Contains the hand-curated `golden_set.csv` test set.
- `models/`: Stub models representing the baseline classification, embedding-based classification, and priority models.
- `reports/`: Evaluation outputs (not tracked in Git, except `reports/.gitkeep`).
  - `latest.json`: The results of the most recent evaluation run.
  - `history/`: Historical evaluation records stamped by execution time.
- `tests/`: Unit tests for evaluation metrics.
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

## Running Evaluation

To execute the evaluation harness and generate report files:
```bash
.venv\Scripts\python eval.py
```
This script runs the prediction stubs on the golden test set, computes precision, recall, and class-wise F1 scores, writes the JSON reports, and exits.
