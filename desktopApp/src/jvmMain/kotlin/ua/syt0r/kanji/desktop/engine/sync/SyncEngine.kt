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

    fun manifest(
        cards: List<DesktopCard>,
        reviewLog: List<ReviewLogEntry>,
        summaries: List<StudyDaySummary>
    ): SyncManifest {
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        return SyncManifest(
            blobs = listOf(
                SyncBlob("cards", payload = json.encodeToString(cards)),
                SyncBlob("review-log", payload = json.encodeToString(reviewLog)),
                SyncBlob("summaries", payload = json.encodeToString(summaries))
            )
        )
    }

    fun decodeCards(blob: SyncBlob): List<DesktopCard> {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return json.decodeFromString<List<DesktopCard>>(blob.payload)
    }
}

/**
 * The sync coordinator. Diffing is pure; transports are injected.
 * Persists last-seen blob versions so future runs can detect changes.
 */
class SyncEngine(
    private val codec: SyncCodec = SyncCodec()
) {

    private val lastSeen = mutableMapOf<String, Long>()

    fun restoreLastSeen(map: Map<String, Long>) {
        lastSeen.clear()
        lastSeen.putAll(map)
    }

    fun lastSeenSnapshot(): Map<String, Long> = lastSeen.toMap()

    fun diff(local: SyncManifest, remote: List<SyncBlob>): List<BlobDiff> {
        val localByName = local.blobs.associateBy { it.name }
        return remote.map { remoteBlob ->
            val localBlob = localByName[remoteBlob.name]
            when {
                localBlob == null -> BlobDiff(remoteBlob.name, null, remoteBlob, SyncDirection.Pull)
                localBlob.version > remoteBlob.version -> BlobDiff(remoteBlob.name, localBlob, remoteBlob, SyncDirection.Push)
                remoteBlob.version > localBlob.version -> BlobDiff(remoteBlob.name, localBlob, remoteBlob, SyncDirection.Pull)
                else -> BlobDiff(remoteBlob.name, localBlob, remoteBlob, null)
            }
        }
    }

    /** Apply a direction to a set of diffs, respecting conflict resolution. */
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

        val localByName = local.blobs.associateBy { it.name }
        for (blob in remote) {
            val localBlob = localByName[blob.name]
            when {
                localBlob == null -> {
                    // Remote-only: pull unless resolution says otherwise.
                    transport.download(blob.name).let {
                        pulled++
                        lastSeen[it.name] = it.version
                    }
                }
                localBlob.version > blob.version -> {
                    transport.upload(localBlob)
                    pushed++
                    lastSeen[localBlob.name] = localBlob.version
                }
                blob.version > localBlob.version -> {
                    val downloaded = transport.download(blob.name)
                    pulled++
                    lastSeen[downloaded.name] = downloaded.version
                }
                else -> skipped++
            }
        }
        // Local-only blobs: push them.
        remoteNames@ for (localBlob in local.blobs) {
            if (remote.any { it.name == localBlob.name }) continue@remoteNames
            transport.upload(localBlob)
            pushed++
            lastSeen[localBlob.name] = localBlob.version
        }

        return SyncResult(pushed = pushed, pulled = pulled, skipped = skipped)
    }
}

data class SyncResult(val pushed: Int, val pulled: Int, val skipped: Int)

/** Scheduler that invokes a sync at a cadence; engine-pure, timing agnostic. */
class SyncScheduler(private val engine: SyncEngine) {

    fun shouldRunNow(lastRunAt: Instant?, intervalMinutes: Long = 30): Boolean {
        val last = lastRunAt ?: return true
        val elapsed = Clock.System.now() - last
        return elapsed.inWholeMinutes >= intervalMinutes
    }
}
