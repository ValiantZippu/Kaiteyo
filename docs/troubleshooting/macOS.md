# macOS

macOS is required for iOS builds and supports desktop packaging through the Compose desktop tasks.

## Xcode or Apple SDK Is Missing

**Status:** Open

**Symptoms:** Xcode build commands fail because the developer tools, simulator runtime, or signing configuration is unavailable.

**Cause:** The iOS target depends on Apple's SDKs and Xcode toolchain, which are installed and selected separately from JDK and Gradle.

**Diagnosis:**

```bash
xcode-select -p
xcodebuild -version
./gradlew :core:compileKotlinJvm
```

**Fix:** Install Xcode from the App Store, open it once to accept the license and install components, then select it:

```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
xcodebuild -runFirstLaunch
```

**Verification:** `xcodebuild -version` reports the expected Xcode and `iosApp` builds in Xcode.

**Prevention:** Keep Xcode aligned with the supported macOS release and record Xcode changes in [Required Software](../setup/RequiredSoftware.md).

**Related Issues:** [iOS](iOS.md), [Java](Java.md), [Desktop](Desktop.md), [Fresh Setup](../setup/FreshSetup.md).

Common links: [Troubleshooting index](README.md) | [Dependency problems](Gradle.md) | [Setup guide](../setup/FreshSetup.md) | [Git guide](../guides/GIT_GUIDE.md)
