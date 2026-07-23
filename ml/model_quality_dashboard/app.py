"""
Model Quality Dashboard

Displays evaluation metrics and trends for DeskFlow ML models:
- category_baseline
- category_embeddings  
- priority_model
"""

import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime
from typing import List, Dict, Any

from data_loader import load_reports, get_latest_report, get_data_source_from_env


# Page configuration
st.set_page_config(
    page_title="Model Quality Dashboard",
    page_icon="📊",
    layout="wide"
)

# Styling
st.markdown("""
<style>
    .metric-card {
        background-color: #f0f2f6;
        padding: 20px;
        border-radius: 10px;
        margin: 10px 0;
    }
    .stMetric {
        background-color: white;
        padding: 10px;
        border-radius: 5px;
    }
</style>
""", unsafe_allow_html=True)


@st.cache_data(ttl=300)  # Cache for 5 minutes
def load_data(data_source: str):
    """Load and cache evaluation reports."""
    return load_reports(data_source)


def format_timestamp(timestamp_str: str) -> str:
    """Format ISO timestamp to readable format."""
    try:
        dt = datetime.fromisoformat(timestamp_str.replace('+00-00', '+00:00'))
        return dt.strftime("%Y-%m-%d %H:%M")
    except:
        return timestamp_str


def calculate_macro_f1(f1_dict: Dict[str, float]) -> float:
    """Calculate macro-averaged F1 score."""
    if not f1_dict:
        return 0.0
    return sum(f1_dict.values()) / len(f1_dict)


def calculate_macro_precision_recall(pr_dict: Dict[str, Dict[str, float]]) -> tuple:
    """Calculate macro-averaged precision and recall."""
    if not pr_dict:
        return 0.0, 0.0
    precisions = [metrics['precision'] for metrics in pr_dict.values()]
    recalls = [metrics['recall'] for metrics in pr_dict.values()]
    return sum(precisions) / len(precisions), sum(recalls) / len(recalls)


def display_latest_metrics(latest_report: Dict[str, Any]):
    """Display the latest evaluation metrics."""
    st.header("📈 Latest Evaluation Metrics")
    
    metrics = latest_report.get('aggregate_metrics', {})
    timestamp = latest_report.get('timestamp', 'Unknown')
    git_commit = latest_report.get('git_commit', 'Unknown')
    
    col1, col2 = st.columns(2)
    with col1:
        st.metric("Timestamp", format_timestamp(timestamp))
    with col2:
        st.metric("Git Commit", git_commit)
    
    st.markdown("---")
    
    # Category models F1 scores
    st.subheader("🏷️ Category Classification Models")
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.markdown("**category_baseline**")
        baseline_f1 = metrics.get('category_baseline_f1', {})
        if baseline_f1:
            macro_f1 = calculate_macro_f1(baseline_f1)
            st.metric("Macro F1", f"{macro_f1:.3f}")
            
            # Per-class F1 scores
            df_baseline = pd.DataFrame([
                {"Category": cat, "F1 Score": score}
                for cat, score in baseline_f1.items()
            ]).sort_values("F1 Score", ascending=False)
            st.dataframe(df_baseline, hide_index=True, use_container_width=True)
    
    with col2:
        st.markdown("**category_embeddings**")
        embeddings_f1 = metrics.get('category_embeddings_f1', {})
        if embeddings_f1:
            macro_f1 = calculate_macro_f1(embeddings_f1)
            st.metric("Macro F1", f"{macro_f1:.3f}")
            
            # Per-class F1 scores
            df_embeddings = pd.DataFrame([
                {"Category": cat, "F1 Score": score}
                for cat, score in embeddings_f1.items()
            ]).sort_values("F1 Score", ascending=False)
            st.dataframe(df_embeddings, hide_index=True, use_container_width=True)
    
    st.markdown("---")
    
    # Priority model precision/recall
    st.subheader("⚡ Priority Classification Model")
    
    priority_pr = metrics.get('priority_precision_recall', {})
    if priority_pr:
        macro_prec, macro_rec = calculate_macro_precision_recall(priority_pr)
        
        col1, col2 = st.columns(2)
        with col1:
            st.metric("Macro Precision", f"{macro_prec:.3f}")
        with col2:
            st.metric("Macro Recall", f"{macro_rec:.3f}")
        
        # Per-class precision/recall
        df_priority = pd.DataFrame([
            {
                "Priority": priority,
                "Precision": metrics['precision'],
                "Recall": metrics['recall']
            }
            for priority, metrics in priority_pr.items()
        ])
        
        # Order by priority level
        priority_order = ["Urgent", "High", "Medium", "Low"]
        df_priority['priority_sort'] = df_priority['Priority'].apply(
            lambda x: priority_order.index(x) if x in priority_order else 999
        )
        df_priority = df_priority.sort_values('priority_sort').drop('priority_sort', axis=1)
        
        st.dataframe(df_priority, hide_index=True, use_container_width=True)


def display_trends(reports: List[Dict[str, Any]]):
    """Display metric trends over time."""
    st.header("📊 Metric Trends")
    
    if len(reports) < 2:
        st.info("Need at least 2 evaluation runs to display trends.")
        return
    
    # Prepare data for trends
    trend_data = []
    for report in reports:
        timestamp = report.get('timestamp', '')
        metrics = report.get('aggregate_metrics', {})
        
        # Category baseline macro F1
        baseline_f1 = metrics.get('category_baseline_f1', {})
        baseline_macro = calculate_macro_f1(baseline_f1)
        
        # Category embeddings macro F1
        embeddings_f1 = metrics.get('category_embeddings_f1', {})
        embeddings_macro = calculate_macro_f1(embeddings_f1)
        
        # Priority model macro precision/recall
        priority_pr = metrics.get('priority_precision_recall', {})
        priority_prec, priority_rec = calculate_macro_precision_recall(priority_pr)
        
        trend_data.append({
            'Timestamp': format_timestamp(timestamp),
            'timestamp_raw': timestamp,
            'category_baseline': baseline_macro,
            'category_embeddings': embeddings_macro,
            'priority_precision': priority_prec,
            'priority_recall': priority_rec
        })
    
    df_trends = pd.DataFrame(trend_data)
    
    # Category models F1 trend
    st.subheader("Category Models - Macro F1 Trend")
    fig_category = go.Figure()
    fig_category.add_trace(go.Scatter(
        x=df_trends['Timestamp'],
        y=df_trends['category_baseline'],
        mode='lines+markers',
        name='category_baseline',
        line=dict(color='#1f77b4', width=2),
        marker=dict(size=8)
    ))
    fig_category.add_trace(go.Scatter(
        x=df_trends['Timestamp'],
        y=df_trends['category_embeddings'],
        mode='lines+markers',
        name='category_embeddings',
        line=dict(color='#ff7f0e', width=2),
        marker=dict(size=8)
    ))
    fig_category.update_layout(
        xaxis_title="Evaluation Run",
        yaxis_title="Macro F1 Score",
        yaxis_range=[0, 1],
        hovermode='x unified',
        template='plotly_white',
        height=400
    )
    st.plotly_chart(fig_category, use_container_width=True)
    
    # Priority model precision/recall trend
    st.subheader("Priority Model - Precision & Recall Trend")
    fig_priority = go.Figure()
    fig_priority.add_trace(go.Scatter(
        x=df_trends['Timestamp'],
        y=df_trends['priority_precision'],
        mode='lines+markers',
        name='Precision',
        line=dict(color='#2ca02c', width=2),
        marker=dict(size=8)
    ))
    fig_priority.add_trace(go.Scatter(
        x=df_trends['Timestamp'],
        y=df_trends['priority_recall'],
        mode='lines+markers',
        name='Recall',
        line=dict(color='#d62728', width=2),
        marker=dict(size=8)
    ))
    fig_priority.update_layout(
        xaxis_title="Evaluation Run",
        yaxis_title="Score",
        yaxis_range=[0, 1],
        hovermode='x unified',
        template='plotly_white',
        height=400
    )
    st.plotly_chart(fig_priority, use_container_width=True)


def display_confidence_distribution(reports: List[Dict[str, Any]]):
    """Display confidence distribution across models."""
    st.header("🎯 Confidence Distribution")
    
    # Get latest report predictions
    latest_report = reports[-1]
    predictions = latest_report.get('predictions', [])
    
    if not predictions:
        st.warning("No predictions found in latest report.")
        return
    
    # Model selector
    models = sorted(list(set(p['model'] for p in predictions)))
    selected_model = st.selectbox("Select Model", models)
    
    # Filter predictions by model
    model_predictions = [p for p in predictions if p['model'] == selected_model]
    confidences = [p['confidence'] for p in model_predictions]
    
    if confidences:
        # Create histogram
        fig = px.histogram(
            confidences,
            nbins=20,
            labels={'value': 'Confidence', 'count': 'Number of Predictions'},
            title=f'Confidence Distribution - {selected_model}'
        )
        fig.update_layout(
            showlegend=False,
            template='plotly_white',
            height=400
        )
        fig.update_traces(marker_color='#636EFA')
        st.plotly_chart(fig, use_container_width=True)
        
        # Summary stats
        col1, col2, col3 = st.columns(3)
        with col1:
            st.metric("Mean Confidence", f"{sum(confidences)/len(confidences):.3f}")
        with col2:
            st.metric("Min Confidence", f"{min(confidences):.3f}")
        with col3:
            st.metric("Max Confidence", f"{max(confidences):.3f}")


def display_volume_by_category(reports: List[Dict[str, Any]]):
    """Display prediction volume by category."""
    st.header("📦 Volume by Category")
    
    # Get latest report predictions
    latest_report = reports[-1]
    predictions = latest_report.get('predictions', [])
    
    if not predictions:
        st.warning("No predictions found in latest report.")
        return
    
    # Filter for category models only
    category_predictions = [
        p for p in predictions 
        if p['model'] in ['category_baseline', 'category_embeddings']
    ]
    
    if not category_predictions:
        st.warning("No category predictions found.")
        return
    
    # Count by true_label
    category_counts = {}
    for pred in category_predictions:
        label = pred['true_label']
        category_counts[label] = category_counts.get(label, 0) + 1
    
    # Create bar chart
    df_volume = pd.DataFrame([
        {"Category": cat, "Count": count}
        for cat, count in category_counts.items()
    ]).sort_values("Count", ascending=False)
    
    fig = px.bar(
        df_volume,
        x='Category',
        y='Count',
        title='Prediction Volume by True Category',
        labels={'Count': 'Number of Predictions'}
    )
    fig.update_layout(
        template='plotly_white',
        height=400
    )
    fig.update_traces(marker_color='#AB63FA')
    st.plotly_chart(fig, use_container_width=True)


def main():
    """Main dashboard application."""
    st.title("🤖 Model Quality Dashboard")
    st.markdown("**DeskFlow ML Model Evaluation Metrics**")
    
    # Data source indicator
    data_source = get_data_source_from_env()
    if data_source == "sample":
        st.info("📁 Using sample data (local files). Set DATA_SOURCE=s3 to use S3.")
    else:
        st.success("☁️ Using S3 data source.")
    
    # Load data
    try:
        reports = load_data(data_source)
        
        if not reports:
            st.error("No evaluation reports found.")
            return
        
        st.success(f"✅ Loaded {len(reports)} evaluation report(s)")
        
        # Get latest report
        latest_report = reports[-1]
        
        # Display sections
        display_latest_metrics(latest_report)
        
        st.markdown("---")
        
        display_trends(reports)
        
        st.markdown("---")
        
        col1, col2 = st.columns(2)
        
        with col1:
            display_confidence_distribution(reports)
        
        with col2:
            display_volume_by_category(reports)
            
    except Exception as e:
        st.error(f"❌ Error loading data: {e}")
        st.exception(e)


if __name__ == "__main__":
    main()
