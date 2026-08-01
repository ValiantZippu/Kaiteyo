# Linux

Linux supports the desktop JVM target and Linux packaging when the required system libraries and JDK are installed.

## `Permission denied` for `gradlew`

**Status:** Open

**Symptoms:** The shell reports `./gradlew: Permission denied`.

**Cause:** Git or an archive removed the executable bit from the Unix wrapper script. The script cannot be launched even though its contents are present.

**Diagnosis:**

```bash
ls -l gradlew
file gradlew
```

**Fix:**

```bash
chmod +x gradlew
./gradlew --version
```

`chmod` restores the executable bit; the second command proves the wrapper can launch.

**Verification:** `./gradlew :desktopApp:compileKotlinJvm` completes successfully.

**Prevention:** Preserve executable permissions in Git and prefer a normal clone over a file archive.

**Related Issues:** [Git](Git.md), [Desktop](Desktop.md), [Fresh Setup](../setup/FreshSetup.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
