# Java

Kaiteyo development targets Java 17. Gradle uses `JAVA_HOME` or the Java executable on `PATH` to start its daemon.

## Wrong Java Runtime

**Status:** Open

**Symptoms:** Gradle refuses to start, reports an unsupported class version, or uses a different JDK than `java --version`.

**Cause:** A JRE, an older JDK, or a different `JAVA_HOME` takes precedence over the intended JDK. Gradle then runs with an incompatible JVM even if another Java installation exists on the machine.

**Diagnosis:**

```powershell
java --version
javac --version
$env:JAVA_HOME
where.exe java
.\gradlew.bat --version
```

**Fix:** Install a JDK 17 distribution, set `JAVA_HOME` to its installation directory, open a new terminal, and run the verification commands again. On Windows, use System Properties > Environment Variables and set `JAVA_HOME` to the JDK directory, not its `bin` directory.

**Verification:** ` .\gradlew.bat --version` reports JVM 17 and the wrapper starts.

**Prevention:** Keep one documented JDK 17 path per machine and check `JAVA_HOME` after IDE or JDK upgrades.

**Related Issues:** [Fresh Setup](../setup/FreshSetup.md), [Windows](Windows.md), [Gradle](Gradle.md), [Git Guide](../guides/GIT_GUIDE.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)

## Gradle Uses JDK 25 While `PATH` Uses JDK 17

**Status:** Solved by aligning `JAVA_HOME` with JDK 17
**First seen:** 2026-08-01
**Last verified:** 2026-08-01

### Symptoms

```text
java --version: openjdk 17.0.20
JAVA_HOME: C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot
Gradle --version: JVM 25.0.4
java.lang.IllegalArgumentException: 25.0.4
at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse
```

### Cause

The shell found Java 17 through `PATH`, but Gradle honors `JAVA_HOME` when starting its JVM. The project uses JDK 17 toolchains; Gradle launched on JDK 25 instead. Kotlin's embedded Java-version parser rejected the JDK 25 version string before source compilation began.

### Diagnosis

```powershell
java --version
javac --version
where.exe java
$env:JAVA_HOME
.\gradlew.bat --version
```

`java`, `javac`, `JAVA_HOME`, and Gradle's `JVM` must all identify JDK 17.

### Fix

For the current PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;" + (($env:Path -split ';' | Where-Object { $_ -notmatch 'Eclipse Adoptium\\jdk-' }) -join ';')
java --version
.\gradlew.bat --stop
.\gradlew.bat --version
```

To persist the setting for future terminals:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot', 'User')
```

Close and reopen VS Code after changing the persistent environment variable.

### Verification

```powershell
.\gradlew.bat --version
.\gradlew.bat :core:compileKotlinJvm
```

Expected: Gradle reports `JVM: 17.0.20` and the compile reaches Kotlin without `JavaVersion.parse` failure.

### Prevention

Keep `JAVA_HOME`, `PATH`, the IDE runtime, and Gradle's JVM on JDK 17. After installing another JDK, rerun `where.exe java` and `gradlew.bat --version`.

### Related Issues

- [Gradle](Gradle.md)
- [Build Errors](BuildErrors.md)
- [Windows](Windows.md)
- [Fresh Setup](../setup/FreshSetup.md)
- [Git Guide](../guides/GIT_GUIDE.md)
