from typing import List, Literal
from pydantic import BaseModel


class Sentiment(BaseModel):
    label: Literal["Negative", "Neutral", "Positive"]
    score: float


class FeatureOut(BaseModel):
    keywords: List[str]
    category: str
    requester_role: str
    sentiment: Sentiment
    description_length: int
