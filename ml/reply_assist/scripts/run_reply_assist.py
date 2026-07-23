from datetime import datetime

from reply_assist.generator.reply_generator import ReplyGenerator
from reply_assist.models import (
    Comment,
    Conversation,
    Ticket,
)
from reply_assist.providers.openrouter_client import OpenRouterClient
from reply_assist.services.reply_service import ReplyService
from reply_assist.summarizer.summarizer_service import SummarizerService


def main():

    ticket = Ticket(
        ticket_id=1,
        title="Cannot login",
        description="Password reset isn't working.",
        category="Account Access",
    )

    comments = [
        Comment(
            comment_id=1,
            author="Customer",
            message="I already tried clearing cookies.",
            created_at=datetime.now(),
        ),
        Comment(
            comment_id=2,
            author="Agent",
            message="Can you try another browser?",
            created_at=datetime.now(),
        ),
    ]

    conversation = Conversation(
        ticket=ticket,
        comments=comments,
    )

    from reply_assist.providers import OpenRouterClient

    llm = OpenRouterClient()

    summarizer = SummarizerService(llm)

    generator = ReplyGenerator(
        llm,
        summarizer,
    )

    service = ReplyService(generator)

    reply = service.generate_reply(conversation)

    print("\nConversation Summary\n")
    print(reply.summary)

    print("\nSuggested Reply\n")
    print(reply.suggested_reply)


if __name__ == "__main__":
    main()