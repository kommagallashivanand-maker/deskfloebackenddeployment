import json

from ml.similar_tickets.vector_index.faiss_manager import FaissManager
from ml.similar_tickets.vector_index.search_service import SearchService


def test_search_service(tmp_path):
    file = tmp_path / "embeddings.json"

    with file.open("w") as f:
        json.dump(
            [
                {
                    "ticket_id": "A",
                    "embedding": [1.0, 0.0],
                },
                {
                    "ticket_id": "B",
                    "embedding": [0.0, 1.0],
                },
            ],
            f,
        )

    manager = FaissManager()
    manager.build(file)

    service = SearchService(manager)

    result = service.search_similar([1.0, 0.0], top_k=1)

    assert len(result) == 1
    assert result[0]["ticket_id"] == "A"