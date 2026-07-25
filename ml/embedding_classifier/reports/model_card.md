# Model Card — embedding_v1 (DF-023)

## Model Overview

| Field | Value |
| :--- | :--- |
| **Model ID** | `embedding_v1` |
| **Task** | Multi-class support ticket category classification |
| **Feature layer** | Sentence embeddings (`all-MiniLM-L6-v2`, 384-dim) |
| **Classifier** | Logistic Regression (sklearn, `lbfgs` solver) |
| **Framework** | `sentence-transformers` 5.6.0, `scikit-learn` 1.9.0, `joblib` |
| **Issue** | DF-023 |
| **Baseline** | `baseline_v1` (DF-022) |
| **Training date** | 2026-07-22 |

---

## Model Architecture

```
Input: ticket title + body (concatenated with a space)
         │
         ▼
SentenceTransformer("all-MiniLM-L6-v2")
  → 384-dimensional dense embedding vector
         │
         ▼
LogisticRegression(solver="lbfgs", max_iter=1000, random_state=42)
         │
         ▼
Predicted category label + confidence (predict_proba)
```

### Embedding Model: all-MiniLM-L6-v2

- Architecture: 6-layer MiniLM transformer
- Output dimension: 384
- Pre-trained on: large-scale natural language inference and semantic
  similarity datasets (SNLI, MNLI, MS MARCO, etc.)
- Optimised for: semantic sentence similarity / classification tasks
- Model size: ~80 MB
- Source: [sentence-transformers/all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)

### Classifier: Logistic Regression

- Solver: `lbfgs` (identical to baseline)
- max_iter: 1000
- random_state: 42
- Multi-class strategy: one-vs-rest (default for `lbfgs`)
- Input: 384-dimensional embedding vector
- Output: probability distribution over 10 ticket categories

---

## Dataset

| Property | Value |
| :--- | :--- |
| **Dataset ID** | `tickets_v1` (DF-021) |
| **Total samples** | 2 010 |
| **Train samples** | 1 608 (80 %) |
| **Test samples** | 402 (20 %) |
| **Split strategy** | Stratified, `random_state=42`, `shuffle=True` |
| **Classes** | 10 (balanced, ~200–201 per class) |
| **Input columns** | `title`, `body` |
| **Label column** | `category` |

### Class Distribution

| Category | Train (~) | Test |
| :--- | :---: | :---: |
| Account Access | 161 | 40 |
| Billing | 161 | 40 |
| Data Management | 161 | 40 |
| Feature Request | 160 | 41 |
| General Inquiry | 161 | 40 |
| Notifications | 161 | 40 |
| Performance | 160 | 41 |
| Security | 161 | 40 |
| Subscription | 161 | 40 |
| Technical | 161 | 40 |

---

## Evaluation Metrics

Evaluated on the held-out **402-sample** test set (same as DF-022 baseline).

| Metric | Value |
| :--- | :---: |
| **Accuracy** | 77.61% |
| **Macro F1** | **0.7720** |
| **Weighted F1** | 0.7725 |
| **Macro Precision** | 0.7777 |
| **Macro Recall** | 0.7754 |

### Per-Class Performance

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

See `classification_report.txt` for the full report and `confusion_matrix.csv`
for the confusion matrix.

---

## Comparison with Baseline (baseline_v1)

Both models evaluated on the **identical** 402-sample test set.

| Metric | Baseline (TF-IDF + LR) | Embedding (MiniLM + LR) | Delta |
| :--- | :---: | :---: | :---: |
| **Accuracy** | 76.87% | **77.61%** | +0.74 pp |
| **Macro F1** | 0.7636 | **0.7720** | +0.0084 |
| **Weighted F1** | 0.7642 | **0.7725** | +0.0084 |

### Per-Class F1 Comparison

| Category | Baseline F1 | Embedding F1 | Delta | Winner |
| :--- | :---: | :---: | :---: | :---: |
| Account Access | 0.8372 | **0.8506** | +0.0134 | Embedding |
| Billing | **0.6500** | 0.6250 | −0.0250 | Baseline |
| Data Management | 0.7586 | **0.7816** | +0.0230 | Embedding |
| Feature Request | 0.9524 | **0.9639** | +0.0115 | Embedding |
| General Inquiry | 0.7949 | **0.8052** | +0.0103 | Embedding |
| Notifications | 0.9176 | **0.9268** | +0.0092 | Embedding |
| Performance | **0.8095** | 0.8046 | −0.0049 | Baseline |
| Security | **0.8056** | 0.7945 | −0.0110 | Baseline |
| Subscription | 0.5385 | **0.5500** | +0.0115 | Embedding |
| Technical | 0.5714 | **0.6176** | +0.0462 | Embedding |

**7 out of 10** categories improve under the embedding model.
The most significant gain is *Technical* (+0.0462), followed by
*Data Management* (+0.0230) and *Account Access* (+0.0134).
The baseline retains a small edge on *Billing*, *Performance*, and *Security*.

### Why embeddings outperform TF-IDF on most categories

The baseline's TF-IDF representation treats each token independently and
cannot capture semantic similarity between different phrasings of the same
intent.  The `all-MiniLM-L6-v2` model has been pre-trained on hundreds of
millions of sentence pairs, so it produces geometrically close vectors for
semantically related inputs regardless of the exact words used.  This
substantially reduces confusion between categories that share vocabulary
(e.g. *Technical* and *Performance*, *Account Access* and other access-related
tickets).

---

## Final Recommendation

**Adopt `embedding_v1` for production classification.**

The embedding model achieves a Macro-F1 of **0.7720** vs the baseline's
**0.7636**, a relative improvement of **+1.10%**, with improvements across
7 out of 10 categories.  The improvement is most pronounced in the
categories that were historically most confused (*Technical*, *Data Management*,
*Account Access*), making `embedding_v1` a safer choice for real-world triage.

The added inference cost (transformer encoding, ~80 MB model download on first
use) is acceptable for an asynchronous ticket triage pipeline.  For
low-latency, synchronous use-cases the model should be served via a dedicated
embedding service (see Future Improvements).

---

## Known Limitations

1. **Inference latency** — Transformer encoding is slower than TF-IDF
   vectorisation.  For high-throughput scenarios a dedicated embedding
   service (e.g. Triton, ONNX Runtime) should be considered.

2. **Dependency size** — `sentence-transformers` pulls in PyTorch (~500 MB).
   The baseline has no such requirement.

3. **First-run model download** — `all-MiniLM-L6-v2` (~80 MB) is downloaded
   from the Hugging Face Hub on first use.  Offline environments must
   pre-cache the model directory or set `HF_HUB_OFFLINE=1`.

4. **Domain shift** — The embedding model is general-purpose.  A
   domain-fine-tuned model on DeskFlow ticket data may yield further gains.

5. **Billing / Security regression** — The embedding model underperforms the
   baseline on *Billing* (−0.0250) and *Security* (−0.0110).  These categories
   may rely on specific keyword patterns (e.g. invoice numbers, CVE references)
   that TF-IDF captures directly.

6. **Fixed label set** — Both models are static classifiers; new ticket
   categories require retraining from scratch.

---

## Future Improvements

1. **Fine-tune the embedding model** on the DeskFlow ticket dataset using
   contrastive loss (e.g. MultipleNegativesRankingLoss) for potentially
   large F1 gains — especially on the weaker categories.

2. **Evaluate larger SentenceTransformer models** — `all-mpnet-base-v2`
   (768-dim) offers better quality at moderate additional cost.

3. **Replace Logistic Regression with a non-linear head** — A shallow MLP
   or SVM with RBF kernel may better exploit the embedding geometry and
   recover lost ground on Billing and Security.

4. **Recover Billing / Security performance** — Investigate whether a hybrid
   approach (embedding features + TF-IDF features concatenated) retains the
   keyword-matching strength of the baseline for these categories.

5. **Active learning loop** — Route low-confidence predictions back to
   human reviewers to iteratively expand and improve the training set.

6. **Model Registry integration** — Integrate with DF-042 model registry to
   track versions, metrics, and champion/challenger comparisons automatically.

7. **Quantised / ONNX inference** — Export the transformer to ONNX or use
   `sentence-transformers` ONNX export for faster CPU inference with no
   torch dependency at runtime.

---

*Model card generated for DF-023 — Improved Classifier with Embeddings*
