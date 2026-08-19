package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.core.app_data.data.FuriganaString

// ============================================================
// KAITEYO KNOWLEDGE CORE — DOMAIN MODELS
// ------------------------------------------------------------
// First-class entities for the Japanese knowledge system:
// kanji, radicals, components, words, sentences and grammar.
//
// These models are derived from (never a substitute for) the
// bundled authoritative dictionary (JMdict / KANJIDIC-derived
// tables). Every field maps to a real database column or is
// explicitly absent — no fabricated linguistic information.
//
// The five questions the product must answer for any entity:
//   1. WHAT IS THIS?        (meaning, readings, classification)
//   2. HOW IS IT BUILT?     (radical, components, decomposition)
//   3. HOW IS IT USED?      (words, compounds, sentences)
//   4. WHAT IS IT CONNECTED TO? (relationships, variants)
//   5. HOW SHOULD I LEARN IT?   (study state, examples)
// ============================================================

/** Typed kanji classification. DB stores `n5`, `g1`, `w42` (see [fromDbValue]). */
@Serializable
sealed interface KanjiTag {

    /** The raw database value, e.g. "n5". */
    val dbValue: String

    /** Short display label, e.g. "N5", "Grade 2", "WaniKani 8". */
    val label: String

    /** Stable sort key so JLPT/grade ordering is natural. */
    val sortKey: Int

    @Serializable
    data class Jlpt(val level: Int) : KanjiTag {
        override val dbValue: String = "n$level"
        override val label: String = "N$level"
        override val sortKey: Int = level
    }

    @Serializable
    data class Grade(val number: Int) : KanjiTag {
        override val dbValue: String = "g$number"
        override val label: String = "Grade $number"
        override val sortKey: Int = number
    }

    @Serializable
    data class Wanikani(val level: Int) : KanjiTag {
        override val dbValue: String = "w$level"
        override val label: String = "WaniKani $level"
        override val sortKey: Int = level
    }

    companion object {
        /** Parses a raw classification value defensively. Unknown forms are dropped. */
        fun fromDbValue(value: String): KanjiTag? {
            val prefix = value.firstOrNull() ?: return null
            val number = value.drop(1).toIntOrNull() ?: return null
            return when (prefix) {
                'n' -> if (number in 1..5) Jlpt(number) else null
                'g' -> if (number in 1..10) Grade(number) else null
                'w' -> if (number in 1..60) Wanikani(number) else null
                else -> null
            }
        }

        fun fromDbValues(values: List<String>): List<KanjiTag> =
            values.mapNotNull { fromDbValue(it) }
                .sortedBy { tag -> tag.sortKey }
    }
}

/**
 * Kanji knowledge classification derived from the school-grade tags.
 *
 * KANJIDIC-style grade semantics: grades 1–6 are the kyōiku grades, grade 8
 * is the remaining jōyō set, grade 9 is jinmeiyō (name use), grade 10 is the
 * catch-all for non-jōyō/variant characters. This derivation is a convenience
 * view over the real grade tag — the raw tag always stays authoritative.
 */
enum class KanjiSetKind(val label: String) {
    Kyōiku("Kyōiku"),
    Joyo("Jōyō"),
    Jinmeiyo("Jinmeiyō"),
    Supplementary("Supplementary"),
    Unknown("Unclassified")
}

fun KanjiTag.Grade.setKind(): KanjiSetKind = when (number) {
    in 1..6 -> KanjiSetKind.Kyōiku
    8 -> KanjiSetKind.Joyo
    9 -> KanjiSetKind.Jinmeiyo
    else -> KanjiSetKind.Supplementary
}

/** A fully-loaded kanji entry — answers WHAT / HOW IT IS BUILT. */
@Serializable
data class KanjiKnowledge(
    val character: String,
    val meanings: List<String> = emptyList(),
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    /** KANJIDIC frequency rank (lower = more frequent). Null when the dataset has no rank. */
    val frequencyRank: Int? = null,
    val classifications: List<KanjiTag> = emptyList(),
    val strokeCount: Int? = null,
    val variantFamily: String? = null,
    val strokePaths: List<String> = emptyList()
) {
    /** Orientation keyword: the first meaning, never a full definition. */
    val keyword: String?
        get() = meanings.firstOrNull()

    val isJoyo: Boolean
        get() = classifications.any { it is KanjiTag.Grade && it.setKind() != KanjiSetKind.Supplementary }

    val jlpt: KanjiTag.Jlpt?
        get() = classifications.filterIsInstance<KanjiTag.Jlpt>().minByOrNull { it.level }
}

/** A radical as a first-class entity (not a decorative tag). */
@Serializable
data class RadicalKnowledge(
    val radical: String,
    val strokeCount: Int
)

/**
 * A radical enriched with the number of kanji that use it — the explorer's
 * grid sort/filter metadata (real counts, from real queries).
 */
@Serializable
data class RadicalStats(
    val radical: String,
    val strokeCount: Int,
    val kanjiCount: Int
)

/** A radical found inside a kanji, with its position in the stroke sequence. */
@Serializable
data class RadicalInKanji(
    val radical: String,
    val startStroke: Int,
    val strokesCount: Int
)

/**
 * A component is a structural part of a kanji. In the bundled dataset the
 * component graph is radical-derived (KANJIDIC radical decomposition), so
 * every component carries the radical it maps to plus a source label. A
 * dedicated decomposition dataset can extend this later without touching
 * the model.
 */
@Serializable
data class ComponentKnowledge(
    val component: String,
    /** The radical this component corresponds to, when one exists. */
    val radicalOf: String? = null,
    val strokesCount: Int = 0,
    val source: ComponentSource = ComponentSource.RadicalDecomposition
)

enum class ComponentSource {
    /** Derived from the bundled KANJIDIC radical decomposition. */
    RadicalDecomposition
}

/** A word (JMdict-derived) — first-class vocabulary entity. */
@Serializable
data class WordKnowledge(
    val id: Long,
    val kanjiReading: String? = null,
    val kanaReading: String = "",
    val furigana: FuriganaString? = null,
    val glossary: List<String> = emptyList(),
    val partOfSpeech: List<String> = emptyList()
) {
    val displaySpelling: String
        get() = kanjiReading ?: kanaReading

    fun combinedGlossary(): String = glossary.joinToString("; ")
}

/** A corpus sentence (Tatoeba-derived) with its translation and furigana. */
@Serializable
data class SentenceKnowledge(
    val text: String,
    val translation: String,
    val furigana: FuriganaString,
    /** Where this sentence came from — never fabricated (spec §27). */
    val provenance: ContentProvenance = ContentProvenance()
) {
    val isEmpty: Boolean get() = text.isBlank()
}

// ============================================================
// CONTENT PROVENANCE (spec §12, §27)
// ------------------------------------------------------------
// Every piece of learner-facing content can be tagged with where
// it came from and how much to trust it. AUTHORITATIVE data is
// the bundled dictionary/corpus; AI/USER/COMMUNITY content is
// always labelled as such — AI content never silently replaces
// authoritative dictionary data. Confidence is a display
// hint, not a substitute for the source label.
//
// The source-type and confidence enums live with the learning-
// content model (LearningContent.kt) so sentences and learning
// content share one provenance vocabulary.
// ============================================================

/** Provenance metadata for any learner-facing content. */
@Serializable
data class ContentProvenance(
    val sourceType: ContentSourceType = ContentSourceType.Authoritative,
    val sourceLabel: String = "",
    val confidence: ContentConfidence = ContentConfidence.High
)

/** A recognized grammar pattern occurrence inside a sentence. */
@Serializable
data class GrammarMatch(
    val patternId: String,
    val matchedText: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * A grammar pattern from the built-in reference catalog. This is curated
 * starter data shipped with the app — it is NOT claimed to be an
 * authoritative corpus. Extending it with a licensed grammar dataset is a
 * documented roadmap item.
 */
@Serializable
data class GrammarPattern(
    val id: String,
    /** The pattern as written, e.g. "〜てみる". */
    val pattern: String,
    val meaning: String,
    val formation: String? = null,
    val register: String? = null,
    val jlpt: Int? = null,
    val keywords: List<String> = emptyList()
) {
    /** Matchable kana forms (the 〜 marker is stripped). */
    val kanaForms: List<String>
        get() = pattern.replace("〜", "")
            .replace("…", "")
            .let { forms ->
                if (forms.isEmpty()) emptyList()
                else listOf(forms)
            }
}
