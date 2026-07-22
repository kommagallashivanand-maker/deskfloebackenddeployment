# DeskFlow AI Module Documentation

Feature: Priority Prediction – Feature Engineering & Extraction

Project: DeskFlow

Module: AI/ML

Author: AI/ML Team

Version: 1.0

## 1. Objective
The objective of this module is to extract meaningful features from a newly created ticket that will later be used by the Priority Prediction Model to classify the ticket as:
- High
- Medium
- Low

This module does not perform the priority prediction. It prepares structured input for the prediction model.

## 2. Business Requirement
When an employee creates a ticket, the system should collect relevant information and transform it into machine-readable features that help the priority prediction model determine the urgency of the ticket.

## 3. Ticket Creation
The employee submits the following information.
| Field	| Required | Source |
|-------| ---------| --------|
| Subject |	Yes |	Employee |
| Description |	Yes |	Employee |
| Category |	Yes | 	Employee |
| Attachments |	Optional |	Employee |
| Requester Role |	System | Authenticated User |

#### Note
Requester Role is not entered by the employee.
It is extracted from the authenticated user.
Example
authenticated_user.role

## 4. Feature Extraction Pipeline
``` 
Employee
    │
    ▼
Create Ticket
    │
    ▼
Subject
Description
Category
Requester Role
    │
    ▼
Feature Extraction Module
    │
    ├── Keyword Extraction
    ├── Sentiment Analysis
    ├── Category Extraction
    └── Requester Role Extraction

    │
    ▼
Structured Features
    │
    ▼
Priority Prediction Model
```

## 5. Features
### 5.1 Keywords
Source
- Subject
- Description

Extract important words that indicate the nature or urgency of the issue.
#### Example
#### Input
VPN not connecting.
Need access before client meeting.
#### Output
vpn
client meeting
connecting

### 5.2 Requester Role
#### Source
- Authenticated User
- Provides business context for the ticket.
#### Example
- Employee
- Manager
- HR
- Finance
- Admin

### 5.3 Category

Employee Selected Category

Possible values
- Technical
- Billing
- Account Access
- General
- Feature Request

Provides contextual information for the prediction model.

### 5.4 Sentiment
- Subject
- Description

Measure the emotional tone of the ticket.
#### Example
Everything is down.

Cannot work.

Need immediate help.

#### Output
Label : Negative

Score : -0.91

### 6. Feature Extraction Output
``` 
Input
{
    "subject": "VPN not connecting",
    "description": "Cannot access VPN. Need access before client meeting.",
    "category": "Network",
    "requester_role": "Employee"
}
Output
{
    "keywords": [
        "vpn",
        "client meeting",
        "cannot access"
    ],
    "requester_role": "Employee",
    "category": "Network",
    "sentiment": {
        "label": "Negative",
        "score": -0.82
    }
}
```
### 7. Project Structure
```
priority_prediction/
│
├── README.md
├── requirements.txt
├── pyproject.toml
├── .gitignore
│
├── app/
│   ├── config/
│   │   └── settings.py
│   │
│   ├── models/
│   │   ├── ticket.py
│   │   └── features.py
│   │
│   ├── feature_extraction/
│   │   ├── feature_extractor.py
│   │   ├── keyword_extractor.py
│   │   ├── sentiment_analyzer.py
│   │   ├── category_feature.py
│   │   ├── requester_role.py
│   │   └── preprocessing.py
│   │
│   ├── utils/
│   │   ├── text_cleaner.py
│   │   └── logger.py
│   │
│   └── main.py
│
├── tests/
│   ├── test_feature_extractor.py
│   ├── test_keywords.py
│   ├── test_sentiment.py
│   ├── test_category.py
│   └── test_requester_role.py
│
├── docs/
│   ├── feature_list.md
│   ├── leakage_check.md
│   └── architecture.md
│
└── sample_data/
    ├── sample_tickets.json
    └── sample_output.json
```
### 8. Responsibilities
```feature_extractor.py```
Coordinates the complete feature extraction process.

```keyword_extractor.py```
Extracts important keywords from the Subject and Description.

```sentiment_analyzer.py```
Calculates sentiment score and sentiment label.

```category_feature.py```
Reads the category selected by the employee.

```requester_role.py```
Retrieves the requester role from the authenticated user.

```preprocessing.py```
Performs text preprocessing.
#### Example
- Lowercase conversion
- Remove punctuation
- Remove extra spaces
- Normalize text

### 9. Unit Testing
The module should be tested for the following scenarios.

| Test Case	| Expected Result |
|-----------| ----------------|
| Subject exists |	Keywords extracted |
| Description exists |	Sentiment generated|
| Category selected	| Category returned|
| Requester role available	| Correct role returned|
| Empty description	| Graceful handling |
| Empty subject	| Graceful handling |

### 10. Data Leakage Check
Only features available at ticket creation should be used.
#### Allowed Features
| Feature |	Reason |
|---------|---------|
| Subject |	Available during ticket creation |
| Description |	Available during ticket creation |
| Category |	Selected by employee |
| Requester Role | Available from authenticated user |
| Keywords |	Derived from subject and description |
| Sentiment |	Derived from description |

#### Not Allowed Features
| Feature |	Reason |
| --------|--------|
| Assigned Agent |	Known after assignment |
| Resolution Time |	Known after resolution |
| Final Ticket Status |	Known after processing |
| Resolution Notes |	Generated after ticket completion |
| Customer Rating |	Available after closure |

### 11. Future Scope
This module prepares the features required by the Priority Prediction Model. In future iterations, the extracted features can be consumed by a machine learning model to classify tickets into:
- High
- Medium
- Low

Potential future enhancements include:
- Additional derived features (e.g., text length, urgency keyword count)
- Embedding-based semantic features
- Attachment metadata (if relevant)
- Historical ticket patterns
- Confidence scoring and feature importance analysis
