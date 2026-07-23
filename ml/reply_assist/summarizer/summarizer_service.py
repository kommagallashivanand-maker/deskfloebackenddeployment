from ..models import Conversation
from ..exceptions import InvalidConversationError
from .prompt_builder import SummaryPromptBuilder
from ..llm_client import LLMClient


class SummarizerService:
    """
    Generates a concise summary of a ticket conversation.
    """

    def __init__(self, llm_client: LLMClient):
        self._llm = llm_client

    def summarize(self, conversation: Conversation) -> str:
        """
        Generate a conversation summary.

        Returns:
            str: Summary in <=3 bullet points.
        """

        self._validate(conversation)

        cleaned = self._prepare(conversation)

        prompt = SummaryPromptBuilder.build(cleaned)

        return self._llm.generate(prompt)

    def _validate(self, conversation: Conversation) -> None:
        """
        Validate conversation input.
        """

        if conversation is None:
            raise InvalidConversationError("Conversation cannot be None.")

        if conversation.ticket is None:
            raise InvalidConversationError("Ticket is required.")

        if not conversation.ticket.title.strip():
            raise InvalidConversationError("Ticket title cannot be empty.")

        if not conversation.ticket.description.strip():
            raise InvalidConversationError(
                "Ticket description cannot be empty."
            )

    def _prepare(self, conversation: Conversation) -> Conversation:
        """
        Clean and normalize conversation before summarization.
        """

        conversation.comments = self._sort_comments(
            conversation.comments
        )

        conversation.comments = self._remove_empty_comments(
            conversation.comments
        )

        conversation.comments = self._remove_duplicate_comments(
            conversation.comments
        )

        return conversation

    @staticmethod
    def _sort_comments(comments):
        return sorted(
            comments,
            key=lambda comment: comment.created_at,
        )

    @staticmethod
    def _remove_empty_comments(comments):

        cleaned = []

        for comment in comments:

            if comment.message.strip():
                cleaned.append(comment)

        return cleaned

    @staticmethod
    def _remove_duplicate_comments(comments):

        unique = []
        seen = set()

        for comment in comments:

            key = (
                comment.author.lower(),
                comment.message.strip().lower(),
            )

            if key not in seen:
                seen.add(key)
                unique.append(comment)

        return unique