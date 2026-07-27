"""
Simple test for SimilarTicketCache.
"""

import time

from ml.similar_tickets.services.cache import SimilarTicketCache
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(levelname)s: %(message)s",
)

def main():
    cache = SimilarTicketCache(ttl=5)

    key = ("ticket-101", 5)

    print("\nChecking empty cache...")

    print(cache.get(key))

    print("\nSaving result...")

    cache.set(
        key,
        [
            {
                "ticket_id": "10",
                "score": 0.96,
            },
            {
                "ticket_id": "20",
                "score": 0.94,
            },
        ],
    )

    print("\nReading immediately...")

    print(cache.get(key))

    print("\nWaiting for cache expiry...")

    time.sleep(6)

    print(cache.get(key))


if __name__ == "__main__":
    main()