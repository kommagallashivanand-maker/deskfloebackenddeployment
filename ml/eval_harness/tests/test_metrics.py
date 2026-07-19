from metrics import f1_per_class, precision_recall_per_class, confusion_matrix

def test_empty_results():
    """Verify that metrics return empty dictionaries for empty input."""
    assert f1_per_class([]) == {}
    assert precision_recall_per_class([]) == {}
    assert confusion_matrix([]) == {}


def test_perfect_predictions():
    """Verify metrics calculation for perfect predictions."""
    results = [
        ("Technical", "Technical", 0.95),
        ("Billing", "Billing", 0.90),
        ("Technical", "Technical", 0.85)
    ]
    
    # Expected F1: 1.0 for all classes
    f1 = f1_per_class(results)
    assert f1["Technical"] == 1.0
    assert f1["Billing"] == 1.0
    
    # Expected Precision/Recall: 1.0
    pr = precision_recall_per_class(results)
    assert pr["Technical"] == {"precision": 1.0, "recall": 1.0}
    assert pr["Billing"] == {"precision": 1.0, "recall": 1.0}
    
    # Expected Confusion Matrix
    cm = confusion_matrix(results)
    assert cm["Technical"]["Technical"] == 2
    assert cm["Technical"]["Billing"] == 0
    assert cm["Billing"]["Billing"] == 1
    assert cm["Billing"]["Technical"] == 0


def test_mixed_predictions():
    """Verify metrics calculation on a hand-constructed mixed results set."""
    # Class mapping:
    # Account Access: 1 sample (true is Account Access, predicted is Billing) -> FN=1, TP=0, FP=0
    # Billing: 2 samples (1 true-true, 1 true-Technical) -> TP=1, FN=1, FP=1 (from Account Access)
    # Technical: 2 samples (2 true-true) -> TP=2, FN=0, FP=1 (from Billing)
    results = [
        ("Billing", "Billing", 0.9),
        ("Billing", "Technical", 0.8),
        ("Technical", "Technical", 0.75),
        ("Technical", "Technical", 0.85),
        ("Account Access", "Billing", 0.6)
    ]
    
    # F1 scores
    f1 = f1_per_class(results)
    assert f1["Account Access"] == 0.0
    assert abs(f1["Billing"] - 0.5) < 1e-9
    assert abs(f1["Technical"] - 0.8) < 1e-9
    
    # Precision and Recall
    pr = precision_recall_per_class(results)
    assert pr["Account Access"] == {"precision": 0.0, "recall": 0.0}
    assert pr["Billing"] == {"precision": 0.5, "recall": 0.5}
    # Technical precision is 2 / (2 + 1) = 2/3, recall is 2 / 2 = 1.0
    assert abs(pr["Technical"]["precision"] - 2.0 / 3.0) < 1e-9
    assert pr["Technical"]["recall"] == 1.0
    
    # Confusion Matrix
    cm = confusion_matrix(results)
    assert cm["Account Access"]["Billing"] == 1
    assert cm["Account Access"]["Technical"] == 0
    assert cm["Account Access"]["Account Access"] == 0
    
    assert cm["Billing"]["Billing"] == 1
    assert cm["Billing"]["Technical"] == 1
    assert cm["Billing"]["Account Access"] == 0
    
    assert cm["Technical"]["Technical"] == 2
    assert cm["Technical"]["Billing"] == 0
    assert cm["Technical"]["Account Access"] == 0


def test_zero_division_cases():
    """Verify metrics handle extreme cases gracefully without division-by-zero errors."""
    # No predictions are correct, everything is misclassified
    results = [
        ("Billing", "Technical", 0.9),
        ("Technical", "Billing", 0.8)
    ]
    
    f1 = f1_per_class(results)
    pr = precision_recall_per_class(results)
    
    assert f1["Billing"] == 0.0
    assert f1["Technical"] == 0.0
    
    assert pr["Billing"] == {"precision": 0.0, "recall": 0.0}
    assert pr["Technical"] == {"precision": 0.0, "recall": 0.0}
