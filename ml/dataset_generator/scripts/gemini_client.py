import os
import logging
from dotenv import load_dotenv
from google import genai
from google.genai import types
from google.genai.errors import APIError
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# Load environment variables
load_dotenv()

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("gemini_client")

# Initialize Gemini Client
api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise ValueError("GEMINI_API_KEY environment variable is not set.")

client = genai.Client(api_key=api_key)
MODEL_NAME = os.getenv("MODEL_NAME", "models/gemini-3.1-flash-lite")


@retry(
    retry=retry_if_exception_type((APIError, Exception)),
    stop=stop_after_attempt(5),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    reraise=True,
    before_sleep=lambda retry_state: logger.warning(
        f"Gemini API call failed (Attempt {retry_state.attempt_number}). "
        f"Retrying in {retry_state.next_action.sleep:.2f}s..."
    )
)
def generate(prompt: str) -> str:
    logger.debug(f"Calling Gemini API with model: {MODEL_NAME}")
    response = client.models.generate_content(
        model=MODEL_NAME,
        contents=prompt,
        config=types.GenerateContentConfig(
            response_mime_type="application/json"
        )
    )

    if not response.text:
        raise ValueError("Received empty response from Gemini API.")

    return response.text
