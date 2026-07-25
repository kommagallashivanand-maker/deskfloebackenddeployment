"""
PostgreSQL connection manager.

Provides reusable PostgreSQL connections for the Similar Tickets module.
Automatically registers pgvector support.
"""

import psycopg
from pgvector.psycopg import register_vector

from ml.similar_tickets.config import DATABASE_URL


class PostgresDB:
    """Database connection manager."""

    @staticmethod
    def get_connection():
        conn = psycopg.connect(DATABASE_URL)
        register_vector(conn)
        return conn