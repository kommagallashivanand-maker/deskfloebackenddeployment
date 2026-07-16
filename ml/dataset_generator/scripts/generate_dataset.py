import os
import argparse
import logging
from tqdm import tqdm

from gemini_client import generate
from prompts import build_prompt
from utils import (
    extract_json,
    generate_ticket_id,
    remove_duplicates,
    save_dataset_csv,
    save_review_sample,
    generate_report
)

CATEGORIES = [
    "Account Access",
    "Billing",
    "Technical",
    "Feature Request",
    "Subscription",
    "Performance",
    "Security",
    "Notifications",
    "Data Management",
    "General Inquiry",
]

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("generate_dataset")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate synthetic customer support ticket dataset using Gemini API."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Run a quick dry run generating only 5 tickets per category."
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=20,
        help="Batch size of tickets generated per API call."
    )
    parser.add_argument(
        "--tickets-per-category",
        type=int,
        default=201,
        help="Number of tickets to generate per category."
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    target_count = 5 if args.dry_run else args.tickets_per_category
    batch_size = args.batch_size

    logger.info(
        f"Starting dataset generation. Mode: "
        f"{'Dry Run (5 tickets/cat)' if args.dry_run else 'Full (200 tickets/cat)'}"
    )
    logger.info(f"Target tickets per category: {target_count}, Batch size: {batch_size}")

    all_tickets: list[dict[str, str]] = []
    global_seen: set[tuple[str, str]] = set()

    total_expected = len(CATEGORIES) * target_count

    with tqdm(total=total_expected, desc="Overall Generation Progress", unit="ticket") as pbar:
        for category in CATEGORIES:
            logger.info(f"Generating tickets for category: {category}")
            category_tickets: list[dict[str, str]] = []

            while len(category_tickets) < target_count:
                prompt = build_prompt(category, batch_size)

                try:
                    response_text = generate(prompt)
                    raw_batch = extract_json(response_text)

                    if not raw_batch:
                        logger.warning(
                            f"Failed to extract JSON or received empty list for category "
                            f"'{category}'. Retrying..."
                        )
                        continue

                    # Filter and validate generated tickets in the batch
                    new_valid_tickets = []
                    for t in raw_batch:
                        title = t.get("title")
                        body = t.get("body")

                        # Validate existence
                        if not title or not isinstance(title, str) or not title.strip():
                            continue
                        if not body or not isinstance(body, str) or not body.strip():
                            continue

                        title_clean = title.strip()
                        body_clean = body.strip()

                        # Validate lengths and rules
                        norm_key = (title_clean.lower(), body_clean.lower())
                        if norm_key in global_seen:
                            logger.debug(f"Duplicate ticket skipped: {title_clean[:30]}...")
                            continue

                        global_seen.add(norm_key)
                        ticket_obj = {
                            "title": title_clean,
                            "body": body_clean,
                            "category": category
                        }
                        new_valid_tickets.append(ticket_obj)

                    # Append new valid tickets up to target count
                    if new_valid_tickets:
                        for t in new_valid_tickets:
                            if len(category_tickets) < target_count:
                                category_tickets.append(t)
                                all_tickets.append(t)
                                pbar.update(1)
                            else:
                                # Clean up global_seen if we discard overflow tickets
                                norm_key = (t["title"].lower(), t["body"].lower())
                                global_seen.discard(norm_key)

                        logger.info(
                            f"Category '{category}': {len(category_tickets)}/{target_count} "
                            f"tickets collected."
                        )
                    else:
                        logger.warning(
                            f"No new unique and valid tickets returned in this batch "
                            f"for category '{category}'."
                        )

                except Exception as e:
                    logger.error(
                        f"Error during generation for category '{category}': {e}",
                        exc_info=True
                    )

    # Final uniqueness verification
    all_tickets = remove_duplicates(all_tickets)

    # Assign sequential ticket IDs locally
    for idx, ticket in enumerate(all_tickets, start=1):
        ticket["ticket_id"] = generate_ticket_id(idx)

    # Setup filepaths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)

    suffix = "_dry_run" if args.dry_run else ""
    tickets_csv = os.path.join(project_root, "datasets", f"tickets_v1{suffix}.csv")
    reviewed_csv = os.path.join(project_root, "datasets", f"reviewed_sample{suffix}.csv")
    report_md = os.path.join(project_root, "docs", f"dataset_report{suffix}.md")

    # Save final datasets
    logger.info("Saving datasets and generating reports...")
    save_dataset_csv(all_tickets, tickets_csv)

    # Save review sample
    sample_size = 200 if not args.dry_run else len(all_tickets)
    save_review_sample(all_tickets, reviewed_csv, sample_size=sample_size)

    # Generate analysis report
    generate_report(all_tickets, report_md)

    logger.info("Dataset generation workflow completed successfully!")


if __name__ == "__main__":
    main()
