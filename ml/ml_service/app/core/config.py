from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parents[2]
ENV_FILE = BASE_DIR / ".env"


class Settings(BaseSettings):
    SERVICE_NAME: str
    SERVICE_VERSION: str

    HOST: str
    PORT: int

    LOG_LEVEL: str

    model_config = SettingsConfigDict(
        env_file=ENV_FILE if ENV_FILE.exists() else None,
        extra="ignore",
    )


settings = Settings()