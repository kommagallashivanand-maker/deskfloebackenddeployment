"""
Business logic for Similar Tickets.
"""

from ml.similar_tickets.config import MODEL_NAME
from ml.similar_tickets.embeddings.embedding_service import EmbeddingService
from ml.similar_tickets.repository.embedding_repository import EmbeddingRepository
from ml.similar_tickets.services.reranker import SimilarTicketReranker
from ml.similar_tickets.services.cache import SimilarTicketCache


class SimilarTicketService:
    """
    Coordinates embedding generation and similarity search.
    """

    def __init__(self):
        self.embedding_service = EmbeddingService()
        self.repository = EmbeddingRepository()
        self.reranker = SimilarTicketReranker()
        self.cache = SimilarTicketCache()

    def index_ticket(
        self,
        ticket_id: str,
    ):
        """
        Generate and store an embedding for a ticket.
        """

        ticket = self.repository.get_ticket(ticket_id)

        if ticket is None:
            raise ValueError(
                f"Ticket '{ticket_id}' not found."
            )

        text = f"{ticket['title']}\n{ticket['description']}"

        embedding = self.embedding_service.generate_embedding(
            text
        )

        self.repository.upsert_embedding(
            ticket_id=ticket_id,
            embedding=embedding,
            model_name=MODEL_NAME,
        )

        self.cache.invalidate(
            ticket_id
        )

    def get_similar_tickets(
        self,
        ticket_id: str,
        limit: int = 5,
    ):
        """
        Retrieve similar tickets.
        """

        cache_key = (ticket_id, limit)

        cached_result = self.cache.get(
            cache_key
        )

        if cached_result is not None:
            return cached_result

        ticket = self.repository.get_ticket(
            ticket_id
        )

        if ticket is None:
            raise ValueError(
                f"Ticket '{ticket_id}' not found."
            )

        embedding = self.repository.get_embedding(
            ticket_id
        )

        if embedding is None:
            raise ValueError(
                f"No embedding found for ticket '{ticket_id}'."
            )

        candidates = self.repository.search_similar(
            ticket_id=ticket_id,
            embedding=embedding,
            limit=20,
        )

        reranked = self.reranker.rerank(
            query_category_id=ticket["category_id"],
            candidates=candidates,
        )

        result = reranked[:limit]

        self.cache.set(
            cache_key,
            result,
        )

        return result

    def delete_ticket_embedding(
        self,
        ticket_id: str,
    ):
        """
        Delete a stored embedding.
        """

        self.repository.delete_embedding(
            ticket_id
        )

        self.cache.invalidate(
            ticket_id
        )