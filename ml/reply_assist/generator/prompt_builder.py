from ..models import Conversation

# Category-specific guidance derived from the design document.
CATEGORY_GUIDANCE = {
    "Account Access": {
        "focus": [
            "Login",
            "Password reset",
            "Account recovery",
            "Verification",
        ],
        "strategy": [
            "Acknowledge the concern.",
            "Explain the next appropriate step.",
            "Avoid guarantees or promises.",
            "Escalate when needed.",
        ],
    },
    "Billing": {
        "focus": [
            "Payments",
            "Refunds",
            "Invoices",
            "Charges",
        ],
        "strategy": [
            "Acknowledge the concern.",
            "Review transaction details.",
            "Do not promise refunds.",
            "Escalate when needed.",
        ],
    },
    "Technical": {
        "focus": [
            "Application errors",
            "API issues",
            "Bugs",
            "Sync problems",
        ],
        "strategy": [
            "Acknowledge the issue.",
            "Collect troubleshooting information.",
            "Avoid assumptions.",
            "Escalate when needed.",
        ],
    },
    "Feature Request": {
        "focus": [
            "Enhancements",
            "Usability",
            "Customization",
        ],
        "strategy": [
            "Thank the customer.",
            "Document the request.",
            "Do not promise implementation.",
        ],
    },
    "Subscription": {
        "focus": [
            "Upgrade",
            "Downgrade",
            "Cancellation",
            "Renewal",
        ],
        "strategy": [
            "Review subscription request.",
            "Clarify available options.",
            "Avoid guarantees.",
        ],
    },
    "Performance": {
        "focus": [
            "Latency",
            "Slow response",
            "Loading",
        ],
        "strategy": [
            "Acknowledge performance issue.",
            "Investigate possible causes.",
            "Avoid assumptions.",
        ],
    },
    "Security": {
        "focus": [
            "Authentication",
            "MFA",
            "Suspicious activity",
        ],
        "strategy": [
            "Prioritize account safety.",
            "Never request passwords or OTPs.",
            "Explain verification steps.",
        ],
    },
    "Notifications": {
        "focus": [
            "Email",
            "SMS",
            "Push notifications",
        ],
        "strategy": [
            "Review notification settings.",
            "Investigate delivery issues.",
        ],
    },
    "Data Management": {
        "focus": [
            "Import",
            "Export",
            "Deletion",
            "Backup",
        ],
        "strategy": [
            "Review requested operation.",
            "Investigate failures.",
        ],
    },
    "General Inquiry": {
        "focus": [
            "General questions",
            "Documentation",
            "Product information",
        ],
        "strategy": [
            "Answer professionally.",
            "Provide guidance.",
        ],
    },
}

DEFAULT_GUIDANCE = {
    "focus": [],
    "strategy": [
        "Acknowledge the customer.",
        "Provide the next appropriate step.",
    ],
}


class ReplyPromptBuilder:

    @staticmethod
    def build(
        conversation: Conversation,
        conversation_summary: str,
    ) -> str:

        guidance = CATEGORY_GUIDANCE.get(
            conversation.ticket.category,
            DEFAULT_GUIDANCE,
        )

        return f"""
You are a professional customer support assistant.

Generate a reply that:

- acknowledges the customer's concern
- is empathetic
- is concise
- explains the next step
- never promises refunds
- never guarantees resolution
- never asks for passwords, OTPs or sensitive information
- sounds professional

Category:
{conversation.ticket.category}

Focus Areas:
{", ".join(guidance["focus"])}

Response Strategy:
{chr(10).join("- " + s for s in guidance["strategy"])}

Ticket Title:
{conversation.ticket.title}

Ticket Description:
{conversation.ticket.description}

Conversation Summary:
{conversation_summary}

Generate ONLY the suggested reply.
""".strip()