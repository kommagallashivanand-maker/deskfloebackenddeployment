"""Loads model artifacts through the registry."""

import json
import logging
from pathlib import Path
from typing import Any

import joblib

from ml.ml_service.registry.registry import ModelRegistry, RegistryError

logger = logging.getLogger(__name__)


def _get_priority_feature_extractor():
    """Import extract_features, TicketIn, AuthContext from ml/priority_feature_extraction.

    Uses ml.common.paths to resolve the directory safely without namespace collision.
    """
    import sys
    from ml.common.paths import get_priority_feature_extraction_dir

    pfe_dir = str(get_priority_feature_extraction_dir())
    if pfe_dir not in sys.path:
        sys.path.insert(0, pfe_dir)

    try:
        from app.models.ticket import TicketIn, AuthContext
        from app.feature_extraction.feature_extractor import extract_features
        return extract_features, TicketIn, AuthContext
    except ImportError as exc:
        raise ImportError(
            f"Failed to import feature extractor from '{pfe_dir}': {exc}"
        ) from exc


class ModelHandle:
    """Wrapper normalizing model artifact formats under a predict interface."""

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

    def _build_category_input(self, title: str, body: str):
        """Build standard title/body DataFrame for category models."""
        import pandas as pd
        return pd.DataFrame([{"title": title or "", "body": body or ""}])

    def _build_priority_input(
        self,
        title: str = "",
        body: str = "",
        category: str | None = None,
        requester_role: str | None = None,
        subject: str | None = None,
        description: str | None = None,
    ):
        """Build flat 6-column DataFrame for priority_model using feature extraction.

        Expected columns matching train_priority_model/train.py:
        keywords, category, sentiment_label, requester_role, sentiment_score, description_length
        """
        import pandas as pd

        extract_features, TicketIn, AuthContext = _get_priority_feature_extractor()

        sub_text = subject if subject is not None else title
        desc_text = description if description is not None else body

        ticket = TicketIn(
            subject=sub_text or "",
            description=desc_text or "",
            category=category,
        )
        auth = AuthContext(
            user_id="ml_service",
            role=requester_role,
        )

        feature_output = extract_features(ticket, auth)

        flat_dict = {
            "keywords": feature_output.keywords,
            "category": feature_output.category,
            "sentiment_label": feature_output.sentiment.label,
            "requester_role": feature_output.requester_role,
            "sentiment_score": feature_output.sentiment.score,
            "description_length": feature_output.description_length,
        }

        return pd.DataFrame([flat_dict])[
            [
                "keywords",
                "category",
                "sentiment_label",
                "requester_role",
                "sentiment_score",
                "description_length",
            ]
        ]

    def _predict_sklearn_pipeline(
        self,
        title: str = "",
        body: str = "",
        category: str | None = None,
        requester_role: str | None = None,
        subject: str | None = None,
        description: str | None = None,
    ) -> dict:
        """Run inference through a standard sklearn Pipeline."""
        pipeline = self._artifact

        if self.family == "priority":
            X = self._build_priority_input(
                title=title,
                body=body,
                category=category,
                requester_role=requester_role,
                subject=subject,
                description=description,
            )
            prediction = pipeline.predict(X)[0]

            confidence = None
            if hasattr(pipeline, "predict_proba"):
                probs = pipeline.predict_proba(X)[0]
                classes = list(pipeline.classes_)
                if prediction in classes:
                    confidence = float(probs[classes.index(prediction)])

            return {"priority": prediction, "confidence": confidence}
        else:
            X = self._build_category_input(title=title, body=body)
            prediction = pipeline.predict(X)[0]

            confidence = None
            if hasattr(pipeline, "predict_proba"):
                probs = pipeline.predict_proba(X)[0]
                clf = pipeline.named_steps.get("clf") or pipeline.steps[-1][1]
                classes = list(clf.classes_)
                if prediction in classes:
                    confidence = float(probs[classes.index(prediction)])

            return {"category": prediction, "confidence": confidence}

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

    def predict(
        self,
        title: str = "",
        body: str = "",
        category: str | None = None,
        requester_role: str | None = None,
        subject: str | None = None,
        description: str | None = None,
    ) -> dict:
        """Predict using the loaded model family."""
        if self.artifact_type == "sklearn_pipeline":
            return self._predict_sklearn_pipeline(
                title=title,
                body=body,
                category=category,
                requester_role=requester_role,
                subject=subject,
                description=description,
            )
        if self.artifact_type == "embedding_artefact":
            return self._predict_embedding_artefact(title=title, body=body)
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
    """Get the shared ModelRegistry instance."""
    global _registry
    if _registry is None:
        _registry = ModelRegistry()
    return _registry


def load_model(family: str, version: str | None = None) -> ModelHandle:
    """Load model artifact and cache it in memory."""
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
    """Pre-load all active models into the in-memory cache."""
    registry = get_registry()
    for family in registry.families:
        try:
            load_model(family)
        except RegistryError as exc:
            logger.error("Failed to preload model for family '%s': %s", family, exc)
