class ReplyAssistError(Exception):
    """Base exception."""


class InvalidConversationError(ReplyAssistError):
    """Conversation is invalid."""


class PromptGenerationError(ReplyAssistError):
    """Prompt generation failed."""


class LLMError(ReplyAssistError):
    """LLM request failed."""