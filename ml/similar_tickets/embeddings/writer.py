"""
Embedding Writer

Responsible for writing generated embeddings and metadata
to persistent storage.
"""

import json
import logging
from datetime import datetime, timezone
from typing import List, Dict, Any

from embeddings.config import OUTPUT_EMBEDDINGS_FILE, OUTPUT_METADATA_FILE
from embeddings.exceptions import OutputWriteError
from embeddings.models.embedding_result import EmbeddingResult

logger = logging.getLogger(__name__)

class EmbeddingWriter:
    def save_embeddings(self, results: List[EmbeddingResult]) -> None:
        output = [
            {
                "ticket_id": result.ticket_id,
                "embedding": result.embedding,
            }
            for result in results
        ]

        try:
            with open(OUTPUT_EMBEDDINGS_FILE, "w", encoding="utf-8") as file:
                json.dump(output, file, indent=4)
        except Exception as exc:
            raise OutputWriteError(f"Failed to save embeddings to {OUTPUT_EMBEDDINGS_FILE}: {exc}") from exc

        logger.info(
            "Successfully saved %d embeddings to %s",
            len(results),
            OUTPUT_EMBEDDINGS_FILE,
        )

    def save_metadata(self, metadata: Dict[str, Any]) -> None:
        # Inject generated_at if not present
        if "generated_at" not in metadata:
            metadata["generated_at"] = datetime.now(timezone.utc).isoformat()

        try:
            with open(OUTPUT_METADATA_FILE, "w", encoding="utf-8") as file:
                json.dump(metadata, file, indent=4)
        except Exception as exc:
            raise OutputWriteError(f"Failed to save metadata to {OUTPUT_METADATA_FILE}: {exc}") from exc

        logger.info("Successfully saved metadata to %s", OUTPUT_METADATA_FILE)
