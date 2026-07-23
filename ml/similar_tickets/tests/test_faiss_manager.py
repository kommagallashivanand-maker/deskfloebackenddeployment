import json

import pytest

from ml.similar_tickets.vector_index.faiss_manager import FaissManager
from ml.similar_tickets.embeddings.models.embedding_result import EmbeddingResult


def create_embeddings(path):
    data = [
        {
            "ticket_id": "T1",
            "embedding": [1.0, 0.0, 0.0],
        },
        {
            "ticket_id": "T2",
            "embedding": [0.0, 1.0, 0.0],
        },
    ]

    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f)


def test_build_index(tmp_path):
    file = tmp_path / "embeddings.json"
    create_embeddings(file)

    manager = FaissManager()
    manager.build(file)

    assert manager.is_loaded()
    assert len(manager.ticket_ids) == 2


def test_search(tmp_path):
    file = tmp_path / "embeddings.json"
    create_embeddings(file)

    manager = FaissManager()
    manager.build(file)

    result = manager.search([1.0, 0.0, 0.0], top_k=1)

    assert result[0]["ticket_id"] == "T1"


def test_add_embedding(tmp_path):
    file = tmp_path / "embeddings.json"
    create_embeddings(file)

    manager = FaissManager()
    manager.build(file)

    manager.add(
        EmbeddingResult(
            ticket_id="T3",
            embedding=[0.0, 0.0, 1.0],
        )
    )

    assert len(manager.ticket_ids) == 3


def test_empty_embeddings(tmp_path):
    file = tmp_path / "embeddings.json"

    with file.open("w") as f:
        json.dump([], f)

    manager = FaissManager()

    with pytest.raises(ValueError):
        manager.build(file)


def test_search_without_build():
    manager = FaissManager()

    with pytest.raises(RuntimeError):
        manager.search([1, 2, 3])