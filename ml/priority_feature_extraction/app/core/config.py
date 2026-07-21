from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "priority_feature_extractor"
    yake_top: int = 6
    yake_ngram_max: int = 2
    sentiment_negative_thresh: float = -0.1
    sentiment_positive_thresh: float = 0.25
    category_mapping_path: str = "sample_data/category_mapping.json"
    use_category_mapping: bool = False
    domain_acronyms_path: str = "sample_data/domain_acronyms.json"

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
    )


settings = Settings()
