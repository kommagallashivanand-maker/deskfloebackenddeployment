import requests

from reply_assist.config import (
    OPENROUTER_API_KEY,
    OPENROUTER_BASE_URL,
    OPENROUTER_MODEL,
    TEMPERATURE,
)
from reply_assist.exceptions import LLMError
from reply_assist.llm_client import LLMClient


class OpenRouterClient(LLMClient):

    def generate(
        self,
        prompt: str,
    ) -> str:

        headers = {
            "Authorization": f"Bearer {OPENROUTER_API_KEY}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": OPENROUTER_MODEL,
            "messages": [
                {
                    "role": "user",
                    "content": prompt,
                }
            ],
            "temperature": TEMPERATURE,
        }

        response = requests.post(
            f"{OPENROUTER_BASE_URL}/chat/completions",
            headers=headers,
            json=payload,
            timeout=60,
        )

        if response.status_code != 200:
            raise LLMError(response.text)

        data = response.json()

        return data["choices"][0]["message"]["content"]