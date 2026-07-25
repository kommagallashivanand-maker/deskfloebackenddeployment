"""
loader.py — DF-042

Loads model artifacts through the registry.

Responsibilities
----------------
- Accept a model family name (e.g. 'category').
- Consult the ModelRegistry to determine the active version.
- Resolve the artifact path.
- Load the artifact using joblib.
- Return a unified ModelHandle that normalises the two artifact formats
  (sklearn_pipeline and embedding_artefact) behind a single predict() interface.

Usage
-----
    from registry.loader import load_model

    model = load_model("category")
    result = model.predict("Cannot login", "Password reset does not work")
"""

import json
import logging
from pathlib import Path
from typing import Any

import joblib

from registry.registry import ModelRegistry, RegistryError

logger = logging.getLogger(__name__)


class ModelHandle:
    """
    A thin wrapper that normalises both artifact formats behind a single
    predict(title, body) interface.

    Parameters
    ----------
    family:
        Model family name (e.g. 'category').
    version:
        Loaded version key (e.g. 'embedding_v1').
    artifact_type:
        Either 'sklearn_pipeline' or 'embedding_artefact'.
    artifact:
        The raw loaded object from joblib.
    metadata:
        Optional dict loaded from the companion metadata JSON file.
    """

    def __init__(
        self,
        family: str,
        version: str,
        artifact_type: str,
        artifact: Any,
        metadata: dict | None = None,
    ) -> None:
        self.family = family
        self.version = version
        self.artifact_type = artifact_type
        self._artifact = artifact
        self.metadata = metadata or {}

        # SentenceTransformer is loaded lazily on first predict() call.
        # Eager loading at startup causes torch DLL import which may be
        # blocked by OS-level Application Control policies.
        self._sentence_model = None

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _load_sentence_model(model_name: str):
        """Lazy-load the SentenceTransformer encoder."""
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise ImportError(
                "sentence-transformers is required for embedding models. "
                "Install with: pip install sentence-transformers"
            ) from exc
        logger.info("Loading SentenceTransformer: %s", model_name)
        return SentenceTransformer(model_name)

    def _predict_sklearn_pipeline(self, title: str, body: str) -> dict:
        """Run inference through a standard sklearn Pipeline."""
        import pandas as pd

        X = pd.DataFrame([{"title": title, "body": body}])
        pipeline = self._artifact

        category = pipeline.predict(X)[0]

        confidence = None
        if hasattr(pipeline, "predict_proba"):
            probs = pipeline.predict_proba(X)[0]
            clf = pipeline.named_steps.get("clf") or pipeline.steps[-1][1]
            classes = list(clf.classes_)
            if category in classes:
                confidence = float(probs[classes.index(category)])

        return {"category": category, "confidence": confidence}

    def _predict_embedding_artefact(self, title: str, body: str) -> dict:
        """Run inference through the embedding artefact dict."""
        import numpy as np

        # Lazy-load the SentenceTransformer on first prediction call
        if self._sentence_model is None:
            self._sentence_model = self._load_sentence_model(
                self._artifact["embedding_model_name"]
            )

        clf = self._artifact["classifier"]
        classes: list = self._artifact["classes"]

        combined_text = str(title).strip() + " " + str(body).strip()
        embedding = self._sentence_model.encode(
            [combined_text], convert_to_numpy=True
        )

        category: str = clf.predict(embedding)[0]

        confidence = None
        if hasattr(clf, "predict_proba"):
            probs = clf.predict_proba(embedding)[0]
            if category in list(clf.classes_):
                confidence = float(probs[list(clf.classes_).index(category)])

        return {"category": category, "confidence": confidence}

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def predict(self, title: str, body: str) -> dict:
        """
        Predict the ticket category for the given title and body.

        Parameters
        ----------
        title:
            Ticket title text.
        body:
            Ticket body text.

        Returns
        -------
        dict with keys:
            - ``category`` (str): Predicted label.
            - ``confidence`` (float | None): Prediction probability.
        """
        if self.artifact_type == "sklearn_pipeline":
            return self._predict_sklearn_pipeline(title, body)
        if self.artifact_type == "embedding_artefact":
            return self._predict_embedding_artefact(title, body)
        raise RegistryError(
            f"Unknown artifact type '{self.artifact_type}' for "
            f"{self.family}/{self.version}. "
            "Supported types: sklearn_pipeline, embedding_artefact."
        )

    def __repr__(self) -> str:
        return (
            f"ModelHandle(family={self.family!r}, version={self.version!r}, "
            f"type={self.artifact_type!r})"
        )


# ---------------------------------------------------------------------------
# Module-level registry instance (shared across the application)
# ---------------------------------------------------------------------------

_registry: ModelRegistry | None = None
_model_cache: dict[str, ModelHandle] = {}


def get_registry() -> ModelRegistry:
    """Return the shared ModelRegistry instance, initialising it if needed."""
    global _registry
    if _registry is None:
        _registry = ModelRegistry()
    return _registry


def load_model(family: str, version: str | None = None) -> ModelHandle:
    """
    Load a model through the registry.

    If ``version`` is None, the active version from registry.json is used.
    Loaded models are cached in memory so repeated calls within the same
    process do not re-deserialise the artifact from disk.

    Parameters
    ----------
    family:
        Model family name (e.g. 'category', 'priority').
    version:
        Explicit version key.  If omitted, the active version is used.

    Returns
    -------
    ModelHandle
        A normalised handle with a predict() method.

    Raises
    ------
    RegistryError:
        If the family or version is not registered, or the artifact is missing.
    """
    registry = get_registry()

    resolved_version = version or registry.get_active_version(family)
    cache_key = f"{family}/{resolved_version}"

    if cache_key in _model_cache:
        logger.debug("Cache hit for %s", cache_key)
        return _model_cache[cache_key]

    logger.info("Loading model %s", cache_key)

    artifact_path = registry.resolve_artifact_path(family, resolved_version)
    artifact_type = registry.get_artifact_type(family, resolved_version)

    try:
        # For sklearn_pipeline artifacts the pickle may reference local
        # utility modules (e.g. baseline_classifier/utils.py).  Add the
        # classifier root directory (parent of models/) to sys.path before
        # loading so that pickle can resolve those modules.
        import sys
        classifier_root = str(artifact_path.parent.parent)
        if classifier_root not in sys.path:
            sys.path.insert(0, classifier_root)

        artifact = joblib.load(artifact_path)
    except Exception as exc:
        raise RegistryError(
            f"Failed to load artifact for '{cache_key}': {exc}"
        ) from exc

    # Load optional metadata JSON
    metadata: dict | None = None
    meta_path = registry.resolve_metadata_path(family, resolved_version)
    if meta_path and meta_path.exists():
        with open(meta_path, encoding="utf-8") as f:
            metadata = json.load(f)

    handle = ModelHandle(
        family=family,
        version=resolved_version,
        artifact_type=artifact_type,
        artifact=artifact,
        metadata=metadata,
    )

    _model_cache[cache_key] = handle
    logger.info("Loaded and cached %s", handle)
    return handle


def preload_active_models() -> None:
    """
    Pre-load all active models into the in-memory cache.

    Called once on application startup so the first request is not
    penalised by model deserialisation time.
    """
    registry = get_registry()
    for family in registry.families:
        try:
            load_model(family)
        except RegistryError as exc:
            logger.error("Failed to preload model for family '%s': %s", family, exc)
