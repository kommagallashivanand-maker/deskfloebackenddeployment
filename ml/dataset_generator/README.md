# DeskFlow - Synthetic Support Ticket Dataset Generator

DeskFlow is a production-quality Python tool designed to generate high-quality, synthetic customer support tickets using the Gemini API. It is built to create balanced, realistic datasets for training AI ticket classification models.

## Project Overview

This utility generates a dataset of 2,000 unique customer support tickets across 10 balanced categories (200 tickets per category). The prompt engineering ensures that tickets do not contain leaky identifiers (such as ticket IDs, email addresses, phone numbers, order numbers, or URLs) and are written using realistic customer language with varying tones and writing styles.

### Supported Categories

1. Account Access
2. Billing
3. Technical
4. Feature Request
5. Subscription
6. Performance
7. Security
8. Notifications
9. Data Management
10. General Inquiry

## Folder Structure

```text
DeskFlow/
├── datasets/                   # Generated output datasets
│   ├── tickets_v1.csv          # Full 2,000 tickets dataset
│   └── reviewed_sample.csv     # 200-row random review sample
├── docs/                       # Reports and documentation
│   └── dataset_report.md       # Analysis report of class balance and text statistics
├── scripts/                    # Core orchestration and utilities
│   ├── gemini_client.py        # Gemini Client initialization and generation wrapper
│   ├── prompts.py              # LLM system prompt and prompt builder
│   ├── utils.py                # Dataset utilities (JSON parsing, deduplication, report generator)
│   └── generate_dataset.py     # Main orchestration pipeline
├── .env                        # Local environment variables configuration
├── requirements.txt            # Project dependencies list
└── README.md                   # Project overview and usage guidelines
```

## Setup & Installation

### Prerequisites

- Python 3.10 or higher
- A Gemini API Key from Google AI Studio

### Installation

1. Navigate to the project root directory:
   ```bash
   cd c:/training/DeskFlow
   ```

2. Create a virtual environment:
   ```bash
   python -m venv .venv
   ```

3. Activate the virtual environment:
   - **PowerShell** (Windows):
     ```powershell
     .venv\Scripts\Activate.ps1
     ```
   - **Command Prompt** (Windows):
     ```cmd
     .venv\Scripts\activate.bat
     ```
   - **Bash/zsh** (Linux/macOS):
     ```bash
     source .venv/bin/activate
     ```

4. Install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Configuration

Create a `.env` file in the root directory (or use the pre-configured one) with the following environment variables:

```ini
GEMINI_API_KEY=your_gemini_api_key_here
MODEL_NAME=models/gemini-3.1-flash-lite
```

## How to Run

### 1. Verification / Test Run (Dry Run)

To run a quick dry-run verification that generates only **5 tickets per category** (50 tickets total) and outputs dry-run datasets and reports:

```bash
python scripts/generate_dataset.py --dry-run
```

This will output:
- `datasets/tickets_v1_dry_run.csv`
- `datasets/reviewed_sample_dry_run.csv`
- `docs/dataset_report_dry_run.md`

### 2. Full Dataset Generation

To generate the full **2,000 tickets dataset** (200 tickets per category):

```bash
python scripts/generate_dataset.py
```

This will run the orchestration script, showing progress using a `tqdm` progress bar and saving logging output to the console.

## Expected Outputs

Upon a successful full run, the following outputs will be created under the project root:

1. **`datasets/tickets_v1.csv`**: The full dataset containing columns `ticket_id`, `title`, `body`, and `category`.
2. **`datasets/reviewed_sample.csv`**: A randomly sampled subset of 200 rows from the full dataset for manual auditing.
3. **`docs/dataset_report.md`**: A detailed report evaluating the class balance, verifying validation checks (no empty fields, no duplicate ticket bodies, etc.), and providing text statistics.

### Dataset Schema

The generated datasets contain the following columns:

- **`ticket_id`**: Locally generated sequential identifier (e.g., `TKT-00001`, `TKT-02000`).
- **`title`**: A short, realistic customer subject line (e.g., `"Cannot reset password"`).
- **`body`**: A descriptive customer issue statement, usually 2 to 5 sentences long (e.g., `"The password reset link expires immediately after clicking it."`).
- **`category`**: The classification category for the ticket (e.g., `"Account Access"`).
