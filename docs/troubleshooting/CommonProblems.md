# Common Problems

This page handles recurring environment problems that cross multiple toolchains. Use the exact issue structure in the [Troubleshooting README](README.md) for new entries.

## Build Is Slow or Runs Out of Memory

**Status:** Open

**Symptoms:** Gradle becomes very slow, the daemon stops, or the build reports an out-of-memory error.

**Cause:** Kotlin Multiplatform, Compose, Android, and generated resources can require substantial heap during compilation. The repository currently requests `-Xmx8192m`; a machine with less available memory may page heavily or fail.

**Diagnosis:**

```powershell
.\gradlew.bat --status
Get-Content gradle.properties | Select-String org.gradle.jvmargs
.\gradlew.bat :core:compileKotlinJvm --scan
```

**Fix:** Close memory-heavy applications, keep the repository setting appropriate for the machine, and run the narrow task first:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :core:compileKotlinJvm
```

Do not blindly increase heap beyond physical memory.

**Verification:** The narrow compile completes without daemon termination.

**Prevention:** Use incremental builds, avoid unnecessary `clean`, and document machine-specific limits instead of changing shared settings casually.

**Related Issues:** [Gradle](Gradle.md), [Java](Java.md), [First Build](../setup/FirstBuild.md), [Git Guide](../guides/GIT_GUIDE.md).

## Network or Proxy Blocks Dependency Downloads

**Status:** Open

**Symptoms:** Gradle reports connection refused, timeouts, TLS failures, or missing artifacts during a first build.

**Cause:** Gradle must contact Google Maven, Maven Central, and JitPack to resolve uncached plugins and libraries. A proxy, firewall, DNS problem, or offline environment interrupts resolution.

**Diagnosis:**

```powershell
.\gradlew.bat :core:compileKotlinJvm --stacktrace --info
Test-NetConnection repo.maven.apache.org -Port 443
```

**Fix:** Configure the network proxy in `~/.gradle/gradle.properties` when required, retry on a network that can reach the repositories, and use `--offline` only when all artifacts are already cached.

**Verification:** A clean terminal can resolve dependencies and the task reaches compilation.

**Prevention:** Complete one online first build and avoid deleting the Gradle cache without a reason.

**Related Issues:** [Gradle](Gradle.md), [Required Software](../setup/RequiredSoftware.md), [Build Errors](BuildErrors.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
