package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseText
import ua.syt0r.kanji.desktop.engine.dictionary.KanjiSpelling
import ua.syt0r.kanji.desktop.engine.jdata.model.ComponentEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.ComponentKind
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.RadicalEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.engine.jdata.normalize.Normalizer

// ============================================================
// KANJI / RADICAL / COMPONENT SUBSYSTEM
// Builds canonical kanji entries (with radical + component
// relationships) from installed dictionary data. Radical meanings
// come from a small documented reference table; the radical set
// itself is whatever the sources declare (KANJIDIC-style lists).
// ============================================================

object KanjiSystem {

    /** Well-known radical meanings (reference subset; sources may override). */
    private val radicalMeanings = mapOf(
        "日" to "sun; day", "月" to "moon; month", "水" to "water", "火" to "fire",
        "木" to "tree", "金" to "metal", "土" to "earth", "人" to "person",
        "口" to "mouth", "心" to "heart", "手" to "hand", "足" to "foot",
        "目" to "eye", "耳" to "ear", "山" to "mountain", "川" to "river",
        "田" to "rice field", "力" to "power", "女" to "woman", "子" to "child",
        "犬" to "dog", "馬" to "horse", "鳥" to "bird", "魚" to "fish",
        "雨" to "rain", "門" to "gate", "車" to "vehicle", "食" to "eat; food",
        "言" to "say; word", "糸" to "thread", "一" to "one", "二" to "two",
        "三" to "three", "上" to "above", "下" to "below", "大" to "big",
        "小" to "small", "中" to "middle", "王" to "king", "玉" to "jewel",
        "石" to "stone", "竹" to "bamboo", "米" to "rice", "貝" to "shell",
        "衣" to "clothes", "見" to "see", "立" to "stand", "虫" to "insect",
        "草" to "grass", "花" to "flower"
    )

    /**
     * All kanji entries across installed dictionaries, merged per character
     * so the same kanji from several sources becomes ONE canonical entry
     * (meanings/readings unioned; provenance kept per source dictionary).
     */
    fun fromRepository(repository: DictionaryRepository): Map<String, KanjiEntry> {
        val byCharacter = mutableMapOf<String, MutableList<Pair<KanjiSpelling, String>>>()
        repository.allEntries().forEach { entry ->
            entry.kanjiSpellings.forEach { spelling ->
                if (spelling.character.isNotEmpty()) {
                    byCharacter.getOrPut(spelling.character) { mutableListOf() }.add(spelling to entry.dictionaryId)
                }
            }
            // Entries whose headword IS a single kanji but carry no KanjiSpelling
            // (common in kanji-only dictionary exports) still contribute.
            if (entry.kanjiSpellings.isEmpty() && entry.headword.length == 1 &&
                JapaneseText.isKanji(entry.headword)
            ) {
                val inferred = KanjiSpelling(
                    character = entry.headword,
                    onReadings = entry.readings.flatMap { readingFromInfo(it.readingInformation, "on:") },
                    kunReadings = entry.readings.flatMap { readingFromInfo(it.readingInformation, "kun:") },
                    meanings = entry.senses.flatMap { it.glosses }
                )
                byCharacter.getOrPut(entry.headword) { mutableListOf() }.add(inferred to entry.dictionaryId)
            }
        }
        val sourceOrder = repository.installedDictionaries().map { it.id }
        return byCharacter.mapValues { (character, pairs) ->
            merge(character, pairs, sourceOrder)
        }
    }

    /** Readings tagged "on:…" / "kun:…" inside reading-information strings. */
    private fun readingFromInfo(info: List<String>, prefix: String): List<String> =
        info.filter { it.startsWith(prefix) }
            .flatMap { it.removePrefix(prefix).split("/", "、") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Merge multiple source spellings for one character. Deterministic:
     * sources are processed in dictionary-priority order, values are
     * unioned, and provenance records every contributing source.
     */
    private fun merge(
        character: String,
        pairs: List<Pair<KanjiSpelling, String>>,
        sourceOrder: List<String>
    ): KanjiEntry {
        val ordered = pairs.sortedBy { (_, dictId) ->
            sourceOrder.indexOf(dictId).let { if (it < 0) Int.MAX_VALUE else it }
        }
        val meanings = ordered.flatMap { it.first.meanings }.distinct()
        val on = ordered.flatMap { it.first.onReadings }.distinct()
        val kun = ordered.flatMap { it.first.kunReadings }.distinct()
        val strokeCount = ordered.firstNotNullOfOrNull { it.first.strokeCounts.firstOrNull() }
        val jlpt = ordered.firstNotNullOfOrNull { it.first.jlpt }
        val grade = ordered.firstNotNullOfOrNull { it.first.grade }
        val frequencyRank = ordered.mapNotNull { it.first.frequency }.minOrNull()
        val radicals = ordered.flatMap { it.first.radicals }.distinct()
        return KanjiEntry(
            id = StableIds.kanji(character),
            character = character,
            meanings = meanings,
            onReadings = on,
            kunReadings = kun,
            strokeCount = strokeCount,
            radicalId = radicals.firstOrNull()?.let { StableIds.radical(it) },
            jlpt = jlpt,
            grade = grade,
            frequencyRank = frequencyRank,
            sources = ordered.map { (_, dictId) -> SourceRef(dictId, character) }.distinct()
        )
    }

    /** Radical entries referenced by the kanji set, with reference meanings. */
    fun radicalsFrom(kanji: Map<String, KanjiEntry>): Map<String, RadicalEntry> =
        kanji.values
            .mapNotNull { it.radicalId }
            .distinct()
            .sorted()
            .associateWith { radicalId ->
                val character = StableIds.radicalCharacter(radicalId)
                RadicalEntry(
                    id = radicalId,
                    character = character,
                    meaning = radicalMeanings[character],
                    strokeCount = kanji[StableIds.kanji(character)]?.strokeCount,
                    sources = listOf(SourceRef("radical-meanings-reference", character))
                )
            }

    /** Components derived from the radical set (radical-kind for now). */
    fun componentsFrom(radicals: Map<String, RadicalEntry>): Map<String, ComponentEntry> =
        radicals.values.associate { radical ->
            StableIds.component(radical.character) to ComponentEntry(
                id = StableIds.component(radical.character),
                character = radical.character,
                kind = ComponentKind.RADICAL,
                sources = radical.sources
            )
        }

    /** Kanji → vocab links: every vocabulary expression containing each kanji. */
    fun kanjiToVocab(
        kanji: Map<String, KanjiEntry>,
        vocab: Collection<VocabEntry>
    ): Map<String, List<String>> {
        val result = kanji.keys.associateWith { mutableListOf<String>() }.toMutableMap()
        vocab.forEach { entry ->
            Normalizer.kanjiCharacters(entry.expression).forEach { ch ->
                result[StableIds.kanji(ch)]?.add(entry.id)
            }
        }
        return result
    }
}
