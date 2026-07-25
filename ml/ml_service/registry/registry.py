"""
registry.py — DF-042

Reads and validates the registry.json configuration file.

Responsibilities
----------------
- Load and parse registry.json.
- Resolve artifact paths relative to the registry directory.
- Expose the active version and its configuration for each model family.
- Validate that referenced artifact files exist on disk.

The registry never loads model objects itself — that is the loader's job.
"""

import json
import logging
from pathlib import Path
from typing import Any

logger = logging.getLogger(__name__)

# Canonical location of the registry configuration file.
REGISTRY_FILE = Path(__file__).resolve().parent / "registry.json"


class RegistryError(Exception):
    """Raised when the registry configuration is missing or invalid."""


class ModelRegistry:
    """
    Lightweight, file-based model registry.

    Reads registry.json on initialisation and exposes helpers for
    querying active versions and resolving artifact paths.

    Parameters
    ----------
    registry_path:
        Path to the registry.json file.  Defaults to the file sitting
        next to this module.
    """

    def __init__(self, registry_path: Path = REGISTRY_FILE) -> None:
        self._registry_path = registry_path
        self._config: dict[str, Any] = self._load()
        logger.info("ModelRegistry loaded from %s", self._registry_path)

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _load(self) -> dict[str, Any]:
        """Read and parse the registry JSON file."""
        if not self._registry_path.exists():
            raise RegistryError(
                f"Registry file not found: {self._registry_path}"
            )
        with open(self._registry_path, encoding="utf-8") as f:
            try:
                config = json.load(f)
            except json.JSONDecodeError as exc:
                raise RegistryError(
                    f"Registry file is not valid JSON: {exc}"
                ) from exc
        return config

    def _resolve_path(self, relative: str) -> Path:
        """Resolve an artifact path relative to the registry directory."""
        return (self._registry_path.parent / relative).resolve()

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    @property
    def families(self) -> list[str]:
        """Return all registered model family names (e.g. ['category', 'priority'])."""
        return list(self._config.keys())

    def get_active_version(self, family: str) -> str:
        """
        Return the active version name for a model family.

        Parameters
        ----------
        family:
            Model family name (e.g. 'category').

        Raises
        ------
        RegistryError:
            If the family is not registered or has no active version set.
        """
        if family not in self._config:
            raise RegistryError(
                f"Model family '{family}' not found in registry. "
                f"Available families: {self.families}"
            )
        active = self._config[family].get("active")
        if not active:
            raise RegistryError(
                f"No active version set for model family '{family}'."
            )
        return active

    def get_version_config(self, family: str, version: str) -> dict[str, Any]:
        """
        Return the full configuration dict for a specific version.

        Parameters
        ----------
        family:
            Model family name.
        version:
            Version key (e.g. 'embedding_v1').

        Raises
        ------
        RegistryError:
            If the family or version is not registered.
        """
        if family not in self._config:
            raise RegistryError(
                f"Model family '{family}' not found in registry."
            )
        versions = self._config[family].get("versions", {})
        if version not in versions:
            raise RegistryError(
                f"Version '{version}' not found for family '{family}'. "
                f"Available versions: {list(versions.keys())}"
            )
        return versions[version]

    def resolve_artifact_path(self, family: str, version: str) -> Path:
        """
        Resolve the absolute artifact path for a given family/version.

        Raises
        ------
        RegistryError:
            If the resolved artifact file does not exist on disk.
        """
        cfg = self.get_version_config(family, version)
        artifact_rel: str = cfg.get("artifact", "")
        if not artifact_rel:
            raise RegistryError(
                f"No artifact path configured for '{family}/{version}'."
            )
        resolved = self._resolve_path(artifact_rel)
        if not resolved.exists():
            raise RegistryError(
                f"Artifact file not found for '{family}/{version}': {resolved}"
            )
        return resolved

    def resolve_metadata_path(self, family: str, version: str) -> Path | None:
        """
        Resolve the absolute metadata JSON path, or None if not configured.
        """
        cfg = self.get_version_config(family, version)
        meta_rel = cfg.get("metadata")
        if not meta_rel:
            return None
        return self._resolve_path(meta_rel)

    def get_artifact_type(self, family: str, version: str) -> str:
        """
        Return the artifact type string for a given family/version.

        Supported values
        ----------------
        - ``sklearn_pipeline``  — a joblib-serialised sklearn Pipeline
        - ``embedding_artefact`` — a joblib-serialised dict with keys
          ``classifier``, ``embedding_model_name``, and ``classes``
        """
        cfg = self.get_version_config(family, version)
        return cfg.get("type", "sklearn_pipeline")

    def active_versions(self) -> dict[str, str]:
        """
        Return a mapping of {family: active_version} for all families.

        Used by the /version endpoint.
        """
        return {family: self.get_active_version(family) for family in self.families}

    def list_versions(self, family: str) -> list[str]:
        """Return all registered version keys for a model family."""
        if family not in self._config:
            raise RegistryError(
                f"Model family '{family}' not found in registry."
            )
        return list(self._config[family].get("versions", {}).keys())
