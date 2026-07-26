"""Generate the security PDF report. Called by the CI workflow."""
import os
import datetime

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable,
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle


def env(k, default="N/A"):
    return os.environ.get(k, default) or default


def status_color(s):
    s = (s or "").upper()
    if s == "PASS":    return colors.HexColor("#28a745")
    if s == "FAIL":    return colors.HexColor("#dc3545")
    if s == "SKIPPED": return colors.HexColor("#6c757d")
    return colors.black


def badge(s):
    return "[" + (s or "N/A").upper() + "]"


def make_table(data, col_widths=None):
    if col_widths is None:
        col_widths = [5 * cm, 12 * cm]
    t = Table(data, colWidths=col_widths)
    t.setStyle(TableStyle([
        ("BACKGROUND",     (0, 0),  (0, -1),  colors.HexColor("#f0f0f0")),
        ("FONTNAME",       (0, 0),  (0, -1),  "Helvetica-Bold"),
        ("FONTSIZE",       (0, 0),  (-1, -1), 9),
        ("GRID",           (0, 0),  (-1, -1), 0.4, colors.lightgrey),
        ("ROWBACKGROUNDS", (0, 0),  (-1, -1), [colors.white, colors.HexColor("#fafafa")]),
        ("LEFTPADDING",    (0, 0),  (-1, -1), 6),
        ("RIGHTPADDING",   (0, 0),  (-1, -1), 6),
    ]))
    return t


def main():
    os.makedirs("security-reports", exist_ok=True)
    doc = SimpleDocTemplate(
        "security-reports/security-report.pdf",
        pagesize=A4,
        leftMargin=2 * cm, rightMargin=2 * cm,
        topMargin=2 * cm,  bottomMargin=2 * cm,
    )

    styles = getSampleStyleSheet()
    H1 = ParagraphStyle("H1", parent=styles["Heading1"], fontSize=18,
                        spaceAfter=6, textColor=colors.HexColor("#1a1a2e"))
    H2 = ParagraphStyle("H2", parent=styles["Heading2"], fontSize=13,
                        spaceAfter=4, textColor=colors.HexColor("#16213e"))
    SM = ParagraphStyle("SM", parent=styles["Normal"], fontSize=9,
                        textColor=colors.grey)

    story = []
    story.append(Spacer(1, 1 * cm))
    story.append(Paragraph("Security CI Report", H1))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#1a1a2e")))
    story.append(Spacer(1, 0.4 * cm))

    # General Information
    story.append(Paragraph("General Information", H2))
    story.append(make_table([
        ["Repository",   env("GITHUB_REPOSITORY")],
        ["Branch",       env("GITHUB_REF_NAME")],
        ["Commit SHA",   env("GITHUB_SHA")],
        ["Workflow Run", env("GITHUB_RUN_ID")],
        ["Triggered By", env("GITHUB_EVENT_NAME")],
        ["Commit Range", env("COMMIT_RANGE")],
        ["Project Type", env("PROJECT_TYPE")],
        ["Report Date",  datetime.datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")],
    ]))
    story.append(Spacer(1, 0.5 * cm))

    # Test Summary
    cov_status = env("COVERAGE_STATUS")
    story.append(Paragraph("Test Summary", H2))
    t2 = make_table([
        ["Tests Executed", "Yes" if env("TESTS_RAN") == "true" else "Skipped"],
        ["Coverage %",     env("COVERAGE_PCT")],
        ["Status",         badge(cov_status)],
        ["Threshold",      env("COV_THRESHOLD", "60") + "%"],
    ])
    t2.setStyle(TableStyle([
        ("TEXTCOLOR", (1, 2), (1, 2), status_color(cov_status)),
        ("FONTNAME",  (1, 2), (1, 2), "Helvetica-Bold"),
    ]))
    story.append(t2)
    story.append(Spacer(1, 0.5 * cm))

    # Dependency Audit
    audit_status = env("AUDIT_STATUS")
    story.append(Paragraph("Dependency Audit", H2))
    t3 = make_table([
        ["Tool",            env("AUDIT_TOOL")],
        ["Total Vulns",     env("TOTAL_VULNS")],
        ["High / Critical", env("CRITICAL_HIGH")],
        ["Status",          badge(audit_status)],
    ])
    t3.setStyle(TableStyle([
        ("TEXTCOLOR", (1, 3), (1, 3), status_color(audit_status)),
        ("FONTNAME",  (1, 3), (1, 3), "Helvetica-Bold"),
    ]))
    story.append(t3)
    story.append(Spacer(1, 0.5 * cm))

    # SAST
    sast_status = env("SAST_STATUS")
    story.append(Paragraph("SAST - Semgrep", H2))
    t4 = make_table([
        ["Total Findings",     env("SEMGREP_FINDINGS")],
        ["Severity Breakdown", env("SEVERITY_SUMMARY")],
        ["Status",             badge(sast_status)],
    ])
    t4.setStyle(TableStyle([
        ("TEXTCOLOR", (1, 2), (1, 2), status_color(sast_status)),
        ("FONTNAME",  (1, 2), (1, 2), "Helvetica-Bold"),
    ]))
    story.append(t4)
    story.append(Spacer(1, 0.5 * cm))

    # Secret Scan
    secret_status = env("SECRET_STATUS")
    story.append(Paragraph("Secret Detection - Gitleaks", H2))
    t5 = make_table([
        ["Secrets Found", env("SECRETS_FOUND")],
        ["Status",        badge(secret_status)],
    ])
    t5.setStyle(TableStyle([
        ("TEXTCOLOR", (1, 1), (1, 1), status_color(secret_status)),
        ("FONTNAME",  (1, 1), (1, 1), "Helvetica-Bold"),
    ]))
    story.append(t5)
    story.append(Spacer(1, 0.6 * cm))

    # Final Summary
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#1a1a2e")))
    story.append(Spacer(1, 0.3 * cm))
    story.append(Paragraph("Final CI Summary", H2))

    statuses = [cov_status, audit_status, sast_status, secret_status]
    overall  = "FAIL" if "FAIL" in [s.upper() for s in statuses] else "PASS"

    summary_data = [
        ["Check",            "Status"],
        ["Coverage",         badge(cov_status)],
        ["Dependency Audit", badge(audit_status)],
        ["SAST",             badge(sast_status)],
        ["Secret Scan",      badge(secret_status)],
        ["OVERALL",          badge(overall)],
    ]
    style_cmds = [
        ("BACKGROUND",     (0, 0),  (-1, 0),  colors.HexColor("#1a1a2e")),
        ("TEXTCOLOR",      (0, 0),  (-1, 0),  colors.white),
        ("FONTNAME",       (0, 0),  (-1, 0),  "Helvetica-Bold"),
        ("FONTSIZE",       (0, 0),  (-1, -1), 10),
        ("GRID",           (0, 0),  (-1, -1), 0.4, colors.lightgrey),
        ("BACKGROUND",     (0, -1), (-1, -1), colors.HexColor("#1a1a2e")),
        ("TEXTCOLOR",      (0, -1), (-1, -1), colors.white),
        ("FONTNAME",       (0, -1), (-1, -1), "Helvetica-Bold"),
        ("ROWBACKGROUNDS", (0, 1),  (-1, -2), [colors.white, colors.HexColor("#fafafa")]),
        ("LEFTPADDING",    (0, 0),  (-1, -1), 8),
        ("TOPPADDING",     (0, 0),  (-1, -1), 5),
        ("BOTTOMPADDING",  (0, 0),  (-1, -1), 5),
    ]
    for i in range(1, len(summary_data)):
        style_cmds.append(("TEXTCOLOR", (1, i), (1, i),
                            status_color(summary_data[i][1].strip("[]"))))
    t6 = Table(summary_data, colWidths=[8 * cm, 9 * cm])
    t6.setStyle(TableStyle(style_cmds))
    story.append(t6)
    story.append(Spacer(1, 0.4 * cm))
    story.append(Paragraph("Generated automatically by the Security CI pipeline.", SM))

    doc.build(story)
    print("PDF generated: security-reports/security-report.pdf")


if __name__ == "__main__":
    main()
