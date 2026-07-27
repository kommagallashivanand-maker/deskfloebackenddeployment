"""
Repository for ticket embedding operations.

Handles all database interactions related to ticket embeddings.
"""

import psycopg

from ml.similar_tickets.database.postgres import PostgresDB


class EmbeddingRepository:
    """Repository for CRUD and similarity search operations."""

    def get_ticket(
        self,
        ticket_id,
    ):
        """
        Fetch ticket title and description.
        """

        query = """
        SELECT
            id,
            title,
            description
        FROM tickets
        WHERE id = %s;
        """

        with PostgresDB.get_connection() as conn:
            with conn.cursor(
                row_factory=psycopg.rows.dict_row,
            ) as cursor:
                cursor.execute(
                    query,
                    (ticket_id,),
                )

                return cursor.fetchone()

    def upsert_embedding(
        self,
        ticket_id,
        embedding,
        model_name,
    ):
        """
        Insert a new embedding or update an existing one.
        """

        query = """
        INSERT INTO ticket_embeddings (
            ticket_id,
            embedding,
            model_name
        )
        VALUES (%s, %s, %s)
        ON CONFLICT (ticket_id)
        DO UPDATE
        SET
            embedding = EXCLUDED.embedding,
            model_name = EXCLUDED.model_name,
            updated_at = CURRENT_TIMESTAMP;
        """

        with PostgresDB.get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    query,
                    (
                        ticket_id,
                        embedding,
                        model_name,
                    ),
                )

    def get_embedding(
        self,
        ticket_id,
    ):
        """
        Fetch the embedding for a ticket.
        """

        query = """
        SELECT embedding
        FROM ticket_embeddings
        WHERE ticket_id = %s;
        """

        with PostgresDB.get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    query,
                    (ticket_id,),
                )

                row = cursor.fetchone()

        if row is None:
            return None

        return row[0]

    def search_similar(
        self,
        ticket_id,
        embedding,
        limit=5,
    ):
        """
        Find the most similar tickets using pgvector cosine similarity.
        """

        query = """
        SELECT
            t.id AS ticket_id,
            t.title,
            1 - (e.embedding <=> %s) AS similarity
        FROM ticket_embeddings e
        INNER JOIN tickets t
            ON t.id = e.ticket_id
        WHERE e.ticket_id <> %s
        ORDER BY e.embedding <=> %s
        LIMIT %s;
        """

        with PostgresDB.get_connection() as conn:
            with conn.cursor(
                row_factory=psycopg.rows.dict_row,
            ) as cursor:
                cursor.execute(
                    query,
                    (
                        embedding,
                        ticket_id,
                        embedding,
                        limit,
                    ),
                )

                rows = cursor.fetchall()

        return rows

    def delete_embedding(
        self,
        ticket_id,
    ):
        """
        Delete a ticket embedding.
        """

        query = """
        DELETE FROM ticket_embeddings
        WHERE ticket_id = %s;
        """

        with PostgresDB.get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    query,
                    (ticket_id,),
                )