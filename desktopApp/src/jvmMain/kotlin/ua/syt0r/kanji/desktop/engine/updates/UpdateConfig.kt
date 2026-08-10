package ua.syt0r.kanji.desktop.engine.updates

import java.io.File

// ============================================
// UPDATE RUNTIME CONFIG
// Where the app looks for updates, where update
// artifacts land, and how it reports its own
// version. Mirrors installer/common/version.json
// where a runtime value is needed.
// ============================================

/** Base URL of the update feed directory. Each channel appends its feed file
 *  (update-stable.json / update-beta.json / update-nightly.json).
 *
 *  Points at the dedicated `update-feed` release (a prerelease that CI
 *  refreshes on every tagged release) rather than `releases/latest`:
 *  GitHub's `latest` only resolves to the newest *stable* release, which would
 *  make beta/nightly channel checks silently read the stable feed. */
const val UPDATE_FEED_BASE_URL: String =
    "https://github.com/ValiantZippu/Kaiteyo/releases/download/update-feed"

/** Downloads and rollback markers live here, inside the user data dir —
 *  never under the app install dir. */
fun updatesDataDir(): File =
    File(System.getProperty("user.home"), ".kaiteyo/updates")

/** The running app version, resolved from the generated BuildConfig.
 *  Falls back to a placeholder when unavailable (e.g. running in tests). */
fun currentAppVersion(): AppVersionInfo {
    val versionName = runCatching { ua.syt0r.kanji.BuildConfig.versionName }
        .getOrNull() ?: "0.0.0"
    // BuildConfig generates versionCode as a Long (AppVersion.versionCode.toLong());
    // AppVersionInfo carries an Int, matching the manifest's version_code.
    val versionCode = runCatching { ua.syt0r.kanji.BuildConfig.versionCode }
        .getOrNull()?.toInt() ?: 0
    return AppVersionInfo(versionName = versionName, versionCode = versionCode)
}

/** Normalized OS name used for update artifact selection. */
fun currentOsName(): String {
    val name = System.getProperty("os.name", "").lowercase()
    return when {
        name.contains("win") -> "windows"
        name.contains("mac") -> "macos"
        name.contains("linux") -> "linux"
        else -> "unknown"
    }
}

/** Normalized architecture key ("arm" / "intel") matching the feed artifact keys. */
fun currentArchName(): String {
    val arch = System.getProperty("os.arch", "").lowercase()
    return when {
        arch == "aarch64" || arch == "arm64" -> "arm"
        arch == "amd64" || arch == "x86_64" -> "intel"
        else -> arch
    }
}
