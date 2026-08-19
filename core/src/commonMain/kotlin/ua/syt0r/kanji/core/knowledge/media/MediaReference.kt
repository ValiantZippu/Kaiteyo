package ua.syt0r.kanji.core.knowledge.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// MEDIA REFERENCES — knowledge ⇄ media connections (spec §28)
// ------------------------------------------------------------
// When the user bookmarks or mines something in the Media Centre
// (desktop), the media engine records a MediaReference here — the
// media title, the Japanese text, and the timestamp. Knowledge
// pages (word / kanji entries) then show a real "Found in your
// media" card: text, source title, and a jump-back timestamp.
//
// Honest scope: references are recorded only on real user actions
// (bookmarking / mining / cue capture) in the desktop media
// engine. Nothing is fabricated — if no media has been touched,
// the card is simply absent.
// ============================================================

@Serializable
enum class MediaReferenceKind { Subtitle, Bookmark, Mined }

/** A Japanese text occurrence inside user media, with its timestamp. */
@Serializable
data class MediaReference(
    val kind: MediaReferenceKind,
    /** Media file / episode title, e.g. "Attack on Titan S1E1.mkv". */
    val title: String,
    /** The Japanese text recorded from this media (cue or mined token). */
    val text: String,
    /** Playback position in the media, milliseconds. */
    val timestampMs: Long,
    val recordedAt: Long,
    /**
     * The card created when this reference was MINED, when the desktop
     * mining engine reports it (null for bookmarks / unmined cues). Lets the
     * node layer build a real mined_from edge (ADR-0013, spec §149) — the
     * card id is the desktop card's own id, never fabricated.
     */
    val cardId: String? = null
)

/** Persisted media-reference history, newest first, capped. */
@Serializable
data class MediaReferenceHistory(
    val references: List<MediaReference> = emptyList()
) {
    fun sanitized(): MediaReferenceHistory = MediaReferenceHistory(
        references = references
            .filter { it.text.isNotBlank() }
            .take(MAX_REFERENCES)
    )

    companion object {
        const val MAX_REFERENCES = 200
    }
}

/**
 * Persists [MediaReferenceHistory] as JSON in app preferences.
 * Corrupt or stale blobs fall back to empty — a hand-edited
 * preference can never break a knowledge page.
 */
class MediaReferenceStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): MediaReferenceHistory {
        val raw = preferences.mediaReferencesJson.get()
        if (raw.isBlank()) return MediaReferenceHistory()
        return runCatching {
            Json.decodeFromString<MediaReferenceHistory>(raw).sanitized()
        }.getOrDefault(MediaReferenceHistory())
    }

    private suspend fun save(history: MediaReferenceHistory) {
        preferences.mediaReferencesJson.set(Json.encodeToString(history.sanitized()))
    }

    /** Records a media reference (newest first, deduped by title+text+time). */
    suspend fun record(reference: MediaReference) {
        if (reference.text.isBlank()) return
        val current = load()
        val deduped = current.references.filterNot {
            it.title == reference.title && it.text == reference.text && it.timestampMs == reference.timestampMs
        }
        save(MediaReferenceHistory(listOf(reference) + deduped))
    }

    /** References whose text contains [text] (case-insensitive). */
    suspend fun matching(text: String, limit: Int = 8): List<MediaReference> {
        if (text.isBlank()) return emptyList()
        val needle = text.lowercase()
        return load().references
            .filter { it.text.lowercase().contains(needle) }
            .take(limit)
    }

    suspend fun reset() {
        preferences.mediaReferencesJson.set("")
    }
}
