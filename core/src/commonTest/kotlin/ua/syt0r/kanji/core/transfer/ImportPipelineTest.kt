package ua.syt0r.kanji.core.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

class ImportPipelineTest {

    private val pipeline = ImportPipeline()

    private fun transferCard(
        id: String = "c1",
        character: String = "水",
        interval: Int = 5,
        modified: String = "2026-01-01"
    ) = TransferCard.fromKaiteyoCard(
        KaiteyoCard(
            id = id,
            character = character,
            meaning = "Water",
            reading = "みず",
            interval = interval,
            status = CardStatus.Young,
            modifiedAt = modified
        )
    )

    private fun kaiteyoCard(id: String, character: String, interval: Int = 5) =
        TransferCard.toKaiteyoCard(transferCard(id, character, interval))

    // ------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------

    @Test
    fun previewReportsValidAndInvalidCards() {
        val text = """[
            {"id": "a", "character": "水", "meaning": "Water", "reading": "みず",
             "interval": 5, "ease": 2.5, "accuracy": 0.8},
            {"id": "b", "character": "", "meaning": "No char", "reading": ""}
        ]"""
        val preview = pipeline.preview(text, TransferFormat.Json).getOrThrow()
        assertEquals(2, preview.total)
        assertEquals(1, preview.valid)
        assertEquals(1, preview.invalid)
        assertTrue(preview.issues.any { it.severity == ValidationSeverity.Error })
    }

    @Test
    fun previewRejectsMalformedJson() {
        val result = pipeline.preview("{ nope", TransferFormat.Json)
        assertTrue(result.isFailure, "Malformed import must not crash the pipeline")
    }

    @Test
    fun previewFlagsOutOfRangeScheduling() {
        val card = TransferCard.fromKaiteyoCard(
            KaiteyoCard(id = "x", character = "水", meaning = "Water", interval = -3, ease = 0.5f, accuracy = 2f)
        )
        val issues = pipeline.validateCard(TransferCard.toKaiteyoCard(card))
        assertTrue(issues.any { it.severity == ValidationSeverity.Error })
    }

    // ------------------------------------------------------------
    // Duplicates
    // ------------------------------------------------------------

    @Test
    fun findDuplicatesDetectsIdCollisions() {
        val cards = listOf(transferCard("a"), transferCard("a"), transferCard("b"))
        assertEquals(1, pipeline.findDuplicates(cards).size)
    }

    // ------------------------------------------------------------
    // Conflict policies
    // ------------------------------------------------------------

    @Test
    fun applyImportsNewCards() {
        val existing = listOf(kaiteyoCard("a", "水"))
        val incoming = listOf(transferCard("b", "猫"))
        val result = pipeline.apply(existing, incoming, ConflictPolicy.KeepExisting)
        assertEquals(1, result.imported)
        assertEquals(2, result.combined.size)
    }

    @Test
    fun keepExistingCreatesCopiesOnConflict() {
        val existing = listOf(kaiteyoCard("a", "水", interval = 5))
        val incoming = listOf(transferCard("a", "水", interval = 99))
        val result = pipeline.apply(existing, incoming, ConflictPolicy.KeepExisting)
        assertEquals(1, result.createdCopies)
        assertEquals(2, result.combined.size)
        assertEquals(5, result.combined.first { it.id == "a" }.interval)
    }

    @Test
    fun overwriteReplacesConflictingCards() {
        val existing = listOf(kaiteyoCard("a", "水", interval = 5))
        val incoming = listOf(transferCard("a", "水", interval = 99))
        val result = pipeline.apply(existing, incoming, ConflictPolicy.OverwriteExisting)
        assertEquals(1, result.replaced)
        assertEquals(99, result.combined.first { it.id == "a" }.interval)
    }

    @Test
    fun skipIgnoresConflictingCards() {
        val existing = listOf(kaiteyoCard("a", "水", interval = 5))
        val incoming = listOf(transferCard("a", "水", interval = 99))
        val result = pipeline.apply(existing, incoming, ConflictPolicy.Skip)
        assertEquals(1, result.skipped)
        assertEquals(5, result.combined.first { it.id == "a" }.interval)
    }

    @Test
    fun keepNewestPicksNewerCard() {
        val existing = listOf(kaiteyoCard("a", "水"))
        val older = listOf(transferCard("a", "水", modified = "2025-01-01"))
        val newer = listOf(transferCard("a", "水", modified = "2026-06-01"))

        val result = pipeline.apply(existing, older, ConflictPolicy.KeepNewest)
        assertEquals(1, result.skipped, "Older import must be skipped")

        val result2 = pipeline.apply(existing, newer, ConflictPolicy.KeepNewest)
        assertEquals(1, result2.replaced, "Newer import must replace")
    }
}
