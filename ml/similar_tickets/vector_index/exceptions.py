"""
Custom exceptions for the vector index.
"""


class IndexNotBuiltError(RuntimeError):
    """Raised when the FAISS index has not been built."""