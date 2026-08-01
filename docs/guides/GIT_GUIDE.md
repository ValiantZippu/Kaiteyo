# Kaiteyo — Git Guide

## What is Git?

Git is a version control system that tracks changes to files. It allows multiple developers to work on the same project simultaneously without conflicts.

## Basic Commands

### Setup
```bash
# Configure your identity
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Recommended settings
git config --global pull.rebase true
git config --global core.longpaths true  # Windows only
```

### Daily Workflow
```bash
# Get latest code
git pull

# Create a new branch
git checkout -b feature/my-feature

# Check what files changed
git status

# See what changed in files
git diff

# Stage changes
git add file1.kt file2.kt
# Or stage everything
git add .

# Commit changes
git commit -m "feat: add floating window controls"

# Push to GitHub
git push origin feature/my-feature
```

### Branch Management
```bash
# List branches
git branch

# Switch to a branch
git checkout develop

# Create and switch to new branch
git checkout -b fix/window-drag

# Delete a branch (local)
git branch -d feature/old-feature

# Delete a branch (remote)
git push origin --delete feature/old-feature
```

### Undoing Changes
```bash
# Unstage a file (keep changes)
git reset HEAD file.kt

# Discard changes to a file
git checkout -- file.kt

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1
```

## Branch Strategy

```
main              — Production-ready code
  └── develop     — Integration branch
       ├── feature/*   — New features
       ├── fix/*       — Bug fixes
       ├── docs/*      — Documentation
       └── refactor/*  — Code restructuring
```

## Pull Requests

1. Push your branch to GitHub
2. Go to GitHub.com → repository → Pull Requests
3. Click "New Pull Request"
4. Base: `develop` → Compare: `your-branch`
5. Write description
6. Create PR

## Merging

```bash
# Switch to target branch
git checkout develop

# Merge feature branch
git merge feature/my-feature

# Push merged changes
git push origin develop
```

## Tags and Releases

```bash
# Create a tag
git tag v1.1.0

# Push tag to GitHub
git push origin v1.1.0

# List tags
git tag

# Delete a tag
git tag -d v1.1.0
git push origin --delete v1.1.0
```

## Fork Workflow

For external contributors:

```bash
# Fork on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/kaiteyo.git

# Add original repo as upstream
git remote add upstream https://github.com/ORIGINAL_OWNER/kaiteyo.git

# Get latest from upstream
git fetch upstream
git checkout develop
git merge upstream/develop

# Push to your fork
git push origin develop
```

## Resolving Conflicts

When Git can't automatically merge:

1. Open the conflicted files
2. Look for `<<<<<<<`, `=======`, `>>>>>>>` markers
3. Choose which version to keep (or edit manually)
4. Remove the conflict markers
5. Save the file
6. `git add file.kt`
7. `git commit`

## GitHub Actions

When you push a tag, GitHub Actions automatically:
1. Builds the desktop app (MSI, DMG, Deb)
2. Builds the Android app (APK)
3. Creates a GitHub Release
4. Uploads artifacts

## Best Practices

1. **Commit often** — Small, focused commits are easier to review
2. **Write good messages** — Use conventional commits format
3. **Pull before pushing** — Avoid conflicts
4. **Don't commit secrets** — Never commit passwords, API keys, or tokens
5. **Keep branches short-lived** — Merge feature branches quickly
6. **Delete merged branches** — Keep the repository clean
