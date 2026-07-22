from app.feature_extraction.sentiment_analyzer import analyze_sentiment


def test_sentiment_neutral_for_empty():
    score, label = analyze_sentiment("")
    assert label == "Neutral"
    assert score == 0.0
