package io.kaiteyo.kjd.resolve

import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.FrequencyRecord
import io.kaiteyo.kjd.model.JlptClassification
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.KanaCharacter
import io.kaiteyo.kjd.model.Meaning
import io.kaiteyo.kjd.model.Reading
import io.kaiteyo.kjd.model.SourceRef
import io.kaiteyo.kjd.model.Stroke
import io.kaiteyo.kjd.model.Syllabary
import io.kaiteyo.kjd.model.VocabularyEntry
import io.kaiteyo.kjd.model.VocabularyReading
import io.kaiteyo.kjd.model.Sense
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import io.kaiteyo.kjd.parser.RawJmdictEntry
import io.kaiteyo.kjd.parser.RawJmdictSense
import io.kaiteyo.kjd.parser.RawKanjidicCharacter
import io.kaiteyo.kjd.parser.RawKanjiVgCharacter
import io.kaiteyo.kjd.parser.RawJlptClassification
import io.kaiteyo.kjd.parser.RawFrequencyRecord
import io.kaiteyo.kjd.parser.RawYomichanJlptVocab
import io.kaiteyo.kjd.parser.RawFuriganaRecord
import io.kaiteyo.kjd.parser.KanjiVgParser
import io.kaiteyo.kjd.parser.JmdictFuriganaParser
import io.kaiteyo.kjd.source.SourceMetadata
import io.kaiteyo.kjd.source.SourceIds
import io.kaiteyo.kjd.source.toSourceRef

/**
 * Holds the accumulated canonical database being built by the resolver.
 * Resolution is a multi-pass pipeline: characters first, then vocabulary,
 * then relationships. Each pass may add source-specific facts that the next
 * pass uses (e.g. JLPT from Tanos attaches to kanji after KANJIDIC).
 */
class CanonicalDatabaseBuilder {

    private val kanjiByLiteral = LinkedHashMap<String, Kanji>()
    private val kanaByLiteral = LinkedHashMap<String, KanaCharacter>()
    private val vocabByKey = LinkedHashMap<String, VocabularyEntry>()
    private val senses = LinkedHashMap<EntityId, Sense>()
    private val radicalList = mutableListOf<io.kaiteyo.kjd.model.Radical>()
    private val componentList = mutableListOf<io.kaiteyo.kjd.model.Component>()
    private val exampleList = mutableListOf<io.kaiteyo.kjd.model.ExampleSentence>()
    private val relationshipList = mutableListOf<io.kaiteyo.kjd.model.Relationship>()
    private val tagList = mutableListOf<io.kaiteyo.kjd.model.Tag>()

    val kanji: List<Kanji> get() = kanjiByLiteral.values.toList()
    val kana: List<KanaCharacter> get() = kanaByLiteral.values.toList()
    val vocabulary: List<VocabularyEntry> get() = vocabByKey.values.toList()
    val allSenses: List<Sense> get() = senses.values.toList()
    val radicals: List<io.kaiteyo.kjd.model.Radical> get() = radicalList
    val components: List<io.kaiteyo.kjd.model.Component> get() = componentList
    val exampleSentences: List<io.kaiteyo.kjd.model.ExampleSentence> get() = exampleList
    val relationships: List<io.kaiteyo.kjd.model.Relationship> get() = relationshipList
    val tags: List<io.kaiteyo.kjd.model.Tag> get() = tagList

    fun addRadical(radical: io.kaiteyo.kjd.model.Radical) {
        if (radicalList.none { it.id == radical.id }) radicalList.add(radical)
    }

    fun addComponent(component: io.kaiteyo.kjd.model.Component) {
        componentList.add(component)
    }

    fun addExample(example: io.kaiteyo.kjd.model.ExampleSentence) {
        exampleList.add(example)
    }

    fun addRelationship(relationship: io.kaiteyo.kjd.model.Relationship) {
        relationshipList.add(relationship)
    }

    fun addTag(tag: io.kaiteyo.kjd.model.Tag) {
        tagList.add(tag)
    }

    /** Build an immutable snapshot for validation/generation. */
    fun snapshot(): io.kaiteyo.kjd.model.CanonicalDatabase =
        io.kaiteyo.kjd.model.CanonicalDatabase.from(this)

    // ---------------------------------------------------------------
    // Kanji
    // ---------------------------------------------------------------

    fun upsertKanji(kanji: Kanji) {
        kanjiByLiteral[kanji.character.literal] = kanji
    }

    fun kanjiBy(literal: String): Kanji? = kanjiByLiteral[literal]

    fun mergeIntoKanji(literal: String, transform: (Kanji) -> Kanji) {
        val current = kanjiByLiteral[literal] ?: return
        kanjiByLiteral[literal] = transform(current)
    }

    // ---------------------------------------------------------------
    // Kana
    // ---------------------------------------------------------------

    fun upsertKana(kanaChar: KanaCharacter) {
        kanaByLiteral[kanaChar.character.literal] = kanaChar
    }

    fun kanaBy(literal: String): KanaCharacter? = kanaByLiteral[literal]

    // ---------------------------------------------------------------
    // Vocabulary
    // ---------------------------------------------------------------

    fun upsertVocab(entry: VocabularyEntry) {
        vocabByKey[entry.id.value] = entry
    }

    fun vocabBy(id: EntityId): VocabularyEntry? = vocabByKey[id.value]

    fun addSense(sense: Sense) {
        senses[sense.id] = sense
    }
}

/**
 * Implements the canonical entity resolution:
 *
 * 1. Characters: KANJIDIC + KanjiVG merge on literal identity. Kana are
 *    detected from Unicode ranges.
 * 2. Vocabulary: JMdict entries keep their ent_seq as stable id; furigana
 *    attaches by ent_seq; JLPT/frequency attach by expression/reading key.
 * 3. Conflicts between sources are never silently overwritten — each fact
 *    carries a [SourceRef]; canonical selection rules decide which value
 *    wins (e.g. canonical JLPT = Tanos) while the losing value stays
 *    traceable.
 */
class EntityResolver {

    private val builder = CanonicalDatabaseBuilder()

    fun database(): CanonicalDatabaseBuilder = builder

    /** Merge all parsed sources into the canonical database. */
    fun resolve(
        kanjiVg: Map<String, SourceMetadata> = emptyMap(),
        kanjidic: Map<String, SourceMetadata> = emptyMap(),
        jmdict: Map<String, SourceMetadata> = emptyMap(),
        furigana: Map<String, SourceMetadata> = emptyMap(),
        tanosJlpt: Map<String, SourceMetadata> = emptyMap(),
        leedsFrequency: Map<String, SourceMetadata> = emptyMap(),
        yomichanJlptVocab: Map<String, SourceMetadata> = emptyMap(),
        kanjiVgCharacters: List<RawKanjiVgCharacter> = emptyList(),
        kanjidicCharacters: List<RawKanjidicCharacter> = emptyList(),
        jmdictEntries: List<RawJmdictEntry> = emptyList(),
        furiganaRecords: List<RawFuriganaRecord> = emptyList(),
        tanosJlptRecords: List<RawJlptClassification> = emptyList(),
        leedsFrequencyRecords: List<RawFrequencyRecord> = emptyList(),
        yomichanJlptVocabRecords: List<RawYomichanJlptVocab> = emptyList()
    ) {
        resolveCharacters(kanjidicCharacters, kanjidic, kanjiVgCharacters, kanjiVg)
        resolveVocabulary(jmdictEntries, jmdict, furiganaRecords, furigana)
        attachJlpt(tanosJlptRecords, tanosJlpt, yomichanJlptVocabRecords, yomichanJlptVocab)
        attachFrequency(leedsFrequencyRecords, leedsFrequency)
        linkVocabularyToKanji()
    }

    // ===============================================================
    // Characters
    // ===============================================================

    private fun resolveCharacters(
        kanjidicCharacters: List<RawKanjidicCharacter>,
        kanjidic: Map<String, SourceMetadata>,
        kanjiVgCharacters: List<RawKanjiVgCharacter>,
        kanjiVg: Map<String, SourceMetadata>
    ) {
        // Pass 1: KANJIDIC establishes kanji entities.
        val kanjidicMeta = kanjidic[SourceIds.KANJIDIC]
        for (raw in kanjidicCharacters) {
            val id = EntityId("kanji:${raw.kanji}")
            val charType = classifyCharacter(raw.kanji)
            val source = kanjidicMeta?.toSourceRef(recordId = raw.kanji, isCanonical = true)
                ?: SourceRef(sourceId = SourceIds.KANJIDIC)

            if (charType == io.kaiteyo.kjd.model.CharacterType.Kana) {
                val syllabary = if (raw.kanji.first().code in 0x3040..0x309F) Syllabary.Hiragana else Syllabary.Katakana
                builder.upsertKana(
                    KanaCharacter(
                        id = id,
                        character = io.kaiteyo.kjd.model.Character(
                            id = id,
                            literal = raw.kanji,
                            codepoint = raw.kanji.first().code,
                            normalized = JapaneseNormalizer.toNfc(raw.kanji),
                            characterType = io.kaiteyo.kjd.model.CharacterType.Kana
                        ),
                        syllabary = syllabary,
                        strokeCount = raw.strokeCount,
                        sources = listOf(source)
                    )
                )
                continue
            }

            builder.upsertKanji(
                Kanji(
                    id = id,
                    character = io.kaiteyo.kjd.model.Character(
                        id = id,
                        literal = raw.kanji,
                        codepoint = raw.kanji.first().code,
                        normalized = JapaneseNormalizer.toNfc(raw.kanji),
                        characterType = charType,
                        strokeCount = raw.strokeCount,
                        grade = raw.grade
                    ),
                    onReadings = raw.onReadings.map { Reading(it, type = "on", source = listOf(source)) },
                    kunReadings = raw.kunReadings.map { Reading(it, type = "kun", source = listOf(source)) },
                    meanings = raw.meanings.map { Meaning(it.value, it.language, listOf(source)) },
                    grade = raw.grade,
                    strokeCount = raw.strokeCount,
                    radical = raw.radicalName,
                    sources = listOf(source)
                )
            )
        }

        // Pass 2: KanjiVG attaches stroke data to existing kanji (or creates
        // stroke-less placeholders for kana when the source includes them).
        val kanjiVgMeta = kanjiVg[SourceIds.KANJIVG]
        for (raw in kanjiVgCharacters) {
            val literal = raw.kanji
            val source = kanjiVgMeta?.toSourceRef(recordId = "kvg:kanji_$literal", transformation = "parsed")
                ?: SourceRef(sourceId = SourceIds.KANJIVG)

            val charType = classifyCharacter(literal)
            if (charType == io.kaiteyo.kjd.model.CharacterType.Kana) {
                val existing = builder.kanaBy(literal)
                val strokes = raw.strokes.map { KanjiVgParser.toCanonicalStroke(it, EntityId("kana:$literal"), listOf(source)) }
                if (existing != null) {
                    builder.upsertKana(existing.copy(strokes = strokes, strokeCount = strokes.size))
                } else {
                    val syllabary = if (literal.first().code in 0x3040..0x309F) Syllabary.Hiragana else Syllabary.Katakana
                    val id = EntityId("kana:$literal")
                    builder.upsertKana(
                        KanaCharacter(
                            id = id,
                            character = io.kaiteyo.kjd.model.Character(
                                id = id, literal = literal, codepoint = literal.first().code,
                                normalized = JapaneseNormalizer.toNfc(literal),
                                characterType = io.kaiteyo.kjd.model.CharacterType.Kana,
                                strokeCount = strokes.size
                            ),
                            syllabary = syllabary,
                            strokeCount = strokes.size,
                            strokes = strokes,
                            sources = listOf(source)
                        )
                    )
                }
                continue
            }

            val strokes = raw.strokes.map {
                KanjiVgParser.toCanonicalStroke(it, EntityId("kanji:$literal"), listOf(source))
            }
            builder.mergeIntoKanji(literal) { kanji ->
                val parts = buildSet {
                    raw.parts.forEach { add(it) }
                    raw.radical?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
                val components = parts.map { part ->
                    io.kaiteyo.kjd.model.Component(
                        id = EntityId("component:kanji:$literal:$part"),
                        character = part,
                        role = if (part == raw.radical) "radical" else "graphical",
                        sources = listOf(source)
                    )
                }
                components.forEach { builder.addComponent(it) }
                kanji.copy(
                    strokes = strokes,
                    strokeCount = strokes.size.takeIf { it > 0 } ?: kanji.strokeCount,
                    radical = raw.radical ?: raw.radNumber?.toString() ?: kanji.radical,
                    components = components,
                    sources = kanji.sources + source
                )
            }
        }
    }

    // ===============================================================
    // Vocabulary
    // ===============================================================

    private fun resolveVocabulary(
        jmdictEntries: List<RawJmdictEntry>,
        jmdict: Map<String, SourceMetadata>,
        furiganaRecords: List<RawFuriganaRecord>,
        furigana: Map<String, SourceMetadata>
    ) {
        val jmdictMeta = jmdict[SourceIds.JMDICT]
        val furiganaMeta = furigana[SourceIds.JMDICT_FURIGANA]
        val furiganaBySeq = furiganaRecords.groupBy { it.entSeq }

        for (raw in jmdictEntries) {
            val id = EntityId("vocab:jmdict_${raw.entSeq}")
            val entrySource = jmdictMeta?.toSourceRef(recordId = raw.entSeq.toString(), isCanonical = true)
                ?: SourceRef(sourceId = SourceIds.JMDICT)

            val primaryExpression = raw.kanji.firstOrNull()?.keb ?: raw.readings.firstOrNull()?.reb ?: ""
            if (primaryExpression.isEmpty()) continue

            val readings = raw.readings.map { r ->
                VocabularyReading(
                    value = r.reb,
                    isKanaOnly = JapaneseNormalizer.isKanaOnly(r.reb),
                    noKanji = r.noKanji,
                    source = listOf(entrySource)
                )
            }

            // Furigana attaches by ent_seq + expression/reading match.
            val furiganaForEntry = furiganaBySeq[raw.entSeq]
                ?.firstOrNull { it.expression == primaryExpression }
            val segments = furiganaForEntry?.segments
                ?: raw.readings.firstOrNull()?.let {
                    JmdictFuriganaParser.deriveSegments(primaryExpression, it.reb)
                }
                ?: emptyList()
            val furiganaSource = furiganaMeta?.toSourceRef(recordId = raw.entSeq.toString(), transformation = "furigana")

            val entrySenses = mutableListOf<Sense>()
            raw.senses.forEachIndexed { index, rawSense ->
                val senseId = EntityId("sense:jmdict_${raw.entSeq}:$index")
                entrySenses.add(
                    Sense(
                        id = senseId,
                        vocabularyId = id,
                        index = index,
                        glosses = rawSense.glosses.map { Meaning(it.value, it.language, listOf(entrySource)) },
                        partsOfSpeech = rawSense.pos.map { io.kaiteyo.kjd.model.PartOfSpeech(it, listOf(entrySource)) },
                        fields = rawSense.field,
                        misc = rawSense.misc,
                        restrictions = rawSense.stagk + rawSense.stagr,
                        sources = listOf(entrySource)
                    )
                )
            }

            val entry = VocabularyEntry(
                id = id,
                expression = primaryExpression,
                readings = readings,
                senses = entrySenses,
                furigana = segments,
                partsOfSpeech = raw.senses.flatMap { it.pos }
                    .distinct()
                    .map { io.kaiteyo.kjd.model.PartOfSpeech(it, listOf(entrySource)) },
                sources = listOf(entrySource) + listOfNotNull(furiganaSource)
            )
            builder.upsertVocab(entry)
            entrySenses.forEach { builder.addSense(it) }
        }
    }

    // ===============================================================
    // JLPT
    // ===============================================================

    private fun attachJlpt(
        tanosRecords: List<RawJlptClassification>,
        tanos: Map<String, SourceMetadata>,
        yomichanRecords: List<RawYomichanJlptVocab>,
        yomichan: Map<String, SourceMetadata>
    ) {
        val tanosMeta = tanos[SourceIds.TANOS_JLPT]
        val yomichanMeta = yomichan[SourceIds.YOMICHAN_JLPT_VOCAB]

        // Tanos is canonical for JLPT.
        for (raw in tanosRecords) {
            val source = tanosMeta?.toSourceRef(recordId = raw.item, transformation = "jlpt", isCanonical = true)
                ?: SourceRef(sourceId = SourceIds.TANOS_JLPT, isCanonical = true)
            val classification = JlptClassification(level = raw.level, source = source)

            val kanji = builder.kanjiBy(raw.item)
            if (kanji != null) {
                builder.mergeIntoKanji(raw.item) { k ->
                    k.copy(jlpt = k.jlpt + classification)
                }
                continue
            }
            attachJlptToVocabulary(raw.item, classification)
        }

        // yomichan-jlpt-vocab is a secondary source — never overwrites Tanos.
        for (raw in yomichanRecords) {
            val source = yomichanMeta?.toSourceRef(recordId = raw.expression, transformation = "jlpt")
                ?: SourceRef(sourceId = SourceIds.YOMICHAN_JLPT_VOCAB)
            val classification = JlptClassification(level = raw.level, source = source)
            attachJlptToVocabulary(raw.expression, classification)
        }
    }

    private fun attachJlptToVocabulary(expression: String, classification: JlptClassification) {
        val match = builder.vocabulary
            .firstOrNull { it.expression == expression }
            ?: builder.vocabulary.firstOrNull { entry ->
                entry.readings.any { it.value == expression }
            }
        if (match != null) {
            // Only attach when the level isn't already provided by the
            // canonical source (Tanos).
            val alreadyCanonical = match.jlpt.any { it.source.isCanonical && it.level == classification.level }
            if (!alreadyCanonical) {
                builder.upsertVocab(match.copy(jlpt = match.jlpt + classification))
            }
        }
    }

    // ===============================================================
    // Frequency
    // ===============================================================

    private fun attachFrequency(
        leedsRecords: List<RawFrequencyRecord>,
        leeds: Map<String, SourceMetadata>
    ) {
        val leedsMeta = leeds[SourceIds.LEEDS_FREQUENCY]
        for (raw in leedsRecords) {
            val source = leedsMeta?.toSourceRef(recordId = raw.rank.toString(), transformation = "frequency", isCanonical = true)
                ?: SourceRef(sourceId = SourceIds.LEEDS_FREQUENCY, isCanonical = true)
            val record = FrequencyRecord(
                value = raw.rank,
                source = source,
                methodology = "Leeds Internet corpus frequency rank"
            )

            // Frequency attaches to vocabulary by expression, then kanji by literal.
            val vocabMatch = builder.vocabulary.firstOrNull { it.expression == raw.item }
            if (vocabMatch != null) {
                builder.upsertVocab(vocabMatch.copy(frequency = vocabMatch.frequency + record))
                continue
            }
            val kanjiMatch = builder.kanjiBy(raw.item)
            if (kanjiMatch != null) {
                builder.mergeIntoKanji(raw.item) { k -> k.copy(frequency = k.frequency + record) }
            }
        }
    }

    // ===============================================================
    // Cross-linking
    // ===============================================================

    private fun linkVocabularyToKanji() {
        val kanjiByLiteral = builder.kanji.associateBy { it.character.literal }
        for (entry in builder.vocabulary.toList()) {
            val linkedIds = entry.expression
                .filter { kanjiByLiteral.containsKey(it.toString()) }
                .map { EntityId("kanji:${it}") }
                .distinct()
            if (linkedIds.isNotEmpty()) {
                builder.upsertVocab(entry.copy(kanjiIds = linkedIds))
            }
        }
        // Back-links: kanji → vocabulary.
        for (entry in builder.vocabulary) {
            for (kanjiId in entry.kanjiIds) {
                val literal = kanjiId.value.removePrefix("kanji:")
                builder.mergeIntoKanji(literal) { k ->
                    k.copy(vocabularyIds = k.vocabularyIds + entry.id)
                }
            }
        }
    }

    private fun classifyCharacter(literal: String): io.kaiteyo.kjd.model.CharacterType {
        val code = literal.firstOrNull()?.code ?: return io.kaiteyo.kjd.model.CharacterType.Other
        return when {
            code in 0x3040..0x309F || code in 0x30A0..0x30FF -> io.kaiteyo.kjd.model.CharacterType.Kana
            code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF -> io.kaiteyo.kjd.model.CharacterType.Kanji
            else -> io.kaiteyo.kjd.model.CharacterType.Other
        }
    }
}
