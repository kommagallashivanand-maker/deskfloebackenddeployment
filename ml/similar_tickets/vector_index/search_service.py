"""
Search Service

Provides a simple interface for finding similar tickets
using the FAISS index.
"""

from __future__ import annotations

from typing import List

from .faiss_manager import FaissManager


class SearchService:
    """
    Service responsible for performing similarity search.

    This class wraps FaissManager so the rest of the application
    doesn't interact with FAISS directly.
    """

    def __init__(self, manager: FaissManager) -> None:
        self.manager = manager

    def search_similar(
        self,
        query_embedding: List[float],
        top_k: int = 5,
    ) -> List[dict]:
        """
        Search for the most similar tickets.

        Args:
            query_embedding:
                Embedding of the query ticket.

            top_k:
                Number of similar tickets to return.

        Returns:
            List containing ticket_id and similarity score.
        """

        return self.manager.search(
            query_embedding=query_embedding,
            top_k=top_k,
        )