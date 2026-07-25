import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
ENV_PATH = BASE_DIR / ".env"

load_dotenv(dotenv_path=ENV_PATH)

OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")

OPENROUTER_MODEL = os.getenv(
    "OPENROUTER_MODEL",
    "nvidia/nemotron-3-nano-30b-a3b:free",
)

OPENROUTER_BASE_URL = os.getenv(
    "OPENROUTER_BASE_URL",
    "https://openrouter.ai/api/v1",
)

TEMPERATURE = float(
    os.getenv(
        "TEMPERATURE",
        "0.2",
    )
)

MAX_SUMMARY_WORDS = int(
    os.getenv(
        "MAX_SUMMARY_WORDS",
        "60",
    )
)

MAX_SUMMARY_BULLETS = int(
    os.getenv(
        "MAX_SUMMARY_BULLETS",
        "3",
    )
)
