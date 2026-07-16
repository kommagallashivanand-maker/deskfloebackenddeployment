"""
Ticket Domain Model

Represents a support ticket used by the embedding pipeline.
"""

from dataclasses import dataclass

@dataclass(slots=True)
class Ticket:
    id: str
    title: str
    description: str

    @property
    def text(self) -> str:
        return f"{self.title.strip()}\n{self.description.strip()}"