"""
Business logic for reranking similar tickets.
"""


class SimilarTicketReranker:
    """
    Applies business rules to rerank similar tickets.
    """

    def __init__(
        self,
        category_boost: float = 0.05,
    ):
        self.category_boost = category_boost

    def rerank(
        self,
        query_category_id,
        candidates,
    ):
        """
        Boost tickets belonging to the same category and
        return candidates sorted by final score.
        """

        reranked = []

        for candidate in candidates:

            score = candidate["similarity"]

            if (
                candidate["category_id"]
                == query_category_id
            ):
                score += self.category_boost

            candidate["score"] = score

            reranked.append(candidate)

        reranked.sort(
            key=lambda ticket: ticket["score"],
            reverse=True,
        )

        return reranked