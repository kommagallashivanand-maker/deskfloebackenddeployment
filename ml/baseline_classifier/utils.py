import logging
import sys
import pandas as pd
import numpy as np

def setup_logging():
    """Configure logging for the baseline classifier."""
    logger = logging.getLogger("baseline_classifier")
    logger.setLevel(logging.INFO)
    
    # Avoid duplicate handlers if setup is called multiple times
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter(
            '[%(asctime)s] %(levelname)s - %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        handler.setFormatter(formatter)
        logger.addHandler(handler)
    return logger

def combine_title_body(X):
    """
    Concatenate title and body with a space.
    Handles pandas DataFrame, numpy arrays, and lists of dicts/tuples/strings.
    """
    if isinstance(X, pd.DataFrame):
        title = X['title'].fillna("").astype(str)
        body = X['body'].fillna("").astype(str)
        return (title + " " + body).values
    elif isinstance(X, np.ndarray):
        if len(X.shape) == 1:
            return X.astype(str)
        elif X.shape[1] >= 2:
            return (X[:, 0].astype(str) + " " + X[:, 1].astype(str))
        else:
            raise ValueError("Input numpy array must have at least 2 columns")
    elif isinstance(X, list):
        if len(X) == 0:
            return []
        first = X[0]
        if isinstance(first, dict):
            return [str(item.get('title', '')).strip() + " " + str(item.get('body', '')).strip() for item in X]
        elif isinstance(first, (list, tuple)):
            return [str(item[0]).strip() + " " + str(item[1]).strip() for item in X]
        else:
            return [str(item).strip() for item in X]
    else:
        raise TypeError(f"Unsupported input type for combine_title_body: {type(X)}")

def load_and_validate_dataset(csv_path):
    """
    Load dataset from CSV path and validate constraints.
    Raises clear exceptions on validation failure.
    """
    logger = logging.getLogger("baseline_classifier")
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
    
    # Identify empty fields (whitespace only or nulls or 'nan' string)
    def is_empty(series):
        # Fill NaN with empty, strip whitespaces, then check if empty
        cleaned = series.fillna("").astype(str).str.strip()
        return (cleaned == "") | (cleaned.str.lower() == "nan")
        
    is_empty_title = is_empty(df["title"])
    is_empty_body = is_empty(df["body"])
    is_empty_category = is_empty(df["category"])
    
    invalid_rows = is_empty_title | is_empty_body | is_empty_category
    df_clean = df[~invalid_rows].copy()
    
    removed_count = initial_count - len(df_clean)
    if removed_count > 0:
        logger.warning(f"Removed {removed_count} rows with empty title, body, or category.")
        
    if len(df_clean) == 0:
        err_msg = "Dataset is empty after cleaning."
        logger.error(err_msg)
        raise ValueError(err_msg)
        
    return df_clean
