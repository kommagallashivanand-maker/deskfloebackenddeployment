import logging
import sys
import pandas as pd
import numpy as np
from pathlib import Path
from typing import Optional


def setup_logging() -> logging.Logger:
    """Configure and return the embedding_classifier logger."""
    logger = logging.getLogger("embedding_classifier")
    logger.setLevel(logging.INFO)

    # Avoid duplicate handlers if setup is called multiple times
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter(
            "[%(asctime)s] %(levelname)s - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
        handler.setFormatter(formatter)
        logger.addHandler(handler)

    return logger


def combine_title_body(df: pd.DataFrame) -> pd.Series:
    """
    Concatenate the title and body columns into a single text string per row.

    Args:
        df: DataFrame containing at least 'title' and 'body' columns.

    Returns:
        pandas Series of concatenated strings.
    """
    title = df["title"].fillna("").astype(str)
    body = df["body"].fillna("").astype(str)
    return title + " " + body


def load_and_validate_dataset(csv_path) -> pd.DataFrame:
    """
    Load the ticket dataset from a CSV file and validate its schema.

    Raises clear exceptions on validation failure.  Rows with empty
    title, body, or category fields are dropped with a warning.

    Args:
        csv_path: Path-like object or string pointing to the CSV file.

    Returns:
        Cleaned DataFrame with columns: ticket_id, title, body, category.
    """
    logger = logging.getLogger("embedding_classifier")
    logger.info(f"Loading dataset from {csv_path}")

    try:
        df = pd.read_csv(csv_path)
    except FileNotFoundError as e:
        logger.error(f"Dataset file not found at {csv_path}")
        raise FileNotFoundError(f"Dataset file not found at {csv_path}") from e
    except Exception as e:
        logger.error(f"Failed to read CSV dataset: {str(e)}")
        raise ValueError(f"Failed to read CSV dataset: {str(e)}") from e

    # Validate required columns
    required_columns = ["ticket_id", "title", "body", "category"]
    missing_cols = [col for col in required_columns if col not in df.columns]
    if missing_cols:
        err_msg = f"Dataset is missing required columns: {missing_cols}"
        logger.error(err_msg)
        raise ValueError(err_msg)

    initial_count = len(df)

    def is_empty(series: pd.Series) -> pd.Series:
        cleaned = series.fillna("").astype(str).str.strip()
        return (cleaned == "") | (cleaned.str.lower() == "nan")

    invalid_rows = (
        is_empty(df["title"]) | is_empty(df["body"]) | is_empty(df["category"])
    )
    df_clean = df[~invalid_rows].copy()

    removed_count = initial_count - len(df_clean)
    if removed_count > 0:
        logger.warning(
            f"Removed {removed_count} rows with empty title, body, or category."
        )

    if len(df_clean) == 0:
        err_msg = "Dataset is empty after cleaning."
        logger.error(err_msg)
        raise ValueError(err_msg)

    return df_clean


def encode_texts(
    texts: pd.Series,
    model_name: str,
    batch_size: int = 64,
    show_progress_bar: bool = False,
) -> np.ndarray:
    """
    Encode a series of text strings into sentence embeddings.

    Uses SentenceTransformers to produce dense vector representations.
    The model is downloaded on first use and cached by the library.

    Args:
        texts:              pandas Series of strings to encode.
        model_name:         SentenceTransformers model identifier (e.g. 'all-MiniLM-L6-v2').
        batch_size:         Encoding batch size.
        show_progress_bar:  Whether to display a tqdm progress bar.

    Returns:
        numpy array of shape (n_samples, embedding_dim).
    """
    logger = logging.getLogger("embedding_classifier")

    try:
        from sentence_transformers import SentenceTransformer
    except ImportError as exc:
        raise ImportError(
            "sentence-transformers is required. "
            "Install it with: pip install sentence-transformers"
        ) from exc

    logger.info(f"Loading SentenceTransformer model: {model_name}")
    model = SentenceTransformer(model_name)

    text_list = texts.tolist()
    logger.info(f"Encoding {len(text_list)} samples (batch_size={batch_size})")

    embeddings = model.encode(
        text_list,
        batch_size=batch_size,
        show_progress_bar=show_progress_bar,
        convert_to_numpy=True,
    )

    logger.info(f"Encoding complete. Embedding shape: {embeddings.shape}")
    return embeddings
