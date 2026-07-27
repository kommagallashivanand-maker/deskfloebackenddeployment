"""
Embedding service.

Responsible only for generating embeddings.
"""

from sentence_transformers import SentenceTransformer

from ml.similar_tickets.config import MODEL_NAME, NORMALIZE_EMBEDDINGS


class EmbeddingService:
    """Embedding generation service."""

    def __init__(self):
        self.model = SentenceTransformer(MODEL_NAME)

    def generate_embedding(self, text: str):
        """
        Generate an embedding for a text.
        """

        return self.model.encode(
            text,
            normalize_embeddings=NORMALIZE_EMBEDDINGS,
        )