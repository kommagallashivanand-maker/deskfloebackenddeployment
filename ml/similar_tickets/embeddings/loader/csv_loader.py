import csv
import logging
from pathlib import Path
from typing import List, Set

from ml.similar_tickets.config import INPUT_CSV_PATH
from ..exceptions import DatasetValidationError
from ..loader.base_loader import BaseLoader
from ..models.ticket import Ticket

logger = logging.getLogger(__name__)

class CsvLoader(BaseLoader):
    """Loads tickets from a CSV file."""

    def __init__(self, input_file: Path = INPUT_CSV_PATH):
        self.input_file = input_file

    def load_tickets(self) -> List[Ticket]:
        logger.info("Loading tickets from %s", self.input_file)
        if not self.input_file.exists():
            raise FileNotFoundError(f"Ticket dataset not found: {self.input_file}")

        validated_tickets = []
        seen_ids: Set[str] = set()

        try:
            with open(self.input_file, mode="r", encoding="utf-8") as file:
                reader = csv.DictReader(file)
                for row_num, row in enumerate(reader, start=2): # Header is line 1
                    ticket = self._validate_and_parse_row(row, row_num, seen_ids)
                    validated_tickets.append(ticket)
                    seen_ids.add(ticket.id)
        except Exception as exc:
            # We catch specific validation errors in the loop, but this catches broader IO/CSV errors
            if isinstance(exc, DatasetValidationError):
                raise
            raise DatasetValidationError(f"Failed to read CSV file: {exc}") from exc

        logger.info("Successfully loaded %d tickets.", len(validated_tickets))
        return validated_tickets

    def _validate_and_parse_row(self, row: dict, row_num: int, seen_ids: Set[str]) -> Ticket:
        """Validates a single CSV row and converts it to a Ticket."""
        required_fields = {"ticket_id", "title", "body"}
        missing = required_fields - row.keys()
        if missing:
            raise DatasetValidationError(f"Row {row_num}: Missing required columns: {missing}")

        try:
            raw_ticket_id = row["ticket_id"]
            if not raw_ticket_id:
                raise ValueError("Empty ID")
            
            # The database schema uses UUIDs, but we store and treat them as simple strings
            ticket_id = str(raw_ticket_id).strip()
            
        except (ValueError, TypeError, KeyError) as exc:
            raise DatasetValidationError(f"Row {row_num}: ticket_id must be a valid string format, got '{row.get('ticket_id')}'") from exc

        if ticket_id in seen_ids:
            raise DatasetValidationError(f"Row {row_num}: Duplicate ticket_id found: {ticket_id}")

        title = row.get("title", "").strip()
        body = row.get("body", "").strip()

        if not title:
            raise DatasetValidationError(f"Row {row_num}: title cannot be empty.")
        if not body:
            raise DatasetValidationError(f"Row {row_num}: body cannot be empty.")

        # Map to domain model
        return Ticket(
            id=ticket_id,
            title=title,
            description=body
        )
