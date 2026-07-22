"""
Embedding Result Model

Represents the embedding generated for a ticket.
"""

from dataclasses import dataclass
from typing import List

@dataclass(slots=True)
class EmbeddingResult:
    ticket_id: str
    embedding: List[float]