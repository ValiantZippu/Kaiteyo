# Kaiteyo — GitHub Workflow

## Overview

This guide explains the Git and GitHub workflow for Kaiteyo development.

## Branch Strategy

```
main              — Production-ready code, protected
  └── develop     — Integration branch, default branch
       ├── feature/*   — New features (feature/floating-sidebar)
       ├── fix/*       — Bug fixes (fix/window-drag)
       └── docs/*      — Documentation (docs/architecture)
```

### Branch Naming

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New features | `feature/floating-sidebar` |
| `fix/` | Bug fixes | `fix/window-drag-region` |
| `docs/` | Documentation | `docs/theme-system` |
| `refactor/` | Code refactoring | `refactor/settings-module` |
| `release/` | Release preparation | `release/v1.1.0` |
| `hotfix/` | Emergency fixes | `hotfix/v1.1.1` |

## Daily Workflow

### Starting a Session
```bash
# Get latest develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/my-feature
```

### Making Changes
```bash
# Make changes in code
# Compile to verify
./gradlew :desktopApp:compileKotlinJvm

# Stage changes
git add .

# Commit with conventional message
git commit -m "feat: add floating window controls"

# Push branch
git push origin feature/my-feature
```

### Creating a Pull Request
1. Go to GitHub.com → repository → Pull Requests
2. Click "New Pull Request"
3. Base: `develop` → Compare: `feature/my-feature`
4. Write description following PR template
5. Add reviewers if needed
6. Click "Create Pull Request"

### Merging
- Squash merge for feature branches
- Regular merge for release branches
- Delete branch after merge

## Commit Message Convention

```
type(scope): description

[optional body]

[optional footer]
```

### Types
| Type | Usage |
|------|-------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `refactor` | Code restructuring |
| `perf` | Performance improvement |
| `test` | Adding/modifying tests |
| `style` | Formatting, styling |
| `chore` | Build, dependencies, CI |

### Examples
```
feat(window): add floating window controls
fix(theme): correct animateColorAsState import
docs(architecture): add module dependency diagram
refactor(settings): extract AppearanceStudio into separate file
perf(sidebar): reduce recomposition on hover
```

## Fork Workflow (External Contributors)

1. Fork the repository on GitHub
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/kaiteyo.git`
3. Add upstream: `git remote add upstream https://github.com/ORIGINAL_OWNER/kaiteyo.git`
4. Fetch upstream: `git fetch upstream`
5. Sync develop: `git checkout develop && git merge upstream/develop`
6. Create feature branch from develop
7. Push to your fork
8. Open PR from your fork to original repo's develop

## Git Configuration

### Recommended .gitignore
The project includes `.gitignore` for:
- Build outputs (`build/`, `out/`)
- IDE files (`.idea/`, `*.iml`)
- OS files (`.DS_Store`, `Thumbs.db`)
- Gradle wrapper jar (but not wrapper properties)
- Local properties (`local.properties`)

### Recommended Settings
```bash
# Use rebase by default
git config --global pull.rebase true

# Better diff display
git config --global diff.colorMoved zebra

# Enable long paths on Windows
git config --global core.longpaths true
```

## Tags and Releases

### Creating a Tag
```bash
# From develop (after merging release branch)
git checkout main
git pull origin main
git tag v1.1.0
git push origin v1.1.0
```

### GitHub Actions
On tag push, GitHub Actions automatically:
1. Builds desktop app (MSI, DMG, Deb)
2. Builds Android app (APK)
3. Creates GitHub Release
4. Uploads artifacts

## Protected Branches

`main` is protected:
- Requires PR review
- Requires status checks
- No direct pushes
- Linear history required

`develop` is protected:
- Requires status checks
- No direct pushes (enforced for most contributors)
