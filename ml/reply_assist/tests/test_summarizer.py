from unittest.mock import Mock

from ..models import (
    Conversation,
    Ticket,
)
from ..summarizer.summarizer_service import (
    SummarizerService,
)


def test_summarizer_returns_string():

    llm = Mock()

    llm.generate.return_value = "- Summary"

    ticket = Ticket(
        ticket_id=1,
        title="Title",
        description="Description",
        category="Technical",
    )

    conversation = Conversation(ticket=ticket)

    service = SummarizerService(llm)

    result = service.summarize(conversation)

    assert result == "- Summary"