import re
from typing import Tuple
from app.core.config import settings

try:
    from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
    _analyzer = SentimentIntensityAnalyzer()
except Exception:
    _analyzer = None

# IT-domain negation/failure signals — each match shifts the compound score more negative.
# These are patterns VADER consistently misreads as neutral or positive in ticket context.
_NEGATIVE_SIGNALS = [
    r"\bcannot\b",
    r"\bcan'?t\b",
    r"\bnot\s+\w+ing\b",   # "not connecting", "not working", "not loading"
    r"\bnot\s+able\b",
    r"\bunable\b",
    r"\bfail(ed|ing|ure)?\b",
    r"\bdown\b",
    r"\bbroken\b",
    r"\berror\b",
    r"\bcrash(ed|ing)?\b",
    r"\bblocked?\b",
    r"\bno\s+access\b",
    r"\blost\s+access\b",
]

# Urgency amplifiers — these don't make sentiment negative on their own,
# but they amplify an already-negative signal.
_URGENCY_SIGNALS = [
    r"\burgent\b",
    r"\basap\b",
    r"\bimmediately\b",
    r"\bcritical\b",
    r"\bemergency\b",
    r"\bdeadline\b",
    r"\bmeeting\s+in\b",
    r"\bminutes?\b",
]

_NEGATIVE_PER_MATCH = -0.15   # score shift per negative signal found
_URGENCY_AMPLIFIER = -0.10    # additional shift per urgency signal when negatives present
_FLOOR = -1.0                 # never go below VADER's own floor


def _domain_adjust(text: str, compound: float) -> float:
    """Shift the VADER compound score based on IT-domain negative and urgency signals."""
    lower = text.lower()

    negative_hits = sum(
        1 for pattern in _NEGATIVE_SIGNALS if re.search(pattern, lower)
    )
    urgency_hits = sum(
        1 for pattern in _URGENCY_SIGNALS if re.search(pattern, lower)
    )

    if negative_hits == 0:
        return compound  # no domain signals — trust VADER as-is

    adjustment = (negative_hits * _NEGATIVE_PER_MATCH) + (urgency_hits * _URGENCY_AMPLIFIER)
    adjusted = compound + adjustment
    return max(adjusted, _FLOOR)


def analyze_sentiment(text: str) -> Tuple[float, str]:
    if not text:
        return 0.0, "Neutral"
    if _analyzer is None:
        return 0.0, "Neutral"

    scores = _analyzer.polarity_scores(text)
    compound = scores.get("compound", 0.0)
    compound = _domain_adjust(text, compound)

    if compound <= settings.sentiment_negative_thresh:
        label = "Negative"
    elif compound >= settings.sentiment_positive_thresh:
        label = "Positive"
    else:
        label = "Neutral"
    return round(float(compound), 4), label
