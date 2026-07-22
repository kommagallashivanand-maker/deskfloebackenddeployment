"""
Custom Exceptions for Embedding Pipeline
"""

class DatasetValidationError(Exception):
    """Raised when the dataset fails validation."""
    pass

class EmbeddingModelError(Exception):
    """Raised when the embedding model encounters an error."""
    pass

class OutputWriteError(Exception):
    """Raised when failing to write output files."""
    pass
