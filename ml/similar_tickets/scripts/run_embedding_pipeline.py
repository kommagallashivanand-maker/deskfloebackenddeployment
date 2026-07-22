"""
Development runner for the Embedding Pipeline.

This script is intended for local development and testing.
"""

import logging
from ml.similar_tickets.embeddings.pipeline import EmbeddingPipeline

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)


def main() -> None:
    pipeline = EmbeddingPipeline()
    results = pipeline.run()
    print(f"\nSuccessfully generated {len(results)} embeddings.")


if __name__ == "__main__":
    main()