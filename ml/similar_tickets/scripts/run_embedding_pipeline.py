"""
Development runner for the Embedding Pipeline.

This script is intended for local development and testing.
"""

from itertools import count
import logging
from ml.similar_tickets.embeddings.pipeline import EmbeddingPipeline

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)


def main() -> None:
    pipeline = EmbeddingPipeline()
    count = pipeline.run()
    print(f"\nSuccessfully indexed {count} tickets.")


if __name__ == "__main__":
    main()