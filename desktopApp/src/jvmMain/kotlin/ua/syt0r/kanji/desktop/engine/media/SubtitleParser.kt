package ua.syt0r.kanji.desktop.engine.media

import kotlinx.serialization.Serializable
import java.io.File

// ============================================
// KAITEYO SUBTITLE SYSTEM
// Parses SRT, ASS, SSA and VTT subtitle files
// into a uniform track model with millisecond
// timestamps, cue text, per-word tokens and
// association to audio/video timing.
// ============================================

/** A single subtitle cue. */
@Serializable
data class SubtitleCue(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val style: String = "",
    val speaker: String = ""
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0)

    /** Split into individual word tokens (keeps Japanese intact). */
    fun tokens(): List<String> =
        text.split(Regex("\\s+")).map { it.trim().replace(Regex("[\\[\\](){}\"“”]"), "") }.filter { it.isNotEmpty() }
}

/** A parsed subtitle track. */
@Serializable
data class SubtitleTrack(
    val name: String,
    val cues: List<SubtitleCue>,
    val format: SubtitleFormat,
    val language: String = ""
) {
    fun cueAt(ms: Long): SubtitleCue? = cues.firstOrNull { ms in it.startMs..it.endMs }
}

@Serializable
enum class SubtitleFormat { Srt, Ass, Ssa, Vtt }

object SubtitleParser {

    fun detectFormat(file: File): SubtitleFormat = when (file.extension.lowercase()) {
        "ass" -> SubtitleFormat.Ass
        "ssa" -> SubtitleFormat.Ssa
        "vtt" -> SubtitleFormat.Vtt
        else -> SubtitleFormat.Srt
    }

    /** Parse any supported subtitle file into a track. */
    fun parse(file: File, name: String = file.nameWithoutExtension): SubtitleTrack =
        when (detectFormat(file)) {
            SubtitleFormat.Srt -> parseSrt(file.readText(), name)
            SubtitleFormat.Vtt -> parseVtt(file.readText(), name)
            SubtitleFormat.Ass -> parseAss(file.readText(), name, SubtitleFormat.Ass)
            SubtitleFormat.Ssa -> parseAss(file.readText(), name, SubtitleFormat.Ssa)
        }

    /** SRT parsing with robust handling of CRLF, BOM and stray timestamps. */
    fun parseSrt(text: String, name: String = "srt"): SubtitleTrack {
        val normalized = text.removePrefix("\uFEFF").replace("\r\n", "\n")
        val cues = mutableListOf<SubtitleCue>()
        var block = mutableListOf<String>()
        var time = ""

        fun flush() {
            if (block.isEmpty()) return
            val start = parseTimestamp(time)
            if (start != null) {
                val id = "cue-${cues.size}"
                val body = block.joinToString("\n")
                val speaker = Regex("^([A-Za-z0-9_\\-\\u4e00-\\u9fff]+)[:：]\\s*").find(body)?.groupValues?.get(1).orEmpty()
                cues.add(SubtitleCue(id, start.first, start.second, body, speaker = speaker))
            }
            block.clear(); time = ""
        }

        normalized.split("\n").forEach { line ->
            when {
                line.isBlank() -> flush()
                line.contains("-->") -> { time = line; }
                block.isEmpty() && line.toIntOrNull() != null && cues.isNotEmpty() -> Unit // cue number
                else -> block.add(line.trim())
            }
        }
        flush()
        return SubtitleTrack(name, cues, SubtitleFormat.Srt)
    }

    /** WebVTT parsing (also tolerates a leading WEBVTT header). */
    fun parseVtt(text: String, name: String = "vtt"): SubtitleTrack {
        val normalized = text.removePrefix("\uFEFF").replace("\r\n", "\n")
        val cues = mutableListOf<SubtitleCue>()
        val lines = normalized.split("\n").toMutableList()

        // Strip header + NOTE blocks.
        while (lines.isNotEmpty() && !lines.first().contains("-->")) lines.removeAt(0)

        var block = mutableListOf<String>()
        var time = ""
        fun flush() {
            if (time.isBlank()) return
            val t = parseTimestamp(time.replace('.', ','))
            if (t != null) {
                cues.add(SubtitleCue("vtt-${cues.size}", t.first, t.second, block.joinToString("\n").trim()))
            }
            block.clear(); time = ""
        }
        lines.forEach { line ->
            when {
                line.isBlank() -> flush()
                line.contains("-->") -> { time = line }
                line.startsWith("NOTE") -> Unit
                else -> block.add(line.trim())
            }
        }
        flush()
        return SubtitleTrack(name, cues, SubtitleFormat.Vtt)
    }

    /** ASS/SSA: reads [Events] section with Format: and Dialogue: lines. */
    fun parseAss(text: String, name: String = "ass", format: SubtitleFormat = SubtitleFormat.Ass): SubtitleTrack {
        val normalized = text.removePrefix("\uFEFF").replace("\r\n", "\n")
        var inEvents = false
        var formatLine: List<String> = emptyList()
        val cues = mutableListOf<SubtitleCue>()

        normalized.split("\n").forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("[Events]")) {
                inEvents = true
                formatLine = emptyList()
            } else if (line.startsWith("[")) {
                inEvents = false
            } else if (inEvents) {
                if (line.startsWith("Format:")) {
                    formatLine = line.removePrefix("Format:").split(",").map { it.trim() }
                } else if (line.startsWith("Dialogue:")) {
                    val payload = line.removePrefix("Dialogue:")
                    val idxStart = formatLine.indexOf("Start")
                    val idxEnd = formatLine.indexOf("End")
                    val idxText = formatLine.indexOf("Text")
                    if (idxStart >= 0 && idxEnd >= 0 && idxText >= 0) {
                        // Commas inside text are legal; split only the header fields.
                        val commaSplits = payload.split(",", limit = (formatLine.size - idxText))
                        val text = commaSplits.drop(idxText).joinToString(",")
                        val startMs = assTimeToMs(commaSplits.getOrNull(idxStart).orEmpty())
                        val endMs = assTimeToMs(commaSplits.getOrNull(idxEnd).orEmpty())
                        val speaker = if (formatLine.contains("Name")) {
                            val idxName = formatLine.indexOf("Name")
                            commaSplits.getOrNull(idxName).orEmpty()
                        } else ""
                        val style = if (formatLine.contains("Style")) {
                            val idxStyle = formatLine.indexOf("Style")
                            commaSplits.getOrNull(idxStyle).orEmpty()
                        } else ""
                        val clean = stripAssTags(text)
                        if (clean.isNotBlank()) {
                            cues.add(SubtitleCue("ass-${cues.size}", startMs, endMs, clean, style, speaker))
                        }
                    }
                }
            }
        }
        return SubtitleTrack(name, cues, format)
    }

    private fun stripAssTags(text: String): String =
        text.replace(Regex("\\{\\\\[^}]*\\}"), "").replace("\\N", "\n").replace("\\n", "\n")

    private fun assTimeToMs(t: String): Long {
        val parts = t.split(":", limit = 3)
        if (parts.size != 3) return 0L
        val h = parts[0].toLongOrNull() ?: return 0L
        val m = parts[1].toLongOrNull() ?: return 0L
        val sec = parts[2].toDoubleOrNull() ?: return 0L
        return ((h * 3600 + m * 60 + sec) * 1000).toLong()
    }

    /** Parse "HH:MM:SS,mmm" or "MM:SS,mmm" into start/end millis. */
    private fun parseTimestamp(raw: String): Pair<Long, Long>? {
        val regex = Regex("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})[,.:](\\d{1,3})\\s*-->\\s*(\\d{1,2}):(\\d{1,2}):(\\d{1,2})[,.:](\\d{1,3})")
        val m = regex.find(raw) ?: return null
        val start = m.groupValues[1].toLong() * 3600000 + m.groupValues[2].toLong() * 60000 +
            m.groupValues[3].toLong() * 1000 + m.groupValues[4].padEnd(3, '0').toLong()
        val end = m.groupValues[5].toLong() * 3600000 + m.groupValues[6].toLong() * 60000 +
            m.groupValues[7].toLong() * 1000 + m.groupValues[8].padEnd(3, '0').toLong()
        return start to end
    }
}