"""
Script to rebuild the FAISS index.

Usage:
    python -m ml.similar_tickets.scripts.run_index_rebuild
"""

from __future__ import annotations

from ml.similar_tickets.vector_index.rebuild_index import rebuild_index


def main() -> None:
    """Rebuild the FAISS index from stored embeddings."""

    try:
        manager = rebuild_index()

        print("======================================")
        print("FAISS index rebuilt successfully.")
        print(f"Indexed vectors : {len(manager.ticket_ids)}")
        print("======================================")

    except Exception as exc:
        print("======================================")
        print("Failed to rebuild FAISS index.")
        print(f"Reason: {exc}")
        print("======================================")


if __name__ == "__main__":
    main()