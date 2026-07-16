import os
import json
import logging
import pandas as pd

# Setup logging
logger = logging.getLogger("gemini_client")


def extract_json(response: str) -> list[dict[str, str]]:
    clean_text = response.strip()

    # Remove markdown code block wrappers if present
    if clean_text.startswith("```"):
        # Split into lines and skip markdown code block delimiters
        lines = clean_text.splitlines()
        if lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        clean_text = "\n".join(lines).strip()

    try:
        data = json.loads(clean_text)
        if not isinstance(data, list):
            logger.warning("Extracted JSON is not a list.")
            return []
        return data
    except json.JSONDecodeError as e:
        logger.warning(f"Initial JSON decode failed: {e}. Attempting fallback extraction.")
        # Fallback: extract substring between the first '[' and last ']'
        start_idx = clean_text.find('[')
        end_idx = clean_text.rfind(']')
        if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
            try:
                data = json.loads(clean_text[start_idx:end_idx + 1])
                if isinstance(data, list):
                    return data
            except json.JSONDecodeError as fallback_err:
                logger.error(f"Fallback JSON extraction failed: {fallback_err}")
        return []


def generate_ticket_id(index: int) -> str:
    return f"TKT-{index:05d}"


def remove_duplicates(tickets: list[dict[str, str]]) -> list[dict[str, str]]:
    seen = set()
    unique_tickets = []
    for ticket in tickets:
        title = ticket.get("title", "").strip()
        body = ticket.get("body", "").strip()
        # Create a normalized identifier key
        norm_key = (title.lower(), body.lower())
        if norm_key not in seen:
            seen.add(norm_key)
            clean_ticket = ticket.copy()
            clean_ticket["title"] = title
            clean_ticket["body"] = body
            unique_tickets.append(clean_ticket)
    return unique_tickets


def save_dataset_csv(tickets: list[dict[str, str]], filepath: str) -> None:
    os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)
    df = pd.DataFrame(tickets)
    # Ensure correct columns order
    columns = ["ticket_id", "title", "body", "category"]
    df = df[[col for col in columns if col in df.columns]]
    df.to_csv(filepath, index=False)
    logger.info(f"Dataset successfully saved to {filepath} ({len(df)} rows).")


def save_review_sample(tickets: list[dict[str, str]], filepath: str, sample_size: int = 200) -> None:
    if len(tickets) < sample_size:
        logger.warning(
            f"Dataset size ({len(tickets)}) is smaller than requested sample size ({sample_size}). "
            f"Saving all available tickets to review sample."
        )
        sample_size = len(tickets)

    os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)
    df = pd.DataFrame(tickets)
    sample_df = df.sample(n=sample_size, random_state=42)
    sample_df.to_csv(filepath, index=False)
    logger.info(f"Review sample saved to {filepath} ({len(sample_df)} rows).")


def generate_report(tickets: list[dict[str, str]], filepath: str) -> None:
    os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)

    df = pd.DataFrame(tickets)
    total_tickets = len(df)

    # Category breakdown
    category_counts = df["category"].value_counts()

    # Text length statistics
    df["title_words"] = df["title"].apply(lambda x: len(str(x).split()))
    df["body_words"] = df["body"].apply(lambda x: len(str(x).split()))
    df["body_sentences"] = df["body"].apply(lambda x: len([s for s in str(x).split('.') if s.strip()]))

    avg_title_words = df["title_words"].mean()
    avg_body_words = df["body_words"].mean()
    avg_body_sentences = df["body_sentences"].mean()

    # Validation checks
    empty_titles = df["title"].isna().sum() + (df["title"].astype(str).str.strip() == "").sum()
    empty_bodies = df["body"].isna().sum() + (df["body"].astype(str).str.strip() == "").sum()
    duplicate_count = df.duplicated(subset=["title", "body"]).sum()

    # Build report lines
    report = [
        "# Synthetic Customer Support Ticket Dataset Report",
        "",
        "## Dataset Overview",
        f"- **Total Tickets**: {total_tickets}",
        f"- **Date Generated**: {pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')}",
        "",
        "## Category Class Balance",
        "| Category | Ticket Count | Percentage |",
        "| --- | --- | --- |"
    ]

    for cat in sorted(df["category"].unique()):
        count = category_counts.get(cat, 0)
        pct = (count / total_tickets) * 100
        report.append(f"| {cat} | {count} | {pct:.1f}% |")

    report.extend([
        "",
        "## Text Statistics (Averages)",
        f"- **Average Title Word Count**: {avg_title_words:.2f} words",
        f"- **Average Body Word Count**: {avg_body_words:.2f} words",
        f"- **Average Body Sentence Count**: {avg_body_sentences:.2f} sentences",
        "",
        "## Dataset Quality & Validation Summary",
        "| Check | Expected | Actual | Status |",
        "| --- | --- | --- | --- |",
        f"| Missing/Empty Titles | 0 | {empty_titles} | {'✅ Pass' if empty_titles == 0 else '❌ Fail'} |",
        f"| Missing/Empty Bodies | 0 | {empty_bodies} | {'✅ Pass' if empty_bodies == 0 else '❌ Fail'} |",
        f"| Duplicate (Title+Body) | 0 | {duplicate_count} | {'✅ Pass' if duplicate_count == 0 else '❌ Fail'} |",
        f"| Category Balance | 200 per category | {', '.join([f'{c}: {category_counts.get(c, 0)}' for c in sorted(df['category'].unique())])} | {'✅ Pass' if all(category_counts == 200) else '⚠️ Warning'} |",
        "",
        "## Sample Tickets Preview",
        ""
    ])

    for cat in sorted(df["category"].unique()):
        cat_df = df[df["category"] == cat].head(2)
        report.append(f"### Category: {cat}")
        for _, row in cat_df.iterrows():
            report.append(f"**Ticket ID**: {row.get('ticket_id', 'N/A')} - *{row['title']}*")
            report.append(f"> {row['body']}")
            report.append("")

    with open(filepath, "w", encoding="utf-8") as f:
        f.write("\n".join(report))

    logger.info(f"Dataset report successfully written to {filepath}")
