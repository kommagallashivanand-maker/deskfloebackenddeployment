def f1_per_class(results: list[tuple[str, str, float]]) -> dict[str, float]:
    """
    Computes the F1 score for each class.
    
    Args:
        results: A list of (true_label, predicted_label, confidence) tuples.
        
    Returns:
        A dictionary mapping class labels to their F1 score.
    """
    if not results:
        return {}
        
    # Get all unique classes seen in either true or predicted labels
    classes = sorted(list(set(t for t, p, _ in results) | set(p for t, p, _ in results)))
    
    f1_scores = {}
    for cls in classes:
        tp = sum(1 for t, p, _ in results if t == cls and p == cls)
        fp = sum(1 for t, p, _ in results if t != cls and p == cls)
        fn = sum(1 for t, p, _ in results if t == cls and p != cls)
        
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        
        f1 = (2 * precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0
        f1_scores[cls] = f1
        
    return f1_scores


def precision_recall_per_class(results: list[tuple[str, str, float]]) -> dict[str, dict[str, float]]:
    """
    Computes the precision and recall for each class.
    
    Args:
        results: A list of (true_label, predicted_label, confidence) tuples.
        
    Returns:
        A dictionary mapping class labels to a dict containing 'precision' and 'recall'.
    """
    if not results:
        return {}
        
    classes = sorted(list(set(t for t, p, _ in results) | set(p for t, p, _ in results)))
    
    pr_scores = {}
    for cls in classes:
        tp = sum(1 for t, p, _ in results if t == cls and p == cls)
        fp = sum(1 for t, p, _ in results if t != cls and p == cls)
        fn = sum(1 for t, p, _ in results if t == cls and p != cls)
        
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        
        pr_scores[cls] = {
            "precision": precision,
            "recall": recall
        }
        
    return pr_scores


def confusion_matrix(results: list[tuple[str, str, float]]) -> dict[str, dict[str, int]]:
    """
    Computes the confusion matrix.
    
    Args:
        results: A list of (true_label, predicted_label, confidence) tuples.
        
    Returns:
        A nested dictionary of true_label -> predicted_label -> count.
    """
    if not results:
        return {}
        
    classes = sorted(list(set(t for t, p, _ in results) | set(p for t, p, _ in results)))
    
    # Initialize the matrix with all combinations set to 0
    matrix = {t: {p: 0 for p in classes} for t in classes}
    
    for t, p, _ in results:
        matrix[t][p] += 1
        
    return matrix
