package ua.syt0r.kanji.desktop.engine.jdata.integration

import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.jdata.engine.KanaSystem
import ua.syt0r.kanji.desktop.engine.jdata.engine.KanjiSystem
import ua.syt0r.kanji.desktop.engine.jdata.engine.KanjiVgGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.KanjiVgSource
import ua.syt0r.kanji.desktop.engine.jdata.engine.NoStrokeGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.RelationshipGraph
import ua.syt0r.kanji.desktop.engine.jdata.engine.StrokeGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.StrokeSystem
import ua.syt0r.kanji.desktop.engine.jdata.engine.VocabSystem
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.profiles.DataPart
import ua.syt0r.kanji.desktop.engine.jdata.profiles.DatabaseProfile
import ua.syt0r.kanji.desktop.engine.jdata.schema.SchemaSql
import ua.syt0r.kanji.desktop.engine.jdata.source.SourceDefinition

// ============================================================
// PLATFORM BUILDER
// The single ingestion path from installed dictionary sources into
// the canonical [PlatformData]. Applies entity resolution (merge per
// character / per identity), profile filtering and relationship
// construction. Deterministic: iteration is sorted, no randomness.
// ============================================================

object PlatformBuilder {

    private const val CURATED_KANA_SOURCE = "kaiteyo-kana-reference"
    private const val CURATED_RADICAL_SOURCE = "kaiteyo-radical-reference"

    fun fromRepository(
        repository: DictionaryRepository,
        profile: DatabaseProfile = DatabaseProfile.Standard,
        generatedAt: String = "",
        geometry: StrokeGeometryProvider = NoStrokeGeometryProvider
    ): PlatformData {
        // 1. Canonical entities (full union before profile filtering).
        val kanji = KanjiSystem.fromRepository(repository)
        val kana = KanaSystem.all()
        val vocab = VocabSystem.fromRepository(repository, kanjiReadingLookup(kanji))
        val radicals = KanjiSystem.radicalsFrom(kanji)
        val components = KanjiSystem.componentsFrom(radicals)
        val geometrySourceId = (geometry as? KanjiVgGeometryProvider)?.sourceId
        val strokeSets = StrokeSystem.fromPlatform(kanji, kana, geometry, geometrySourceId)
        val sources = sourcesFrom(repository)

        // 2. Profile filtering (honest: drop parts the profile excludes).
        val filteredKanji = if (profile.includes(DataPart.Kanji)) kanji else emptyMap()
        val filteredKana = if (profile.includes(DataPart.Kana)) kana else emptyMap()
        val filteredVocab = if (profile.includes(DataPart.Vocabulary)) vocab else emptyMap()
        val filteredRadicals = if (profile.includes(DataPart.Radicals)) radicals else emptyMap()
        val filteredComponents = if (profile.includes(DataPart.Components)) components else emptyMap()
        val filteredStrokes = if (profile.includes(DataPart.Strokes)) strokeSets else emptyMap()

        val strippedVocab = if (profile.includes(DataPart.Jlpt) || profile.includes(DataPart.Frequency) || profile.includes(DataPart.Furigana)) {
            filteredVocab.mapValues { (_, v) ->
                v.copy(
                    jlpt = if (profile.includes(DataPart.Jlpt)) v.jlpt else null,
                    frequencies = if (profile.includes(DataPart.Frequency)) v.frequencies else emptyList(),
                    furigana = if (profile.includes(DataPart.Furigana)) v.furigana else emptyList()
                )
            }
        } else filteredVocab

        val strippedKanji = if (profile.includes(DataPart.Jlpt) || profile.includes(DataPart.Frequency)) {
            filteredKanji.mapValues { (_, k) ->
                k.copy(
                    jlpt = if (profile.includes(DataPart.Jlpt)) k.jlpt else null,
                    frequencyRank = if (profile.includes(DataPart.Frequency)) k.frequencyRank else null
                )
            }
        } else filteredKanji

        // 3. Relationships over the *final* entity set (no dangling edges).
        val provisional = PlatformData(
            schemaVersion = SchemaSql.SchemaVersion,
            generatedAt = generatedAt,
            kanji = strippedKanji,
            kana = filteredKana,
            vocab = strippedVocab,
            radicals = filteredRadicals,
            components = filteredComponents,
            strokeSets = filteredStrokes,
            relations = emptyList(),
            sources = sources
        )
        val relations = RelationshipGraph.build(provisional)

        return PlatformData(
            schemaVersion = SchemaSql.SchemaVersion,
            generatedAt = generatedAt,
            kanji = strippedKanji,
            kana = filteredKana,
            vocab = strippedVocab,
            radicals = filteredRadicals,
            components = filteredComponents,
            strokeSets = filteredStrokes,
            relations = relations,
            sources = sources
        )
    }

    /** Reading lookup for furigana: on/kun with okurigana-dot pre-forms. */
    private fun kanjiReadingLookup(kanji: Map<String, ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry>): (String) -> List<String> = lambda@{ ch ->
        val entry = kanji[StableIds.kanji(ch)] ?: return@lambda emptyList()
        val readings = mutableListOf<String>()
        entry.onReadings.forEach { readings.addReadingVariants(it) }
        entry.kunReadings.forEach { readings.addReadingVariants(it) }
        readings.distinct()
    }

    private fun MutableList<String>.addReadingVariants(reading: String) {
        add(reading)
        reading.split('.').firstOrNull()?.takeIf { it != reading && it.isNotBlank() }?.let { add(it) }
        reading.replace(".", "").takeIf { it != reading }?.let { add(it) }
    }

    /** Source definitions: installed dictionaries + the curated reference tables. */
    fun sourcesFrom(repository: DictionaryRepository): Map<String, SourceDefinition> {
        val sources = repository.installedDictionaries().associate { dict ->
            dict.id to SourceDefinition(
                id = dict.id,
                name = dict.name,
                version = dict.revision,
                homepage = dict.tags.firstOrNull { it.startsWith("homepage:") }?.removePrefix("homepage:") ?: "",
                licenseName = dict.tags.firstOrNull { it.startsWith("license:") }?.removePrefix("license:") ?: "Not declared by source",
                licenseUrl = dict.tags.firstOrNull { it.startsWith("license-url:") }?.removePrefix("license-url:") ?: "",
                retrievalDate = "",
                format = dict.format.name,
                priority = dict.priority,
                tags = dict.tags
            )
        }.toMutableMap()

        sources[KanjiVgSource.SourceId] = KanjiVgSource.Definition
        sources[CURATED_KANA_SOURCE] = SourceDefinition(
            id = CURATED_KANA_SOURCE,
            name = "Kaiteyo kana reference table",
            version = "1",
            licenseName = "Reference facts; no third-party license applies",
            format = "curated",
            priority = 1_000,
            tags = listOf("builtin", "kana")
        )
        sources[CURATED_RADICAL_SOURCE] = SourceDefinition(
            id = CURATED_RADICAL_SOURCE,
            name = "Kaiteyo radical meanings reference",
            version = "1",
            licenseName = "Reference facts; no third-party license applies",
            format = "curated",
            priority = 1_001,
            tags = listOf("builtin", "radical")
        )
        return sources
    }
}
