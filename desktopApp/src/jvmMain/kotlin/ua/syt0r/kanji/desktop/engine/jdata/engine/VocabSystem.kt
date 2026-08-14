package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryEntry
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.jdata.model.FrequencyValue
import ua.syt0r.kanji.desktop.engine.jdata.model.ReadingInfo
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabSense

// ============================================================
// VOCABULARY SUBSYSTEM
// Deeply structured vocabulary: expression + multiple readings +
// multiple structured senses + furigana + frequency + provenance.
// Readings are never flattened; each sense keeps its own POS/field/
// misc/restrictions so consumers can render or filter precisely.
// ============================================================

object VocabSystem {

    /**
     * All vocabulary entries across installed dictionaries, merged so the
     * same (expression, reading) from several sources is ONE canonical
     * entry with unioned senses/frequencies and full provenance.
     */
    fun fromRepository(
        repository: DictionaryRepository,
        kanjiReadingLookup: (String) -> List<String> = { emptyList() }
    ): Map<String, VocabEntry> {
        val byIdentity = mutableMapOf<String, MutableList<DictionaryEntry>>()
        repository.allEntries().forEach { entry ->
            if (entry.headword.isBlank()) return@forEach
            entry.readings.forEach { reading ->
                val identity = "(${entry.headword}\u0000${reading.reading})"
                byIdentity.getOrPut(identity) { mutableListOf() }.add(entry)
            }
            // Entries without explicit readings still get one identity.
            if (entry.readings.isEmpty()) {
                byIdentity.getOrPut("(${entry.headword}\u0000)") { mutableListOf() }.add(entry)
            }
        }
        return byIdentity.mapValues { (_, entries) ->
            merge(entries, kanjiReadingLookup)
        }
    }

    private fun merge(
        entries: List<DictionaryEntry>,
        kanjiReadingLookup: (String) -> List<String>
    ): VocabEntry {
        val expression = entries.first().headword
        val readings = entries
            .flatMap { it.readings }
            .distinctBy { it.reading }
            .map { r ->
                ReadingInfo(
                    kana = r.reading,
                    restrictions = r.readingInformation,
                    pitchAccents = r.pitchAccents.map { p ->
                        ua.syt0r.kanji.desktop.engine.jdata.model.PitchMarker(p.position, p.downstep)
                    }
                )
            }
        val primaryReading = readings.firstOrNull()?.kana ?: ""
        val senses = entries.flatMapIndexed { eIndex, entry ->
            entry.senses.mapIndexed { sIndex, sense ->
                VocabSense(
                    glosses = sense.glosses,
                    partOfSpeech = sense.partOfSpeech,
                    misc = sense.tags,
                    restrictions = sense.restrictions,
                    sourceRefs = listOf(SourceRef(entry.dictionaryId, entry.headword))
                )
            }
        }
        val frequencies = entries.mapNotNull { e ->
            e.frequency.rank?.let { FrequencyValue(e.dictionaryId, rank = it) }
                ?: e.frequency.score?.let { FrequencyValue(e.dictionaryId, value = it) }
        }
        val sources = entries.map { SourceRef(it.dictionaryId, it.headword) }.distinct()
        val jlpt = entries.firstNotNullOfOrNull { it.kanjiSpellings.firstOrNull()?.jlpt }
        return VocabEntry(
            id = StableIds.vocab(expression, primaryReading),
            expression = expression,
            readings = readings,
            senses = senses,
            furigana = if (primaryReading.isNotBlank()) {
                FuriganaEngine.parse(expression, primaryReading, kanjiReadingLookup)
            } else emptyList(),
            frequencies = frequencies,
            jlpt = jlpt,
            sources = sources
        )
    }

    /** Kotlin-visible entry mapper for adapters/tests (single-entry form). */
    fun fromEntry(entry: DictionaryEntry): VocabEntry? {
        if (entry.headword.isBlank()) return null
        return merge(listOf(entry)) { emptyList() }
    }
}
