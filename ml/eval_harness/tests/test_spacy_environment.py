"""
Ensure eval/CI environments load the spaCy model used by DF-026 keyword extraction.

Without en_core_web_sm, keyword_extractor silently falls back to YAKE/bigrams only,
which diverges from production and invalidates priority_model eval metrics.
"""

import os
import sys
from pathlib import Path

_repo_root = Path(__file__).resolve().parent.parent.parent.parent
if str(_repo_root) not in sys.path:
    sys.path.insert(0, str(_repo_root))

from ml.common.paths import get_priority_feature_extraction_dir

_feature_dir = get_priority_feature_extraction_dir()
if str(_feature_dir) not in sys.path:
    sys.path.insert(0, str(_feature_dir))

from app.feature_extraction import keyword_extractor


def test_spacy_model_loaded_for_keyword_extraction():
    assert keyword_extractor.NLP is not None, (
        "spaCy model 'en_core_web_sm' is not loaded. Install it via "
        "ml/eval_harness/requirements.txt or run: python -m spacy download en_core_web_sm"
    )
