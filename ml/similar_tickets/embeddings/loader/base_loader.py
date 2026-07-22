from abc import ABC, abstractmethod
from typing import List
from embeddings.models.ticket import Ticket

class BaseLoader(ABC):
    """Abstract base class for ticket loaders."""

    @abstractmethod
    def load_tickets(self) -> List[Ticket]:
        """
        Load and return a list of tickets.

        Returns:
            List[Ticket]: A list of validated Ticket objects.
        """
        pass
