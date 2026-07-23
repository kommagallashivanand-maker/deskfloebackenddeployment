from unittest.mock import Mock

from ..models import (
    Conversation,
    ReplySuggestion,
    Ticket,
)
from ..services.reply_service import ReplyService


def test_reply_service():

    generator = Mock()

    generator.generate.return_value = ReplySuggestion(
        ticket_id=1,
        summary="Summary",
        suggested_reply="Reply",
    )

    service = ReplyService(generator)

    conversation = Conversation(
        ticket=Ticket(
            ticket_id=1,
            title="Title",
            description="Description",
            category="Technical",
        )
    )

    result = service.generate_reply(conversation)

    assert result.ticket_id == 1

    assert result.summary == "Summary"

    assert result.suggested_reply == "Reply"