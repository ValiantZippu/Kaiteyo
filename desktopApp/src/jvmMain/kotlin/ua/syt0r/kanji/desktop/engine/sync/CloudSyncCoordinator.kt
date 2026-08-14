package ua.syt0r.kanji.desktop.engine.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.account.AccountEngine
import ua.syt0r.kanji.desktop.engine.account.ProviderKind
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.model.ToastKind
import java.io.File

// ============================================
// CLOUD SYNC COORDINATOR
// Bridges the AccountEngine's provider
// connections with the pure SyncEngine:
//
//   1. Builds a versioned local manifest from
//      AppState data (versions derived from the
//      last-seen remote state so unchanged data
//      is skipped).
//   2. Reconciles against the connected GitHub
//      account via a real gist-backed transport.
//   3. Applies pulled blobs back into AppState
//      (cards, review log, daily summaries).
//   4. Records the sync on the account (device
//      last-sync + provider last-used).
//   5. Schedules automatic syncs from the
//      account settings (interval + on-start).
//
// State lives in ~/.kaiteyo/sync/ — the last-seen
// manifest (versions + timestamps) and the gist id.
// ============================================

class CloudSyncCoordinator(
    private val state: AppState,
    private val account: AccountEngine,
    private val engine: SyncEngine = SyncEngine(),
    private val codec: SyncCodec = SyncCodec(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val syncDir: File = File(System.getProperty("user.home"), ".kaiteyo/sync").apply { mkdirs() }
    private val lastSeenFile: File get() = File(syncDir, "last-seen.json")
    private val gistStateDir: File get() = File(syncDir, "gists")

    /**
     * Settings that describe this device (window/launcher geometry, onboarding
     * state, plugin registry, browsing position) and must never travel between
     * machines — otherwise one device's layout overwrites another's.
     */
    private val nonPortableSettingKeys = setOf(
        "launcher.pos-x", "launcher.pos-y", "launcher.pos-x-phone", "launcher.pos-y-phone",
        "navigation.position", "navigation.sidebar-width", "navigation.compact-position",
        "workspace.panels", "plugins.installed", "onboarding.completed", "onboarding.version",
        "account.joined-at", "account.last-backup-at",
        "browser.library-sort", "browser.library-scope",
        "browser.library-filter-jlpt", "browser.library-filter-difficulty", "browser.library-filter-favorites"
    )

    /** The settings subset that is safe (and meaningful) to share across devices. */
    fun portableSettings(): Map<String, String> =
        state.settings.snapshot().filterKeys { it !in nonPortableSettingKeys }

    /** The most recent remote manifest we observed — used to compute version bumps. */
    private var lastSeen: SyncManifest? = null

    private var autoSyncJob: Job? = null
    private var startupSyncDone = false

    init {
        // Start the auto-sync scheduler, reacting to account settings changes.
        scope.launch {
            lastSeen = loadLastSeen()
            account.settingsData.collect { settings ->
                scheduleAutoSync(settings.autoSync, settings.syncIntervalMinutes)
                // One-shot at startup when the user opted in but auto-sync is off.
                if (!startupSyncDone && settings.syncOnStart && !settings.autoSync) {
                    startupSyncDone = true
                    syncNow(manual = false, notify = false)
                } else if (!startupSyncDone) {
                    startupSyncDone = true
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    /** Whether a cloud provider suitable for syncing is currently connected. */
    fun isConnected(): Boolean =
        account.connections.value.any { it.kind == ProviderKind.GitHub && it.isConnected }

    fun connectedAccountName(): String =
        account.connections.value.firstOrNull { it.kind == ProviderKind.GitHub && it.isConnected }
            ?.displayName ?: ""

    /**
     * Run a full push/pull cycle. Returns the result or null when nothing ran.
     * @param resolution overrides the account's configured conflict resolution;
     * when null the Account → Sync setting is used.
     */
    suspend fun syncNow(
        manual: Boolean = true,
        notify: Boolean = true,
        resolution: ConflictResolution? = null
    ): SyncResult? {
        if (state.syncBusy) return null
        val connection = account.connections.value.firstOrNull { it.kind == ProviderKind.GitHub && it.isConnected }
        if (connection == null) {
            if (manual) {
                state.lastSyncMessage = "Connect a GitHub account to sync"
                if (notify) state.toastHost.show("Connect GitHub in Account to sync", kind = ToastKind.Info)
            }
            return null
        }

        state.syncBusy = true
        return try {
            val transport = GitHubGistSyncTransport(
                stateDir = gistStateDir,
                tokenProvider = { account.githubAccessToken() }
            )

            val local = codec.manifest(
                cards = state.cards.toList(),
                reviewLog = state.reviewLog.toList(),
                summaries = state.summaries.toList(),
                lastSeen = lastSeen,
                modifiedAt = { name -> contentModifiedAt(name) },
                decks = state.library.decks.toList(),
                collections = state.collections.collections,
                savedFilters = state.filterStore.saved,
                settings = portableSettings()
            )
            val effectiveResolution = resolution ?: account.settingsData.value.conflictResolution
            val result = engine.reconcile(transport, local, effectiveResolution)

            // Apply pulled blobs into live state.
            result.pulledBlobs.forEach { blob -> applyBlob(blob) }

            // Remember the remote state so unchanged data skips next run.
            lastSeen = transport.list().let { SyncManifest(deviceId = connection.userId, blobs = it) }
            persistLastSeen(lastSeen)

            val now = Clock.System.now()
            state.lastSyncAt = now
            state.lastSyncMessage =
                "Synced with ${connection.displayName} — pushed ${result.pushed}, pulled ${result.pulled}, " +
                    "skipped ${result.skipped} (${effectiveResolution.label})"
            state.activityLog.record(
                ActivityCategory.Sync,
                "Cloud sync completed (${connection.displayName})",
                details = "pushed ${result.pushed}, pulled ${result.pulled}, skipped ${result.skipped} " +
                    "(${effectiveResolution.label})",
                affectedCount = result.pushed + result.pulled
            )
            account.recordSync(ProviderKind.GitHub, now.toEpochMilliseconds())

            if (notify && account.settingsData.value.notifySyncCompleted) {
                state.toastHost.show(state.lastSyncMessage, kind = ToastKind.Success)
            }
            result
        } catch (e: Exception) {
            state.lastSyncMessage = "Sync failed — ${e.message ?: "unknown error"}"
            state.activityLog.record(ActivityCategory.Sync, "Cloud sync failed", details = e.message ?: "")
            if (notify && account.settingsData.value.notifySyncFailed) {
                state.toastHost.show(state.lastSyncMessage, kind = ToastKind.Error)
            }
            null
        } finally {
            state.syncBusy = false
        }
    }

    /** Verify the connected provider works end-to-end. */
    suspend fun testConnection(): Result<String> = runCatching {
        val transport = GitHubGistSyncTransport(
            stateDir = gistStateDir,
            tokenProvider = { account.githubAccessToken() }
        )
        transport.testConnection().getOrThrow()
    }

    // ------------------------------------------------------------
    // Applying pulled data
    // ------------------------------------------------------------

    private fun applyBlob(blob: SyncBlob) {
        when (blob.name) {
            "cards" -> {
                val cards = codec.decodeCards(blob)
                state.cards.clear()
                state.cards.addAll(cards)
            }
            "review-log" -> {
                val entries = codec.decodeReviewLog(blob)
                state.reviewLog.clear()
                state.reviewLog.addAll(entries)
            }
            "summaries" -> {
                val summaries = codec.decodeSummaries(blob)
                state.summaries.clear()
                state.summaries.addAll(summaries)
            }
            "decks" -> {
                val decks = codec.decodeDecks(blob)
                state.library.restoreDecks(decks)
            }
            "collections" -> {
                val collections = codec.decodeCollections(blob)
                state.collections.load(collections)
            }
            "saved-filters" -> {
                val filters = codec.decodeSavedFilters(blob)
                state.filterStore.loadSaved(filters)
            }
            "settings" -> {
                val settings = codec.decodeSettings(blob)
                state.settings.restore(settings)
            }
            else -> Logger.d("CloudSync: ignoring unknown blob '${blob.name}'")
        }
    }

    /**
     * Newest timestamp inside each blob's data. An empty blob reports the
     * epoch, so LWW never lets an empty device overwrite real remote data.
     */
    private fun contentModifiedAt(name: String): kotlinx.datetime.Instant = when (name) {
        "cards" -> state.cards
            .maxOfOrNull { maxOf(it.createdAt, it.lastReviewedAt ?: it.createdAt) }
            ?: Instant.fromEpochMilliseconds(0L)

        "review-log" -> state.reviewLog
            .maxOfOrNull { it.reviewedAt }
            ?: Instant.fromEpochMilliseconds(0L)

        "summaries" -> state.summaries
            .mapNotNull { runCatching { LocalDate.parse(it.day).atStartOfDayIn(TimeZone.UTC) }.getOrNull() }
            .maxOrNull()
            ?: Instant.fromEpochMilliseconds(0L)

        "decks" -> state.library.decks
            .maxOfOrNull { maxOf(it.createdAt, it.importedAt ?: it.createdAt) }
            ?: Instant.fromEpochMilliseconds(0L)

        "collections" -> state.collections.collections
            .maxOfOrNull { it.createdAt }
            ?: Instant.fromEpochMilliseconds(0L)

        "saved-filters" -> state.filterStore.saved
            .maxOfOrNull { Instant.fromEpochMilliseconds(it.lastUsedAt) }
            ?: Instant.fromEpochMilliseconds(0L)

        // Settings have no per-value timestamps. A snapshot that still matches the
        // factory defaults reports the epoch (never overwrites cloud settings via
        // LWW); otherwise the last user change wins.
        "settings" -> {
            val snapshot = portableSettings()
            val defaults = state.settings.defs
                .filter { it.key !in nonPortableSettingKeys }
                .associate { it.key to it.normalizedDefault }
            if (snapshot == defaults) Instant.fromEpochMilliseconds(0L)
            else state.settings.lastModifiedAt()
        }

        else -> Instant.fromEpochMilliseconds(0L)
    }

    // ------------------------------------------------------------
    // Last-seen persistence
    // ------------------------------------------------------------

    private fun loadLastSeen(): SyncManifest? = runCatching {
        if (lastSeenFile.exists()) json.decodeFromString<SyncManifest>(lastSeenFile.readText()) else null
    }.onFailure { Logger.w("CloudSync: failed to read last-seen: ${it.message}") }.getOrNull()

    private fun persistLastSeen(manifest: SyncManifest?) {
        runCatching {
            if (manifest == null) {
                lastSeenFile.delete()
            } else {
                syncDir.mkdirs()
                lastSeenFile.writeText(json.encodeToString(manifest))
            }
        }.onFailure { Logger.w("CloudSync: failed to persist last-seen: ${it.message}") }
    }

    // ------------------------------------------------------------
    // Auto-sync scheduling
    // ------------------------------------------------------------

    private fun scheduleAutoSync(enabled: Boolean, intervalMinutes: Int) {
        autoSyncJob?.cancel()
        autoSyncJob = null
        if (!enabled) return
        val intervalMs = (intervalMinutes.coerceAtLeast(1)) * 60_000L
        autoSyncJob = scope.launch {
            while (true) {
                delay(intervalMs)
                if (isConnected()) syncNow(manual = false, notify = true)
            }
        }
    }
}
