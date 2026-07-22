# Git Security & Secret Protection

This repository uses automated security scanning to prevent sensitive files and secrets from being accidentally committed.

## 🛡️ What Is Protected

The security checks detect:

* Environment files: `.env`, `.env.local`, `.env.*`
* Private keys: `*.pem`, `*.key`, `*.ppk`, `id_rsa`
* Certificates: `*.p12`, `*.pfx`, `*.crt`
* Credential files: `credentials.json`, `service-account*.json`
* API keys and tokens
* Passwords and authentication secrets
* AWS/cloud credentials
* GitHub/GitLab tokens
* JWT secrets

## 🚀 First-Time Setup

After cloning the repository, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

This will:

* Configure the Git pre-commit hook
* Configure security scanning
* Update `.gitignore`
* Scan the working directory
* Check Git history for sensitive files

### Prerequisites

* Git
* Gitleaks (recommended)

Install Gitleaks:

```powershell
choco install gitleaks
```

## 📋 Normal Workflow

```bash
git add .
git commit -m "Your commit message"
git push
```

When `git commit` runs, the pre-commit hook automatically:

1. Checks staged sensitive filenames.
2. Scans staged content for secrets.
3. Blocks the commit if a security issue is detected.
4. Allows the commit when all checks pass.

## 🔍 Manual Security Scans

### Scan Working Directory

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\security\scan-working-dir.ps1
```

### Scan Git History

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\security\scan-git-history.ps1
```

## 🚨 Sensitive File in Git History

If a sensitive file was already committed, install `git-filter-repo`:

```powershell
pip install git-filter-repo
```

Remove the file from history:

```bash
git filter-repo --path <file-name> --invert-paths
```

⚠️ This rewrites Git history. Coordinate with the team before force-pushing.

If a real API key, password, token, or credential was exposed, **revoke or rotate it immediately**.

## 📁 Security Files

```text
.githooks/
└── pre-commit

scripts/security/
├── scan-working-dir.ps1
├── scan-git-history.ps1
└── sensitive-patterns.txt

.gitleaks.toml
setup.ps1
SECURITY.md
```

## 🔧 Add Custom Sensitive Patterns

Edit:

```text
scripts/security/sensitive-patterns.txt
```

Example:

```text
*.custom-secret
my-credentials-*
internal-keys.json
```

## 🛠️ Hook Not Running

Check:

```bash
git config core.hooksPath
```

Expected:

```text
.githooks
```

If not configured, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

## 🔐 Best Practices

### DO

* Store secrets outside Git-tracked files.
* Use `.env.example` only for variable names/examples.
* Rotate accidentally exposed credentials.
* Run `setup.ps1` after cloning.

### DON'T

* Commit `.env` or private key files.
* Store real secrets in `.env.example`.
* Ignore security warnings.
* Use `git commit --no-verify` unless absolutely necessary.

## Additional Security

For production repositories, also consider:

* GitHub Secret Scanning
* Branch protection
* CI/CD Gitleaks scanning
* CODEOWNERS
* AWS Secrets Manager / Parameter Store or another secrets manager

**Remember:** Pre-commit security checks help prevent accidental secret exposure, but developers are still responsible for protecting credentials.
