# Similar Tickets Module

## Overview

This module implements the AI pipeline for semantic ticket similarity in DeskFlow.

It is responsible for generating semantic embeddings that will be used to retrieve similar historical tickets and assist support agents.

## Current Implementation

### DF-031 – Embedding Pipeline

This implementation:

- Loads ticket data from the prepared dataset.
- Validates the input data.
- Generates semantic embeddings using Sentence Transformers.
- Produces deterministic embeddings for identical input.
- Stores embeddings with their corresponding ticket IDs.

## Project Structure

```
similar_tickets/
├── embeddings/
│   ├── config.py
│   ├── embedding_service.py
│   ├── exceptions.py
│   ├── pipeline.py
│   ├── loader/
│   ├── models/
│   └── writer.py
└── scripts/
    └── run_embedding_pipeline.py
```

## Future Work

This module will be extended through the following issues:

- DF-032 – Vector Index
- DF-033 – Similar Ticket Search API
- DF-034 – Evaluation
- DF-035 – Re-ranking

## Notes

This module is implemented as an offline preprocessing pipeline and does not expose any API endpoints.