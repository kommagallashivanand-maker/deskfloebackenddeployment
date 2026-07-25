from ..generator.reply_generator import ReplyGenerator
from ..models import (
    Conversation,
    ReplySuggestion,
)


class ReplyService:
    """
    Facade for the Reply Assist module.
    """

    def __init__(
        self,
        reply_generator: ReplyGenerator,
    ):
        self._reply_generator = reply_generator

    def generate_reply(
        self,
        conversation: Conversation,
    ) -> ReplySuggestion:
        """
        Generate a reply suggestion.

        Args:
            conversation: Ticket and conversation history.

        Returns:
            ReplySuggestion
        """

        return self._reply_generator.generate(conversation)