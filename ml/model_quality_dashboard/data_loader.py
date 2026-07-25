"""
Data loader for model quality dashboard.

Supports two modes:
- 'sample': loads from local sample_data/ directory (default)
- 's3': loads from S3 bucket (requires AWS credentials and bucket configuration)
"""

import json
import os
from pathlib import Path
from typing import List, Dict, Any
from datetime import datetime


def load_reports(data_source: str = "sample") -> List[Dict[str, Any]]:
    """
    Load all evaluation reports from the configured data source.
    
    Args:
        data_source: Either 'sample' or 's3'
        
    Returns:
        List of report dictionaries, sorted by timestamp (oldest to newest)
    """
    if data_source == "sample":
        return _load_sample_reports()
    elif data_source == "s3":
        return _load_s3_reports()
    else:
        raise ValueError(f"Unknown data source: {data_source}. Must be 'sample' or 's3'")


def get_latest_report(data_source: str = "sample") -> Dict[str, Any]:
    """
    Load the most recent evaluation report.
    
    Args:
        data_source: Either 'sample' or 's3'
        
    Returns:
        The latest report dictionary
    """
    reports = load_reports(data_source)
    if not reports:
        raise ValueError("No reports found")
    return reports[-1]  # Returns the most recent (last in sorted list)


def _load_sample_reports() -> List[Dict[str, Any]]:
    """Load reports from local sample_data directory."""
    sample_dir = Path(__file__).parent / "sample_data"
    
    if not sample_dir.exists():
        raise FileNotFoundError(f"Sample data directory not found: {sample_dir}")
    
    reports = []
    for json_file in sample_dir.glob("*.json"):
        try:
            with open(json_file, 'r') as f:
                report = json.load(f)
                reports.append(report)
        except Exception as e:
            print(f"Warning: Failed to load {json_file}: {e}")
    
    # Sort by timestamp
    reports.sort(key=lambda r: r.get("timestamp", ""))
    
    return reports


def _load_s3_reports() -> List[Dict[str, Any]]:
    """
    Load reports from S3 bucket.
    
    Configuration via environment variables:
    - EVAL_REPORTS_BUCKET: S3 bucket name (default: 'deskflow-ml-eval-reports')
    - EVAL_REPORTS_PREFIX: Prefix/path within bucket (default: 'eval-reports/')
    - AWS_REGION: AWS region (default: 'us-east-1')
    
    TODO: Update bucket name and prefix once finalized with team.
    """
    try:
        import boto3
        from botocore.exceptions import ClientError
    except ImportError:
        raise ImportError(
            "boto3 is required for S3 data source. "
            "Install with: pip install boto3"
        )
    
    # Configuration - UPDATE THESE once confirmed with team
    bucket_name = os.getenv("EVAL_REPORTS_BUCKET", "deskflow-ml-eval-reports")
    prefix = os.getenv("EVAL_REPORTS_PREFIX", "eval-reports/")
    region = os.getenv("AWS_REGION", "us-east-1")
    
    # TODO: Confirm exact S3 bucket structure:
    # Expected structure: s3://<bucket-name>/eval-reports/<model-name>/history/<timestamp>.json
    # For now, assuming all reports are under a common history/ directory
    
    s3_client = boto3.client('s3', region_name=region)
    
    reports = []
    
    try:
        # List all JSON files in the history directory
        paginator = s3_client.get_paginator('list_objects_v2')
        pages = paginator.paginate(Bucket=bucket_name, Prefix=f"{prefix}history/")
        
        for page in pages:
            if 'Contents' not in page:
                continue
                
            for obj in page['Contents']:
                key = obj['Key']
                if not key.endswith('.json'):
                    continue
                
                try:
                    # Download and parse the report
                    response = s3_client.get_object(Bucket=bucket_name, Key=key)
                    report_data = json.loads(response['Body'].read().decode('utf-8'))
                    reports.append(report_data)
                except Exception as e:
                    print(f"Warning: Failed to load {key}: {e}")
        
        # Also try to load latest.json if it exists
        try:
            response = s3_client.get_object(Bucket=bucket_name, Key=f"{prefix}latest.json")
            latest_report = json.loads(response['Body'].read().decode('utf-8'))
            # Only add if not already in history
            if not any(r.get('timestamp') == latest_report.get('timestamp') for r in reports):
                reports.append(latest_report)
        except ClientError as e:
            if e.response['Error']['Code'] != 'NoSuchKey':
                print(f"Warning: Error loading latest.json: {e}")
        
        # Sort by timestamp
        reports.sort(key=lambda r: r.get("timestamp", ""))
        
        return reports
        
    except ClientError as e:
        error_code = e.response['Error']['Code']
        if error_code == 'NoSuchBucket':
            raise ValueError(
                f"S3 bucket '{bucket_name}' does not exist. "
                "Please verify EVAL_REPORTS_BUCKET environment variable."
            )
        elif error_code == 'AccessDenied':
            raise ValueError(
                f"Access denied to S3 bucket '{bucket_name}'. "
                "Please verify AWS credentials and bucket permissions."
            )
        else:
            raise


def get_data_source_from_env() -> str:
    """
    Get the configured data source from environment.
    
    Checks DATA_SOURCE environment variable, defaults to 'sample'.
    """
    return os.getenv("DATA_SOURCE", "sample").lower()
