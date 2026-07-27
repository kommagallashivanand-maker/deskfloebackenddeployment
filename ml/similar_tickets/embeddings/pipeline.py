"""
Embedding Pipeline

Coordinates the embedding generation workflow.
"""

import logging

from .loader.csv_loader import CsvLoader
from  ml.similar_tickets.services.similar_ticket_service import SimilarTicketService

logger = logging.getLogger(__name__)


class EmbeddingPipeline:
    """Coordinates the embedding indexing workflow."""

    def __init__(self):
        self.loader = CsvLoader()
        self.similar_ticket_service = SimilarTicketService()

    def run(self) -> int:
        """
        Load tickets and index their embeddings into PostgreSQL.
        """

        logger.info("Starting embedding pipeline...")

        # 1. Load dataset
        tickets = self.loader.load_tickets()

        # 2. Ensure deterministic ordering
        tickets.sort(key=lambda ticket: ticket.id)

        indexed_count = 0

        # 3. Generate and store embeddings
        for ticket in tickets:
            self.similar_ticket_service.index_ticket(
                ticket_id=ticket.id,
                text=ticket.text,
            )
            indexed_count += 1

        logger.info(
            "Embedding pipeline completed successfully. Indexed %d tickets.",
            indexed_count,
        )

        return indexed_count