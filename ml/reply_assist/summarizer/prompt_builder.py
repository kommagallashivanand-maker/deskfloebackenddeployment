from ..models import Conversation
from ..config import (
    MAX_SUMMARY_BULLETS,
    MAX_SUMMARY_WORDS,
)


class SummaryPromptBuilder:

    @staticmethod
    def build(conversation: Conversation) -> str:

        if conversation.comments:
            thread = "\n".join(
                f"{comment.author}: {comment.message}"
                for comment in conversation.comments
            )
        else:
            thread = "No conversation yet."

        return f"""
You are a customer support conversation summarizer.

Rules:

- Produce at most {MAX_SUMMARY_BULLETS} bullet points.
- Keep the summary under {MAX_SUMMARY_WORDS} words.
- Do not invent information.
- Preserve only important context.
- If there are no comments, summarize the original ticket.

Ticket Title:
{conversation.ticket.title}

Ticket Description:
{conversation.ticket.description}

Conversation:

{thread}

Return only bullet points.
""".strip()