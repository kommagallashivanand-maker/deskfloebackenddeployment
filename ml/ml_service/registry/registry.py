"""Model registry reader and resolver."""

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
    """File-based model registry that manages active model versions."""

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
        """List of registered model family names."""
        return list(self._config.keys())

    def get_active_version(self, family: str) -> str:
        """Get active version name for a model family."""
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
        """Get configuration dict for a specific version."""
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
        """Resolve absolute path to model artifact file."""
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
        """Resolve absolute path to metadata file, if configured."""
        cfg = self.get_version_config(family, version)
        meta_rel = cfg.get("metadata")
        if not meta_rel:
            return None
        return self._resolve_path(meta_rel)

    def get_artifact_type(self, family: str, version: str) -> str:
        """Get artifact type for a model family and version."""
        cfg = self.get_version_config(family, version)
        return cfg.get("type", "sklearn_pipeline")

    def active_versions(self) -> dict[str, str]:
        """Map of model families to active versions."""
        return {family: self.get_active_version(family) for family in self.families}

    def list_versions(self, family: str) -> list[str]:
        """Return all registered version keys for a model family."""
        if family not in self._config:
            raise RegistryError(
                f"Model family '{family}' not found in registry."
            )
        return list(self._config[family].get("versions", {}).keys())
