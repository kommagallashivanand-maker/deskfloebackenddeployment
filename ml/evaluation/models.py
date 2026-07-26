from sentence_transformers import SentenceTransformer
import pandas as pd
import numpy as np


class TicketEmbeddingModel:
    """
    Handles ticket loading and embedding generation.
    """

    def __init__(
        self,
        model_name: str = "sentence-transformers/all-MiniLM-L6-v2",
    ):
        self.model = SentenceTransformer(model_name)

    def load_dataset(self, csv_path: str) -> pd.DataFrame:
        """
        Load ticket dataset.
        """

        df = pd.read_csv(csv_path)

        required_columns = {
            "ticket_id",
            "title",
            "body",
            "category",
        }

        missing = required_columns - set(df.columns)

        if missing:
            raise ValueError(
                f"Missing columns: {', '.join(sorted(missing))}"
            )

        return df

    def build_documents(
        self,
        dataframe: pd.DataFrame,
    ) -> list[str]:
        """
        Combine title and body.
        """

        documents = (
            dataframe["title"].fillna("")
            + ". "
            + dataframe["body"].fillna("")
        )

        return documents.tolist()

    def generate_embeddings(
        self,
        documents: list[str],
    ) -> np.ndarray:
        """
        Generate sentence embeddings.
        """

        embeddings = self.model.encode(
            documents,
            convert_to_numpy=True,
            show_progress_bar=True,
        )

        return embeddings.astype("float32")