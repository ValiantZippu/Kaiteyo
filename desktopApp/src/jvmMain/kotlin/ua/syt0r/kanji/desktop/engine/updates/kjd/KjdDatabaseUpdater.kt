package ua.syt0r.kanji.desktop.engine.updates.kjd

import io.kaiteyo.kjd.patch.DatabasePatcher
import io.kaiteyo.kjd.patch.DatabasePatch
import io.kaiteyo.kjd.patch.PatchResult
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================
// KJD DATABASE UPDATER — coordinator
//
// check() → download() → apply() against the
// bundled KJD language database, mirroring the
// app UpdateService. The apply path is
// non-destructive by construction: the patch is
// fingerprint-verified, applied in one
// transaction, index-rebuilt, re-verified and
// backed up before mutation. All state is
// exposed as a StateFlow so Settings UI can
// bind directly.
// ============================================

sealed interface KjdDatabaseUpdateState {
    data object Idle : KjdDatabaseUpdateState
    data object NoBundledDatabase : KjdDatabaseUpdateState
    data class Checking(val channel: String) : KjdDatabaseUpdateState
    data class Available(
        val entry: KjdPatchFeed.PatchEntry,
        val feedDatabaseVersion: String
    ) : KjdDatabaseUpdateState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long?) : KjdDatabaseUpdateState
    data class ReadyToApply(val patchFile: File) : KjdDatabaseUpdateState
    data class Applying(val patchFile: File) : KjdDatabaseUpdateState
    data class UpToDate(val databaseVersion: String, val fingerprint: String) : KjdDatabaseUpdateState
    data class Error(val reason: String, val retryable: Boolean) : KjdDatabaseUpdateState
}

@Serializable
data class KjdAppliedState(
    val databaseVersion: String = "",
    val fingerprint: String = "",
    val appliedAt: String = ""
)

/**
 * Coordinates KJD patch application for the bundled language database.
 *
 * @param scope            background coroutine scope for check/download/apply
 * @param checker          feed checker (HTTP or test fake)
 * @param downloader       patch downloader (HTTP or test fake)
 * @param locator          resolves the bundled database file
 * @param dataDir          staging dir for downloads + backups + state file
 *
 * Hook [onApplied] / [onChecked] to mirror results into host settings.
 */
class KjdDatabaseUpdater(
    private val scope: CoroutineScope,
    private val checker: KjdPatchChecker,
    private val downloader: KjdPatchDownloader,
    private val locator: KjdDatabaseLocator = KjdDatabaseLocator(),
    private val dataDir: File = kjdUpdatesDataDir()
) {

    private val _state = MutableStateFlow<KjdDatabaseUpdateState>(KjdDatabaseUpdateState.Idle)
    val state: StateFlow<KjdDatabaseUpdateState> = _state.asStateFlow()

    /** Invoked after a successful apply; the host app mirrors it into Settings. */
    var onApplied: (KjdAppliedState) -> Unit = {}

    /** Invoked after any completed check (applied or not); carries the checkedAt
     *  timestamp so the host can record updates.kjd-last-checked. */
    var onChecked: (String) -> Unit = {}

    private val json = Json { ignoreUnknownKeys = true }
    private val stateFile: File = File(dataDir, "kjd-applied-state.json")

    private var lastEntry: KjdPatchFeed.PatchEntry? = null

    /** The last recorded applied state (restored from disk on creation and
     *  kept current after each apply, so callers always see live state). */
    val appliedState: KjdAppliedState
        get() = appliedStateCache

    private var appliedStateCache: KjdAppliedState = loadAppliedState()

    init {
        dataDir.mkdirs()
    }

    // ------------------------------------------------------------
    // Startup entry point
    // ------------------------------------------------------------

    /**
     * Runs the full check→download→apply pipeline once, quietly, when the
     * user opted in. Never throws; failures surface via [state].
     */
    fun checkOnStartup(channelName: String = "stable") {
        val database = locator.resolveDatabaseFile() ?: run {
            _state.value = KjdDatabaseUpdateState.NoBundledDatabase
            return
        }
        check(channelName, database)
    }

    // ------------------------------------------------------------
    // Phases
    // ------------------------------------------------------------

    fun check(channelName: String = "stable", database: File = locator.databaseFile) {
        val state = locator.readState(database)
        if (state == null) {
            _state.value = KjdDatabaseUpdateState.NoBundledDatabase
            return
        }
        // No short-circuit here on purpose: the feed may contain a *next* hop
        // in a release chain (1.1.0 → 1.2.0 → 1.3.0). Only the checker knows
        // whether another patch applies; its NoPatch verdict is the "already
        // up to date" signal.
        _state.value = KjdDatabaseUpdateState.Checking(channelName)
        scope.launch {
            val checkedAt = Instant.now().toString()
            when (val result = checker.check(channel(channelName), state)) {
                is KjdPatchCheckResult.PatchAvailable -> {
                    lastEntry = result.entry
                    onChecked(checkedAt)
                    _state.value = KjdDatabaseUpdateState.Available(
                        entry = result.entry,
                        feedDatabaseVersion = result.feed.databaseVersion
                    )
                }
                is KjdPatchCheckResult.NoPatch -> {
                    lastEntry = null
                    onChecked(checkedAt)
                    _state.value = KjdDatabaseUpdateState.UpToDate(
                        databaseVersion = state.databaseVersion ?: "",
                        fingerprint = state.fingerprint
                    )
                }
                is KjdPatchCheckResult.Failed ->
                    _state.value = KjdDatabaseUpdateState.Error(result.reason, retryable = true)
            }
        }
    }

    fun download() {
        val entry = lastEntry ?: return
        _state.value = KjdDatabaseUpdateState.Downloading(0, entry.sizeBytes.takeIf { it > 0 })
        scope.launch {
            when (val result = downloader.download(entry, dataDir) { done, total ->
                _state.value = KjdDatabaseUpdateState.Downloading(done, total)
            }) {
                is KjdPatchDownloadResult.Downloaded ->
                    _state.value = KjdDatabaseUpdateState.ReadyToApply(result.file)
                is KjdPatchDownloadResult.Failed ->
                    _state.value = KjdDatabaseUpdateState.Error(result.reason, retryable = true)
            }
        }
    }

    fun apply() {
        val ready = _state.value as? KjdDatabaseUpdateState.ReadyToApply ?: return
        val entry = lastEntry ?: run {
            _state.value = KjdDatabaseUpdateState.Error(
                "No patch selected — re-run the update check.",
                retryable = true
            )
            return
        }
        _state.value = KjdDatabaseUpdateState.Applying(ready.patchFile)
        scope.launch {
            val database = locator.databaseFile
            val patch = runCatching { json.decodeFromString<DatabasePatch>(ready.patchFile.readText()) }
                .getOrElse {
                    _state.value = KjdDatabaseUpdateState.Error(
                        "Downloaded patch is unreadable: ${it.message}",
                        retryable = true
                    )
                    return@launch
                }
            try {
                when (val result = DatabasePatcher().apply(
                    target = database,
                    patch = patch,
                    backupDir = File(dataDir, "backups")
                )) {
                    is PatchResult.Applied -> {
                        val applied = KjdAppliedState(
                            databaseVersion = entry.toDatabaseVersion,
                            fingerprint = result.targetFingerprint,
                            appliedAt = Instant.now().toString()
                        )
                        saveAppliedState(applied)
                        onApplied(applied)
                        _state.value = KjdDatabaseUpdateState.UpToDate(
                            databaseVersion = entry.toDatabaseVersion,
                            fingerprint = result.targetFingerprint
                        )
                    }
                    PatchResult.AlreadyApplied -> {
                        val applied = KjdAppliedState(
                            databaseVersion = entry.toDatabaseVersion,
                            fingerprint = entry.toFingerprint,
                            appliedAt = Instant.now().toString()
                        )
                        saveAppliedState(applied)
                        _state.value = KjdDatabaseUpdateState.UpToDate(
                            databaseVersion = entry.toDatabaseVersion,
                            fingerprint = entry.toFingerprint
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.value = KjdDatabaseUpdateState.Error(
                    t.message ?: "KJD patch apply failed",
                    retryable = false
                )
            }
        }
    }

    // ------------------------------------------------------------
    // Applied-state persistence (survives app restarts)
    // ------------------------------------------------------------

    private fun saveAppliedState(applied: KjdAppliedState) {
        dataDir.mkdirs()
        stateFile.writeText(json.encodeToString(KjdAppliedState.serializer(), applied))
        appliedStateCache = applied
    }

    private fun loadAppliedState(): KjdAppliedState = runCatching {
        if (!stateFile.isFile) return@runCatching KjdAppliedState()
        json.decodeFromString<KjdAppliedState>(stateFile.readText())
    }.getOrElse { KjdAppliedState() }

    private fun channel(name: String) =
        ua.syt0r.kanji.desktop.engine.updates.UpdateChannel.fromName(name)
}
