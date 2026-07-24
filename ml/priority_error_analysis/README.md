# Priority Model Error Analysis

Analysis module for identifying and documenting misclassification patterns in the DeskFlow priority prediction model.

## Purpose

This module analyzes the eval harness output for the priority_model to:
1. Build a confusion matrix showing true vs predicted priority labels
2. Identify the top-3 most frequent misclassification patterns
3. Provide explanations for these error patterns
4. Support targeted improvements to the model training pipeline

## Structure

```
ml/priority_error_analysis/
├── analysis.py          # Main analysis script
├── report.md            # Human-readable analysis report with findings and fixes
├── requirements.txt     # Dependencies (none - uses stdlib only)
├── .gitignore
├── README.md
└── setup.bat            # Windows setup script
```

## Usage

### Prerequisites

1. Run the eval harness to generate fresh predictions:
   ```powershell
   cd ml\eval_harness
   .venv\Scripts\python eval.py
   ```

### Running the Analysis

From the `ml/priority_error_analysis/` directory:

```powershell
python analysis.py
```

This will:
- Load `ml/eval_harness/reports/latest.json`
- Filter to priority_model predictions only
- Build a confusion matrix
- Identify top-3 error patterns
- Print detailed metrics and explanations

## Output

The script outputs:
1. **Confusion Matrix**: 4×4 table (Low/Medium/High/Urgent) showing true vs predicted counts
2. **Top-3 Error Patterns**: Most frequent misclassifications with counts
3. **Error Explanations**: Analysis of why each pattern occurs
4. **Per-Class Metrics**: Precision, recall, TP, FP, FN for each priority level
5. **Macro Averages**: Overall precision and recall across all classes

## Integration with GitHub Issue DF-028

This module was created to support DF-028 "Error analysis" for the AI Priority module. Findings from this analysis inform targeted fixes to `ml/train_priority_model/train.py`.

## Report

The complete analysis, including the confusion matrix, error patterns, implemented fix, and before/after metrics, is documented in `report.md`.
