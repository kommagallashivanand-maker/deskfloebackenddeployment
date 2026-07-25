"""
API routes for Similar Tickets.
"""

from fastapi import APIRouter, HTTPException

from ml.similar_tickets.api.models import (
    SimilarTicket,
    SimilarTicketsResponse,
)
from ml.similar_tickets.services.similar_ticket_service import SimilarTicketService

router = APIRouter(
    prefix="/similar-tickets",
    tags=["Similar Tickets"],
)

service = SimilarTicketService()


@router.get(
    "/{ticket_id}",
    response_model=SimilarTicketsResponse,
)
def get_similar_tickets(
    ticket_id: str,
    limit: int = 5,
):
    try:
        results = service.get_similar_tickets(
            ticket_id=ticket_id,
            limit=limit,
        )

        similar_tickets = [
            SimilarTicket(
                ticket_id=row[0],
                similarity=row[2],
            )
            for row in results
            if row[0] != ticket_id
        ]

        return SimilarTicketsResponse(
            ticket_id=ticket_id,
            similar_tickets=similar_tickets,
        )

    except ValueError as exc:
        raise HTTPException(
            status_code=404,
            detail=str(exc),
        )

    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=str(exc),
        )