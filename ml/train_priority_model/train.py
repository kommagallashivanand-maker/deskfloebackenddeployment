"""
train.py — Priority Model Training Pipeline
============================================
Trains a RandomForestClassifier to predict support ticket priority
(Low / Medium / High / Urgent) using extracted features from
tickets_extracted_features.csv located at the repository root.

Usage (from repo root):
    ml/train_priority_model/.venv/Scripts/python ml/train_priority_model/train.py

Outputs:
    ml/train_priority_model/models/priority_model_v1.0.0.joblib
    ml/train_priority_model/models/priority_model_latest.joblib
"""

import os
import sys
import shutil
import pathlib

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import classification_report
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

MODEL_VERSION = "v1.0.0"
SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent           # DeskFlow-backend/
DATA_PATH = REPO_ROOT / "tickets_extracted_features.csv"
MODELS_DIR = SCRIPT_DIR / "models"

# Feature column groups
TEXT_FEATURES = ["keywords"]
CATEGORICAL_FEATURES = ["category", "sentiment_label"]
NUMERICAL_FEATURES = ["sentiment_score", "description_length"]
DROPPED_FEATURES = ["requester_role"]           # constant across all rows
TARGET = "priority"
ID_COLUMN = "ticket_id"

# Priority label ordering for consistent reporting
PRIORITY_ORDER = ["Low", "Medium", "High", "Urgent"]

N_FOLDS = 5
RANDOM_STATE = 42

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def load_data(path: pathlib.Path) -> pd.DataFrame:
    """Load the feature CSV and perform basic validation."""
    print(f"[INFO] Loading data from: {path}")
    if not path.exists():
        print(f"[ERROR] Data file not found: {path}", file=sys.stderr)
        sys.exit(1)

    df = pd.read_csv(path)
    required_cols = [ID_COLUMN, TARGET] + TEXT_FEATURES + CATEGORICAL_FEATURES + NUMERICAL_FEATURES
    missing = set(required_cols) - set(df.columns)
    if missing:
        print(f"[ERROR] Missing columns in CSV: {missing}", file=sys.stderr)
        sys.exit(1)

    print(f"[INFO] Loaded {len(df):,} rows × {len(df.columns)} columns")
    return df


def summarise_target(series: pd.Series) -> None:
    """Print class distribution of the target variable."""
    counts = series.value_counts()
    total = len(series)
    print("\n[INFO] Target class distribution:")
    for label in PRIORITY_ORDER:
        n = counts.get(label, 0)
        pct = n / total * 100
        print(f"       {label:<8} {n:>5}  ({pct:5.1f}%)")
    print()


def build_preprocessor() -> ColumnTransformer:
    """Construct the column-wise preprocessing pipeline."""
    text_transformer = TfidfVectorizer(
        analyzer="word",
        token_pattern=r"[^,]+",   # comma-separated keyword tokens
        strip_accents="unicode",
        lowercase=True,
        max_features=500,
    )
    categorical_transformer = OneHotEncoder(
        handle_unknown="ignore",
        sparse_output=False,
    )
    numerical_transformer = StandardScaler()

    preprocessor = ColumnTransformer(
        transformers=[
            ("tfidf_keywords",  text_transformer,          "keywords"),
            ("ohe_categoricals", categorical_transformer,  CATEGORICAL_FEATURES),
            ("scaler_numericals", numerical_transformer,   NUMERICAL_FEATURES),
        ],
        remainder="drop",   # silently drops ticket_id, requester_role, priority
    )
    return preprocessor


def build_pipeline() -> Pipeline:
    """Assemble the full preprocessing + classifier pipeline."""
    preprocessor = build_preprocessor()
    classifier = RandomForestClassifier(
        n_estimators=300,
        class_weight="balanced",
        max_features="sqrt",
        random_state=RANDOM_STATE,
        n_jobs=-1,
    )
    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier),
        ]
    )
    return pipeline


def run_cross_validation(pipeline: Pipeline, X: pd.DataFrame, y: pd.Series) -> None:
    """Run stratified k-fold CV and print per-class classification report."""
    print(f"[INFO] Running Stratified {N_FOLDS}-Fold Cross-Validation ...")
    cv = StratifiedKFold(n_splits=N_FOLDS, shuffle=True, random_state=RANDOM_STATE)

    y_pred_cv = cross_val_predict(pipeline, X, y, cv=cv, n_jobs=-1)

    print("\n" + "=" * 62)
    print("  Cross-Validation Classification Report  (averaged over folds)")
    print("=" * 62)
    report = classification_report(
        y,
        y_pred_cv,
        labels=PRIORITY_ORDER,
        zero_division=0,
    )
    print(report)


def save_artifacts(pipeline: Pipeline) -> None:
    """Persist the trained pipeline to versioned and 'latest' files."""
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    versioned_path = MODELS_DIR / f"priority_model_{MODEL_VERSION}.joblib"
    latest_path    = MODELS_DIR / "priority_model_latest.joblib"

    joblib.dump(pipeline, versioned_path)
    shutil.copy2(versioned_path, latest_path)

    print(f"\n[SUCCESS] Model saved:")
    print(f"          Versioned -> {versioned_path}")
    print(f"          Latest    -> {latest_path}")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------


def main() -> None:
    print("=" * 62)
    print("  DeskFlow AI — Priority Model Training Pipeline")
    print("=" * 62)

    # 1. Load data
    df = load_data(DATA_PATH)
    summarise_target(df[TARGET])

    # 2. Separate features and target
    feature_cols = TEXT_FEATURES + CATEGORICAL_FEATURES + NUMERICAL_FEATURES
    X = df[feature_cols]
    y = df[TARGET]

    # 3. Build pipeline
    pipeline = build_pipeline()

    # 4. Stratified cross-validation (evaluation only — does not fit the final model)
    run_cross_validation(pipeline, X, y)

    # 5. Final training on full dataset
    print("[INFO] Training final model on full dataset ...")
    pipeline.fit(X, y)
    print("[INFO] Training complete.")

    # 6. Save versioned + latest artifacts
    save_artifacts(pipeline)

    print("\n[DONE] All steps completed successfully.\n")


if __name__ == "__main__":
    main()
