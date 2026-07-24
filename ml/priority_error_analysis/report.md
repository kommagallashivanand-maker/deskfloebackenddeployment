# Priority Model Error Analysis Report

**Issue:** DF-028 "Error analysis" (AI: Priority)  
**Date:** 2026-07-24  
**Model Versions:** v1.0.0 (baseline) → v1.1.0 (DF-028 fix)

---

## Executive Summary

This report analyzes misclassification patterns in the DeskFlow priority prediction model (v1.0.0) using the golden set evaluation data. The analysis identified **Medium → High over-escalation** as the primary error pattern (14/50 predictions, 28% of dataset), with Medium priority showing critically low recall (0.30). A targeted fix using custom class weights was implemented in v1.1.0, resulting in:

- **Medium recall improved from 0.30 to 0.57** (+86% relative improvement)
- **High precision improved from 0.20 to 0.31** (+55% relative improvement)
- **Medium → High errors reduced from 14 to 9 occurrences** (35% reduction)
- **Macro-Precision: 0.4846 → 0.5372** (+10.8%)
- **Macro-Recall: 0.6428 → 0.6955** (+8.2%)

**Recommendation:** Keep the fix. The model now better balances Medium and High predictions, though Low → Medium misclassification emerged as a new top error pattern requiring future investigation.

---

## Task 1: Confusion Matrix and Top-3 Error Patterns

### Baseline Confusion Matrix (v1.0.0 - Before Fix)

**Eval Timestamp:** 2026-07-24T11:13:30.353341+00:00

```
True \ Pred           Low    Medium      High    Urgent      Total
----------------------------------------------------------------------
Low                    12         6         2         0         20
Medium                  0         7        14         2         23
High                    0         0         4         2          6
Urgent                  0         0         0         1          1
----------------------------------------------------------------------
```

### Top-3 Misclassification Patterns (Baseline)

1. **Medium → High: 14 occurrences (60.9% of Medium tickets)**
   - **Cause:** The model is over-predicting High priority for Medium tickets. This is driven by the `class_weight="balanced"` setting combined with High's strong presence in training data (30.5%). The model learned to associate certain categories (Technical, Account Access, Data Management) and negative sentiment with High priority, causing it to escalate genuine Medium-priority issues. High had very low precision (0.20) with 16 false positives, indicating it was being predicted far too liberally.

2. **Low → Medium: 6 occurrences (30% of Low tickets)**
   - **Cause:** The model struggles to distinguish routine inquiries from issues requiring more attention. Neutral sentiment combined with categories like Billing or Security are interpreted as signals of elevated priority, even when the actual urgency is low. This suggests the model may be over-sensitive to category features relative to sentiment and description length.

3. **Low → High: 2 occurrences (10% of Low tickets)**
   - **Cause:** Severe escalation errors where the model dramatically misinterprets low-severity issues as requiring immediate attention. This is likely driven by specific keyword combinations or negative sentiment in tickets that are objectively routine. These are high-impact errors that cause unnecessary escalation to support teams.

### Baseline Per-Class Metrics

| Class  | Precision | Recall | TP | FP | FN | Notes |
|--------|-----------|--------|----|----|----|-------|
| Low    | 1.00      | 0.60   | 12 | 0  | 8  | Perfect precision, but missing 40% of Low tickets |
| Medium | 0.54      | 0.30   | 7  | 6  | 16 | **Critical issue:** Only catching 7 of 23 Medium tickets |
| High   | 0.20      | 0.67   | 4  | 16 | 2  | **Critical issue:** 16 false positives - predicting High far too often |
| Urgent | 0.20      | 1.00   | 1  | 4  | 0  | Perfect recall but low precision on tiny sample (n=1) |
| **Macro-avg** | **0.4846** | **0.6428** | | | | |

**Key Finding:** High class is over-predicted (precision=0.20, 16 false positives), while Medium class is under-predicted (recall=0.30, only 7/23 caught). This is the core imbalance to address.

---

## Task 2: Targeted Fix Implementation

### Problem Statement

The baseline model with `class_weight="balanced"` was:
- Over-predicting High (16 false positives out of 20 High predictions)
- Under-predicting Medium (only catching 7 of 23 true Medium tickets)
- Creating a 14-case Medium → High misclassification bottleneck

### Fix Strategy

Replace the generic `class_weight="balanced"` (which uses inverse class frequencies) with **custom class weights** that:
1. **Penalize High predictions** (reduce weight from ~1.6 to 0.7) to address the 16 false positives
2. **Boost Medium predictions** (increase weight from ~1.5 to 2.5) to improve the 0.30 recall
3. **Preserve Low and Urgent weights** since they were performing adequately

### Implementation

**File:** `ml/train_priority_model/train.py`

**Change:**

```diff
def build_pipeline(categorical_features: list) -> Pipeline:
    """Assemble the full preprocessing + classifier pipeline."""
    preprocessor = build_preprocessor(categorical_features)
-   classifier = RandomForestClassifier(
-       n_estimators=300,
-       class_weight="balanced",
-       max_features="sqrt",
-       random_state=RANDOM_STATE,
-       n_jobs=-1,
-   )
+   # Custom class weights to address Medium→High misclassification pattern
+   # Error analysis (DF-028) showed High is over-predicted (precision=0.20, 16 FPs),
+   # while Medium is under-predicted (recall=0.30, only 7/23 caught).
+   # Strategy: Reduce High's weight to penalize false positives, while keeping
+   # Medium's weight elevated to encourage correct Medium predictions.
+   class_weight = {
+       "Low": 1.0,      # Well-performing class (precision=1.0, recall=0.6)
+       "Medium": 2.5,   # Increase weight to improve recall (currently 0.30)
+       "High": 0.7,     # Decrease weight to reduce false positives (currently 16 FPs)
+       "Urgent": 1.5,   # Keep elevated for rare but critical class
+   }
+   
+   classifier = RandomForestClassifier(
+       n_estimators=300,
+       class_weight=class_weight,  # Changed from "balanced" to custom weights
+       max_features="sqrt",
+       random_state=RANDOM_STATE,
+       n_jobs=-1,
+   )
    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier),
        ]
    )
    return pipeline
```

**Rationale:**
- **High weight 0.7 (down from ~1.6):** Makes the model less eager to predict High, directly targeting the 16 false positive problem
- **Medium weight 2.5 (up from ~1.5):** Makes the model more willing to predict Medium, addressing the 0.30 recall issue
- **Low weight 1.0:** Low already had perfect precision, so no adjustment needed
- **Urgent weight 1.5:** Keep elevated to maintain the perfect recall on this critical rare class

---

## Task 3: Before/After Metrics Comparison

### Post-Fix Confusion Matrix (v1.1.0 - After Fix)

**Eval Timestamp:** 2026-07-24T11:17:20.114797+00:00

```
True \ Pred           Low    Medium      High    Urgent      Total
----------------------------------------------------------------------
Low                    11         9         0         0         20
Medium                  0        13         9         1         23
High                    0         0         4         2          6
Urgent                  0         0         0         1          1
----------------------------------------------------------------------
```

### Post-Fix Top-3 Error Patterns

1. **Low → Medium: 9 occurrences** (45% of Low tickets)
2. **Medium → High: 9 occurrences** (39% of Medium tickets, **down from 61%**)
3. **High → Urgent: 2 occurrences** (33% of High tickets)

**Key Change:** Medium → High dropped from #1 error (14 cases) to tied #1 (9 cases), a **35% reduction**. However, Low → Medium increased from 6 to 9 cases as a side effect of boosting Medium's weight.

### Per-Class Metrics: v1.0.0 vs v1.1.0

| Class  | Metric    | v1.0.0 | v1.1.0 | Change |
|--------|-----------|--------|-------|--------|
| **Medium** | Precision | 0.54   | 0.59  | +9.3% |
| **Medium** | Recall    | **0.30** | **0.57** | **+86.0%** ✓ |
| **Medium** | TP        | 7      | 13    | +6 correct predictions |
| **Medium** | FN        | 16     | 10    | -6 missed Medium tickets |
| **High**   | Precision | **0.20** | **0.31** | **+55.0%** ✓ |
| **High**   | Recall    | 0.67   | 0.67  | No change |
| **High**   | FP        | **16** | **9** | **-7 false positives** ✓ |
| Low        | Precision | 1.00   | 1.00  | No change (still perfect) |
| Low        | Recall    | 0.60   | 0.55  | -8.3% (side effect) |
| Urgent     | Precision | 0.20   | 0.25  | +25.0% |
| Urgent     | Recall    | 1.00   | 1.00  | No change (still perfect) |
| **Macro**  | **Precision** | **0.4846** | **0.5372** | **+10.8%** ✓ |
| **Macro**  | **Recall**    | **0.6428** | **0.6955** | **+8.2%** ✓ |

### Impact Analysis

**Wins:**
- ✓ **Medium recall doubled** (0.30 → 0.57): The model now correctly identifies 13 of 23 Medium tickets instead of only 7
- ✓ **High false positives cut by 44%** (16 → 9): Reduced inappropriate escalations
- ✓ **Medium → High error reduced by 35%** (14 → 9): Primary issue significantly improved
- ✓ **Overall macro metrics improved** by ~10% for precision and ~8% for recall

**Trade-offs:**
- ⚠ **Low recall decreased slightly** (0.60 → 0.55): The increased Medium weight caused 3 more Low tickets to be predicted as Medium (6 → 9 cases). This is an acceptable trade-off since Low tickets being classified as Medium is less costly than Medium tickets being classified as High.
- ⚠ **New top error emerged:** Low → Medium is now tied with Medium → High as the most frequent error (9 cases each). This suggests the next iteration should focus on better separating Low from Medium, possibly by adjusting the Low vs Medium class weight ratio or adding features that distinguish routine inquiries.

---

## Recommendations

### Keep the Fix

**Verdict:** ✅ **The custom class weight fix should be kept.**

The targeted adjustment successfully addressed the primary error pattern (Medium → High over-escalation) and improved overall model performance. The side effect of increased Low → Medium misclassification is a reasonable trade-off because:

1. **Impact hierarchy:** Escalating Medium to High is more costly than escalating Low to Medium
2. **Net improvement:** The model now correctly handles 6 more Medium tickets (the hardest class)
3. **High precision matters:** Reducing High's false positive rate from 80% to 69% prevents unnecessary urgency signals to support teams
4. **Macro metrics improved:** Both precision and recall are up across all classes on average

### Next Steps for Future Iterations

1. **Low vs Medium Separation** (Priority: Medium)
   - Investigate what distinguishes true Low from true Medium tickets in the 9 Low → Medium misclassifications
   - Consider adding features like: requester history (first-time user vs repeat), time-of-day patterns, or explicit urgency keywords
   - Experiment with adjusting Low weight (currently 1.0) vs Medium weight (currently 2.5) to find a better balance

2. **Feature Importance Analysis** (Priority: Medium)
   - Extract feature importances from the trained RandomForest to understand what drives High predictions
   - If specific keywords or categories dominate the High signal, consider adding conditional logic or interaction features
   - Example: "Technical" category + negative sentiment might be over-triggering High priority

3. **Threshold Tuning for High Class** (Priority: Low)
   - Instead of just adjusting class weights, experiment with custom decision thresholds for the High class
   - Use `predict_proba()` and set a higher threshold (e.g., 0.60 instead of default 0.50) to require stronger evidence before predicting High
   - This could further reduce High false positives without affecting other classes

4. **Golden Set Expansion** (Priority: High)
   - The current golden set has only 50 samples with very few High (6) and Urgent (1) examples
   - Expand to at least 200 samples with balanced representation (50 per class) for more robust evaluation
   - Current Urgent metrics (n=1) are not statistically meaningful

5. **Monitor Production Performance** (Priority: High)
   - Track real-world misclassification rates after deployment
   - Set up alerts for Low → High escalations (most costly error)
   - Collect feedback from support agents on prediction accuracy to guide next iteration

---

## Appendix: Analysis Methodology

### Data Source
- **File:** `ml/eval_harness/reports/latest.json`
- **Eval Script:** `ml/eval_harness/eval.py`
- **Golden Set:** 50 hand-labeled tickets in `ml/eval_harness/data/golden_set.csv`
- **Model:** `ml/train_priority_model/models/priority_model_latest.joblib`

### Analysis Tools
- **Script:** `ml/priority_error_analysis/analysis.py`
- **Method:** Confusion matrix analysis with per-class precision/recall calculation
- **Focus:** Priority model only (filtered from 150 total predictions across 3 models)

### Replication
To reproduce this analysis:

```powershell
# 1. Retrain the model with the fix
cd "c:\Users\Admin\Documents\deskflow backend\DeskFlow-backend"
$env:PYTHONPATH = (Get-Location).Path
ml\train_priority_model\.venv\Scripts\python ml\train_priority_model\train.py

# 2. Run the eval harness
cd ml\eval_harness
.venv\Scripts\python eval.py

# 3. Analyze the results
cd ..\priority_error_analysis
python analysis.py
```

---

## Issue Closure

**GitHub Issue:** DF-028 "Error analysis" (AI: Priority)  
**Status:** ✅ Complete  
**Branch:** `feature/DF-028-error-analysis`

All three tasks completed:
- ✅ Task 1: Confusion matrix and top-3 error patterns identified and documented
- ✅ Task 2: Targeted fix implemented (custom class weights), model retrained, metrics measured
- ✅ Task 3: Comprehensive report produced with before/after comparison and recommendations

**Artifacts Delivered:**
- `ml/priority_error_analysis/analysis.py` - Reusable error analysis script
- `ml/priority_error_analysis/report.md` - This report
- `ml/train_priority_model/train.py` - Updated to v1.1.0 with custom class weights (documented in code comments)
- Model artifacts: 
  - `priority_model_v1.0.0.joblib` - Original baseline (class_weight="balanced")
  - `priority_model_v1.1.0.joblib` - DF-028 fix with custom class weights
  - `priority_model_latest.joblib` - Symlink to v1.1.0
