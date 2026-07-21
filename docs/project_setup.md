# Developer Setup Guide

## Prerequisites

Ensure the following tools are installed:

* Git
* Docker Desktop with Docker Compose
* Node.js and npm

---

## 1. Clone / Pull Repository

### First-Time Setup

```bash
git clone https://github.com/p99softadmin/DeskFlow-backend.git
```

### Existing Repository

```bash
git pull
```

Navigate to the project root:

```bash
cd <folder_name>
```

---

## 2. Run Security Setup (Very important)

After cloning the repository for the **first time**, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

**What it does:** Configures the Git pre-commit security hook and performs initial working-directory and Git-history security checks.

Verify:

```bash
git config core.hooksPath .githooks
```

Expected:

```text
.githooks
```

---

## 3. Configure Environment Variables

Create `.env.local` from `.env.example`:

### Windows PowerShell

```powershell
Copy-Item .env.example .env.local
```

### Linux/macOS

```bash
cp .env.example .env.local
```

Add your local values to `.env.local`.

```env
DB_HOST=localhost
DB_PORT=5432
JWT_SECRET=<your-local-secret>
API_KEY=<your-local-api-key>
```

### Important

* Never commit `.env.local` or other environment-specific secret files.
* Keep only variable names/examples in `.env.example`.
* Never store real passwords, API keys, or tokens in `.env.example`.

---

4. Install Dependencies
Python
pip install -r requirements.txt

Whenever new Python dependencies are added:

Update requirements.txt
Commit the updated file with your code changes
FastAPI

Install dependencies:

pip install -r requirements.txt

If required, install FastAPI and Uvicorn:

pip install fastapi uvicorn
Java

For Maven projects:

mvn clean install

For Gradle projects:

./gradlew build

On Windows:

gradlew.bat build
5. Run the Application Locally
Python
python <filename>.py

Example:

python app.py
FastAPI
uvicorn main:app --reload

Where:

main = Python filename (main.py)
app = FastAPI application object
Java

For a simple Java application:

javac Main.java
java Main

For a Maven Spring Boot application:

mvn spring-boot:run

For a Gradle Spring Boot application:

./gradlew bootRun

On Windows:

gradlew.bat bootRun


### Using Docker Compose

First-time build:

```bash
docker compose up --build
```

Next time:

```bash
docker compose up
```

After Dockerfile or dependency changes:

```bash
docker compose up --build
```

Check running containers and ports:

```bash
docker compose ps
```

Open:

```text
http://localhost:<PORT>
```

---

## 6. Pre-Commit Security Hook

The security hook runs **automatically whenever you execute `git commit`**.

Normal workflow:

```bash
git add .
git commit -m "your changes"
```

The hook automatically:

* Checks staged files for sensitive filenames.
* Scans staged content for secrets/API keys/passwords/tokens.
* Blocks the commit if a security issue is detected.
* Allows the commit when all security checks pass.

### Expected Success

```text
[OK] No sensitive file names detected
[OK] No secrets detected in file content
[OK] Security scan passed. Safe to commit.
```

### If a Secret Is Detected

The commit will be blocked:

```text
[SECURITY CHECK FAILED]
Potential secrets detected in staged file content.
```

Fix it by:

1. Remove the secret from the source code.
2. Move it to `.env.local` or the approved secrets store.
3. Unstage the affected file if required:

```bash
git restore --staged <file_name>
```

4. Fix the file and stage it again:

```bash
git add <file_name>
```

5. Commit again:

```bash
git commit -m "your changes"
```

---

## 7. Manual Security Scans

These scripts can be run manually when required.

### Scan Working Directory

Run before staging to find sensitive files:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\security\scan-working-dir.ps1
```

### Scan Git History

Check whether sensitive files were previously committed:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\security\scan-git-history.ps1
```

If a real credential was previously committed, remove it from Git history using the remediation instructions provided by the scanner and **rotate/revoke the exposed credential immediately**.

---

## 8. Development Workflow

Create a branch:

```bash
git checkout -b feature/<feature-name>
```

Make changes and stage them:

```bash
git add .
```

Commit — security hook runs automatically:

```bash
git commit -m "Describe your changes"
```

Push:

```bash
git push origin feature/<feature-name>
```

Create a Pull Request for code review.

---

## Common Docker Commands

| Command                     | Purpose                    |
| --------------------------- | -------------------------- |
| `docker compose up --build` | Build and start containers |
| `docker compose up`         | Start containers           |
| `docker compose stop`       | Stop containers            |
| `docker compose start`      | Start stopped containers   |
| `docker compose restart`    | Restart containers         |
| `docker compose down`       | Stop and remove containers |
| `docker compose ps`         | Show containers and ports  |
| `docker compose logs -f`    | View live logs             |

---

## Expected Setup Time

A new developer should be able to complete the setup and start the application in **less than 30 minutes**.
