package ua.syt0r.kanji.desktop.engine.updates

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ============================================
// UPDATE SERVICE — coordinator
//
// check() → download() → verify → apply() →
// relaunch/rollback window. All state is exposed
// as a StateFlow so the Settings UI can bind
// directly. Not enabled by default; callers opt
// in (see installer/docs/UPDATES.md).
// ============================================

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Checking(val channel: UpdateChannel) : UpdateState
    data class Available(
        val manifest: UpdateManifest,
        val artifact: UpdateManifest.UpdateArtifact?
    ) : UpdateState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long?) : UpdateState
    data class ReadyToApply(val packageFile: File) : UpdateState
    data class Applying(val packageFile: File) : UpdateState
    data class UpToDate(val channel: UpdateChannel) : UpdateState
    data class Error(val reason: String, val retryable: Boolean) : UpdateState
}

class UpdateService(
    private val scope: CoroutineScope,
    private val checker: UpdateChecker,
    private val downloader: UpdateDownloader,
    private val installer: UpdateInstaller,
    private val policy: UpdatePolicy,
    private val dataDir: File
) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    // Kept across state transitions so apply() can re-read the feed after the
    // UI state has moved on to Downloading/ReadyToApply.
    private var lastManifest: UpdateManifest? = null

    private var current: AppVersionInfo

    init {
        // The running version is the app's own — provided by the caller via
        // configure() before the first check. Defaults keep the class usable
        // in isolation (e.g. tests).
        current = AppVersionInfo(versionName = "0.0.0", versionCode = 0)
    }

    /** Wires in the real running version. Call once at startup. */
    fun configure(appVersion: AppVersionInfo) {
        current = appVersion
    }

    /** Switches the active channel (persisted by the caller, e.g. Settings). */
    fun setChannel(channel: UpdateChannel) = policy.setChannel(channel)

    fun currentChannel(): UpdateChannel = policy.channel.value

    fun check() {
        scope.launch {
            val channel = policy.channel.value
            _state.value = UpdateState.Checking(channel)
            when (val result = checker.check(channel, current, policy)) {
                is UpdateCheckResult.UpToDate ->
                    _state.value = UpdateState.UpToDate(result.channel)

                is UpdateCheckResult.Available -> {
                    lastManifest = result.manifest
                    if (!policy.canApply(current, result.manifest.latest)) {
                        _state.value = UpdateState.Error(
                            reason = "Update ${result.manifest.latest.version} requires a newer app version.",
                            retryable = false
                        )
                    } else {
                        _state.value = UpdateState.Available(
                            manifest = result.manifest,
                            artifact = result.manifest.artifactFor(currentOsName(), currentArchName())
                        )
                    }
                }

                is UpdateCheckResult.Failed ->
                    _state.value = UpdateState.Error(result.reason, retryable = true)
            }
        }
    }

    fun download() {
        val available = _state.value as? UpdateState.Available ?: return
        val artifact = available.artifact ?: run {
            _state.value = UpdateState.Error("No artifact for this platform yet.", retryable = false)
            return
        }
        scope.launch {
            _state.value = UpdateState.Downloading(0, artifact.sizeBytes)
            when (val result = downloader.download(artifact, dataDir) { done, total ->
                _state.value = UpdateState.Downloading(done, total)
            }) {
                is DownloadResult.Downloaded -> {
                    _state.value = UpdateState.ReadyToApply(result.file)
                }
                is DownloadResult.Failed ->
                    _state.value = UpdateState.Error(result.reason, retryable = true)
            }
        }
    }

    fun apply() {
        val ready = _state.value as? UpdateState.ReadyToApply ?: return
        val manifest = lastManifest ?: run {
            _state.value = UpdateState.Error("Update feed unavailable — retry the check.", retryable = true)
            return
        }
        _state.value = UpdateState.Applying(ready.packageFile)
        when (val result = installer.apply(ready.packageFile, manifest, current)) {
            is ApplyResult.Applied -> {
                // The new version launched successfully — the rollback window can close.
                installer.recordSuccessfulLaunch(manifest.latest.version)
                _state.value = UpdateState.Idle
            }
            is ApplyResult.NeedsRelaunch -> {
                // App restarts itself; the rollback marker is written on next launch.
                _state.value = UpdateState.UpToDate(policy.channel.value)
            }
            is ApplyResult.Failed ->
                _state.value = UpdateState.Error(result.reason, retryable = true)
            is ApplyResult.Cancelled ->
                _state.value = UpdateState.Idle
        }
    }
}
