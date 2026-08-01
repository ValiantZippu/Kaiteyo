# Known Limitations

Use one status per item: `Open`, `Investigating`, `Solved`, `Won't Fix`, or `Deferred`. Update this list whenever a limitation changes.

| Limitation | Status | Impact | Related documentation |
|---|---|---|---|
| iOS targets cannot compile on Windows/Linux | Solved for non-Apple hosts | iOS development requires macOS and Xcode | [iOS troubleshooting](../troubleshooting/iOS.md) |
| Desktop configuration emits duplicate `jvmRun` warning | Investigating | Run may work, but task registration is ambiguous | [Gradle](../troubleshooting/Gradle.md) |
| First build requires network access | Open | Uncached plugins and libraries cannot resolve offline | [Common Problems](../troubleshooting/CommonProblems.md) |
| Desktop packaging is host-specific | Open | MSI, DMG, and Deb packaging must run on matching platforms | [Desktop](../troubleshooting/Desktop.md) |
| Android build requires local SDK configuration | Open | Android tasks cannot run until SDK packages are installed | [Android](../troubleshooting/Android.md) |
| Aggregate build requires a discoverable Android SDK | Investigating | `build` stops while configuring `:app` without SDK location | [Android SDK troubleshooting](../troubleshooting/Android.md) |
| Gradle can select JDK 25 through `JAVA_HOME` | Solved by JDK 17 alignment | Kotlin compiler rejects `25.0.4` before compilation | [Java](../troubleshooting/Java.md) |
| Experimental Material 3 opt-in was missing in four deck files | Solved | JVM compile failed in deck helper composables | [Build Errors](../troubleshooting/BuildErrors.md) |
| AboutLibraries and mediaGenerator deprecation warnings remain | Open | Build is noisy; future plugin updates may require migration | [Build Errors](../troubleshooting/BuildErrors.md) |

Add a new row only with a status, observed impact, and a troubleshooting link.

Related: [Version History](VersionHistory.md), [Fresh Setup](../setup/FreshSetup.md), [Troubleshooting index](../troubleshooting/README.md).
