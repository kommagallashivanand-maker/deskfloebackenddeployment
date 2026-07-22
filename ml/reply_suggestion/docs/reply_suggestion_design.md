# DF-036 – Reply Suggestion Design

## Objective
The Reply Suggestion module generates a professional first-response draft for support agents based on the ticket category and ticket content. The generated reply is only a suggestion and must be reviewed by a human agent before being sent.

## Goals
- Reduce response time.
- Maintain consistent communication.
- Improve reply quality.
- Follow organizational safety rules.

## High-Level Workflow

```text
Customer Ticket
      │
      ▼
Category Classification
      │
      ▼
Prompt Template Selection
      │
      ▼
Prompt Builder
      │
      ▼
LLM
      │
      ▼
Suggested Reply
      │
      ▼
Agent Review
      │
      ▼
Customer
```

## Generic Prompt Template

```text
You are a professional customer support assistant.

Category:
{category}

Ticket Title:
{title}

Customer Issue:
{description}

Generate a concise first response that:
- acknowledges the issue,
- is empathetic,
- explains the next step,
- does not promise outcomes,
- does not request passwords, OTPs or other sensitive information,
- remains professional.
```

# Category-wise Design

## Account Access

### Prompt Focus
- Login
- Password reset
- Account recovery
- Verification

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Cannot log in after password reset

**Customer Issue**

> I requested a password reset link earlier today and followed the instructions to change it. Now, whenever I try to sign in with the new password, the page just refreshes and keeps me on the login screen. I have tried clearing my browser cache but the issue persists.

**Suggested Reply**

Thank you for contacting us. We're sorry you're experiencing this issue. We'll help review your account access concern. Please avoid sharing passwords or OTPs. We'll guide you through the appropriate verification steps and work with you toward restoring access.

---

### Worked Example 2

**Ticket Title**

Locked out of my account

**Customer Issue**

> I keep getting an error saying my account is locked due to too many failed login attempts. I am positive I typed my password correctly, but maybe I had caps lock on by accident. Can you please unlock it for me?

**Suggested Reply**

Thank you for contacting us. We're sorry you're experiencing this issue. We'll help review your account access concern. Please avoid sharing passwords or OTPs. We'll guide you through the appropriate verification steps and work with you toward restoring access.

---

### Worked Example 3

**Ticket Title**

Two-factor authentication code not arriving

**Customer Issue**

> I am trying to log in, but the 2FA code is never sent to my device. I have checked my spam folder and waited several minutes, but nothing shows up. How can I get back into my account?

**Suggested Reply**

Thank you for contacting us. We're sorry you're experiencing this issue. We'll help review your account access concern. Please avoid sharing passwords or OTPs. We'll guide you through the appropriate verification steps and work with you toward restoring access.

---


## Billing

### Prompt Focus
- Payments
- Refunds
- Invoices
- Charges

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Double charge on my monthly subscription

**Customer Issue**

> I noticed two identical charges on my bank statement for this month. My subscription should only be billed once. Could you please look into this and process a refund for the extra charge?

**Suggested Reply**

Thank you for reaching out. We understand your billing concern. We'll review the transaction or subscription details and provide clarification. If additional information is required, we'll let you know during the investigation.

---

### Worked Example 2

**Ticket Title**

Why did my monthly rate increase?

**Customer Issue**

> My bill was ten dollars higher this month than it was last month. I haven't changed my plan or added any new services. Can someone explain why the price went up without any warning?

**Suggested Reply**

Thank you for reaching out. We understand your billing concern. We'll review the transaction or subscription details and provide clarification. If additional information is required, we'll let you know during the investigation.

---

### Worked Example 3

**Ticket Title**

Unable to update payment method

**Customer Issue**

> I have been trying to swap my expired credit card for a new one, but the website keeps giving me an error message. I don't want my service to get interrupted, so please help me fix this. The error code I am seeing is 402.

**Suggested Reply**

Thank you for reaching out. We understand your billing concern. We'll review the transaction or subscription details and provide clarification. If additional information is required, we'll let you know during the investigation.

---


## Technical

### Prompt Focus
- Application errors
- API issues
- Bugs
- Sync problems

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Application crashing on startup

**Customer Issue**

> Every time I try to launch the desktop application, it immediately crashes to the desktop. I have already tried reinstalling the software twice, but the issue persists. Could you please let me know what logs I need to send over for analysis?

**Suggested Reply**

Thank you for reporting this issue. We apologize for the inconvenience. We'll investigate the reported technical problem. If available, please keep any error messages or steps to reproduce the issue for troubleshooting.

---

### Worked Example 2

**Ticket Title**

API authentication failing

**Customer Issue**

> I am receiving a 403 Forbidden error when attempting to authenticate via the API using my standard token. It was working perfectly yesterday, but all calls are now being rejected. Is there a known outage or a change in the authentication requirements?

**Suggested Reply**

Thank you for reporting this issue. We apologize for the inconvenience. We'll investigate the reported technical problem. If available, please keep any error messages or steps to reproduce the issue for troubleshooting.

---

### Worked Example 3

**Ticket Title**

Data synchronization not working

**Customer Issue**

> My changes on the mobile app are not syncing to the web dashboard. I have checked my internet connection and logged out and back in, but the data is still missing. How can I force a manual sync?

**Suggested Reply**

Thank you for reporting this issue. We apologize for the inconvenience. We'll investigate the reported technical problem. If available, please keep any error messages or steps to reproduce the issue for troubleshooting.

---


## Feature Request

### Prompt Focus
- Enhancements
- Usability
- New functionality
- Customization

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Add Dark Mode to the desktop application

**Customer Issue**

> I spend hours every day working in your app and the bright white background is really starting to strain my eyes. Could you please implement a dark mode option in the settings menu? It would be a huge help for those of us working late shifts.

**Suggested Reply**

Thank you for your suggestion. We appreciate your feedback and understand how this enhancement could improve the product. We've documented your request and will share it with the product team for future evaluation.

---

### Worked Example 2

**Ticket Title**

Request for bulk export functionality

**Customer Issue**

> Currently, I have to download my data files one by one, which is extremely tedious. Is there any way you could add a button to export all reports as a single zip file? This feature would save me hours of manual labor every week.

**Suggested Reply**

Thank you for your suggestion. We appreciate your feedback and understand how this enhancement could improve the product. We've documented your request and will share it with the product team for future evaluation.

---

### Worked Example 3

**Ticket Title**

Need custom reporting dashboard widgets

**Customer Issue**

> The standard charts are okay, but they don't show the specific metrics my team cares about. Can we get the ability to create custom dashboard widgets? Being able to pin specific data points would make our daily standups much more efficient.

**Suggested Reply**

Thank you for your suggestion. We appreciate your feedback and understand how this enhancement could improve the product. We've documented your request and will share it with the product team for future evaluation.

---


## Subscription

### Prompt Focus
- Upgrade
- Downgrade
- Cancellation
- Renewal

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Unable to upgrade my plan

**Customer Issue**

> I have been trying to upgrade to the premium tier for the last hour, but the button stays greyed out. I am currently on the basic plan and really need the extra storage for my project. Can you please help me process this change?

**Suggested Reply**

Thank you for contacting us regarding your subscription. We'll review your request and help clarify the available options related to your subscription plan.

---

### Worked Example 2

**Ticket Title**

Unexpected renewal charge

**Customer Issue**

> I noticed a charge on my bank statement today for a subscription I thought I had canceled last month. I received no confirmation that my account was still active. Please issue a refund immediately.

**Suggested Reply**

Thank you for contacting us regarding your subscription. We'll review your request and help clarify the available options related to your subscription plan.

---

### Worked Example 3

**Ticket Title**

How do I switch to annual billing?

**Customer Issue**

> I'm currently on a monthly billing cycle, but I'd like to switch to the annual plan to take advantage of the discount. I can't seem to find the toggle for this in the billing settings. Could you guide me through the process?

**Suggested Reply**

Thank you for contacting us regarding your subscription. We'll review your request and help clarify the available options related to your subscription plan.

---


## Performance

### Prompt Focus
- Slow response
- Latency
- Loading
- Optimization

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Application keeps freezing when opening large files

**Customer Issue**

> Every time I try to load a project file over 50MB, the interface completely locks up for several minutes. I have checked my memory usage, and it seems like the software just hits a wall. Is there a cache setting I can adjust to help this?

**Suggested Reply**

Thank you for reporting the performance issue. We're sorry for the inconvenience. We'll investigate the reported slowdown and review whether any service conditions may be contributing to the behavior.

---

### Worked Example 2

**Ticket Title**

Severe latency issues during peak hours

**Customer Issue**

> The system is consistently sluggish between 2 PM and 5 PM every day. It takes nearly ten seconds just to switch between modules, which is killing my productivity. Is this a known server capacity issue?

**Suggested Reply**

Thank you for reporting the performance issue. We're sorry for the inconvenience. We'll investigate the reported slowdown and review whether any service conditions may be contributing to the behavior.

---

### Worked Example 3

**Ticket Title**

Dashboard takes forever to load

**Customer Issue**

> My main dashboard is taking way too long to populate the widgets. It sits on the loading spinner for at least thirty seconds before showing any data. This started happening after the last update.

**Suggested Reply**

Thank you for reporting the performance issue. We're sorry for the inconvenience. We'll investigate the reported slowdown and review whether any service conditions may be contributing to the behavior.

---


## Security

### Prompt Focus
- Authentication
- Suspicious activity
- MFA
- Account protection

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Suspicious login attempt from unknown location

**Customer Issue**

> I received an alert about a sign-in attempt from a different country this morning. I definitely did not try to access my account from there. Please lock my profile immediately to ensure my data remains safe.

**Suggested Reply**

Thank you for reporting this security concern. Protecting customer accounts is our priority. We'll review the reported activity and guide you through any necessary verification steps.

---

### Worked Example 2

**Ticket Title**

Two-factor authentication code not arriving

**Customer Issue**

> I have been trying to log in for the past hour, but the SMS code never reaches my phone. I have checked my signal and restarted my device multiple times. Is there an issue with your authentication server?

**Suggested Reply**

Thank you for reporting this security concern. Protecting customer accounts is our priority. We'll review the reported activity and guide you through any necessary verification steps.

---

### Worked Example 3

**Ticket Title**

Request to reset security questions

**Customer Issue**

> I have completely forgotten the answers to my security questions. How can I go about resetting these so I can manage my account settings again? Please let me know what verification steps are required.

**Suggested Reply**

Thank you for reporting this security concern. Protecting customer accounts is our priority. We'll review the reported activity and guide you through any necessary verification steps.

---


## Notifications

### Prompt Focus
- Email
- SMS
- Push notifications
- Alerts

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Not receiving push notifications on iPhone

**Customer Issue**

> My app notifications stopped showing up on my lock screen after the latest update. I have checked my settings and they are enabled for both the system and the app. Please help me fix this.

**Suggested Reply**

Thank you for letting us know. We'll review your notification settings and investigate whether there are any delivery issues affecting your account.

---

### Worked Example 2

**Ticket Title**

Email alert delays

**Customer Issue**

> I am consistently getting my activity notifications about six hours late. This is making it impossible to respond to urgent requests in real time. Can you investigate if there is a lag on your server side?

**Suggested Reply**

Thank you for letting us know. We'll review your notification settings and investigate whether there are any delivery issues affecting your account.

---

### Worked Example 3

**Ticket Title**

Too many marketing messages

**Customer Issue**

> I keep getting promotional notifications even though I unchecked every box in my profile settings. Please stop sending me these updates immediately. It is becoming quite annoying.

**Suggested Reply**

Thank you for letting us know. We'll review your notification settings and investigate whether there are any delivery issues affecting your account.

---


## Data Management

### Prompt Focus
- Import
- Export
- Deletion
- Backup

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Data export failing to CSV format

**Customer Issue**

> I am trying to export my monthly analytics report, but the download keeps failing at 99%. I have tried using both Chrome and Firefox but the result is the same. Could you please check if there is an issue with the export server?

**Suggested Reply**

Thank you for reporting this issue. We'll investigate the requested data operation and help determine why it is not completing as expected.

---

### Worked Example 2

**Ticket Title**

Unable to delete duplicate contact records

**Customer Issue**

> My contact list is full of duplicates that were created during the last sync. When I try to select and delete them, the system throws an error message about a foreign key constraint. Can someone assist me in cleaning up this data without affecting my active campaigns?

**Suggested Reply**

Thank you for reporting this issue. We'll investigate the requested data operation and help determine why it is not completing as expected.

---

### Worked Example 3

**Ticket Title**

Import tool is not recognizing column headers

**Customer Issue**

> I'm uploading a spreadsheet to update my inventory, but the mapping tool isn't detecting the headers correctly. It keeps treating the first row as actual data instead of labels. What is the correct format I need to follow for a smooth import?

**Suggested Reply**

Thank you for reporting this issue. We'll investigate the requested data operation and help determine why it is not completing as expected.

---


## General Inquiry

### Prompt Focus
- Information
- Documentation
- General help
- Product questions

### Tone
- Professional
- Empathetic
- Action-oriented

### Response Strategy
- Acknowledge the concern.
- Explain the next appropriate step.
- Avoid guarantees or promises.
- Escalate when needed.

### Worked Example 1

**Ticket Title**

Business hours inquiry

**Customer Issue**

> Could you please let me know if your support team is available on weekends? I am trying to plan a project launch and need to know when I can reach someone for assistance.

**Suggested Reply**

Thank you for contacting us. We'd be happy to assist with your question and provide the relevant information or guidance.

---

### Worked Example 2

**Ticket Title**

How do I update my profile picture?

**Customer Issue**

> I have been looking around the settings menu for a while now but I cannot figure out where to change my avatar. Is there a specific button I am missing or do I need to go to a different page?

**Suggested Reply**

Thank you for contacting us. We'd be happy to assist with your question and provide the relevant information or guidance.

---

### Worked Example 3

**Ticket Title**

Information regarding software compatibility

**Customer Issue**

> Does your platform currently support integration with third-party project management tools? I am looking to streamline my workflow and need to confirm if this is possible before I commit.

**Suggested Reply**

Thank you for contacting us. We'd be happy to assist with your question and provide the relevant information or guidance.

---


# Guardrails

## Privacy
- Never expose PII.
- Never reveal internal customer information.
- Never ask for passwords, OTPs, CVV or security answers.

## Business Rules
- Never promise refunds.
- Never guarantee issue resolution or timelines.
- Never fabricate account details.

## Communication
- Maintain a professional and respectful tone.
- Show empathy.
- Keep replies concise.
- Escalate sensitive issues to a human agent when appropriate.

# Conclusion

This document proposes a prompt-template approach for generating safe, consistent, and professional first-response suggestions across all ticket categories in the dataset. The examples demonstrate how category-aware prompting can help support agents respond more efficiently while preserving human review.
