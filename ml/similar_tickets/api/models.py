"""
Pydantic models for Similar Tickets API.
"""

from pydantic import BaseModel


class SimilarTicket(BaseModel):
    ticket_id: str
    title: str
    similarity: float


class SimilarTicketsResponse(BaseModel):
    ticket_id: str
    similar_tickets: list[SimilarTicket]