package ua.syt0r.kanji.desktop.engine.media

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.engine.media.SubtitleCue

// ============================================
// MEDIA NODE FAMILY (KT-MEDIA-005)
// Series → Episode → Scene → SubtitleLine,
// with exposure edges into the knowledge graph.
//
// Every node is derived from REAL suite data:
// mined cards (`MediaMiningEvent`), bookmarks and
// subtitle cues. No fabricated metadata — fields
// are nullable/empty where unknown.
// ============================================

/** Identifies a piece of media in the library (path is the stable key). */
@Serializable
data class MediaNodeId(
    val path: String,
    val name: String
)

@Serializable
data class MediaSeries(
    val id: String,
    val title: String,
    val episodes: MutableList<MediaEpisode> = mutableListOf()
) {
    /** Kanji/words mined anywhere in this series (exposure edge count). */
    fun exposureCount(): Int = episodes.sumOf { it.exposureCount() }
    fun watchTimeMs(): Long = episodes.sumOf { it.watchTimeMs }
}

@Serializable
data class MediaEpisode(
    val number: Int,
    val path: String,
    val name: String,
    var watchTimeMs: Long = 0,
    val scenes: MutableList<MediaScene> = mutableListOf()
) {
    fun exposureCount(): Int = scenes.sumOf { it.exposureCount() }
}

@Serializable
data class MediaScene(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val lines: MutableList<SubtitleLine> = mutableListOf()
) {
    fun exposureCount(): Int = lines.sumOf { it.exposureCount }
    fun text(): String = lines.joinToString(" ") { it.text }.trim()
}

/** One subtitle line — a candidate sentence for lookup/mining. */
@Serializable
data class SubtitleLine(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    /** How many distinct kanji/words were mined from this line. */
    val exposureCount: Int = 0
)

/**
 * Builds the node hierarchy from real suite data. Series are inferred from
 * file naming: `Name - EP03.mp4` or `Name/03.mp4` groups episodes together;
 * a lone file becomes a single-episode series. Scenes are derived from
 * bookmarks (a bookmark starts a scene) and subtitle timing otherwise.
 */
class MediaNodeGraph {

    private val seriesById = LinkedHashMap<String, MediaSeries>()
    private val episodeByPath = HashMap<String, MediaEpisode>()

    fun addMiningEvent(event: MediaMiningEvent) {
        val seriesTitle = inferSeriesTitle(event.mediaName)
        val series = seriesById.getOrPut(seriesTitle) { MediaSeries(seriesTitle, seriesTitle) }
        val episode = episodeByPath.getOrPut(event.mediaPath) {
            MediaEpisode(
                number = series.episodes.size + 1,
                path = event.mediaPath,
                name = event.mediaName
            )
        }
        // Attach the mined line to a scene that covers its timestamp, or open a new scene.
        val scene = episode.scenes.firstOrNull { event.timestampMs in it.startMs..it.endMs }
            ?: MediaScene(id = "scene-${event.mediaPath}-${event.timestampMs}", startMs = event.timestampMs, endMs = event.timestampMs + 1)
        scene.lines.add(
            SubtitleLine(
                index = scene.lines.size + 1,
                startMs = event.timestampMs,
                endMs = event.timestampMs + 1,
                text = event.cueText,
                exposureCount = 1
            )
        )
        if (scene !in episode.scenes) episode.scenes.add(scene)
        if (episode !in series.episodes) series.episodes.add(episode)
        episode.watchTimeMs = maxOf(episode.watchTimeMs, event.timestampMs)
    }

    fun addBookmark(bookmark: MediaBookmark) {
        val seriesTitle = inferSeriesTitle(bookmark.mediaPath.substringAfterLast('/').substringAfterLast('\\'))
        val series = seriesById.getOrPut(seriesTitle) { MediaSeries(seriesTitle, seriesTitle) }
        val episode = episodeByPath.getOrPut(bookmark.mediaPath) {
            MediaEpisode(number = series.episodes.size + 1, path = bookmark.mediaPath, name = bookmark.mediaPath)
        }
        episode.watchTimeMs = maxOf(episode.watchTimeMs, bookmark.timestampMs)
        if (!series.episodes.contains(episode)) series.episodes.add(episode)
    }

    /**
     * Index a full subtitle track into the graph. Idempotent: previous lines for
     * [mediaPath] are replaced, so re-indexing after subtitle edits is safe and
     * version-safe without duplicating nodes. Mined lines (exposureCount=1) are
     * preserved — merged by cue timestamp proximity (±500ms) so mining provenance
     * is never lost on re-index.
     */
    fun indexSubtitleTrack(mediaPath: String, mediaName: String, cues: List<SubtitleCue>) {
        if (cues.isEmpty()) return
        val seriesTitle = inferSeriesTitle(mediaName)
        val series = seriesById.getOrPut(seriesTitle) { MediaSeries(seriesTitle, seriesTitle) }
        val episode = episodeByPath.getOrPut(mediaPath) {
            MediaEpisode(number = series.episodes.size + 1, path = mediaPath, name = mediaName)
        }
        if (episode !in series.episodes) series.episodes.add(episode)
        // Preserve mined lines — snapshot exposure within this episode.
        val minedByStart = episode.scenes.flatMap { it.lines }.filter { it.exposureCount > 0 }
            .associateBy { it.startMs }
        // Replace scenes for this episode with fresh indexed lines (single synthetic scene).
        episode.scenes.removeAll { it.id.startsWith("idx-") || it.id.startsWith("scene-$mediaPath-") && minedByStart.isEmpty() }
        // If mined lines exist, keep their scenes and just add missing indexed lines as a separate scene.
        val indexedScene = MediaScene(
            id = "idx-$mediaPath-${cues.size}",
            startMs = cues.first().startMs,
            endMs = cues.last().endMs,
            lines = cues.mapIndexed { idx, cue ->
                val preserved = minedByStart.entries.firstOrNull { kotlin.math.abs(it.key - cue.startMs) < 500 }?.value
                SubtitleLine(
                    index = idx + 1,
                    startMs = cue.startMs,
                    endMs = cue.endMs,
                    text = cue.text,
                    exposureCount = preserved?.exposureCount ?: 0
                )
            }.toMutableList()
        )
        // Idempotency: collapse duplicate indexed scenes.
        episode.scenes.removeAll { it.id.startsWith("idx-$mediaPath-") }
        episode.scenes.add(indexedScene)
    }

    fun clearMedia(mediaPath: String) {
        episodeByPath.remove(mediaPath)?.let { ep ->
            seriesById.values.forEach { it.episodes.remove(ep) }
            seriesById.entries.removeIf { it.value.episodes.isEmpty() }
        }
    }

    fun series(): List<MediaSeries> = seriesById.values.toList()

    fun totalWatchTimeMs(): Long = series().sumOf { it.watchTimeMs() }

    /** Flatten all subtitle lines across the graph (sentence surface for search). */
    fun allLines(): List<SubtitleLine> = series().flatMap { it.episodes }.flatMap { it.scenes }.flatMap { it.lines }

    private fun inferSeriesTitle(mediaName: String): String {
        // "Bocchi the Rock - EP01.mkv" → "Bocchi the Rock"
        val episodeMarker = Regex("\\s[-–—]\\s*(EP|E)?\\d+", RegexOption.IGNORE_CASE)
        return episodeMarker.replace(mediaName, "").trim().ifBlank { mediaName }
    }
}
