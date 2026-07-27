from pathlib import Path

import pandas as pd


class EvaluationReport:
    """
    Handles saving evaluation results and generating
    an evaluation summary report.
    """

    def __init__(self, output_dir: str = "results"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def save_results(
        self,
        results: pd.DataFrame,
    ) -> None:
        """
        Save detailed evaluation results.
        """

        output_file = self.output_dir / "results.csv"

        results.to_csv(
            output_file,
            index=False,
        )

        print(f"Results saved to: {output_file}")

    def generate_report(
        self,
        average_precision: float,
        total_queries: int,
        results: pd.DataFrame,
    ) -> None:
        """
        Generate evaluation summary report.
        """

        report_file = self.output_dir / "evaluation_report.md"

        best_query = results.loc[
            results["precision_at_5"].idxmax()
        ]

        worst_query = results.loc[
            results["precision_at_5"].idxmin()
        ]

        failures = results[
            results["precision_at_5"] < 1.0
        ]

        with open(
            report_file,
            "w",
            encoding="utf-8",
        ) as report:

            report.write("# Similar Ticket Retrieval Evaluation\n\n")

            report.write("## Summary\n\n")

            report.write(f"- Total Queries Evaluated: {total_queries}\n")
            report.write(f"- Average Precision@5: {average_precision:.4f}\n")
            report.write(
                f"- Perfect Retrievals: {(results['precision_at_5'] == 1.0).sum()}\n"
            )
            report.write(
                f"- Failure Cases: {len(failures)}\n\n"
            )

            report.write("## Best Performing Query\n\n")

            report.write(
                f"- Query Ticket: {best_query['query_ticket_id']}\n"
            )
            report.write(
                f"- Precision@5: {best_query['precision_at_5']:.2f}\n\n"
            )

            report.write("## Worst Performing Query\n\n")

            report.write(
                f"- Query Ticket: {worst_query['query_ticket_id']}\n"
            )
            report.write(
                f"- Precision@5: {worst_query['precision_at_5']:.2f}\n\n"
            )

            report.write("## Failure Cases\n\n")

            if failures.empty:
                report.write(
                    "No failure cases identified.\n"
                )

            else:

                for _, row in failures.iterrows():

                    report.write(
                        f"### {row['query_ticket_id']}\n"
                    )
                    report.write(
                        f"- Precision@5: {row['precision_at_5']:.2f}\n"
                    )
                    report.write(
                        f"- Retrieved: {row['retrieved_ticket_ids']}\n"
                    )
                    report.write(
                        f"- Expected: {row['expected_ticket_ids']}\n\n"
                    )

        print(f"Report saved to: {report_file}")