# Developer Setup Guide

## Prerequisites

Ensure the following tools are installed on your system:

* Git
* Docker Desktop and Docker Compose

## Setup Steps

### 1. Clone/pull the Repository
Used when you want to download a repository for the first time.

```bash
git clone https://github.com/p99softadmin/DeskFlow-backend.git
cd DeskFlow
```
git pull

Used when you already have the repository and want the latest changes from GitHub.

git pull https://github.com/p99softadmin/DeskFlow-backend.git


### 2. Create Local Environment Configuration

Create a local environment file by copying the example file:

```bash
cp .env.example .env.local
```

For Windows PowerShell:

```powershell
Copy-Item .env.example .env.local
```

### 3. Configure Environment Variables

Copy the required environment variable names from `.env.example` and provide appropriate local values in `.env.local`.

Example:

**.env.example**

```env
DB_HOST=
DB_PORT=
JWT_SECRET=
API_KEY=
```

**.env.local**

```env
DB_HOST=localhost
DB_PORT=5432
JWT_SECRET=my-local-secret
API_KEY=my-local-api-key
```

### Important Guidelines

* Do not commit or push `.env.local` to GitHub.
* If a new environment variable is required for the application, add its name and description to `.env.example`.
* Never store actual secrets, passwords, API keys, or tokens in `.env.example`.
* Keep all local and environment-specific values only in `.env.local`.

### 4. Install Required Dependencies



If the project uses Python services or scripts, install dependencies from:

```bash
pip install -r requirements.txt
```

Whenever new packages or libraries are added:

* Update `package.json` and `package-lock.json` for Node.js dependencies.
* Update `requirements.txt` for  dependencies.
* Commit these dependency files along with your code changes.

### 5. Start the Application

Using Docker Compose:

```bash
docker compose up
```

Or run the application locally:

```bash
 Ex :- npm run dev
```

### 6. Verify the Application

Open the application in your browser and verify that all services are running correctly.

### 7. Development Workflow

* Create a new branch for your changes.

```bash
git checkout -b feature/<feature-name>
```

* Make your changes and commit them.

```bash
git add .
git commit -m "Describe your changes"
```


## 8. Secret Scanning with Gitleaks

To prevent accidental commits of passwords, API keys, tokens, and other sensitive information, DeskFlow uses **Gitleaks** for secret scanning.

### Install Gitleaks (Windows)

Using Chocolatey:

```powershell
choco install gitleaks
```

Verify installation:

```powershell
gitleaks version
```

### Run Secret Scan

Scan the complete Git history:

```powershell
gitleaks git
```

Scan the current directory:

```powershell
gitleaks dir .
```

### What Gitleaks Detects

Gitleaks scans for sensitive information such as:

* AWS Access Keys
* GitHub Tokens
* OpenAI API Keys
* Database Passwords
* JWT Secrets
* API Keys
* Private Keys and Certificates

### Example Output

No secrets found:

```text
INFO no leaks found
```

If Secrets detected:

```text
WRN leaks found: 1
```

### If Secrets Are Detected

1. Remove the secret from the source code or environment file.
2. Move the value to `.env.local`, `.env.staging`, or `.env.production`.
3. Ensure the environment files are ignored using `.gitignore`.

Example:

```gitignore
.env
.env.*
!.env.example
```

4. If the secret was already committed, remove it from Git history using:

```bash
git filter-repo --path .env.local --invert-paths
```

5. Run the scan again:

```powershell
gitleaks git
```

Expected result:

```text
INFO no leaks found
```

### Purpose of Gitleaks

* Prevents accidental exposure of credentials.
* Ensures secrets are not stored in Git history.
* Helps satisfy security and compliance requirements.
* Supports the project requirement:

```text
Secrets via env files or vault pattern, never in Git.
```


* Push the branch to GitHub.

```bash
git push origin feature/<feature-name>
```

* Create a Pull Request (PR) for code review if required by the project workflow.

## Expected Setup Time

A new developer should be able to complete the setup and start the application in  less than 30 minutes.