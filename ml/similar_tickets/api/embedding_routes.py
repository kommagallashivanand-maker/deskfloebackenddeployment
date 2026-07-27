"""
API routes for Ticket Embedding.
"""

from fastapi import APIRouter, HTTPException

from ml.similar_tickets.services.similar_ticket_service import (
    SimilarTicketService,
)

router = APIRouter(
    prefix="/embeddings",
    tags=["Embeddings"],
)

service = SimilarTicketService()


@router.post(
    "/index/{ticket_id}",
)
def index_ticket(
    ticket_id: str,
):
    """
    Generate and store an embedding for a ticket.
    """

    try:
        service.index_ticket(
            ticket_id=ticket_id,
        )

        return {
            "status": "success",
            "message": "Ticket indexed successfully.",
            "ticket_id": ticket_id,
        }

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