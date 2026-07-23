# Quick Start Guide

Get the dashboard running in 3 steps:

## 1. Setup
```cmd
setup.bat
```

## 2. Activate Environment
```cmd
.venv\Scripts\activate.bat
```

## 3. Run Dashboard
```cmd
streamlit run app.py
```

The dashboard will open in your browser at http://localhost:8501

## What You'll See

- **Latest metrics** from the most recent eval run
- **Trend charts** showing model performance over time
- **Confidence distribution** for each model
- **Volume by category** for category classification

## Using Real S3 Data

Before running step 3:
```cmd
set DATA_SOURCE=s3
set EVAL_REPORTS_BUCKET=your-bucket-name
```

See [README.md](README.md) for full configuration details.
