package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.serialization.encodeToString
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.StudyDaySummary

// ============================================
// IMPORT PIPELINE
// Automatic validation, duplicate detection,
// conflict resolution, preview before import.
// ============================================

enum class ConflictPolicy { KeepExisting, OverwriteExisting, Skip, KeepNewest }

enum class DuplicatePolicy { Skip, Replace, CreateCopy }

enum class ValidationSeverity { Info, Warning, Error }

data class ValidationIssue(
    val severity: ValidationSeverity,
    val line: Int? = null,
    val message: String
)

data class ImportPreview(
    val format: TransferFormat,
    val total: Int,
    val valid: Int,
    val invalid: Int,
    val duplicates: Int,
    val issues: List<ValidationIssue>,
    val cards: List<DesktopCard>
)

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val replaced: Int,
    val createdCopies: Int,
    val issues: List<ValidationIssue>,
    val combined: List<DesktopCard> = emptyList()
)

class ImportPipeline {

    fun preview(text: String, format: TransferFormat): Result<ImportPreview> = runCatching {
        val parsed = when (format) {
            TransferFormat.Json -> Codecs.fromJson(text)
            TransferFormat.Csv -> Codecs.fromCsv(text)
            TransferFormat.Tsv -> Codecs.fromTsv(text)
            TransferFormat.Txt -> Codecs.fromTxt(text)
        }
        val issues = mutableListOf<ValidationIssue>()
        val valid = parsed.filter { card ->
            val cardIssues = validateCard(card)
            issues.addAll(cardIssues)
            cardIssues.none { it.severity == ValidationSeverity.Error }
        }
        val invalid = parsed.size - valid.size
        ImportPreview(
            format = format,
            total = parsed.size,
            valid = valid.size,
            invalid = invalid,
            duplicates = findDuplicates(valid).size,
            issues = issues,
            cards = valid
        )
    }

    /** Build a preview from already-parsed cards (e.g. read from an APKG package). */
    fun previewCards(cards: List<DesktopCard>): ImportPreview {
        val issues = mutableListOf<ValidationIssue>()
        val valid = cards.filter { card ->
            val cardIssues = validateCard(card)
            issues.addAll(cardIssues)
            cardIssues.none { it.severity == ValidationSeverity.Error }
        }
        val invalid = cards.size - valid.size
        return ImportPreview(
            format = TransferFormat.Json,
            total = cards.size,
            valid = valid.size,
            invalid = invalid,
            duplicates = findDuplicates(valid).size,
            issues = issues,
            cards = valid
        )
    }

    fun validateCard(card: DesktopCard): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (card.character.isBlank()) {
            issues.add(ValidationIssue(ValidationSeverity.Error, null, "Card has no character"))
        }
        if (card.character.length > 8) {
            issues.add(ValidationIssue(ValidationSeverity.Warning, null, "Unusual character length: ${card.character}"))
        }
        if (card.meaning.isBlank()) {
            issues.add(ValidationIssue(ValidationSeverity.Warning, null, "Card '${card.character}' has no meaning"))
        }
        if (card.intervalDays < 0 || card.accuracy !in 0f..1f || card.ease < 1.0) {
            issues.add(ValidationIssue(ValidationSeverity.Error, null, "Card '${card.character}' has out-of-range scheduling values"))
        }
        return issues
    }

    /** Detect cards that collide with existing ids. */
    fun findDuplicates(cards: List<DesktopCard>): List<DesktopCard> {
        val seen = mutableSetOf<String>()
        val dup = mutableListOf<DesktopCard>()
        cards.forEach { card ->
            if (!seen.add(card.id)) dup.add(card)
        }
        return dup
    }

    /**
     * Merge parsed cards into the existing pool applying a conflict
     * policy for id collisions.
     */
    fun apply(
        existing: List<DesktopCard>,
        incoming: List<DesktopCard>,
        policy: ConflictPolicy = ConflictPolicy.KeepExisting
    ): ImportResult {
        val byId = existing.associateBy { it.id }.toMutableMap()
        var imported = 0
        var skipped = 0
        var replaced = 0
        var createdCopies = 0

        incoming.forEach { card ->
            val current = byId[card.id]
            when {
                current == null -> {
                    byId[card.id] = card
                    imported++
                }
                policy == ConflictPolicy.OverwriteExisting -> {
                    byId[card.id] = card
                    replaced++
                }
                policy == ConflictPolicy.KeepNewest -> {
                    val newer = card.createdAt >= current.createdAt
                    if (newer) {
                        byId[card.id] = card
                        replaced++
                    } else skipped++
                }
                policy == ConflictPolicy.Skip -> skipped++
                policy == ConflictPolicy.KeepExisting -> {
                    if (card == current) skipped++
                    else {
                        // Create a copy with a fresh id (DuplicatePolicy.CreateCopy).
                        val copy = card.copy(id = card.id + "-copy-${kotlin.random.Random.nextLong().toString(36)}")
                        byId[copy.id] = copy
                        createdCopies++
                    }
                }
            }
        }

        return ImportResult(
            imported = imported,
            skipped = skipped,
            replaced = replaced,
            createdCopies = createdCopies,
            issues = emptyList(),
            combined = byId.values.toList()
        )
    }
}

// ============================================
// EXPORT PIPELINE
// Cards, statistics, collections, tags, flags,
// settings, themes, or the entire profile.
// ============================================

data class ExportBundle(
    val cards: List<DesktopCard>,
    val reviewLog: List<ReviewLogEntry> = emptyList(),
    val summaries: List<StudyDaySummary> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

object ExportPipeline {

    fun serialize(bundle: ExportBundle, format: TransferFormat): String = when (format) {
        TransferFormat.Json -> JsonExport.export(bundle)
        TransferFormat.Csv -> Codecs.toCsv(bundle.cards)
        TransferFormat.Tsv -> Codecs.toTsv(bundle.cards)
        TransferFormat.Txt -> Codecs.toTxt(bundle.cards)
    }

    /** Export a full profile snapshot (everything) as JSON. */
    fun exportProfile(
        cards: List<DesktopCard>,
        reviewLog: List<ReviewLogEntry>,
        summaries: List<StudyDaySummary>,
        extra: Map<String, String> = emptyMap()
    ): String = JsonExport.exportProfile(cards, reviewLog, summaries, extra)
}

private object JsonExport {
    private val json = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    data class Profile(
        val version: Int = 1,
        val exportedAt: String,
        val app: String = "kaiteyo-desktop",
        val cards: List<DesktopCard>,
        val reviewLog: List<ReviewLogEntry>,
        val summaries: List<StudyDaySummary>,
        val metadata: Map<String, String> = emptyMap()
    )

    @kotlinx.serialization.Serializable
    data class CardExport(val cards: List<DesktopCard>)

    fun export(bundle: ExportBundle): String = json.encodeToString(CardExport(bundle.cards))

    fun exportProfile(
        cards: List<DesktopCard>,
        reviewLog: List<ReviewLogEntry>,
        summaries: List<StudyDaySummary>,
        extra: Map<String, String>
    ): String = json.encodeToString(
        Profile(
            exportedAt = kotlinx.datetime.Clock.System.now().toString(),
            cards = cards,
            reviewLog = reviewLog,
            summaries = summaries,
            metadata = extra
        )
    )
}
