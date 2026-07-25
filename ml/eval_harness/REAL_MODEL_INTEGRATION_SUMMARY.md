# Real Model Integration Summary (DF-047)

## Overview

Successfully replaced all three placeholder model stubs with real trained model inference. All models now load their artifacts once at module-level, perform genuine predictions, and return actual confidence scores.

## Task 1: Model Wrapper Implementation

### 1. category_baseline.py
**Model**: TF-IDF + Logistic Regression pipeline  
**Artifact**: `ml/baseline_classifier/models/baseline_v1.pkl`  
**Serialization**: joblib (pickle)

**Implementation details**:
- Loads sklearn Pipeline at module level
- Adds `ml/baseline_classifier` to sys.path for pickle dependency resolution
- Expects input DataFrame with `title` and `body` columns
- Returns predicted class and actual confidence from `predict_proba()`

**Key code**:
```python
_pipeline = joblib.load(_model_path)
X_input = pd.DataFrame([{'title': subject or "", 'body': description or ""}])
predicted_label = _pipeline.predict(X_input)[0]
probas = _pipeline.predict_proba(X_input)[0]
confidence = float(probas[predicted_class_idx])
```

### 2. category_embeddings.py
**Model**: SentenceTransformers (`all-MiniLM-L6-v2`) + Logistic Regression  
**Artifact**: `ml/embedding_classifier/models/embedding_v1.pkl`  
**Serialization**: joblib (pickle with dict structure)

**Implementation details**:
- Loads model artifact dict containing `classifier`, `embedding_model_name`, `classes`
- Initializes SentenceTransformer encoder at module level
- Adds `ml/embedding_classifier` to sys.path for pickle dependency resolution
- Combines subject and description with `\n\n` separator (matches training)
- Encodes text to embeddings before prediction

**Key code**:
```python
_model_artifact = joblib.load(_model_path)
_classifier = _model_artifact["classifier"]
_encoder = SentenceTransformer(_embedding_model_name)

combined_text = f"{subject or ''}\n\n{description or ''}".strip()
embedding = _encoder.encode([combined_text], convert_to_numpy=True)
predicted_label = _classifier.predict(embedding)[0]
```

### 3. priority_model.py
**Model**: RandomForest with preprocessor (TF-IDF + OneHotEncoder + StandardScaler)  
**Artifact**: `ml/train_priority_model/models/priority_model_latest.joblib`  
**Serialization**: joblib

**Implementation details**:
- Loads sklearn Pipeline at module level
- Implements simplified inline feature extraction matching DF-026 logic:
  - **Keywords**: Regex-based token extraction with stopword filtering
  - **Sentiment**: Pattern matching for negative/urgency signals
  - **Category**: Passed through (normalized to "General Inquiry" if missing)
  - **Requester role**: Passed through (normalized to "unknown" if missing)
  - **Description length**: Character count
- Creates DataFrame with feature columns matching training pipeline expectations

**Key code**:
```python
_pipeline = joblib.load(_model_path)

# Feature extraction
keywords = _extract_keywords_simple(subject, description, top=3)
sentiment_score, sentiment_label = _analyze_sentiment_simple(f"{subject} {description}")
features = pd.DataFrame([{
    'keywords': keywords,
    'category': category or "General Inquiry",
    'sentiment_label': sentiment_label,
    'requester_role': requester_role or "unknown",
    'sentiment_score': sentiment_score,
    'description_length': len(description or "")
}])
predicted_label = _pipeline.predict(features)[0]
```

### Error Handling
All three wrappers:
- Fail loudly with `FileNotFoundError` if model artifact is missing
- Fail loudly with `RuntimeError` if loading/initialization fails
- Never silently fall back to random predictions
- Handle None/empty inputs gracefully

## Task 2: Test Results

### Test Execution
```bash
cd ml/eval_harness
.venv\Scripts\python -m pytest tests/ -v
```

**Result**: ✅ All 14 tests passed

**Test breakdown**:
- `tests/test_metrics.py`: 4 tests (metric calculation correctness)
- `tests/test_models.py`: 10 tests (3 models × 3-4 tests each)
  - Each model tested for: valid predictions, empty inputs, None inputs, optional fields

### Evaluation Run
```bash
cd ml/eval_harness
.venv\Scripts\python eval.py
```

**Result**: ✅ Evaluation completed successfully (exit code 0)

**Output**:
```
Loaded 50 records. Running model predictions...
Evaluating metrics...
Saving reports...
PASS: All models meet CI quality thresholds:
  category_baseline:    macro-F1 = 0.9209 (>= 0.7800)
  category_embeddings:  macro-F1 = 0.8163 (>= 0.6900)
  priority_model:       macro-Precision = 0.5232 (>= 0.4400)
  priority_model:       macro-Recall = 0.6694 (>= 0.5700)
Evaluation completed successfully.
```

### Real Aggregate Metrics (Baseline Run, 2026-07-23)

**category_baseline** (TF-IDF + LogisticRegression):
- **Macro-F1**: 0.9209
- Per-class F1 scores:
  - Account Access: 0.8889
  - Billing: 0.8000
  - Data Management: 0.9091
  - Feature Request: 1.0000
  - General Inquiry: 0.8889
  - Notifications: 1.0000
  - Performance: 1.0000
  - Security: 1.0000
  - Subscription: 0.8333
  - Technical: 0.8889

**category_embeddings** (SentenceTransformers + LogisticRegression):
- **Macro-F1**: 0.8163
- Per-class F1 scores:
  - Account Access: 0.8000
  - Billing: 0.6000
  - Data Management: 0.7273
  - Feature Request: 1.0000
  - General Inquiry: 0.8889
  - Notifications: 1.0000
  - Performance: 0.9091
  - Security: 1.0000
  - Subscription: 0.6667
  - Technical: 0.5714

**priority_model** (RandomForest + extracted features):
- **Macro-Precision**: 0.5232 ❌ **INCORRECT** (measured with simplified inline feature extraction)
- **Macro-Recall**: 0.6694 ❌ **INCORRECT**

**CORRECTED baseline (2026-07-24, v1.0.0 model)** after fixing feature extraction to use REAL DF-026 pipeline:
- **Macro-Precision**: 0.4846 (local eval, YAKE+spaCy keywords, VADER sentiment)
- **Macro-Recall**: 0.6428
- Per-class precision/recall:
  - Urgent: Precision=0.2000, Recall=1.0000
  - High: Precision=0.2000, Recall=0.6667
  - Medium: Precision=0.5385, Recall=0.3043
  - Low: Precision=1.0000, Recall=0.6000

**Current CI baseline (2026-07-24, priority_model v1.1.0, spaCy required)**:
- **Macro-Precision**: 0.5372
- **Macro-Recall**: 0.6955
- Per-class precision/recall:
  - Urgent: Precision=0.2500, Recall=1.0000
  - High: Precision=0.3077, Recall=0.6667
  - Medium: Precision=0.5909, Recall=0.5652
  - Low: Precision=1.0000, Recall=0.5500

### Environment consistency

Priority baselines above assume **`en_core_web_sm` is loaded**. Production installs it via `ml/priority_feature_extraction/requirements.txt`. Until the spaCy parity fix, `ml/eval_harness/requirements.txt` only listed `spacy` (no model wheel) and `.github/workflows/eval-harness.yml` did not run `spacy download`, so CI used the YAKE/bigram fallback and reported much lower recall (~0.37). Documented v1.0.0/v1.1.0 numbers were measured **locally with spaCy** and were not validated in CI until the fix.

Pre-`1ba540a` eval used **inline simplified** keyword extraction; metrics 0.5232 / 0.6694 are not comparable to DF-026 or production.

## Task 3: CI Thresholds

### Threshold Selection Rationale

Thresholds are set at **15% below baseline performance** to:
1. Allow for natural variance in model predictions (non-determinism in embeddings, dataset changes)
2. Catch genuine regressions (e.g., broken model loading, feature extraction bugs)
3. Avoid false-positive CI failures from minor fluctuations

### Implemented Thresholds (Corrected 2026-07-24, v1.1.0 + spaCy parity)

```python
# In ml/eval_harness/eval.py check_thresholds()

THRESHOLD_BASELINE_F1 = 0.78          # 15% below 0.9209 (unchanged)
THRESHOLD_EMBEDDINGS_F1 = 0.69        # 15% below 0.8163 (unchanged)
THRESHOLD_PRIORITY_PRECISION = 0.46   # 15% below 0.5372 (v1.1.0, spaCy-consistent)
THRESHOLD_PRIORITY_RECALL = 0.59      # 15% below 0.6955 (v1.1.0, spaCy-consistent)
```

**Threshold history**: Priority thresholds were first corrected after DF-026 integration (0.41 / 0.55 vs v1.0.0 baseline 0.4846 / 0.6428). They were recalibrated again after confirming v1.1.0 baselines with spaCy installed in both local and CI environments.

### Failure Behavior

When any threshold is not met:
- `check_thresholds()` returns `False`
- Script prints clear failure message identifying the failing model/metric
- Script exits with code 1 (CI failure)

**Example failure message**:
```
FAIL: category_baseline macro-F1 (0.7500) is below threshold 0.7800
Evaluation failed validation thresholds.
```

When all thresholds pass:
- `check_thresholds()` returns `True`
- Script prints success message with all metric values
- Script exits with code 0 (CI success)

### Documentation

Thresholds and baseline numbers are documented in:
1. **ml/eval_harness/README.md** - CI Thresholds section with full context
2. **ml/eval_harness/eval.py** - Inline docstring in `check_thresholds()` function
3. **This file** - Complete implementation summary

## Verification Summary

✅ **All 3 model wrappers implemented with real inference**  
✅ **All 15 tests pass (metrics + model smoke tests + spaCy env check)**  
✅ **Eval harness runs successfully with real predictions**  
✅ **Real confidence values across 0.0-1.0 range (not uniform/random)**  
✅ **CI thresholds set with clear rationale**  
✅ **Exit code 0 on threshold pass, exit code 1 on threshold fail**  
✅ **Documentation updated in README.md**

## Files Modified/Created

### Modified:
- `ml/eval_harness/models/category_baseline.py` - Real TF-IDF model wrapper
- `ml/eval_harness/models/category_embeddings.py` - Real embeddings model wrapper
- `ml/eval_harness/models/priority_model.py` - Real priority model wrapper with feature extraction
- `ml/eval_harness/eval.py` - Updated `check_thresholds()` with real thresholds
- `ml/eval_harness/README.md` - Added CI thresholds, model descriptions
- `ml/eval_harness/requirements.txt` - Added model dependencies

### Created:
- `ml/eval_harness/tests/test_models.py` - Smoke tests for all 3 models
- `ml/eval_harness/tests/test_spacy_environment.py` - Asserts en_core_web_sm is loaded for CI
- `ml/eval_harness/display_metrics.py` - Utility to display metrics in readable format
- `ml/eval_harness/REAL_MODEL_INTEGRATION_SUMMARY.md` - This summary document

## Dependencies Added

To `ml/eval_harness/requirements.txt`:
- joblib (model loading)
- scikit-learn (ML pipelines)
- pandas (DataFrame inputs)
- numpy (numerical operations)
- sentence-transformers (embeddings model)
- pydantic / pydantic-settings / vaderSentiment / yake / spacy (DF-026 feature extraction)
- en_core_web_sm wheel URL (spaCy English model — required for priority eval parity with production)
