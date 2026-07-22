from app.models.ticket import TicketIn, AuthContext
from app.models.features import FeatureOut, Sentiment
from .keyword_extractor import extract_keywords
from .sentiment_analyzer import analyze_sentiment
from .category_feature import normalize_category
from .requester_role import normalize_role


def extract_features(ticket: TicketIn, auth: AuthContext, top_k: int = None) -> FeatureOut:
    top_k = top_k or None
    keywords = extract_keywords(ticket.subject or "", ticket.description or "", top=top_k)
    text = f"{ticket.subject or ''} {ticket.description or ''}".strip()
    score, label = analyze_sentiment(text)
    category = normalize_category(ticket.category)
    requester_role = normalize_role(auth.role)
    sentiment = Sentiment(label=label, score=score)
    description_length = len(ticket.description or "")
    return FeatureOut(
        keywords=", ".join(keywords),
        category=category,
        requester_role=requester_role,
        sentiment=sentiment,
        description_length=description_length,
    )
