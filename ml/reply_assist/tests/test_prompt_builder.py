from ..generator.prompt_builder import (
    ReplyPromptBuilder,
)
from ..models import (
    Conversation,
    Ticket,
)


def test_prompt_contains_ticket_information():

    ticket = Ticket(
        ticket_id=1,
        title="Cannot Login",
        description="Password reset failed.",
        category="Account Access",
    )

    conversation = Conversation(ticket=ticket)

    prompt = ReplyPromptBuilder.build(
        conversation,
        "- Summary",
    )

    assert "Cannot Login" in prompt

    assert "Password reset failed." in prompt

    assert "- Summary" in prompt