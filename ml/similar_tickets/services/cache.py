"""
In-memory cache for similar ticket search results.
"""

import time

import logging

logger = logging.getLogger(__name__)

class SimilarTicketCache:
    """
    Simple in-memory cache with TTL support.
    """

    def __init__(
        self,
        ttl: int = 300,
    ):
        self.ttl = ttl
        self.cache = {}

    def get(
        self,
        key,
    ):
        """
        Retrieve a cached value if it exists and is not expired.
        """

        item = self.cache.get(key)

        if item is None:
            logger.info("Cache MISS")
            return None

        if time.time() > item["expires_at"]:
            del self.cache[key]
            logger.info("Cache EXPIRED")
            return None
        
        logger.info("Cache HIT")
        return item["value"]

    def set(
        self,
        key,
        value,
    ):
        """
        Store a value in the cache.
        """

        self.cache[key] = {
            "value": value,
            "expires_at": time.time() + self.ttl,
        }

    def invalidate(
        self,
        ticket_id,
    ):
        """
        Remove all cached entries for a ticket.
        """

        keys_to_delete = [
            key
            for key in self.cache
            if key[0] == ticket_id
        ]

        for key in keys_to_delete:
            del self.cache[key]

    def clear(self):
        """
        Clear the entire cache.
        """

        self.cache.clear()