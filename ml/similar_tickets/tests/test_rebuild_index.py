import json

from ml.similar_tickets.vector_index.rebuild_index import rebuild_index


def test_rebuild_index(tmp_path):
    file = tmp_path / "embeddings.json"

    with file.open("w") as f:
        json.dump(
            [
                {
                    "ticket_id": "A",
                    "embedding": [1.0, 2.0],
                }
            ],
            f,
        )

    manager = rebuild_index(file)

    assert manager.is_loaded()