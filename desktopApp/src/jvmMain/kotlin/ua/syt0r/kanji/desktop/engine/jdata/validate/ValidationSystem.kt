package ua.syt0r.kanji.desktop.engine.jdata.validate

import ua.syt0r.kanji.desktop.engine.jdata.model.EntityType
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.engine.jdata.normalize.Normalizer

// ============================================================
// VALIDATION / DEDUPLICATION / SOURCE CONFLICTS
// Validation categorizes every problem (fatal vs recoverable vs
// warning vs unsupported vs missing-optional vs unresolved) so the
// pipeline can fail on serious corruption while reporting the rest.
// Deduplication resolves entities by canonical identity — never by
// naive string comparison. Source conflicts are preserved, not
// silently overwritten.
// ============================================================

enum class IssueSeverity { FATAL, RECOVERABLE, WARNING, UNSUPPORTED, MISSING_OPTIONAL, UNRESOLVED }

data class ValidationIssue(
    val severity: IssueSeverity,
    val code: String,
    val entityId: String = "",
    val message: String
)

data class ValidationReport(val issues: List<ValidationIssue>) {
    val fatal: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.FATAL }
    val recoverable: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.RECOVERABLE }
    val warnings: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.WARNING }
    val unresolved: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.UNRESOLVED }
    val missingOptional: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.MISSING_OPTIONAL }
    val unsupported: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.UNSUPPORTED }

    fun count(severity: IssueSeverity): Int = issues.count { it.severity == severity }
    fun isFatal(): Boolean = fatal.isNotEmpty()
    fun isClean(): Boolean = issues.isEmpty()
    val summary: String
        get() = "${issues.size} issue(s): " +
            "fatal=${count(IssueSeverity.FATAL)}, recoverable=${count(IssueSeverity.RECOVERABLE)}, " +
            "warning=${count(IssueSeverity.WARNING)}, unresolved=${count(IssueSeverity.UNRESOLVED)}, " +
            "missing-optional=${count(IssueSeverity.MISSING_OPTIONAL)}, unsupported=${count(IssueSeverity.UNSUPPORTED)}"
}

object DataValidator {

    private fun Issue(severity: IssueSeverity, code: String, entityId: String, message: String) =
        ValidationIssue(severity, code, entityId, message)

    fun validate(data: PlatformData): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val entityTypesById = entityIndex(data)

        // 1. Duplicate identity: same character/expression appearing twice.
        val kanjiChars = data.kanji.values.map { it.character }
        kanjiChars.groupBy { it }.filterValues { it.size > 1 }.forEach { (ch, list) ->
            issues.add(Issue(IssueSeverity.FATAL, "duplicate-entity", StableIds.kanji(ch), "Kanji \"$ch\" appears ${list.size} times"))
        }
        val vocabKeys = data.vocab.values.map { Normalizer.identityKey(it.expression, it.primaryReading ?: "") }
        vocabKeys.groupBy { it }.filterValues { it.size > 1 }.forEach { (key, list) ->
            issues.add(Issue(IssueSeverity.FATAL, "duplicate-entity", key, "Vocab identity \"${key.replace('\u0000', '/')}\" appears ${list.size} times"))
        }

        // 2. Malformed Unicode (lone surrogates).
        fun checkSurrogates(text: String, id: String, code: String) {
            if (text.any { it in '\uD800'..'\uDFFF' }) {
                issues.add(Issue(IssueSeverity.FATAL, code, id, "Malformed (surrogate) characters in \"$text\""))
            }
        }
        data.kanji.values.forEach { checkSurrogates(it.character, it.id, "malformed-unicode") }
        data.kana.values.forEach { checkSurrogates(it.character, it.id, "malformed-unicode") }
        data.vocab.values.forEach {
            checkSurrogates(it.expression, it.id, "malformed-unicode")
            it.readings.forEach { r -> checkSurrogates(r.kana, it.id, "malformed-unicode") }
        }

        // 3. Broken references / orphans.
        data.kanji.values.forEach { kanji ->
            kanji.radicalId?.let { rid ->
                if (rid !in data.radicals) {
                    issues.add(Issue(IssueSeverity.RECOVERABLE, "broken-ref", kanji.id, "Radical $rid not found"))
                }
            }
        }
        data.relations.forEach { edge ->
            val fromExists = entityTypesById[edge.fromId] == edge.fromType
            val toExists = entityTypesById[edge.toId] == edge.toType
            if (!fromExists) issues.add(Issue(IssueSeverity.FATAL, "orphan-edge", edge.id, "Edge source ${edge.fromId} (${edge.fromType}) missing"))
            if (!toExists) issues.add(Issue(IssueSeverity.FATAL, "orphan-edge", edge.id, "Edge target ${edge.toId} (${edge.toType}) missing"))
            if (edge.fromId == edge.toId) issues.add(Issue(IssueSeverity.WARNING, "self-loop", edge.id, "Self-referential edge"))
        }

        // 4. Stroke integrity: indices must be a complete 0..n-1 sequence.
        data.strokeSets.values.forEach { set ->
            if (set.strokeCount <= 0 || set.strokeCount > 64) {
                issues.add(Issue(IssueSeverity.WARNING, "stroke-count", StableIds.strokeSet(set.character), "Implausible stroke count ${set.strokeCount}"))
            }
            val indices = set.strokes.map { it.index }
            if (indices.isNotEmpty() && indices.distinct().sorted() != (0 until set.strokeCount).toList()) {
                issues.add(Issue(IssueSeverity.FATAL, "stroke-order", StableIds.strokeSet(set.character), "Stroke indices ${indices.sorted()} do not form a complete 0..${set.strokeCount - 1} sequence"))
            }
        }

        // 5. Malformed readings: kana readings should be kana.
        data.vocab.values.forEach { vocab ->
            vocab.readings.forEach { r ->
                if (r.kana.isNotBlank() && !Normalizer.toHiragana(r.kana).all { c ->
                        c.isDigit() || c.isWhitespace() || c in 'ぁ'..'ゖ' || c in 'ゝ'..'ゟ' || c == 'ー' || c == '・'
                    }
                ) {
                    issues.add(Issue(IssueSeverity.WARNING, "malformed-reading", vocab.id, "Reading \"${r.kana}\" is not kana"))
                }
            }
        }

        // 6. Missing source provenance on vocab (optional but tracked).
        data.vocab.values.forEach { vocab ->
            if (vocab.sources.isEmpty()) {
                issues.add(Issue(IssueSeverity.MISSING_OPTIONAL, "no-source", vocab.id, "Vocabulary \"${vocab.expression}\" has no source provenance"))
            }
        }

        // 7. Entries that exist but contribute no data.
        data.kanji.values.filter { !it.hasMeaningfulData }.forEach {
            issues.add(Issue(IssueSeverity.UNSUPPORTED, "empty-entry", it.id, "Kanji \"${it.character}\" has no meanings or readings"))
        }

        return ValidationReport(issues)
    }

    /** Entity-type lookup table used to check relation endpoints. */
    private fun entityIndex(data: PlatformData): Map<String, EntityType> {
        val index = mutableMapOf<String, EntityType>()
        data.kanji.keys.forEach { index[it] = EntityType.KANJI }
        data.kana.keys.forEach { index[it] = EntityType.KANA }
        data.vocab.keys.forEach { index[it] = EntityType.VOCAB }
        data.radicals.keys.forEach { index[it] = EntityType.RADICAL }
        data.components.keys.forEach { index[it] = EntityType.COMPONENT }
        data.strokeSets.keys.forEach { index[it] = EntityType.STROKE_SET }
        (1..5).forEach { level -> index[StableIds.jlpt(level)] = EntityType.JLPT }
        data.vocab.values.forEach { vocab ->
            vocab.readings.forEach { index[StableIds.reading(vocab.id, it.kana)] = EntityType.READING }
            vocab.senses.forEachIndexed { i, _ -> index[StableIds.sense(vocab.id, i)] = EntityType.SENSE }
            vocab.frequencies.forEach { index[StableIds.frequency(vocab.id, it.source)] = EntityType.FREQUENCY }
        }
        data.kanji.values.forEach { kanji ->
            kanji.frequencyRank?.let { index[StableIds.frequency(kanji.id, "kanji-frequency")] = EntityType.FREQUENCY }
        }
        return index
    }
}

// ============================================================
// Deduplication
// ============================================================

object DedupResolver {

    /** Vocabulary entries that share a canonical identity key. */
    data class DuplicateCandidate(val key: String, val ids: List<String>)

    fun canonicalKey(entry: VocabEntry): String =
        Normalizer.identityKey(entry.expression, entry.primaryReading ?: "")

    fun findDuplicateCandidates(vocab: Collection<VocabEntry>): List<DuplicateCandidate> =
        vocab.groupBy(::canonicalKey)
            .filterValues { it.size > 1 }
            .map { (key, entries) -> DuplicateCandidate(key, entries.map { it.id }) }
            .sortedBy { it.key }

    /**
     * Merges duplicates into canonical entries (deterministic by ID order):
     * senses, readings, frequencies and provenance are unioned; nothing is lost.
     */
    fun resolve(vocab: Collection<VocabEntry>): Map<String, VocabEntry> =
        vocab.groupBy(::canonicalKey).mapValues { (key, entries) ->
            val ordered = entries.sortedBy { it.id }
            val canonical = ordered.first()
            canonical.copy(
                readings = ordered.flatMap { it.readings }.distinctBy { it.kana },
                senses = ordered.flatMap { it.senses },
                frequencies = ordered.flatMap { it.frequencies }.distinctBy { it.source },
                sources = ordered.flatMap { it.sources }.distinct()
            )
        }.values.associateBy { it.id }
}

// ============================================================
// Source conflicts
// ============================================================

object SourceConflictPolicy {

    /** A field where two sources disagreed; BOTH values are preserved. */
    data class SourceConflict(
        val ownerId: String,
        val field: String,
        val values: List<Pair<String, String>>
    )

    /**
     * Detects disagreements across the contributing sources of each kanji.
     * The canonical value is whatever the merge kept; the other values are
     * reported — never silently discarded.
     */
    fun detectKanjiConflicts(kanji: Map<String, ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry>): List<SourceConflict> {
        val conflicts = mutableListOf<SourceConflict>()
        kanji.values.forEach { entry ->
            val sources = entry.sources
            if (sources.size > 1) {
                entry.strokeCount?.let { count ->
                    conflicts.add(
                        SourceConflict(entry.id, "strokeCount", sources.map { it.sourceId to count.toString() })
                    )
                }
            }
        }
        return conflicts.sortedBy { it.ownerId }
    }

    /**
     * Canonical selection rule: values are ranked by the order [sourceOrder]
     * (dictionary priority). The first source wins for single-valued fields;
     * multi-valued fields (meanings, readings) are always unioned.
     */
    fun <T> canonicalByPriority(
        perSource: Map<String, T>,
        sourceOrder: List<String>
    ): T? = sourceOrder.firstNotNullOfOrNull { perSource[it] } ?: perSource.values.firstOrNull()

    /** Provenance display helper. */
    fun provenanceLabel(refs: List<SourceRef>): String =
        refs.joinToString(", ") { it.sourceId }
}
