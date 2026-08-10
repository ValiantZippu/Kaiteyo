package ua.syt0r.kanji.desktop.engine.sync

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.StudyDaySummary

// ============================================
// SYNC ARCHITECTURE (preparation only)
// Provider abstraction ready for Git, Google Drive,
// Dropbox, OneDrive, GitHub, or a custom server.
// The engines are pure; providers implement IO.
// ============================================

@Serializable
enum class SyncProviderType { LocalFolder, Git, GitHub, GoogleDrive, Dropbox, OneDrive, WebDav, CustomServer }

@Serializable
data class SyncProfile(
    val id: String,
    val name: String,
    val provider: SyncProviderType,
    val endpoint: String = "",
    val enabled: Boolean = true,
    val autoSync: Boolean = false,
    val createdAt: Instant = Clock.System.now()
)

/** Versioned blob that represents one syncable unit of user data. */
@Serializable
data class SyncBlob(
    val name: String,
    val version: Long = 1,
    val modifiedAt: Instant = Clock.System.now(),
    val payload: String = ""
)

@Serializable
data class SyncManifest(
    val schema: Int = 1,
    val createdAt: Instant = Clock.System.now(),
    val deviceId: String = "desktop-1",
    val blobs: List<SyncBlob> = emptyList()
)

/** The physical transport contract. Implementations do real IO. */
interface SyncTransport {
    val type: SyncProviderType

    /** List known remote blobs (name + version + modifiedAt). */
    suspend fun list(): List<SyncBlob>

    /** Download a blob by name. */
    suspend fun download(name: String): SyncBlob

    /** Upload a blob; returns the stored version. */
    suspend fun upload(blob: SyncBlob): Long

    suspend fun delete(name: String)

    suspend fun testConnection(): Result<String>
}

/** Pull/push directions. */
enum class SyncDirection { Push, Pull }

/** Result of comparing a local and remote blob. */
data class BlobDiff(
    val name: String,
    val local: SyncBlob?,
    val remote: SyncBlob?,
    val direction: SyncDirection?
)

enum class ConflictResolution { LocalWins, RemoteWins, Skip, Manual }

/** Serializes/deserializes user data into blobs for transport. */
class SyncCodec {

    private val json = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun manifest(
        cards: List<DesktopCard>,
        reviewLog: List<ReviewLogEntry>,
        summaries: List<StudyDaySummary>
    ): SyncManifest = manifest(cards, reviewLog, summaries, lastSeen = null)

    /**
     * Build a manifest whose versions are derived from the last-seen remote
     * state: unchanged blobs keep their version (diff → skip), changed or
     * brand-new blobs bump to version + 1 (diff → push).
     */
    fun manifest(
        cards: List<DesktopCard>,
        reviewLog: List<ReviewLogEntry>,
        summaries: List<StudyDaySummary>,
        lastSeen: SyncManifest?
    ): SyncManifest {
        fun versioned(name: String, payload: String): SyncBlob {
            val previous = lastSeen?.blobs?.firstOrNull { it.name == name }
            val changed = previous == null || previous.payload != payload
            return if (changed) {
                SyncBlob(
                    name = name,
                    version = (previous?.version ?: 0L) + 1,
                    modifiedAt = Clock.System.now(),
                    payload = payload
                )
            } else {
                SyncBlob(name = name, version = previous!!.version, modifiedAt = previous.modifiedAt, payload = payload)
            }
        }
        return SyncManifest(
            blobs = listOf(
                versioned("cards", json.encodeToString(cards)),
                versioned("review-log", json.encodeToString(reviewLog)),
                versioned("summaries", json.encodeToString(summaries))
            )
        )
    }

    fun decodeCards(blob: SyncBlob): List<DesktopCard> =
        json.decodeFromString<List<DesktopCard>>(blob.payload)

    fun decodeReviewLog(blob: SyncBlob): List<ReviewLogEntry> =
        json.decodeFromString<List<ReviewLogEntry>>(blob.payload)

    fun decodeSummaries(blob: SyncBlob): List<StudyDaySummary> =
        json.decodeFromString<List<StudyDaySummary>>(blob.payload)
}

/**
 * The sync coordinator. Diffing is pure; transports are injected.
 * Tracks last-seen blob versions so future runs can detect changes.
 *
 * Version semantics: a blob's version is a monotonic revision counter.
 * When two sides hold the same version but different content, the
 * tie is broken last-write-wins by modifiedAt (local clocks; good
 * enough for a single-user multi-device sync).
 */
class SyncEngine {

    private val lastSeen = mutableMapOf<String, Long>()

    fun restoreLastSeen(map: Map<String, Long>) {
        lastSeen.clear()
        lastSeen.putAll(map)
    }

    fun lastSeenSnapshot(): Map<String, Long> = lastSeen.toMap()

    /**
     * Three-way diff between the local manifest and the remote blob list.
     * Includes remote-only (Pull), local-only (Push) and equal-version
     * divergent (LWW by modifiedAt) blobs.
     */
    fun diff(local: SyncManifest, remote: List<SyncBlob>): List<BlobDiff> {
        val localByName = local.blobs.associateBy { it.name }
        val remoteByName = remote.associateBy { it.name }
        val names = (localByName.keys + remoteByName.keys).distinct()
        return names.map { name ->
            val localBlob = localByName[name]
            val remoteBlob = remoteByName[name]
            val direction = when {
                localBlob == null -> SyncDirection.Pull
                remoteBlob == null -> SyncDirection.Push
                localBlob.version > remoteBlob.version -> SyncDirection.Push
                remoteBlob.version > localBlob.version -> SyncDirection.Pull
                // Same version, divergent content → last-write-wins.
                localBlob.payload != remoteBlob.payload ->
                    if (localBlob.modifiedAt > remoteBlob.modifiedAt) SyncDirection.Push else SyncDirection.Pull

                else -> null
            }
            BlobDiff(name, localBlob, remoteBlob, direction)
        }
    }

    /**
     * Apply a direction to a set of diffs, respecting conflict resolution.
     * Returns the pulled blobs so callers can write them into local state.
     */
    suspend fun reconcile(
        transport: SyncTransport,
        local: SyncManifest,
        resolution: ConflictResolution = ConflictResolution.Skip
    ): SyncResult {
        val remote = transport.list()
        val diffs = diff(local, remote)
        var pushed = 0
        var pulled = 0
        var skipped = 0
        val pulledBlobs = mutableListOf<SyncBlob>()

        for (d in diffs) {
            val direction = when (resolution) {
                ConflictResolution.LocalWins ->
                    // Keep local copies of blobs that exist on both sides.
                    if (d.direction == SyncDirection.Pull && d.local != null) null else d.direction

                ConflictResolution.RemoteWins ->
                    // Keep remote copies of blobs that exist on both sides.
                    if (d.direction == SyncDirection.Push && d.remote != null) null else d.direction

                else -> d.direction
            }
            when (direction) {
                SyncDirection.Push -> {
                    val localBlob = d.local ?: continue
                    val storedVersion = transport.upload(localBlob)
                    pushed++
                    lastSeen[localBlob.name] = storedVersion
                }

                SyncDirection.Pull -> {
                    val downloaded = transport.download(d.name)
                    pulled++
                    lastSeen[downloaded.name] = downloaded.version
                    pulledBlobs += downloaded
                }

                null -> skipped++
            }
        }

        return SyncResult(pushed = pushed, pulled = pulled, skipped = skipped, pulledBlobs = pulledBlobs)
    }
}

data class SyncResult(
    val pushed: Int,
    val pulled: Int,
    val skipped: Int,
    val pulledBlobs: List<SyncBlob> = emptyList()
)

/** Scheduler that invokes a sync at a cadence; engine-pure, timing agnostic. */
class SyncScheduler(private val engine: SyncEngine) {

    fun shouldRunNow(lastRunAt: Instant?, intervalMinutes: Long = 30): Boolean {
        val last = lastRunAt ?: return true
        val elapsed = Clock.System.now() - last
        return elapsed.inWholeMinutes >= intervalMinutes
    }
}
