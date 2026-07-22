
# 1. Network Issue (Negative)

```json
{
  "ticket": {
    "subject": "VPN not connecting",
    "description": "I cannot connect to VPN since this morning. It is urgent because I have a client meeting in 15 minutes.",
    "category": "Network"
  },
  "auth": {
    "user_id": "u101",
    "role": "Employee"
  }
}
```

Expected:

* Keywords: `vpn`, `client meeting`
* Category: `Network`
* Requester Role: `Employee`
* Sentiment: **Negative**

---

# 2. Hardware Failure (Strong Negative)

```json
{
  "ticket": {
    "subject": "Laptop won't start",
    "description": "My laptop is completely dead and shows a black screen. I cannot work at all.",
    "category": "Hardware"
  },
  "auth": {
    "user_id": "u102",
    "role": "Manager"
  }
}
```

Expected:

* Keywords: `laptop`, `black screen`
* Category: `Hardware`
* Role: `Manager`
* Sentiment: **Negative**

---

# 3. Email Issue (Negative)

```json
{
  "ticket": {
    "subject": "Outlook keeps crashing",
    "description": "Outlook crashes every time I open it. I am unable to send or receive emails.",
    "category": "Software"
  },
  "auth": {
    "user_id": "u103",
    "role": "HR"
  }
}
```

Expected:

* Keywords: `outlook`, `emails`
* Category: `Software`
* Role: `HR`
* Sentiment: **Negative**

---

# 4. Password Reset (Neutral)

```json
{
  "ticket": {
    "subject": "Password reset request",
    "description": "I forgot my account password. Please help me reset it.",
    "category": "Access Management"
  },
  "auth": {
    "user_id": "u104",
    "role": "Employee"
  }
}
```

Expected:

* Keywords: `password`, `reset`
* Category: `Access Management`
* Role: `Employee`
* Sentiment: **Neutral**

---

# 5. Printer Issue (Neutral)

```json
{
  "ticket": {
    "subject": "Printer is offline",
    "description": "The office printer is showing offline status and is not printing documents.",
    "category": "Hardware"
  },
  "auth": {
    "user_id": "u105",
    "role": "Employee"
  }
}
```

Expected:

* Keywords: `printer`, `offline`
* Category: `Hardware`
* Role: `Employee`
* Sentiment: **Neutral** (or slightly Negative depending on analyzer)

---

# 6. System Performance (Negative)

```json
{
  "ticket": {
    "subject": "Computer running very slow",
    "description": "The system takes several minutes to open applications and frequently freezes.",
    "category": "Performance"
  },
  "auth": {
    "user_id": "u106",
    "role": "Employee"
  }
}
```

Expected:

* Keywords: `computer`, `applications`, `freezes`
* Category: `Performance`
* Role: `Employee`
* Sentiment: **Negative**

---

# 7. Positive Feedback

```json
{
  "ticket": {
    "subject": "Issue resolved successfully",
    "description": "Thank you for resolving my VPN issue so quickly. Everything is working perfectly now.",
    "category": "Network"
  },
  "auth": {
    "user_id": "u107",
    "role": "Employee"
  }
}
```

Expected:

* Keywords: `vpn`, `working`
* Category: `Network`
* Role: `Employee`
* Sentiment: **Positive**

---

# 8. Critical Server Outage

```json
{
  "ticket": {
    "subject": "Production server is down",
    "description": "The production server is completely down. All users are affected and business operations have stopped.",
    "category": "Infrastructure"
  },
  "auth": {
    "user_id": "u108",
    "role": "Administrator"
  }
}
```

Expected:

* Keywords: `production server`, `users`, `operations`
* Category: `Infrastructure`
* Role: `Administrator`
* Sentiment: **Strong Negative**

---

# 9. Software Installation

```json
{
  "ticket": {
    "subject": "Install Visual Studio Code",
    "description": "Please install Visual Studio Code on my laptop for development work.",
    "category": "Software"
  },
  "auth": {
    "user_id": "u109",
    "role": "Developer"
  }
}
```

Expected:

* Keywords: `visual studio code`, `development`
* Category: `Software`
* Role: `Developer`
* Sentiment: **Neutral**

---

# 10. Internet Connectivity

```json
{
  "ticket": {
    "subject": "Internet connection is unstable",
    "description": "The internet disconnects every few minutes, making it impossible to attend online meetings.",
    "category": "Network"
  },
  "auth": {
    "user_id": "u110",
    "role": "Employee"
  }
}
```

Expected:

* Keywords: `internet`, `online meetings`
* Category: `Network`
* Role: `Employee`
* Sentiment: **Negative**

---

