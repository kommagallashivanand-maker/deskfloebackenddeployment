# Branching Strategy

## Branches

### main
- Production-ready code
- Protected branch
- No direct commits

### staging
- Integration branch
- All feature branches are merged here through Pull Requests

### feature/*
- Used for individual feature development
- Created from `staging`

## Workflow

```
feature/* → Pull Request → develop → Pull Request → main
```

## Rules

- No direct push to `main`
- No direct push to `staging`
- One Pull Request approval is required
- Code Owner approval is required before merge
