from .prompt_builder import ReplyPromptBuilder
from ..llm_client import LLMClient
from ..models import (
    Conversation,
    ReplySuggestion,
)
from ..summarizer.summarizer_service import SummarizerService


class ReplyGenerator:
    """
    Generates a reply suggestion for a support ticket.
    """

    def __init__(
        self,
        llm_client: LLMClient,
        summarizer: SummarizerService,
    ):
        self._llm = llm_client
        self._summarizer = summarizer

    def generate(
        self,
        conversation: Conversation,
    ) -> ReplySuggestion:
        """
        Generate a conversation summary and suggested reply.

        Args:
            conversation: Ticket and conversation history.

        Returns:
            ReplySuggestion
        """

        summary = self._summarizer.summarize(conversation)

        prompt = ReplyPromptBuilder.build(
            conversation=conversation,
            conversation_summary=summary,
        )

        suggested_reply = self._llm.generate(prompt)

        return ReplySuggestion(
            ticket_id=conversation.ticket.ticket_id,
            summary=summary,
            suggested_reply=suggested_reply,
        )