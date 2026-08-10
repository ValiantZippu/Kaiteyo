package ua.syt0r.kanji.desktop.engine.updates

import java.io.File

// ============================================
// UPDATE INSTALLER — interface only
//
// Each platform applies updates through its own
// native mechanism (Inno upgrade, .app swap,
// package manager), so the swap logic lives in
// platform-specific implementations:
//   - Windows: launch the downloaded EXE with
//     /VERYSILENT /SUPPRESSMSGBOXES (Inno keeps
//     user data by construction).
//   - macOS:  replace Contents of the .app only
//     for signed+notarized bundles.
//   - Linux:  AppImage swap + relaunch; deb/rpm/
//     flatpak delegate to the package manager.
// ============================================

sealed interface ApplyResult {
    data object Applied : ApplyResult
    data class NeedsRelaunch(val newVersion: String) : ApplyResult
    data class Failed(val reason: String) : ApplyResult
    data object Cancelled : ApplyResult
}

interface UpdateInstaller {

    /** Applies a verified downloaded artifact. May require a relaunch. */
    fun apply(
        packageFile: File,
        manifest: UpdateManifest,
        current: AppVersionInfo
    ): ApplyResult

    /** Records a successful launch of the new version (feeds the rollback
     *  window — see UpdatePolicy.RollbackPolicy). */
    fun recordSuccessfulLaunch(version: String)
}
