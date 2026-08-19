package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable

// ============================================================
// KEYWORD SYSTEM + DATASET PROVENANCE (spec §13, §46, KT-DATA-002/003)
// ------------------------------------------------------------
// Two metadata systems that keep the knowledge core honest:
//
// 1. KEYWORDS — a keyword is an orientation tool, never a definition.
//    A kanji may have a primary keyword (first meaning), alternate
//    keywords, a learner-friendly meaning, a literal meaning and a
//    component keyword. Keyword lookup is for finding a kanji fast;
//    the dictionary entry remains the source of truth.
//
// 2. DATASET PROVENANCE — every bundled dataset (KANJIDIC, JMdict,
//    Tatoeba, radicals, frequency) is described by source, version,
//    license, record counts and import metadata. UIs can surface
//    "where does this data come from?" without fabricating sources,
//    and the import pipeline records the same shape when it ingests.
// ============================================================

/**
 * Keyword metadata for one kanji. All fields are nullable — an
 * unknown keyword is represented as unavailable, never guessed.
 * [primary] mirrors `KanjiKnowledge.keyword` (first meaning) and is
 * the only field guaranteed non-null when a meaning exists.
 */
@Serializable
data class KanjiKeywordSet(
    val character: String,
    /** Orientation keyword: the first meaning (never a definition). */
    val primary: String? = null,
    /** Alternate learner-friendly names for the character. */
    val alternates: List<String> = emptyList(),
    /** Simplified, beginner-oriented gloss. */
    val learnerMeaning: String? = null,
    /** Literal/etymological meaning where known. */
    val literalMeaning: String? = null,
    /** Keyword of the character's radical/component, for mnemonic aid. */
    val componentKeyword: String? = null
) {
    val allKeywords: List<String>
        get() = buildList {
            primary?.let(::add)
            alternates.forEach(::add)
            learnerMeaning?.let(::add)
            literalMeaning?.let(::add)
            componentKeyword?.let(::add)
        }

    fun matches(query: String): Boolean =
        allKeywords.any { it.contains(query, ignoreCase = true) }
}

/**
 * Central keyword registry. Populated by the data layer when richer
 * keyword data exists (alternates, learner meanings); when only the
 * dictionary's first meaning exists, [forCharacter] falls back to
 * constructing a single-keyword set from it — real data, never fake.
 */
class KeywordRegistry {
    private val sets = mutableMapOf<String, KanjiKeywordSet>()

    fun register(set: KanjiKeywordSet) {
        sets[set.character] = set
    }

    fun registerAll(list: List<KanjiKeywordSet>) {
        list.forEach(::register)
    }

    /** The keyword set for [character], or null when unknown. */
    fun forCharacter(character: String): KanjiKeywordSet? = sets[character]

    /** First meaning fallback for a character known to the dictionary. */
    fun fallbackFor(character: String, firstMeaning: String?): KanjiKeywordSet? =
        if (firstMeaning == null) null
        else KanjiKeywordSet(character = character, primary = firstMeaning)
}

/**
 * Provenance of one bundled or imported dataset. Every field is
 * honest metadata — a dataset without a recorded license says so
 * (`license == null`), it never inherits one.
 */
@Serializable
data class DatasetProvenance(
    /** Canonical dataset id, e.g. "kanjidic2". */
    val id: String,
    /** Human name, e.g. "KANJIDIC2". */
    val name: String,
    /** Upstream version string when known. */
    val version: String? = null,
    /** SPDX-style license identifier when known. */
    val license: String? = null,
    /** URL of the upstream source. */
    val sourceUrl: String? = null,
    /** Record counts per entity kind (kanji, words, sentences, ...). */
    val recordCounts: Map<String, Long> = emptyMap(),
    /** Import date (ISO-8601) of the bundled copy, when recorded. */
    val importedOn: String? = null,
    /** SHA-256 of the ingested artifact, when verified at import. */
    val checksum: String? = null
)

/**
 * Registry of dataset provenance. The data layer registers every
 * bundled dataset here; UIs and the dictionary pages can answer
 * "what is this data and where does it come from?" (spec §46) from
 * one place.
 */
class DatasetProvenanceRegistry {
    private val entries = mutableMapOf<String, DatasetProvenance>()

    fun register(provenance: DatasetProvenance) {
        entries[provenance.id] = provenance
    }

    fun registerAll(list: List<DatasetProvenance>) {
        list.forEach(::register)
    }

    fun forId(id: String): DatasetProvenance? = entries[id]

    val all: List<DatasetProvenance>
        get() = entries.values.toList()

    val isEmpty: Boolean get() = entries.isEmpty()
}
