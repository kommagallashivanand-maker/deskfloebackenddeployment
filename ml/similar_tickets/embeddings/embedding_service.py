"""
Embedding Service

Responsible for generating embeddings from text using
Sentence Transformers.
"""

import logging
from typing import List

from sentence_transformers import SentenceTransformer
from embeddings.config import (
    MODEL_NAME,
    DEVICE,
)
from embeddings.models.embedding_result import EmbeddingResult
from embeddings.models.ticket import Ticket

logger = logging.getLogger(__name__)


class EmbeddingService:
    def __init__(self):
        logger.info("Loading embedding model: %s", MODEL_NAME)
        self.model = SentenceTransformer(
            MODEL_NAME,
            device=DEVICE,
        )

    def generate_embedding(self, text: str) -> List[float]:
        embedding = self.model.encode(
            text,
            convert_to_numpy=True,
            normalize_embeddings=True,
        )
        return embedding.tolist()

    def generate_ticket_embedding(self,ticket: Ticket,) -> EmbeddingResult:
        embedding = self.generate_embedding(ticket.text)
        return EmbeddingResult(
            ticket_id=ticket.id,
            embedding=embedding,
        )

    def generate_batch(self,tickets: List[Ticket],) -> List[EmbeddingResult]:
        logger.info(
            "Generating embeddings for %d tickets...",
            len(tickets),
        )
        texts = [ticket.text for ticket in tickets]
        embeddings = self.model.encode(
            texts,
            batch_size=32,
            convert_to_numpy=True,
            normalize_embeddings=True,
        )
        results = []
        for ticket, embedding in zip(tickets,embeddings,):
            results.append(
                EmbeddingResult(
                    ticket_id=ticket.id,
                    embedding=embedding.tolist(),
                )
            )

        logger.info("Embedding generation completed.")

        return results