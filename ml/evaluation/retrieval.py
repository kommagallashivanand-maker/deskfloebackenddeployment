import faiss
import numpy as np
import pandas as pd


class SimilarTicketRetriever:
    """
    Builds a FAISS index and performs similarity search.
    """

    def __init__(self):
        self.index = None
        self.ticket_ids = []
        self.ticket_lookup = {}

    def build_index(
        self,
        ticket_ids: list[str],
        embeddings: np.ndarray,
        dataframe: pd.DataFrame,
    ) -> None:
        """
        Build the FAISS index.
        """

        embeddings = embeddings.astype("float32")

        faiss.normalize_L2(embeddings)

        dimension = embeddings.shape[1]

        self.index = faiss.IndexFlatIP(dimension)

        self.index.add(embeddings)

        self.ticket_ids = ticket_ids

        self.ticket_lookup = (
            dataframe.set_index("ticket_id")
            .to_dict("index")
        )

        print(f"Indexed {len(ticket_ids)} tickets.")

    def search(
        self,
        query_ticket_id: str,
        query_embedding: np.ndarray,
        top_k: int = 5,
    ) -> pd.DataFrame:
        """
        Search for Top-K similar tickets.
        """

        if self.index is None:
            raise ValueError(
                "FAISS index has not been built."
            )

        if query_embedding.ndim == 1:
            query_embedding = query_embedding.reshape(1, -1)

        query_embedding = query_embedding.astype("float32")

        faiss.normalize_L2(query_embedding)

        scores, indices = self.index.search(
            query_embedding,
            top_k + 1,
        )

        results = []

        for score, idx in zip(scores[0], indices[0]):

            if idx == -1:
                continue

            ticket_id = self.ticket_ids[idx]

            if ticket_id == query_ticket_id:
                continue

            ticket = self.ticket_lookup[ticket_id]

            results.append(
                {
                    "ticket_id": ticket_id,
                    "title": ticket["title"],
                    "category": ticket["category"],
                    "similarity": round(float(score), 4),
                }
            )

            if len(results) == top_k:
                break

        return pd.DataFrame(results)