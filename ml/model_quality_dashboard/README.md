# Model Quality Dashboard

A Streamlit-based dashboard for monitoring and visualizing evaluation metrics for DeskFlow ML models.

## Overview

This dashboard displays evaluation results from the DeskFlow eval harness, providing insights into model performance across evaluation runs. It supports three models:

- **category_baseline**: Baseline category classification model
- **category_embeddings**: Embeddings-based category classification model  
- **priority_model**: Priority classification model

## Features

### 1. Latest Evaluation Metrics
Displays the most recent evaluation run's aggregate metrics:
- **Category Models**: F1 scores per class and macro-averaged F1
- **Priority Model**: Precision and recall per priority level

### 2. Trend View
Visualizes how model performance changes over time:
- Category models macro F1 trend lines
- Priority model precision & recall trends
- Requires at least 2 evaluation runs to display

### 3. Confidence Distribution
Histogram of prediction confidence scores, filterable by model. Shows:
- Distribution of confidence values
- Mean, min, and max confidence statistics

### 4. Volume by Category
Bar chart showing prediction volume by true category label for category models.

## Data Sources

The dashboard supports two data source modes:

### Sample Mode (Default)
Loads evaluation reports from local `sample_data/` directory. This is the default mode and requires no AWS setup.

### S3 Mode
Loads evaluation reports from an S3 bucket. Requires AWS credentials and bucket configuration.

**To switch to S3 mode:**
1. Set environment variable: `DATA_SOURCE=s3`
2. Configure S3 settings (see Configuration section)
3. Ensure AWS credentials are available

## Setup

### Prerequisites
- Python 3.8 or higher
- pip

### Installation

#### Windows
Run the setup script:
```cmd
setup.bat
```

This will:
1. Delete any existing virtual environment
2. Create a new virtual environment in `.venv/`
3. Install all dependencies from `requirements.txt`

#### Manual Setup
```bash
# Create virtual environment
python -m venv .venv

# Activate virtual environment
# Windows:
.venv\Scripts\activate.bat
# Linux/Mac:
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

## Running the Dashboard

### Local Development

1. Activate the virtual environment:
```cmd
.venv\Scripts\activate.bat
```

2. Run the Streamlit app:
```cmd
streamlit run app.py
```

3. Open your browser to the URL shown (typically http://localhost:8501)

### Using Sample Data (Default)

No additional configuration needed. The dashboard will automatically load sample reports from `sample_data/`.

### Using S3 Data

Set the data source environment variable before running:

**Windows (cmd):**
```cmd
set DATA_SOURCE=s3
streamlit run app.py
```

**Windows (PowerShell):**
```powershell
$env:DATA_SOURCE="s3"
streamlit run app.py
```

**Linux/Mac:**
```bash
export DATA_SOURCE=s3
streamlit run app.py
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATA_SOURCE` | Data source mode: `sample` or `s3` | `sample` |
| `EVAL_REPORTS_BUCKET` | S3 bucket name (S3 mode only) | `deskflow-ml-eval-reports` |
| `EVAL_REPORTS_PREFIX` | S3 prefix/path (S3 mode only) | `eval-reports/` |
| `AWS_REGION` | AWS region (S3 mode only) | `us-east-1` |

### S3 Configuration

**⚠️ IMPORTANT**: The S3 bucket name and structure are placeholder values and need to be updated once finalized with the team.

To configure S3 access:

1. **Update S3 settings** in `data_loader.py` or via environment variables:
   ```python
   # Current placeholder values in data_loader.py:
   bucket_name = os.getenv("EVAL_REPORTS_BUCKET", "deskflow-ml-eval-reports")
   prefix = os.getenv("EVAL_REPORTS_PREFIX", "eval-reports/")
   ```

2. **Expected S3 structure**:
   ```
   s3://<bucket-name>/eval-reports/
   ├── history/
   │   ├── 2026-07-18T10-30-00.000000+00-00.json
   │   ├── 2026-07-19T15-50-18.894718+00-00.json
   │   └── ...
   └── latest.json
   ```

3. **AWS credentials**: Ensure credentials are configured via:
   - AWS CLI configuration (`aws configure`)
   - Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
   - IAM role (if running on EC2/ECS)

4. **Required S3 permissions**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:GetObject",
           "s3:ListBucket"
         ],
         "Resource": [
           "arn:aws:s3:::<bucket-name>/*",
           "arn:aws:s3:::<bucket-name>"
         ]
       }
     ]
   }
   ```

## Deployment

### Option 1: Streamlit Community Cloud

1. Push the repository to GitHub
2. Sign in to [Streamlit Community Cloud](https://streamlit.io/cloud)
3. Create a new app pointing to this directory
4. Set environment variables in the app settings:
   - `DATA_SOURCE=s3`
   - `EVAL_REPORTS_BUCKET=<your-bucket>`
   - `AWS_ACCESS_KEY_ID=<your-key>`
   - `AWS_SECRET_ACCESS_KEY=<your-secret>`

### Option 2: Docker Container

Create a `Dockerfile`:
```dockerfile
FROM python:3.10-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8501

CMD ["streamlit", "run", "app.py", "--server.address", "0.0.0.0"]
```

Build and run:
```bash
docker build -t model-quality-dashboard .
docker run -p 8501:8501 \
  -e DATA_SOURCE=s3 \
  -e EVAL_REPORTS_BUCKET=<bucket> \
  model-quality-dashboard
```

### Option 3: Internal Server

1. Set up a dedicated server or VM
2. Clone the repository
3. Run setup and start the app:
   ```bash
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   
   # Run with nohup for background execution
   nohup streamlit run app.py --server.port 8501 &
   ```
4. Configure reverse proxy (nginx/Apache) for HTTPS and authentication
5. Set up process manager (systemd/supervisor) for auto-restart

### Squad Access Considerations

- **Authentication**: Streamlit Community Cloud has built-in authentication. For self-hosted, add authentication layer (e.g., nginx basic auth, OAuth proxy)
- **Network access**: Ensure the deployment is accessible to the team (VPN, internal network, or public with auth)
- **Auto-refresh**: Dashboard data caches for 5 minutes by default (configurable via `@st.cache_data` TTL)
- **Monitoring**: Set up uptime monitoring and alerting for production deployments

## Report Schema

The dashboard expects evaluation reports with the following structure:

```json
{
  "timestamp": "2026-07-19T15:50:18.894718+00:00",
  "git_commit": "a8e2758",
  "predictions": [
    {
      "ticket_id": "TKT-01914",
      "model": "category_baseline",
      "true_label": "General Inquiry",
      "predicted_label": "General Inquiry",
      "confidence": 0.598
    }
  ],
  "aggregate_metrics": {
    "category_baseline_f1": {
      "Technical": 0.74,
      "Billing": 0.70,
      "...": "..."
    },
    "category_embeddings_f1": {
      "Technical": 0.87,
      "...": "..."
    },
    "priority_precision_recall": {
      "Urgent": {"precision": 0.60, "recall": 0.64},
      "High": {"precision": 0.77, "recall": 0.73},
      "...": "..."
    }
  }
}
```

## Sample Data

The `sample_data/` directory contains 5 sample evaluation reports spanning July 18-22, 2026. These demonstrate:
- Different metric values across runs (showing improvement trends)
- Varying prediction confidence distributions
- Multiple categories and priority levels
- Realistic report structure matching the eval harness schema

## Development

### Project Structure

```
ml/model_quality_dashboard/
├── app.py                  # Main Streamlit application
├── data_loader.py          # Data loading logic (sample/S3)
├── sample_data/            # Sample evaluation reports
│   ├── 2026-07-18T10-30-00.000000+00-00.json
│   ├── 2026-07-19T15-50-18.894718+00-00.json
│   ├── 2026-07-20T04-24-34.586639+00-00.json
│   ├── 2026-07-21T09-15-22.123456+00-00.json
│   └── 2026-07-22T14-00-00.000000+00-00.json
├── requirements.txt        # Python dependencies
├── setup.bat              # Windows setup script
├── .gitignore             # Git ignore patterns
└── README.md              # This file
```

### Adding New Visualizations

1. Add new function in `app.py`:
   ```python
   def display_new_metric(reports: List[Dict[str, Any]]):
       st.header("New Metric")
       # Implementation
   ```

2. Call from `main()`:
   ```python
   display_new_metric(reports)
   ```

3. Restart Streamlit to see changes (auto-reloads on file save)

### Modifying Data Source

To support additional data sources, update `data_loader.py`:

1. Add new loading function:
   ```python
   def _load_custom_source() -> List[Dict[str, Any]]:
       # Implementation
       pass
   ```

2. Update `load_reports()`:
   ```python
   elif data_source == "custom":
       return _load_custom_source()
   ```

## Troubleshooting

### Dashboard won't start
- Verify virtual environment is activated
- Check all dependencies are installed: `pip list`
- Ensure Python 3.8+ is being used: `python --version`

### No data displayed
- **Sample mode**: Verify `sample_data/` directory exists and contains JSON files
- **S3 mode**: Check AWS credentials and bucket permissions
- Check browser console for JavaScript errors
- Review Streamlit terminal output for Python errors

### S3 access errors
- Verify `DATA_SOURCE=s3` is set
- Check AWS credentials are configured correctly
- Confirm S3 bucket name and prefix in environment variables
- Verify IAM permissions include `s3:GetObject` and `s3:ListBucket`

### Metrics not updating
- Dashboard caches data for 5 minutes by default
- Force refresh: click the refresh icon in top-right corner
- Or restart the Streamlit server

## Related Components

- **Eval Harness**: `ml/eval_harness/` - Generates the evaluation reports consumed by this dashboard
- **Baseline Classifier**: `ml/baseline_classifier/` - One of the models being evaluated
- **Dataset Generator**: `ml/dataset_generator/` - Creates training/evaluation datasets

## License

Part of the DeskFlow Backend project. See root LICENSE file for details.
