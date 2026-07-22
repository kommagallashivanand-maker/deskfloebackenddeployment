from app.feature_extraction.keyword_extractor import extract_keywords
from app.feature_extraction.sentiment_analyzer import analyze_sentiment


def test_keyword_simple():
    kws = extract_keywords("VPN not connecting", "I cannot connect to VPN since this morning", top=3)
    assert isinstance(kws, list)


def test_sentiment_positive_negative():
    score_pos, label_pos = analyze_sentiment("This is great, works perfectly")
    assert label_pos in ("Positive", "Neutral")
    score_neg, label_neg = analyze_sentiment("I hate this, it fails and is awful")
    assert label_neg in ("Negative", "Neutral")
