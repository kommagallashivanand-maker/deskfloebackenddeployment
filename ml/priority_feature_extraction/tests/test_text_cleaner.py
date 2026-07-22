import pytest
from app.utils.text_cleaner import clean


def test_clean_normal_input():
    assert clean("Hello World") == "hello world"


def test_clean_empty_string():
    assert clean("") == ""


def test_clean_none_input():
    # If the user passes None (despite type hint), it should handle it gracefully
    assert clean(None) == ""


def test_clean_newlines():
    text = "Line 1\nLine 2\r\nLine 3"
    assert clean(text) == "line 1 line 2 line 3"


def test_clean_special_characters():
    text = "hello @world #tag $100%! / [brackets]"
    assert clean(text) == "hello world tag 100 brackets"


def test_clean_preserves_hyphens_and_apostrophes():
    text = "don't log-out, it's a test-case."
    assert clean(text) == "don't log-out it's a test-case"


def test_clean_multiple_spaces():
    text = "   too    many     spaces   "
    assert clean(text) == "too many spaces"
