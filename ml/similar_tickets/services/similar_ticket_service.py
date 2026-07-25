"""
Business logic for Similar Tickets.
"""

from ml.similar_tickets.config import MODEL_NAME
from ml.similar_tickets.embeddings.embedding_service import EmbeddingService
from ml.similar_tickets.repository.embedding_repository import EmbeddingRepository


class SimilarTicketService:
    """Coordinates embedding generation and similarity search."""

    def __init__(self):
        self.embedding_service = EmbeddingService()
        self.repository = EmbeddingRepository()

    def index_ticket(
        self,
        ticket_id: str,
        text: str,
    ):
        """
        Generate and store an embedding.
        """

        embedding = self.embedding_service.generate_embedding(text)

        self.repository.upsert_embedding(
            ticket_id=ticket_id,
            embedding=embedding,
            model_name=MODEL_NAME,
        )

    def get_similar_tickets(
        self,
        ticket_id: str,
        limit: int = 5,
    ):
        """
        Retrieve similar tickets.
        """

        embedding = self.repository.get_embedding(ticket_id)

        if embedding is None:
            raise ValueError(
                f"No embedding found for ticket '{ticket_id}'."
            )

        return self.repository.search_similar(
            embedding,
            limit,
        )

    def delete_ticket_embedding(
        self,
        ticket_id: str,
    ):
        self.repository.delete_embedding(ticket_id)