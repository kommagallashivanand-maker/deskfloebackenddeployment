from __future__ import annotations

import json
import logging
import pathlib
import re
from typing import List, Optional, Sequence

from app.core.config import settings
from app.core.logging import get_logger
from .preprocessing import normalize_text

logger = get_logger(__name__)

# ---------------------------------------------------------------------------
# Domain acronym list — loaded from config-driven JSON file at startup.
# Protects lowercase short tokens (e.g. "ip", "ad", "sso") from being
# dropped by the len <= 2 filter in _is_valid_token.
# Uppercase tokens (e.g. "AWS", "TLS") are handled automatically by the
# isupper() heuristic and do not need to be in this list.
# ---------------------------------------------------------------------------

_ACRONYMS_FALLBACK = {
    "vpn", "mfa", "sql", "api", "dns", "cpu", "ram",
    "outlook", "active directory", "ad", "sso", "ip",
}


def _load_domain_acronyms() -> set:
    """Load domain acronyms from the configured JSON file.

    Falls back to the built-in set if the file is missing or malformed,
    so the service always starts successfully.
    """
    path = pathlib.Path(settings.domain_acronyms_path)
    if not path.is_absolute():
        # Resolve relative to the package root (two levels up from this file)
        path = pathlib.Path(__file__).resolve().parent.parent.parent / path
    try:
        with path.open("r", encoding="utf-8") as fh:
            data = json.load(fh)
        if not isinstance(data, list):
            raise ValueError("domain_acronyms.json must contain a JSON array")
        acronyms = {str(item).strip().lower() for item in data}
        logger.info("Loaded %d domain acronyms from %s", len(acronyms), path)
        return acronyms
    except FileNotFoundError:
        logger.warning(
            "domain_acronyms.json not found at '%s'. Using built-in fallback list.", path
        )
    except Exception:
        logger.exception(
            "Failed to load domain_acronyms.json from '%s'. Using built-in fallback list.", path
        )
    return _ACRONYMS_FALLBACK


ACRONYMS: set = _load_domain_acronyms()

STOP_TOKENS = {
    "the", "and", "for", "with", "from", "that", "this", "a", "an", "of", "in", "on",
    # negation/filler words that produce noisy bigrams
    "not", "no", "nor", "never", "cannot", "cant", "it", "its", "is", "are", "was",
    "be", "been", "have", "has", "had", "do", "does", "did", "will", "would",
    "could", "should", "i", "my", "me", "we", "our", "you", "your",
    # time/duration noise words that rarely signal priority
    "morning", "afternoon", "evening", "night", "today", "yesterday", "tomorrow",
    "minute", "minutes", "hour", "hours", "day", "days", "week", "weeks",
    "since", "time", "moment",
}

try:
    import yake
    YAKE_EXTRACTOR = yake.KeywordExtractor(lan="en", n=settings.yake_ngram_max)
except Exception as exc:
    logger.exception("Failed to initialize YAKE KeywordExtractor")
    YAKE_EXTRACTOR = None

try:
    import spacy
    NLP = spacy.load("en_core_web_sm", exclude=["textcat"])
except Exception as exc:
    logger.warning(
        "spaCy model 'en_core_web_sm' not found. Keyword extraction will fall back to "
        "non-spaCy methods (YAKE/basic bigrams). To install it, run: "
        "python -m spacy download en_core_web_sm"
    )
    NLP = None


def _normalize_candidate(candidate: str) -> str:
    """Normalize a candidate for output and comparison."""
    candidate = candidate.strip().lower()
    candidate = re.sub(r"\s+", " ", candidate)
    return candidate


def _candidate_key(candidate: str) -> str:
    """Create a deterministic comparison key for deduplication."""
    return _normalize_candidate(candidate).lower()


def _is_valid_token(token_text: str) -> bool:
    """Return true for tokens that are meaningful and not too short.

    Short-token rules (len <= 2):
    - Uppercase tokens (e.g. "AWS", "IP") pass automatically via the
      isupper() heuristic — no list entry needed.
    - Lowercase tokens pass only if present in the config-driven ACRONYMS
      set (e.g. "ip", "ad", "sso").
    """
    if not token_text:
        return False
    normalized = token_text.strip().lower()
    if normalized in STOP_TOKENS:
        return False
    if len(normalized) <= 2:
        # Let all-uppercase tokens through automatically (e.g. "IP", "AD")
        if token_text.strip().isupper():
            return True
        # For lowercase short tokens, require an explicit entry in ACRONYMS
        if normalized not in ACRONYMS:
            return False
    return True


def _extract_subject_bigrams(text: str) -> List[str]:
    """Build meaningful bigrams directly from subject tokens for short subject lines."""
    if not text:
        return []
    tokens = [t for t in text.split() if _is_valid_token(t)]
    bigrams = []
    seen: set[str] = set()
    for i in range(len(tokens) - 1):
        bigram = f"{tokens[i]} {tokens[i + 1]}"
        key = _candidate_key(bigram)
        if key not in seen:
            seen.add(key)
            bigrams.append(_normalize_candidate(bigram))
    return bigrams


def _extract_spacy_candidates(text: str, limit: int) -> List[str]:
    """Extract keyword candidates from text using spaCy linguistic signals."""
    if not NLP or not text:
        return []

    doc = NLP(text)
    candidates: List[str] = []
    seen: set[str] = set()

    def add_candidate(candidate: str) -> None:
        normalized = _candidate_key(candidate)
        if not normalized or normalized in seen:
            return
        seen.add(normalized)
        candidates.append(_normalize_candidate(candidate))

    for ent in doc.ents:
        if _is_valid_token(ent.text):
            add_candidate(ent.text)
            if len(candidates) >= limit:
                return candidates[:limit]

    for chunk in doc.noun_chunks:
        if _is_valid_token(chunk.text):
            add_candidate(chunk.text)
            if len(candidates) >= limit:
                return candidates[:limit]

    for token in doc:
        if token.is_stop or token.is_punct or token.like_num:
            continue
        if token.pos_ not in {"NOUN", "PROPN", "ADJ"}:
            continue
        text_token = token.text.strip()
        if not _is_valid_token(text_token):
            continue
        candidate = token.lemma_ if token.lemma_ and token.lemma_.lower() != text_token.lower() else text_token
        add_candidate(candidate)
        if len(candidates) >= limit:
            break

    return candidates[:limit]


def _extract_yake_candidates(text: str, limit: int) -> List[str]:
    """Extract keyword candidates from YAKE."""
    if YAKE_EXTRACTOR is None or not text:
        return []

    try:
        results = YAKE_EXTRACTOR.extract_keywords(text)
    except Exception as exc:
        logger.exception("YAKE extraction failed")
        return []

    candidates: List[str] = []
    seen: set[str] = set()
    for phrase, score in results:
        if not _is_valid_token(phrase):
            continue
        normalized = _candidate_key(phrase)
        if normalized in seen:
            continue
        seen.add(normalized)
        candidates.append(_normalize_candidate(phrase))
        if len(candidates) >= limit:
            break
    return candidates


def _merge_candidates(*candidate_lists: Sequence[List[str]], top: int = 3) -> List[str]:
    """Merge keyword candidate lists while preserving order and uniqueness."""
    merged: List[str] = []
    seen: set[str] = set()
    for candidate_list in candidate_lists:
        for candidate in candidate_list:
            normalized = _candidate_key(candidate)
            if normalized in seen:
                continue
            seen.add(normalized)
            merged.append(candidate)
            if len(merged) >= top:
                return merged
    return merged


def extract_keywords(subject: str, description: str, top: int = None) -> List[str]:
    """Extract the top keywords from subject and description."""
    limit = top or settings.yake_top
    subject_text = normalize_text(subject or "")
    description_text = normalize_text(description or "")
    combined_text = " ".join([subject_text, description_text]).strip()

    if not combined_text:
        return []

    # Subject unigrams from spaCy (highest priority — subject is the most signal-dense)
    subject_candidates = _extract_spacy_candidates(subject_text, limit * 2)
    # Explicit bigrams built from subject tokens (e.g. "vpn connection" from "vpn not connecting")
    subject_bigrams = _extract_subject_bigrams(subject_text)
    # Description candidates for urgency/context signals (e.g. "urgent", "client meeting")
    description_candidates = _extract_spacy_candidates(description_text, limit * 2)
    # YAKE over full combined text for statistical phrase scoring
    yake_candidates = _extract_yake_candidates(combined_text, limit * 3)

    return _merge_candidates(
        subject_candidates,
        subject_bigrams,
        description_candidates,
        yake_candidates,
        top=limit,
    )
