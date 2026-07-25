# Model Comparison Report — DF-023

## Summary

| Metric | Baseline (TF-IDF + LR) | Embedding (MiniLM + LR) | Delta |
| :--- | :---: | :---: | :---: |
| **Accuracy** | 0.7687 (76.87%) | 0.7761 (77.61%) | +0.0075 |
| **Macro F1** | 0.7636 | 0.7720 | +0.0084 |
| **Weighted F1** | 0.7642 | 0.7725 | +0.0084 |
| **Macro Precision** | 0.7664 | 0.7770 | — |
| **Macro Recall** | 0.7680 | 0.7754 | — |

> Both models were evaluated on the **same** 402-sample test set (stratified 80/20 split, `random_state=42`).

---

## Winner: Embedding Classifier

The **Embedding Classifier** achieves the higher Macro-F1 score.

- Macro-F1 improvement: **+0.0084** (+1.10%)
- Test set size: **402 samples** across 10 categories.

---

## Per-Class F1 Comparison

| Category | Baseline F1 | Embedding F1 | Delta | Winner |
| :--- | :---: | :---: | :---: | :---: |
| Account Access | 0.8372 | 0.8506 | +0.0134 | Embedding |
| Billing | 0.6500 | 0.6250 | -0.0250 | Baseline |
| Data Management | 0.7586 | 0.7816 | +0.0230 | Embedding |
| Feature Request | 0.9524 | 0.9639 | +0.0115 | Embedding |
| General Inquiry | 0.7949 | 0.8052 | +0.0103 | Embedding |
| Notifications | 0.9176 | 0.9268 | +0.0092 | Embedding |
| Performance | 0.8095 | 0.8046 | -0.0049 | Baseline |
| Security | 0.8056 | 0.7945 | -0.0110 | Baseline |
| Subscription | 0.5385 | 0.5500 | +0.0115 | Embedding |
| Technical | 0.5714 | 0.6176 | +0.0462 | Embedding |

---

## Strengths and Weaknesses

### Baseline — TF-IDF + Logistic Regression

**Strengths**
- Fast training and inference — no GPU required.
- Entirely self-contained in scikit-learn; minimal dependencies.
- Works well on high-frequency, distinctive vocabulary categories
  (e.g. *Feature Request*, *Notifications*).

**Weaknesses**
- Sparse, bag-of-words representation — loses semantic meaning.
- Struggles with categories that share vocabulary
  (e.g. *Subscription* vs *Billing*, *Technical* vs *Performance*).
- Low-frequency or out-of-vocabulary terms are invisible.

### Embedding Classifier — SentenceTransformer (all-MiniLM-L6-v2) + Logistic Regression

**Strengths**
- Dense semantic embeddings capture meaning beyond surface vocabulary.
- Handles paraphrase, synonyms, and varied phrasing naturally.
- Significantly improves on semantically ambiguous categories.
- Pre-trained on large corpora — generalises better to unseen phrasing.

**Weaknesses**
- Slower inference due to transformer encoding step.
- Larger dependency footprint (sentence-transformers, torch).
- Embeddings are computed outside of a sklearn Pipeline, requiring a
  separate encoding step at prediction time.

---

## Recommendation

**Adopt the Embedding Classifier (`embedding_v1`)** as the production model.
It achieves a Macro-F1 of **0.7720** vs the baseline's
**0.7636**, a relative improvement of **1.10%**.
The improvement is consistent across the majority of categories, particularly
those that were challenging for the TF-IDF approach.

---

*Report generated automatically by `compare.py` (DF-023)*
