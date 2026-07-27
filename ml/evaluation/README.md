# Similar Ticket Retrieval Evaluation

This module evaluates the quality of the Similar Ticket Retrieval system using **Precision@5**. It measures how effectively the retrieval pipeline returns relevant tickets for a given query ticket by comparing the retrieved results against a manually reviewed judgment set.

---

# Project Structure

```
evaluation/
│
├── data/
│   ├── tickets.csv
│   └── judgment_set.csv
│
├── results/
│   ├── results.csv
│   └── evaluation_report.md
│
├── models.py
├── retrieval.py
├── metrics.py
├── report.py
├── evaluate.py
├── generate_judgment_set.py
├── requirements.txt
└── README.md
```

---

# Evaluation Workflow

1. Load the ticket dataset.
2. Generate sentence embeddings using Sentence Transformers.
3. Build a FAISS similarity index.
4. Retrieve the Top-5 most similar tickets for each query.
5. Compare retrieved results against the judgment set.
6. Compute Precision@5 for every query.
7. Generate a detailed evaluation report.

---

# Judgment Set Creation

For each query ticket, the retrieval system generated the Top-10 most semantically similar tickets using sentence embeddings and FAISS similarity search. I manually reviewed the retrieved candidates and selected the Top-5 most relevant tickets for each query. These manually verified relevance judgments were compiled into the judgment set and used as the reference dataset for evaluating retrieval performance.

---

# Evaluation Metric

The evaluation uses **Precision@5**.

Precision@5 measures the proportion of relevant tickets among the top five retrieved results.

```
Precision@5 = Relevant Retrieved Tickets / 5
```

Example:

Expected Relevant Tickets

```
T1 T2 T3 T4 T5
```

Retrieved Tickets

```
T1 T2 T3 T6 T7
```

Precision@5

```
3 / 5 = 0.60
```

---

# Dataset

### tickets.csv

Contains all support tickets.

Required columns:

| Column | Description |
|---------|-------------|
| ticket_id | Unique ticket identifier |
| title | Ticket title |
| body | Ticket description |
| category | Ticket category |

---

### judgment_set.csv

Contains manually reviewed relevance judgments.

| Column | Description |
|---------|-------------|
| query_ticket_id | Query ticket |
| relevant_ticket_ids | Comma-separated list of five relevant tickets |

Example

```
query_ticket_id,relevant_ticket_ids
TKT-00001,TKT-00124,TKT-00087,TKT-00045,TKT-00155,TKT-00098
```

---

# Installation

```bash
pip install -r requirements.txt
```

---

# Generate Judgment Set

```bash
python generate_judgment_set.py
```

---

# Run Evaluation

```bash
python evaluate.py
```

---

# Output

### results.csv

Contains evaluation results for every query.

Columns:

- query_ticket_id
- retrieved_ticket_ids
- expected_ticket_ids
- precision_at_5

---

### evaluation_report.md

Contains

- Overall evaluation summary
- Average Precision@5
- Best performing query
- Lowest performing query
- Query-wise retrieval comparison
- Partial retrieval analysis

---

# Sample Evaluation Results

```
Queries Evaluated : 50
Average Precision@5 : 0.9040
Perfect Retrievals : 28
Partial Matches : 22
```

---

# Technologies Used

- Python
- Sentence Transformers
- FAISS
- Pandas
- NumPy

---

# Acceptance Criteria

- Generates semantic embeddings for all tickets.
- Retrieves Top-5 similar tickets using FAISS.
- Evaluates retrieval quality using Precision@5.
- Produces detailed CSV results.
- Generates a Markdown evaluation report.
- Provides a reproducible evaluation workflow.