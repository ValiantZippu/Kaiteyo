package io.kaiteyo.kjd.validate

import io.kaiteyo.kjd.model.CanonicalDatabase
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.EntityType
import kotlinx.serialization.Serializable

/**
 * A single validation finding. [severity] Fatal findings fail the build;
 * Warnings are reported in the quality report.
 */
data class ValidationFinding(
    val severity: Severity,
    val entityType: EntityType?,
    val entityId: String?,
    val message: String
)

enum class Severity { Fatal, Warning }

/**
 * Validates a canonical database before generation. The validator checks the
 * integrity rules the platform promises:
 *
 *   - no duplicate IDs
 *   - no duplicate entries
 *   - valid Unicode
 *   - required fields present
 *   - no invalid references / orphan relationships
 *   - consistent stroke counts / indexes
 *   - no impossible foreign keys
 *
 * The generator fails loudly when [Severity.Fatal] findings exist.
 */
class DatabaseValidator {

    fun validate(database: CanonicalDatabase): List<ValidationFinding> {
        val findings = mutableListOf<ValidationFinding>()

        validateIds(database, findings)
        validateUnicode(database, findings)
        validateReferences(database, findings)
        validateStrokes(database, findings)
        validateVocab(database, findings)

        return findings
    }

    private fun validateIds(database: CanonicalDatabase, findings: MutableList<ValidationFinding>) {
        val seen = HashSet<String>()

        fun check(id: EntityId, type: EntityType) {
            if (id.value.isBlank()) {
                findings.add(ValidationFinding(Severity.Fatal, type, null, "Blank entity id"))
                return
            }
            if (!seen.add(id.value)) {
                findings.add(ValidationFinding(Severity.Fatal, type, id.value, "Duplicate entity id"))
            }
        }

        database.kanji.forEach { check(it.id, EntityType.Kanji) }
        database.kana.forEach { check(it.id, EntityType.Kana) }
        database.radicals.forEach { check(it.id, EntityType.Radical) }
        database.vocabulary.forEach { check(it.id, EntityType.Vocabulary) }
        database.senses.forEach { check(it.id, EntityType.Sense) }
    }

    private fun validateUnicode(database: CanonicalDatabase, findings: MutableList<ValidationFinding>) {
        for (kanji in database.kanji) {
            val literal = kanji.character.literal
            if (literal.isEmpty()) {
                findings.add(ValidationFinding(Severity.Fatal, EntityType.Kanji, kanji.id.value, "Empty literal"))
            } else if (literal.codePointCount(0, literal.length) != 1) {
                findings.add(ValidationFinding(Severity.Fatal, EntityType.Kanji, kanji.id.value, "Literal is not exactly one codepoint: '$literal'"))
            }
        }
        for (entry in database.vocabulary) {
            if (entry.expression.isBlank()) {
                findings.add(ValidationFinding(Severity.Fatal, EntityType.Vocabulary, entry.id.value, "Blank expression"))
            }
            if (entry.readings.isEmpty()) {
                findings.add(ValidationFinding(Severity.Warning, EntityType.Vocabulary, entry.id.value, "No readings"))
            }
        }
    }

    private fun validateReferences(database: CanonicalDatabase, findings: MutableList<ValidationFinding>) {
        val kanjiIds = database.kanji.map { it.id.value }.toSet()
        val vocabIds = database.vocabulary.map { it.id.value }.toSet()
        val senseIds = database.senses.map { it.id.value }.toSet()

        for (kanji in database.kanji) {
            kanji.vocabularyIds.forEach { vocabId ->
                if (vocabId.value !in vocabIds) {
                    findings.add(ValidationFinding(Severity.Fatal, EntityType.Kanji, kanji.id.value, "Orphan vocabulary link: ${vocabId.value}"))
                }
            }
        }
        for (entry in database.vocabulary) {
            entry.kanjiIds.forEach { kanjiId ->
                if (kanjiId.value !in kanjiIds) {
                    findings.add(ValidationFinding(Severity.Fatal, EntityType.Vocabulary, entry.id.value, "Orphan kanji link: ${kanjiId.value}"))
                }
            }
            entry.senses.forEach { sense ->
                if (sense.id.value !in senseIds) {
                    findings.add(ValidationFinding(Severity.Fatal, EntityType.Vocabulary, entry.id.value, "Sense not registered: ${sense.id.value}"))
                }
            }
        }
    }

    private fun validateStrokes(database: CanonicalDatabase, findings: MutableList<ValidationFinding>) {
        for (kanji in database.kanji) {
            val strokes = kanji.strokes
            if (strokes.isEmpty()) continue
            val expected = kanji.strokeCount
            if (expected != null && strokes.size != expected) {
                findings.add(
                    ValidationFinding(
                        Severity.Warning, EntityType.Kanji, kanji.id.value,
                        "Stroke count mismatch: declared $expected, drawn ${strokes.size}"
                    )
                )
            }
            val indexes = strokes.map { it.index }
            if (indexes.distinct().size != indexes.size) {
                findings.add(ValidationFinding(Severity.Fatal, EntityType.Kanji, kanji.id.value, "Duplicate stroke indexes"))
            }
            if (indexes.any { it < 1 }) {
                findings.add(ValidationFinding(Severity.Fatal, EntityType.Kanji, kanji.id.value, "Invalid stroke index (<1)"))
            }
            if (strokes.any { it.path.isBlank() }) {
                findings.add(ValidationFinding(Severity.Warning, EntityType.Kanji, kanji.id.value, "Blank stroke path"))
            }
        }
    }

    private fun validateVocab(database: CanonicalDatabase, findings: MutableList<ValidationFinding>) {
        for (entry in database.vocabulary) {
            val duplicateReadings = entry.readings.groupBy { it.value }
                .filter { it.value.size > 1 }
            if (duplicateReadings.isNotEmpty()) {
                findings.add(
                    ValidationFinding(
                        Severity.Warning, EntityType.Vocabulary, entry.id.value,
                        "Duplicate readings: ${duplicateReadings.keys.joinToString()}"
                    )
                )
            }
            entry.senses.forEach { sense ->
                if (sense.glosses.isEmpty() && sense.partsOfSpeech.isEmpty()) {
                    findings.add(ValidationFinding(Severity.Warning, EntityType.Sense, sense.id.value, "Sense has no glosses"))
                }
            }
        }
    }
}

/**
 * Produces the data quality report for a generated release: coverage of every
 * optional subsystem, unresolved relationships, source versions and warnings.
 */
@Serializable
data class QualityReport(
    val kanjiCount: Int,
    val vocabularyCount: Int,
    val senseCount: Int,
    val radicalCount: Int,
    val strokeDataCoverage: Double,
    val jlptCoverage: Double,
    val frequencyCoverage: Double,
    val furiganaCoverage: Double,
    val unresolvedVocabularyLinks: Int,
    val warnings: List<String>,
    val sourceVersions: Map<String, String>
) {
    fun toMarkdown(): String = buildString {
        appendLine("# KJD Data Quality Report")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|---|---|")
        appendLine("| Kanji | $kanjiCount |")
        appendLine("| Vocabulary | $vocabularyCount |")
        appendLine("| Senses | $senseCount |")
        appendLine("| Radicals | $radicalCount |")
        appendLine("| Stroke data coverage | ${(strokeDataCoverage * 100).round()}% |")
        appendLine("| JLPT coverage | ${(jlptCoverage * 100).round()}% |")
        appendLine("| Frequency coverage | ${(frequencyCoverage * 100).round()}% |")
        appendLine("| Furigana coverage | ${(furiganaCoverage * 100).round()}% |")
        appendLine()
        if (warnings.isNotEmpty()) {
            appendLine("## Warnings")
            warnings.forEach { appendLine("- $it") }
            appendLine()
        }
        appendLine("## Sources")
        sourceVersions.forEach { (id, version) -> appendLine("- $id: $version") }
    }

    private fun Double.round(): Int = kotlin.math.round(this * 100).toInt()
}

class QualityReporter {
    fun report(database: CanonicalDatabase, findings: List<ValidationFinding>, sourceVersions: Map<String, String>): QualityReport {
        val kanjiCount = database.kanji.size
        val vocabCount = database.vocabulary.size
        val warnings = findings.filter { it.severity == Severity.Warning }.map { it.message }

        fun coverage(has: (Int) -> Boolean): Double {
            val total = kanjiCount.coerceAtLeast(1)
            val covered = database.kanji.count { has(it.strokeCount ?: 0) }
            return covered.toDouble() / total
        }

        val strokeCoverage = if (kanjiCount == 0) 0.0
        else database.kanji.count { it.strokes.isNotEmpty() }.toDouble() / kanjiCount
        val jlptCoverage = if (kanjiCount == 0) 0.0
        else database.kanji.count { it.jlpt.isNotEmpty() }.toDouble() / kanjiCount
        val freqCoverage = if (kanjiCount == 0) 0.0
        else database.kanji.count { it.frequency.isNotEmpty() }.toDouble() / kanjiCount
        val furiganaCoverage = if (vocabCount == 0) 0.0
        else database.vocabulary.count { it.furigana.isNotEmpty() }.toDouble() / vocabCount

        val unresolvedLinks = database.vocabulary.count { entry ->
            entry.expression.any { ch -> ch.code in 0x4E00..0x9FFF } && entry.kanjiIds.isEmpty()
        }

        return QualityReport(
            kanjiCount = kanjiCount,
            vocabularyCount = vocabCount,
            senseCount = database.senses.size,
            radicalCount = database.radicals.size,
            strokeDataCoverage = strokeCoverage,
            jlptCoverage = jlptCoverage,
            frequencyCoverage = freqCoverage,
            furiganaCoverage = furiganaCoverage,
            unresolvedVocabularyLinks = unresolvedLinks,
            warnings = warnings,
            sourceVersions = sourceVersions
        )
    }
}
