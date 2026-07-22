# Environment Promotion Flow

## Overview

DeskFlow follows a three-stage environment promotion process to ensure changes are tested and validated before reaching production.

```text
Local Development → Staging → Production
```

## Environment Details

### 1. Local Environment

* Used by developers for feature development and bug fixes.
* Developers use local configuration files and environment variables.
* Changes are tested locally before being committed to the repository.

### 2. Staging Environment

* Acts as a pre-production testing environment.
* Mirrors the production setup as closely as possible.
* Used for integration testing, QA validation, and user acceptance testing.

### 3. Production Environment

* The live environment used by end users.
* Only tested and approved changes are deployed to production.
* Production secrets and configuration are managed securely and are never stored in Git.

## Promotion Process

1. Developers implement and test changes in the **Local Environment**.
2. Changes are committed and pushed to the GitHub repository.
3. The changes are deployed to the **Staging Environment** for testing and validation.
4. After successful testing and approval, the changes are promoted to the **Production Environment**.
5. Production deployment is performed using production-specific configuration and secrets.

## Promotion Flow Diagram

```text
Developer Machine (Local)
            ↓
     Staging Environment
            ↓
    Production Environment
```
