from abc import ABC
from abc import abstractmethod


class LLMClient(ABC):

    @abstractmethod
    def generate(
        self,
        prompt: str,
    ) -> str:
        """
        Generate a response from the configured LLM.
        """
        raise NotImplementedError