package ua.syt0r.kanji.core.transfer

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

// ============================================
// IMPORT PIPELINE
// Automatic validation, duplicate detection,
// conflict resolution, preview before import.
// ============================================

enum class ConflictPolicy { KeepExisting, OverwriteExisting, Skip, KeepNewest }

enum class DuplicatePolicy { Skip, Replace, CreateCopy }

enum class ValidationSeverity { Info, Warning, Error }

@Serializable
data class ValidationIssue(
    val severity: ValidationSeverity,
    val line: Int? = null,
    val message: String
)

@Serializable
data class ImportPreview(
    val format: TransferFormat,
    val total: Int,
    val valid: Int,
    val invalid: Int,
    val duplicates: Int,
    val issues: List<ValidationIssue>,
    val cards: List<TransferCard>
)

@Serializable
data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val replaced: Int,
    val createdCopies: Int,
    val issues: List<ValidationIssue>,
    val combined: List<TransferCard> = emptyList()
)

class ImportPipeline {

    fun preview(text: String, format: TransferFormat): Result<ImportPreview> = runCatching {
        val parsed = when (format) {
            TransferFormat.Json -> TransferCodecs.fromJson(text).getOrThrow()
            TransferFormat.Csv -> TransferCodecs.fromCsv(text).getOrThrow()
            TransferFormat.Tsv -> TransferCodecs.fromTsv(text).getOrThrow()
            TransferFormat.Txt -> TransferCodecs.fromTxt(text).getOrThrow()
        }
        val issues = mutableListOf<ValidationIssue>()
        val valid = parsed.mapNotNull { kaiteyoCard ->
            val cardIssues = validateCard(kaiteyoCard)
            issues.addAll(cardIssues)
            if (cardIssues.none { it.severity == ValidationSeverity.Error }) {
                TransferCard.fromKaiteyoCard(kaiteyoCard)
            } else {
                null
            }
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
    fun previewCards(cards: List<KaiteyoCard>): ImportPreview {
        val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
        val issues = mutableListOf<ValidationIssue>()
        val valid = transferCards.filter { transferCard ->
            val kaiteyoCard = TransferCard.toKaiteyoCard(transferCard)
            val cardIssues = validateCard(kaiteyoCard)
            issues.addAll(cardIssues)
            cardIssues.none { it.severity == ValidationSeverity.Error }
        }
        val invalid = transferCards.size - valid.size
        return ImportPreview(
            format = TransferFormat.Json,
            total = transferCards.size,
            valid = valid.size,
            invalid = invalid,
            duplicates = findDuplicates(valid).size,
            issues = issues,
            cards = valid
        )
    }

    fun validateCard(card: KaiteyoCard): List<ValidationIssue> {
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
        if (card.interval < 0 || card.accuracy !in 0f..1f || card.ease < 1.0) {
            issues.add(ValidationIssue(ValidationSeverity.Error, null, "Card '${card.character}' has out-of-range scheduling values"))
        }
        return issues
    }

    /** Detect cards that collide with existing ids. */
    fun findDuplicates(cards: List<TransferCard>): List<TransferCard> {
        val seen = mutableSetOf<String>()
        val dup = mutableListOf<TransferCard>()
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
        existing: List<KaiteyoCard>,
        incoming: List<TransferCard>,
        policy: ConflictPolicy = ConflictPolicy.KeepExisting
    ): ImportResult {
        val byId = existing.associateBy { it.id }.toMutableMap()
        var imported = 0
        var skipped = 0
        var replaced = 0
        var createdCopies = 0

        incoming.forEach { transferCard ->
            val card = TransferCard.toKaiteyoCard(transferCard)
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
                    val newer = card.modifiedAt >= current.modifiedAt
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
                        val copy = card.copy(id = "${card.id}-copy-${kotlin.random.Random.nextLong().toString(36)}")
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
            combined = byId.values.map { TransferCard.fromKaiteyoCard(it) }
        )
    }
}

// ============================================
// EXPORT PIPELINE
// Cards, statistics, collections, tags, flags,
// settings, themes, or the entire profile.
// ============================================

@Serializable
data class ExportBundle(
    val cards: List<TransferCard>,
    val metadata: Map<String, String> = emptyMap()
)

object ExportPipeline {

    fun serialize(bundle: ExportBundle, format: TransferFormat): String = when (format) {
        TransferFormat.Json -> {
            val json = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
            json.encodeToString(bundle)
        }
        TransferFormat.Csv -> TransferCodecs.toCsv(bundle.cards.map { TransferCard.toKaiteyoCard(it) })
        TransferFormat.Tsv -> TransferCodecs.toTsv(bundle.cards.map { TransferCard.toKaiteyoCard(it) })
        TransferFormat.Txt -> TransferCodecs.toTxt(bundle.cards.map { TransferCard.toKaiteyoCard(it) })
    }
}