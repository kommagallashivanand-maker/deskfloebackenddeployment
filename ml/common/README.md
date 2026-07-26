# ML Common Utilities

Shared utilities for all DeskFlow ML components.

## Purpose

This module centralizes path resolution logic that was previously duplicated across ML components. Before this module existed, each component independently computed the repository root using fragile relative path traversal (e.g., `Path(__file__).resolve().parent.parent.parent`), which:

1. Breaks silently when files move to different directory depths
2. Assumes a fixed directory structure
3. Fails in Docker containers where the file layout may differ from local development

## Usage

### Path Resolution

```python
from ml.common.paths import get_repo_root, get_priority_model_path

# Get repository root
repo_root = get_repo_root()

# Get specific model artifacts
model_path = get_priority_model_path()
baseline_model = get_baseline_classifier_model_path()
```

### Available Functions

- `get_repo_root()` - Returns the absolute path to DeskFlow-backend/ repository root
- `get_baseline_classifier_dir()` - Path to ml/baseline_classifier/
- `get_baseline_classifier_model_path(model_name="baseline_v1.pkl")` - Path to baseline model artifact
- `get_embedding_classifier_dir()` - Path to ml/embedding_classifier/
- `get_embedding_classifier_model_path(model_name="embedding_v1.pkl")` - Path to embedding model artifact
- `get_priority_model_path(model_name="priority_model_latest.joblib")` - Path to priority model artifact
- `get_priority_feature_extraction_dir()` - Path to ml/priority_feature_extraction/
- `get_training_data_path(filename="tickets_extracted_features.csv")` - Path to training data at repo root

## Docker Support

### Environment Variable Override

Set `DESKFLOW_REPO_ROOT` to override path resolution. This is especially important in Docker containers where the repository may be copied to a different location:

```dockerfile
FROM python:3.10-slim

# Copy repo into container at /app
COPY . /app
WORKDIR /app

# Set environment variable so Python code finds paths correctly
ENV DESKFLOW_REPO_ROOT=/app

# Now all ml/ components will resolve paths correctly
CMD ["python", "ml/eval_harness/eval.py"]
```

### Fallback Behavior

If `DESKFLOW_REPO_ROOT` is not set, the module computes the repo root relative to its own location (`ml/common/paths.py`). This works for:
- Local development (standard repo checkout)
- Containers where the entire repo is copied preserving structure

The fallback includes a sanity check: it verifies that `ml/` directory exists at the computed repo root, and raises a clear error if not.

## Migration from Legacy Path Resolution

**Before** (fragile):
```python
# In ml/eval_harness/models/priority_model.py
_current_dir = Path(__file__).resolve().parent.parent.parent
_model_path = _current_dir / "train_priority_model" / "models" / "priority_model_latest.joblib"
```

**After** (robust):
```python
from ml.common.paths import get_priority_model_path

_model_path = get_priority_model_path()
```

## Adding New Path Helpers

When adding new ML components, add corresponding helper functions to `paths.py`:

```python
def get_new_component_dir() -> Path:
    """Get path to new_component directory."""
    return get_repo_root() / "ml" / "new_component"

def get_new_component_model_path(model_name: str = "model.pkl") -> Path:
    """Get path to new_component model artifact."""
    return get_new_component_dir() / "models" / model_name
```

Then export them in `__init__.py`:

```python
from .paths import get_new_component_dir, get_new_component_model_path

__all__ = [
    # ...existing exports...
    "get_new_component_dir",
    "get_new_component_model_path",
]
```

## Files in This Module

- `__init__.py` - Public API exports
- `paths.py` - Path resolution implementation
- `README.md` - This file
