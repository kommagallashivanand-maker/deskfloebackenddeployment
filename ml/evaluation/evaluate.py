from pathlib import Path
import time

import pandas as pd

from models import TicketEmbeddingModel
from retrieval import SimilarTicketRetriever
from metrics import RetrievalMetrics
from report import EvaluationReport


BASE_DIR = Path(__file__).resolve().parent

TICKETS_FILE = BASE_DIR / "data" / "tickets.csv"
JUDGMENT_FILE = BASE_DIR / "data" / "judgment_set.csv"


def load_judgment_set(path: Path) -> pd.DataFrame:
    """
    Load the judgment set.
    """

    df = pd.read_csv(path)

    required_columns = {
        "query_ticket_id",
        "relevant_ticket_ids",
    }

    missing = required_columns - set(df.columns)

    if missing:
        raise ValueError(
            f"Missing columns in judgment set: {', '.join(sorted(missing))}"
        )

    return df


def main():

    start_time = time.time()

    print("=" * 60)
    print("Similar Tickets Evaluation")
    print("=" * 60)

    embedding_model = TicketEmbeddingModel()

    print("\nLoading tickets...")

    tickets_df = embedding_model.load_dataset(TICKETS_FILE)

    documents = embedding_model.build_documents(tickets_df)

    print("Generating embeddings...")

    embeddings = embedding_model.generate_embeddings(documents)

    print("Building FAISS index...")

    retriever = SimilarTicketRetriever()

    retriever.build_index(
        ticket_ids=tickets_df["ticket_id"].tolist(),
        embeddings=embeddings,
        dataframe=tickets_df,
    )

    print("Loading judgment set...")

    judgment_df = load_judgment_set(JUDGMENT_FILE)

    metrics = RetrievalMetrics()

    evaluation_rows = []

    precision_scores = []

    total_queries = len(judgment_df)

    print("\nRunning evaluation...\n")

    for index, (_, row) in enumerate(
        judgment_df.iterrows(),
        start=1,
    ):

        query_ticket_id = row["query_ticket_id"]

        relevant_ticket_ids = [
            ticket.strip()
            for ticket in row["relevant_ticket_ids"].split(",")
            if ticket.strip()
        ]

        matching_rows = tickets_df.index[
            tickets_df["ticket_id"] == query_ticket_id
        ]

        if matching_rows.empty:
            print(f"[{index}/{total_queries}] Skipping {query_ticket_id} (Not Found)")
            continue

        query_index = matching_rows[0]

        query_embedding = embeddings[query_index]

        results = retriever.search(
            query_ticket_id=query_ticket_id,
            query_embedding=query_embedding,
            top_k=5,
        )

        retrieved_ids = results["ticket_id"].tolist()

        precision = metrics.precision_at_k(
            retrieved_ticket_ids=retrieved_ids,
            relevant_ticket_ids=relevant_ticket_ids,
            k=5,
        )

        precision_scores.append(precision)

        evaluation_rows.append(
            {
                "query_ticket_id": query_ticket_id,
                "retrieved_ticket_ids": ",".join(retrieved_ids),
                "expected_ticket_ids": ",".join(relevant_ticket_ids),
                "precision_at_5": round(precision, 4),
            }
        )

        print(
            f"[{index}/{total_queries}] "
            f"{query_ticket_id:<15} "
            f"Precision@5 = {precision:.2f}"
        )

    average_precision = metrics.average_precision(
        precision_scores
    )

    results_df = pd.DataFrame(evaluation_rows)

    reporter = EvaluationReport(
        output_dir=BASE_DIR / "results"
    )

    reporter.save_results(results_df)

    reporter.generate_report(
        average_precision=average_precision,
        total_queries=len(results_df),
        results=results_df,
    )

    execution_time = time.time() - start_time

    print("\n" + "=" * 60)
    print("Evaluation Complete")
    print("=" * 60)
    print(f"Queries Evaluated : {len(results_df)}")
    print(f"Average Precision@5 : {average_precision:.4f}")
    print(f"Execution Time : {execution_time:.2f} seconds")
    print("Results saved in : results/")
    print("=" * 60)


if __name__ == "__main__":
    main()