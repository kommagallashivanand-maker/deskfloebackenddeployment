"""
Simple test for SimilarTicketReranker.
"""

from ml.similar_tickets.services.reranker import SimilarTicketReranker


def main():
    reranker = SimilarTicketReranker()

    candidates = [
        {
            "ticket_id": "1",
            "title": "VPN Issue",
            "category_id": "A",
            "similarity": 0.90,
        },
        {
            "ticket_id": "2",
            "title": "Login Issue",
            "category_id": "B",
            "similarity": 0.92,
        },
        {
            "ticket_id": "3",
            "title": "Firewall Issue",
            "category_id": "A",
            "similarity": 0.88,
        },
    ]

    print("\nBefore Reranking\n")

    for candidate in candidates:
        print(candidate)

    result = reranker.rerank(
        query_category_id="A",
        candidates=candidates,
    )

    print("\nAfter Reranking\n")

    for candidate in result:
        print(candidate)


if __name__ == "__main__":
    main()