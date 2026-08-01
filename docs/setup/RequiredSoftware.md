# Required Software

| Tool | Required baseline | Verification |
|---|---|---|
| Git | Current stable release | `git --version` |
| JDK | 17 | `java --version`, `javac --version` |
| Gradle | Use committed wrapper | `./gradlew --version` |
| VS Code | Current stable, with Kotlin/Java/Gradle support | Open repository root |
| Android Studio | Required for Android SDK and emulator; install SDK packages used by `compileSdk` 36 | SDK Manager |
| Xcode | Required only for iOS builds | `xcodebuild -version` |

Windows supports JVM/Android development; macOS is required for iOS; Linux supports desktop development. Keep at least 20 GB free for dependencies and build outputs. Before running aggregate `build`, configure `ANDROID_HOME` or `local.properties`. See [Fresh Setup](FreshSetup.md) and [platform troubleshooting](../troubleshooting/README.md).
