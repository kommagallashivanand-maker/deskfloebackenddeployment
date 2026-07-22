"""
Embedding Pipeline

Coordinates the embedding generation workflow.
"""

import logging
from typing import List

from .config import MODEL_NAME, NORMALIZE_EMBEDDINGS, INPUT_CSV_PATH
from .embedding_service import EmbeddingService
from .loader.csv_loader import CsvLoader
from .models.embedding_result import EmbeddingResult
from .writer import EmbeddingWriter

logger = logging.getLogger(__name__)


class EmbeddingPipeline:
    def __init__(self):
        self.loader = CsvLoader()
        self.embedding_service = EmbeddingService()
        self.writer = EmbeddingWriter()

    def run(self) -> List[EmbeddingResult]:
        logger.info("Starting embedding pipeline...")
        
        # 1. Load Dataset
        tickets = self.loader.load_tickets()
        
        # 2. Enforce Deterministic Order (sort by ticket_id)
        tickets.sort(key=lambda t: t.id)
        
        # 3. Generate Embeddings
        results = self.embedding_service.generate_batch(tickets)
        
        # 4. Write Embeddings
        self.writer.save_embeddings(results)
        
        # 5. Write Metadata
        metadata = {
            "model": MODEL_NAME,
            "dimension": len(results[0].embedding) if results else 0,
            "ticket_count": len(results),
            "normalized": NORMALIZE_EMBEDDINGS,
            "dataset": INPUT_CSV_PATH.name
        }
        self.writer.save_metadata(metadata)
        
        logger.info("Embedding pipeline completed successfully.")

        return results