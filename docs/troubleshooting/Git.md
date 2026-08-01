# Git

Use Git to obtain the complete repository, preserve the Gradle wrapper, and keep documentation changes reviewable.

## Repository Has Missing Wrapper or Files

**Status:** Open

**Symptoms:** The clone lacks `gradle/wrapper/gradle-wrapper.jar`, a module directory, or files expected by the build.

**Cause:** A sparse checkout, incomplete archive, ignored file, failed submodule-like transfer, or an interrupted clone can produce a working tree that is not the repository state expected by Gradle.

**Diagnosis:**

```powershell
git status
git branch --show-current
git ls-files gradle/wrapper/gradle-wrapper.jar
git sparse-checkout list
git remote -v
```

**Fix:** From the repository root, fetch and restore tracked files:

```powershell
git fetch --all --prune
git restore --source=origin/main -- gradle/wrapper/gradle-wrapper.jar
git status
```

If the project uses a different default branch, replace `origin/main` with the branch shown by the remote. Do not discard unrelated local work.

**Verification:** `git ls-files` lists the wrapper JAR and ` .\gradlew.bat --version` starts.

**Prevention:** Clone normally, avoid partial archives for development, and review `git status` before build troubleshooting.

**Related Issues:** [GradleWrapperMain](BuildErrors.md), [Fresh Setup](../setup/FreshSetup.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
