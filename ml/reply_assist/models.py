from dataclasses import dataclass, field
from datetime import datetime


@dataclass(slots=True)
class Ticket:
    ticket_id: int
    title: str
    description: str
    category: str


@dataclass(slots=True)
class Comment:
    comment_id: int
    author: str
    message: str
    created_at: datetime


@dataclass(slots=True)
class ReplySuggestion:
    ticket_id: int
    summary: str
    suggested_reply: str


@dataclass(slots=True)
class Conversation:
    ticket: Ticket
    comments: list[Comment] = field(default_factory=list)