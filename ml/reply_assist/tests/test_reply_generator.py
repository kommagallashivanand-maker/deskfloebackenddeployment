from unittest.mock import Mock

from ..generator.reply_generator import ReplyGenerator
from ..models import (
    Conversation,
    ReplySuggestion,
    Ticket,
)


class MockSummarizer:

    def summarize(self, conversation):
        return "- Summary"


def test_reply_generator():

    llm = Mock()

    llm.generate.return_value = "Suggested Reply"

    generator = ReplyGenerator(
        llm,
        MockSummarizer(),
    )

    ticket = Ticket(
        ticket_id=1,
        title="Title",
        description="Description",
        category="Technical",
    )

    conversation = Conversation(ticket=ticket)

    result = generator.generate(conversation)

    assert isinstance(result, ReplySuggestion)

    assert result.ticket_id == 1

    assert result.summary == "- Summary"

    assert result.suggested_reply == "Suggested Reply"