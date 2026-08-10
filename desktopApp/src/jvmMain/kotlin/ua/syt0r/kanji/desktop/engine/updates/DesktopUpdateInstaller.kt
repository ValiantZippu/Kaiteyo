package ua.syt0r.kanji.desktop.engine.updates

import java.io.File
import java.time.Instant

// ============================================
// DESKTOP UPDATE INSTALLER
// Applies verified update packages through the
// platform's native mechanism:
//   - Windows : launch the Inno Setup EXE in
//               silent upgrade mode. Inno keeps
//               user data by construction.
//   - Linux   : not yet enabled — the AppImage
//               swap path is designed but off.
//   - macOS   : not yet enabled — replacements
//               require a signed/notarized bundle.
// Never touches the user data directory.
// ============================================

class DesktopUpdateInstaller(
    private val dataDir: File = updatesDataDir()
) : UpdateInstaller {

    override fun apply(
        packageFile: File,
        manifest: UpdateManifest,
        current: AppVersionInfo
    ): ApplyResult {
        val os = currentOsName()
        return when {
            os == "windows" && packageFile.name.endsWith(".exe") -> {
                runCatching {
                    ProcessBuilder(
                        packageFile.absolutePath,
                        "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART"
                    ).start()
                }.fold(
                    onSuccess = { ApplyResult.NeedsRelaunch(manifest.latest.version) },
                    onFailure = {
                        ApplyResult.Failed("Could not launch the installer: ${it.message}")
                    }
                )
            }

            else -> ApplyResult.Failed(
                "Automatic updates are not enabled for $os yet — " +
                    "download the latest installer from the Kaiteyo website."
            )
        }
    }

    override fun recordSuccessfulLaunch(version: String) {
        // Rollback marker: the new version launched cleanly, so the previous
        // version can be discarded (see UpdatePolicy.RollbackPolicy).
        runCatching {
            dataDir.mkdirs()
            dataDir.resolve("update-applied-$version.json").writeText(
                """{"applied":true,"version":"$version","at":"${Instant.now()}"}"""
            )
        }
    }
}
