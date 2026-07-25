"""
Smoke tests for real model wrappers.

These tests verify that each model loads successfully and produces valid
predictions on sample tickets. They do not test accuracy.
"""

import sys
import os

# Add parent directory to path to import models
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from models import category_baseline, category_embeddings, priority_model


# Sample test tickets
SAMPLE_TICKETS = [
    {
        "subject": "Cannot access my account",
        "description": "I've been trying to log in but keep getting an error message.",
        "category": "Account Access",
        "requester_role": "user"
    },
    {
        "subject": "Billing question about invoice",
        "description": "I was charged twice this month and need a refund.",
        "category": "Billing",
        "requester_role": "user"
    },
    {
        "subject": "Urgent: VPN not connecting",
        "description": "VPN connection fails immediately. I have a client meeting in 30 minutes and need this fixed ASAP.",
        "category": "Technical",
        "requester_role": "admin"
    },
]

# Expected category labels
EXPECTED_CATEGORIES = {
    "Account Access",
    "Billing",
    "Technical",
    "Feature Request",
    "Subscription",
    "Performance",
    "Security",
    "Notifications",
    "Data Management",
    "General Inquiry"
}

# Expected priority labels
EXPECTED_PRIORITIES = {"Low", "Medium", "High", "Urgent"}


class TestCategoryBaseline:
    """Tests for category_baseline model wrapper."""
    
    def test_predict_returns_valid_label_and_confidence(self):
        """Verify predict() returns a valid category and confidence in [0, 1]."""
        for ticket in SAMPLE_TICKETS:
            label, confidence = category_baseline.predict(
                subject=ticket["subject"],
                description=ticket["description"]
            )
            
            assert isinstance(label, str), f"Label should be string, got {type(label)}"
            assert label in EXPECTED_CATEGORIES, f"Invalid category label: {label}"
            assert isinstance(confidence, (int, float)), f"Confidence should be numeric, got {type(confidence)}"
            assert 0.0 <= confidence <= 1.0, f"Confidence {confidence} out of range [0, 1]"
    
    def test_predict_handles_empty_inputs(self):
        """Verify predict() handles empty subject/description gracefully."""
        label, confidence = category_baseline.predict(subject="", description="")
        assert isinstance(label, str)
        assert label in EXPECTED_CATEGORIES
        assert 0.0 <= confidence <= 1.0
    
    def test_predict_handles_none_inputs(self):
        """Verify predict() handles None subject/description gracefully."""
        label, confidence = category_baseline.predict(subject=None, description=None)
        assert isinstance(label, str)
        assert label in EXPECTED_CATEGORIES
        assert 0.0 <= confidence <= 1.0


class TestCategoryEmbeddings:
    """Tests for category_embeddings model wrapper."""
    
    def test_predict_returns_valid_label_and_confidence(self):
        """Verify predict() returns a valid category and confidence in [0, 1]."""
        for ticket in SAMPLE_TICKETS:
            label, confidence = category_embeddings.predict(
                subject=ticket["subject"],
                description=ticket["description"]
            )
            
            assert isinstance(label, str), f"Label should be string, got {type(label)}"
            assert label in EXPECTED_CATEGORIES, f"Invalid category label: {label}"
            assert isinstance(confidence, (int, float)), f"Confidence should be numeric, got {type(confidence)}"
            assert 0.0 <= confidence <= 1.0, f"Confidence {confidence} out of range [0, 1]"
    
    def test_predict_handles_empty_inputs(self):
        """Verify predict() handles empty subject/description gracefully."""
        label, confidence = category_embeddings.predict(subject="", description="")
        assert isinstance(label, str)
        assert label in EXPECTED_CATEGORIES
        assert 0.0 <= confidence <= 1.0
    
    def test_predict_handles_none_inputs(self):
        """Verify predict() handles None subject/description gracefully."""
        label, confidence = category_embeddings.predict(subject=None, description=None)
        assert isinstance(label, str)
        assert label in EXPECTED_CATEGORIES
        assert 0.0 <= confidence <= 1.0


class TestPriorityModel:
    """Tests for priority_model wrapper."""
    
    def test_predict_returns_valid_label_and_confidence(self):
        """Verify predict() returns a valid priority and confidence in [0, 1]."""
        for ticket in SAMPLE_TICKETS:
            label, confidence = priority_model.predict(
                subject=ticket["subject"],
                description=ticket["description"],
                category=ticket["category"],
                requester_role=ticket["requester_role"]
            )
            
            assert isinstance(label, str), f"Label should be string, got {type(label)}"
            assert label in EXPECTED_PRIORITIES, f"Invalid priority label: {label}"
            assert isinstance(confidence, (int, float)), f"Confidence should be numeric, got {type(confidence)}"
            assert 0.0 <= confidence <= 1.0, f"Confidence {confidence} out of range [0, 1]"
    
    def test_predict_without_optional_fields(self):
        """Verify predict() works when category/requester_role are None."""
        label, confidence = priority_model.predict(
            subject="Test subject",
            description="Test description",
            category=None,
            requester_role=None
        )
        assert isinstance(label, str)
        assert label in EXPECTED_PRIORITIES
        assert 0.0 <= confidence <= 1.0
    
    def test_predict_handles_empty_inputs(self):
        """Verify predict() handles empty subject/description gracefully."""
        label, confidence = priority_model.predict(
            subject="",
            description="",
            category="Technical",
            requester_role="user"
        )
        assert isinstance(label, str)
        assert label in EXPECTED_PRIORITIES
        assert 0.0 <= confidence <= 1.0
    
    def test_predict_handles_none_inputs(self):
        """Verify predict() handles None subject/description gracefully."""
        label, confidence = priority_model.predict(
            subject=None,
            description=None,
            category="Technical",
            requester_role="user"
        )
        assert isinstance(label, str)
        assert label in EXPECTED_PRIORITIES
        assert 0.0 <= confidence <= 1.0
