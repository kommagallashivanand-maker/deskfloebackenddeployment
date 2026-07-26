from typing import List


class RetrievalMetrics:
    """
    Evaluation metrics for Similar Ticket retrieval.
    """

    @staticmethod
    def precision_at_k(
        retrieved_ticket_ids: List[str],
        relevant_ticket_ids: List[str],
        k: int = 5,
    ) -> float:
        """
        Compute Precision@K.

        Precision@K = Relevant retrieved in Top-K / K
        """

        retrieved = retrieved_ticket_ids[:k]

        relevant = set(relevant_ticket_ids)

        relevant_found = sum(
            ticket_id in relevant
            for ticket_id in retrieved
        )

        return relevant_found / k

    @staticmethod
    def average_precision(
        precision_scores: List[float],
    ) -> float:
        """
        Compute average Precision@K.
        """

        if not precision_scores:
            return 0.0

        return sum(precision_scores) / len(precision_scores)