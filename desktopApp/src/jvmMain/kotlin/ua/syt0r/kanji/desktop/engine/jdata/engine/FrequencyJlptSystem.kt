package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.jdata.model.FrequencyValue
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry

// ============================================================
// FREQUENCY / JLPT SUBSYSTEM
// Both are source-derived classifications, never intrinsic word
// properties. Frequency values always carry their source so they
// are never compared blindly across corpora; JLPT bands are
// emitted as stable entity IDs that future sources can extend.
// ============================================================

object FrequencyJlptSystem {

    /** Stable entity ID for a JLPT band (n5..n1). */
    fun jlptEntity(level: Int): String = StableIds.jlpt(level)

    /** Reads JLPT out of a tag list ("jlpt-n5", "n4", "jlpt4", "jlpt-n1"). */
    fun jlptFromTags(tags: List<String>): Int? =
        tags.firstNotNullOfOrNull { tag ->
            val lowered = tag.lowercase()
            when {
                lowered.contains("jlpt-n") -> lowered.substringAfter("jlpt-n").toIntOrNull()
                lowered.contains("jlpt") -> lowered.removePrefix("jlpt").removePrefix("-").removePrefix("n").toIntOrNull()
                lowered.startsWith("n") && lowered.length == 2 && lowered[1] in '1'..'5' -> lowered[1].digitToInt()
                else -> null
            }
        }

    /** Bands covered by a dataset, derived from tags, for coverage reports. */
    fun jlptBands(kanji: Collection<KanjiEntry>, vocab: Collection<VocabEntry>): Map<Int, Int> {
        val counts = (5 downTo 1).associateWith { 0 }.toMutableMap()
        kanji.forEach { counts[it.jlpt ?: return@forEach] = (counts[it.jlpt] ?: 0) + 1 }
        vocab.forEach { counts[it.jlpt ?: return@forEach] = (counts[it.jlpt] ?: 0) + 1 }
        return counts
    }

    /** Frequencies merged across sources for one entity, sorted by source name. */
    fun mergedFrequencies(entries: List<VocabEntry>): List<FrequencyValue> =
        entries.flatMap { it.frequencies }
            .distinctBy { it.source }
            .sortedBy { it.source }

    /** Provenance of a kanji's frequency claim. */
    fun kanjiFrequencyProvenance(kanji: KanjiEntry): List<SourceRef> =
        kanji.sources.filter { source ->
            source.sourceId.isNotBlank()
        }
}
