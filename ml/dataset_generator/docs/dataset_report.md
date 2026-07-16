# Synthetic Customer Support Ticket Dataset Report

## Dataset Overview
- **Total Tickets**: 2010
- **Date Generated**: 2026-07-16 12:48:09

## Category Class Balance
| Category | Ticket Count | Percentage |
| --- | --- | --- |
| Account Access | 201 | 10.0% |
| Billing | 201 | 10.0% |
| Data Management | 201 | 10.0% |
| Feature Request | 201 | 10.0% |
| General Inquiry | 201 | 10.0% |
| Notifications | 201 | 10.0% |
| Performance | 201 | 10.0% |
| Security | 201 | 10.0% |
| Subscription | 201 | 10.0% |
| Technical | 201 | 10.0% |

## Text Statistics (Averages)
- **Average Title Word Count**: 5.09 words
- **Average Body Word Count**: 37.71 words
- **Average Body Sentence Count**: 2.68 sentences

## Dataset Quality & Validation Summary
| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Missing/Empty Titles | 0 | 0 | ✅ Pass |
| Missing/Empty Bodies | 0 | 0 | ✅ Pass |
| Duplicate (Title+Body) | 0 | 0 | ✅ Pass |
| Category Balance | 200 per category | Account Access: 201, Billing: 201, Data Management: 201, Feature Request: 201, General Inquiry: 201, Notifications: 201, Performance: 201, Security: 201, Subscription: 201, Technical: 201 | ⚠️ Warning |

## Sample Tickets Preview

### Category: Account Access
**Ticket ID**: TKT-00001 - *Cannot log in after password reset*
> I requested a password reset link earlier today and followed the instructions to change it. Now, whenever I try to sign in with the new password, the page just refreshes and keeps me on the login screen. I have tried clearing my browser cache but the issue persists.

**Ticket ID**: TKT-00002 - *Locked out of my account*
> I keep getting an error saying my account is locked due to too many failed login attempts. I am positive I typed my password correctly, but maybe I had caps lock on by accident. Can you please unlock it for me?

### Category: Billing
**Ticket ID**: TKT-00202 - *Double charge on my monthly subscription*
> I noticed two identical charges on my bank statement for this month. My subscription should only be billed once. Could you please look into this and process a refund for the extra charge?

**Ticket ID**: TKT-00203 - *Why did my monthly rate increase?*
> My bill was ten dollars higher this month than it was last month. I haven't changed my plan or added any new services. Can someone explain why the price went up without any warning?

### Category: Data Management
**Ticket ID**: TKT-01609 - *Data export failing to CSV format*
> I am trying to export my monthly analytics report, but the download keeps failing at 99%. I have tried using both Chrome and Firefox but the result is the same. Could you please check if there is an issue with the export server?

**Ticket ID**: TKT-01610 - *Unable to delete duplicate contact records*
> My contact list is full of duplicates that were created during the last sync. When I try to select and delete them, the system throws an error message about a foreign key constraint. Can someone assist me in cleaning up this data without affecting my active campaigns?

### Category: Feature Request
**Ticket ID**: TKT-00604 - *Add Dark Mode to the desktop application*
> I spend hours every day working in your app and the bright white background is really starting to strain my eyes. Could you please implement a dark mode option in the settings menu? It would be a huge help for those of us working late shifts.

**Ticket ID**: TKT-00605 - *Request for bulk export functionality*
> Currently, I have to download my data files one by one, which is extremely tedious. Is there any way you could add a button to export all reports as a single zip file? This feature would save me hours of manual labor every week.

### Category: General Inquiry
**Ticket ID**: TKT-01810 - *Business hours inquiry*
> Could you please let me know if your support team is available on weekends? I am trying to plan a project launch and need to know when I can reach someone for assistance.

**Ticket ID**: TKT-01811 - *How do I update my profile picture?*
> I have been looking around the settings menu for a while now but I cannot figure out where to change my avatar. Is there a specific button I am missing or do I need to go to a different page?

### Category: Notifications
**Ticket ID**: TKT-01408 - *Not receiving push notifications on iPhone*
> My app notifications stopped showing up on my lock screen after the latest update. I have checked my settings and they are enabled for both the system and the app. Please help me fix this.

**Ticket ID**: TKT-01409 - *Email alert delays*
> I am consistently getting my activity notifications about six hours late. This is making it impossible to respond to urgent requests in real time. Can you investigate if there is a lag on your server side?

### Category: Performance
**Ticket ID**: TKT-01006 - *Application keeps freezing when opening large files*
> Every time I try to load a project file over 50MB, the interface completely locks up for several minutes. I have checked my memory usage, and it seems like the software just hits a wall. Is there a cache setting I can adjust to help this?

**Ticket ID**: TKT-01007 - *Severe latency issues during peak hours*
> The system is consistently sluggish between 2 PM and 5 PM every day. It takes nearly ten seconds just to switch between modules, which is killing my productivity. Is this a known server capacity issue?

### Category: Security
**Ticket ID**: TKT-01207 - *Suspicious login attempt from unknown location*
> I received an alert about a sign-in attempt from a different country this morning. I definitely did not try to access my account from there. Please lock my profile immediately to ensure my data remains safe.

**Ticket ID**: TKT-01208 - *Two-factor authentication code not arriving*
> I have been trying to log in for the past hour, but the SMS code never reaches my phone. I have checked my signal and restarted my device multiple times. Is there an issue with your authentication server?

### Category: Subscription
**Ticket ID**: TKT-00805 - *Unable to upgrade my plan*
> I have been trying to upgrade to the premium tier for the last hour, but the button stays greyed out. I am currently on the basic plan and really need the extra storage for my project. Can you please help me process this change?

**Ticket ID**: TKT-00806 - *Unexpected renewal charge*
> I noticed a charge on my bank statement today for a subscription I thought I had canceled last month. I received no confirmation that my account was still active. Please issue a refund immediately.

### Category: Technical
**Ticket ID**: TKT-00403 - *Application crashing on startup*
> Every time I try to launch the desktop application, it immediately crashes to the desktop. I have already tried reinstalling the software twice, but the issue persists. Could you please let me know what logs I need to send over for analysis?

**Ticket ID**: TKT-00404 - *API authentication failing*
> I am receiving a 403 Forbidden error when attempting to authenticate via the API using my standard token. It was working perfectly yesterday, but all calls are now being rejected. Is there a known outage or a change in the authentication requirements?
