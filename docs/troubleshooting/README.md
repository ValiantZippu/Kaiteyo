# Troubleshooting Knowledge Base

This is the searchable index for build, setup, platform, Git, Gradle, Java, and development problems in Kaiteyo. Record a solved issue immediately using the required structure below.

## Issue Record Standard

Every issue entry must contain:

1. **Title** - the exact error or a precise short name.
2. **Symptoms** - what the developer sees, including terminal output, screenshots, error messages, or stack traces.
3. **Cause** - why the toolchain or application fails.
4. **Diagnosis** - commands and observations that confirm the cause.
5. **Fix** - exact commands or file changes, with an explanation of each step.
6. **Verification** - the command or behavior that proves the fix worked and the expected result.
7. **Prevention** - how to avoid recurrence.
8. **Related Issues** - links to nearby entries and guides.

Use a status of `Open`, `Investigating`, `Solved`, `Won't Fix`, or `Deferred` in each tracked issue.

## Index

- [Build Errors](BuildErrors.md)
- [Gradle](Gradle.md)
- [Java](Java.md)
- [Git](Git.md)
- [VS Code](VSCode.md)
- [Android](Android.md)
- [Desktop](Desktop.md)
- [iOS](iOS.md)
- [Windows](Windows.md)
- [Linux](Linux.md)
- [macOS](macOS.md)
- [Common Problems](CommonProblems.md)

## Related Documentation

- [Fresh Setup](../setup/FreshSetup.md)
- [First Build](../setup/FirstBuild.md)
- [Required Software](../setup/RequiredSoftware.md)
- [Command Library](../development/COMMANDS.md)
- [Git Guide](../guides/GIT_GUIDE.md)
- [Build History](../maintenance/VersionHistory.md)
- [Known Limitations](../maintenance/KnownLimitations.md)

## When to Update This System

Update the relevant page before considering a change complete whenever someone fixes a bug, changes architecture, setup, dependencies, folders, Gradle, Java, Git workflow, project structure, build process, or release process. Add a dated entry to [Version History](../maintenance/VersionHistory.md) and update [Known Limitations](../maintenance/KnownLimitations.md) when the issue remains relevant.

## New Issue Template

```markdown
### <Exact Error or Precise Title>

**Status:** Investigating
**First seen:** YYYY-MM-DD
**Last verified:** YYYY-MM-DD

#### Symptoms

<Exact output, screenshot link, error, or stack trace.>

#### Cause

<Why the failure occurs.>

#### Diagnosis

```text
<Commands and useful output.>
```

#### Fix

```text
<Exact commands.>
```

<Explain each command and file change.>

#### Verification

```text
<Verification command.>
```

Expected: <success condition.>

#### Prevention

<How to avoid recurrence.>

#### Related Issues

- [Related troubleshooting page](BuildErrors.md)
- [Fresh Setup](../setup/FreshSetup.md)
- [Git Guide](../guides/GIT_GUIDE.md)
```
