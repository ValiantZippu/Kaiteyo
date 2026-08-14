package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.mining.AnkiConnectTransport
import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DeckDef
import ua.syt0r.kanji.desktop.model.DesktopCard
import java.io.File
import java.util.Base64

// ============================================
// ANKI → KAITEYO IMPORTER
// Pulls decks, notes, cards and tags out of an
// AnkiConnect instance and lands them in
// Kaiteyo's own library. The Anki deck path is
// preserved as a Kaiteyo deck hierarchy; notes
// become cards with their fields preserved;
// scheduling (interval/due/ease/reps/lapses) is
// carried over approximately and handed to
// Kaiteyo's scheduler; audio/images are
// downloaded into ~/.kaiteyo/anki-media.
//
// Duplicate detection is two-layered: the Anki
// note GUID (stored as externalId) is the
// primary key so re-imports are idempotent, with
// a content fingerprint as fallback for notes
// that predate GUID tracking. Conflict handling
// is user-selectable (skip/update/duplicate).
// ============================================

class AnkiImporter(
    private val state: AppState,
    private val transport: AnkiConnectTransport
) {

    private val mediaRoot: File = File(System.getProperty("user.home"), ".kaiteyo/anki-media")

    /** In-memory deck-path cache so repeated imports reuse one deck object. */
    private val deckCache = HashMap<String, DeckDef>()

    // ------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------

    /** Deck names + note/card counts + sample tags for the import dialog. */
    fun fetchPreview(): Result<List<AnkiDeckPreview>> = runCatching {
        val decks = transport.deckNamesAndIds().getOrThrow()
        decks.map { (name, id) ->
            val notes = transport.findNotes("\"deck:$name\"").getOrDefault(emptyList())
            val cards = transport.findCards("\"deck:$name\"").getOrDefault(emptyList())
            AnkiDeckPreview(
                name = name,
                ankiDeckId = id,
                noteCount = notes.size,
                cardCount = cards.size,
                tags = sampleTags(notes),
                sampleFronts = sampleFronts(notes)
            )
        }.sortedBy { it.name }
    }

    private fun sampleTags(noteIds: List<Long>, limit: Int = 12): List<String> {
        if (noteIds.isEmpty()) return emptyList()
        val sample = noteIds.take(25)
        return transport.notesInfo(sample).getOrDefault(emptyList())
            .flatMap { note ->
                note["tags"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
            }
            .distinct()
            .take(limit)
    }

    private fun sampleFronts(noteIds: List<Long>, limit: Int = 5): List<String> {
        if (noteIds.isEmpty()) return emptyList()
        return transport.notesInfo(noteIds.take(limit)).getOrDefault(emptyList()).mapNotNull { note ->
            val fields = transport.noteFields(note)
            AnkiImportMapper.stripHtml(AnkiImportMapper.pickField(fields, "Front", "Expression", "Word", "Text"))
                .takeIf { it.isNotBlank() }
        }
    }

    // ------------------------------------------------------------
    // Import
    // ------------------------------------------------------------

    /**
     * Import one Anki deck into Kaiteyo. Suspends while the network + disk
     * work runs on the calling dispatcher, then commits to the state on the
     * main thread. [onProgress] reports (notesProcessed, notesTotal).
     */
    suspend fun importDeck(
        preview: AnkiDeckPreview,
        options: AnkiImportOptions,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<AnkiDeckImportResult> = runCatching {
        val deckPath = options.deck.ifBlank { preview.name }
        // Deck hierarchy + existing-state reads touch Compose state — main thread.
        val (deck, existingByExternal, existingByFingerprint) = withContext(Dispatchers.Main) {
            val d = ensureDeckHierarchy(deckPath)
            val byExternal = HashMap<String, DesktopCard>()
            val byFingerprint = HashMap<String, MutableList<DesktopCard>>()
            state.cards.forEach { card ->
                if (card.externalId.isNotBlank()) byExternal[card.externalId] = card
                val fp = AnkiImportMapper.fingerprint(card.character, card.meaning)
                byFingerprint.getOrPut(fp) { mutableListOf() }.add(card)
            }
            Triple(d, byExternal, byFingerprint)
        }

        val noteIds = transport.findNotes("\"deck:${preview.name}\"").getOrThrow()
        if (noteIds.isEmpty()) {
            return@runCatching AnkiDeckImportResult(deckName = deck.name, warnings = listOf("No notes matched"))
        }

        // Card scheduling: cardId → scheduling, keyed for the note's cards.
        val cardIds = transport.findCards("\"deck:${preview.name}\"").getOrDefault(emptyList())
        val schedulingByCard = transport.cardsInfo(cardIds).getOrDefault(emptyList())
            .mapNotNull { info ->
                val cardId = info["cardId"]?.jsonPrimitive?.longOrNull
                    ?: info["id"]?.jsonPrimitive?.longOrNull
                    ?: return@mapNotNull null
                cardId to AnkiSchedulingData(
                    queue = info["queue"]?.jsonPrimitive?.longOrNull ?: 0L,
                    interval = info["interval"]?.jsonPrimitive?.longOrNull ?: 0L,
                    due = info["due"]?.jsonPrimitive?.longOrNull ?: 0L,
                    reps = info["reps"]?.jsonPrimitive?.longOrNull ?: 0L,
                    lapses = info["lapses"]?.jsonPrimitive?.longOrNull ?: 0L,
                    ease = info["factor"]?.jsonPrimitive?.longOrNull ?: info["ease"]?.jsonPrimitive?.longOrNull ?: 2500L
                )
            }
            .toMap()

        val seenExternal = HashSet<String>()
        // cardId → original fields, used later for media extraction.
        val mediaByCard = LinkedHashMap<String, Pair<DesktopCard, Map<String, String>>>()

        val newCards = mutableListOf<DesktopCard>()
        val updatedCards = mutableListOf<DesktopCard>()
        val warnings = mutableListOf<String>()

        var imported = 0
        var updated = 0
        var skipped = 0
        var duplicated = 0

        // Pull notes in chunks so huge collections don't arrive in one payload.
        noteIds.chunked(50).forEach { chunk ->
            transport.notesInfo(chunk).getOrThrow().forEach { note ->
                val guid = note["guid"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val externalId = AnkiImportMapper.externalIdFor(guid)
                // A note with cards in several decks is only imported once.
                if (!seenExternal.add(externalId)) {
                    skipped++
                    return@forEach
                }

                val fields = transport.noteFields(note)
                val noteTags = note["tags"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }

                // Scheduling: first card of the note (notes can have many cards).
                val noteCards = note["cards"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.longOrNull }
                val scheduling = noteCards.firstNotNullOfOrNull { schedulingByCard[it] }

                val card = AnkiImportMapper.noteToCard(
                    guid = guid,
                    fields = fields,
                    tags = noteTags,
                    scheduling = scheduling,
                    deckId = deck.id,
                    includeScheduling = options.includeScheduling,
                    ankiDeckName = preview.name
                )

                // GUID match is the primary key; the content fingerprint is
                // the fallback so native Kaiteyo cards (no GUID) are also
                // caught rather than silently duplicated.
                val existing = AnkiImportMapper.findExisting(existingByExternal, existingByFingerprint, card)

                when {
                    existing == null -> {
                        newCards.add(card)
                        mediaByCard[card.id] = card to fields
                        imported++
                    }
                    options.policy == AnkiConflictPolicy.Skip -> skipped++
                    options.policy == AnkiConflictPolicy.Duplicate -> {
                        val copy = card.copy(
                            id = AnkiImportMapper.cardIdFor(guid) + "-dup-" + duplicated,
                            externalId = externalId + "-dup-" + duplicated
                        )
                        newCards.add(copy)
                        mediaByCard[copy.id] = copy to fields
                        duplicated++
                    }
                    options.policy == AnkiConflictPolicy.Update -> {
                        val merged = card.copy(id = existing.id, createdAt = existing.createdAt)
                        updatedCards.add(merged)
                        mediaByCard[merged.id] = merged to fields
                        updated++
                    }
                }
            }
            onProgress(imported + updated + skipped + duplicated, noteIds.size)
        }

        // Download media referenced by the imported notes, once per file.
        var mediaSaved = 0
        val downloadedFiles = HashSet<String>()
        if (options.includeMedia && mediaByCard.isNotEmpty()) {
            val targetDir = File(mediaRoot, safeSegment(deck.name))
            val fileCache = HashMap<String, String?>() // filename → local path (null = missing)
            mediaByCard.forEach { (cardId, entry) ->
                val (card, fields) = entry
                val lines = mutableListOf<String>()
                AnkiImportMapper.extractMediaFilenames(fields).forEach { filename ->
                    val path = fileCache.getOrPut(filename) { downloadMedia(filename, targetDir) }
                    if (path == null) {
                        warnings.add("Media not found in Anki: $filename")
                    } else {
                        lines.add(mediaLineFor(filename, path))
                        if (downloadedFiles.add(path)) mediaSaved++
                    }
                }
                if (lines.isNotEmpty()) {
                    val enriched = card.copy(note = (card.note.lines() + lines).distinct().joinToString("\n"))
                    val idx = newCards.indexOfFirst { it.id == cardId }
                    if (idx >= 0) newCards[idx] = enriched else {
                        val uIdx = updatedCards.indexOfFirst { it.id == cardId }
                        if (uIdx >= 0) updatedCards[uIdx] = enriched
                    }
                }
            }
        }

        // Commit: persist once instead of per-card writes. State mutation
        // happens on the main thread (Compose snapshots are not thread-safe).
        if (newCards.isNotEmpty() || updatedCards.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                state.importCards(newCards, updatedCards)
                state.library.addCards(deck.id, (newCards + updatedCards).map { it.id })
                state.activityLog.record(
                    ActivityCategory.Import,
                    "Imported \"${deck.name}\" from AnkiConnect: " +
                        "$imported new, $updated updated, $skipped skipped, $duplicated duplicated, $mediaSaved media"
                )
            }
        }

        AnkiDeckImportResult(
            deckName = deck.name,
            imported = imported,
            updated = updated,
            skipped = skipped,
            duplicated = duplicated,
            mediaSaved = mediaSaved,
            warnings = warnings.distinct().take(20)
        )
    }

    // ------------------------------------------------------------
    // Deck hierarchy
    // ------------------------------------------------------------

    /**
     * Ensure the Anki path exists as nested Kaiteyo decks. Reuses decks
     * previously created by an Anki import (tag anki:path:<full>) so
     * re-imports grow the same deck instead of forking a copy.
     */
    private fun ensureDeckHierarchy(fullPath: String): DeckDef {
        val segments = AnkiImportMapper.splitDeckPath(fullPath)
        var parent: DeckDef? = null
        var current = ""
        var deck: DeckDef? = null
        for (segment in segments) {
            current = if (current.isEmpty()) segment else "$current::$segment"
            // A cached deck may have been deleted by the user — re-validate
            // before reusing so re-imports attach cards to a live deck.
            val cached = deckCache[current]?.takeIf { state.library.deck(it.id) != null }
            deck = cached ?: findAnkiDeck(current) ?: createDeck(current, segment, parent)
            deckCache[current] = deck
            parent = deck
        }
        return deck ?: error("Failed to create deck hierarchy for $fullPath")
    }

    private fun findAnkiDeck(fullPath: String): DeckDef? =
        state.library.allDecks().firstOrNull { d -> d.tags.contains("anki:path:$fullPath") }

    private fun createDeck(fullPath: String, displayName: String, parent: DeckDef?): DeckDef {
        val created = state.library.create(
            name = displayName,
            kind = ContentKind.Vocabulary,
            parentId = parent?.id,
            tags = listOf("anki", "anki:path:$fullPath")
        )
        state.library.update(
            created.copy(
                source = "anki",
                importedAt = Clock.System.now(),
                description = "Imported from Anki deck $fullPath"
            )
        )
        return created
    }

    // ------------------------------------------------------------
    // Media
    // ------------------------------------------------------------

    private fun safeSegment(name: String): String =
        name.replace(Regex("[^\\p{Alnum}._-]+"), "-").trim('-').ifBlank { "anki" }

    private fun mediaLineFor(filename: String, path: String): String {
        val audioExts = listOf("mp3", "ogg", "wav", "m4a", "opus", "aac", "flac")
        return if (filename.substringAfterLast('.', "").lowercase() in audioExts) "Audio: $path"
        else "Image: $path"
    }

    private fun downloadMedia(filename: String, targetDir: File): String? {
        val safeName = filename.replace("\\", "/").substringAfterLast('/')
            .ifBlank { return null }
        val target = File(targetDir, safeName)
        if (target.exists()) return target.absolutePath
        val base64 = transport.retrieveMediaFile(filename).getOrNull() ?: return null
        return runCatching {
            targetDir.mkdirs()
            target.writeBytes(Base64.getDecoder().decode(base64))
            target.absolutePath
        }.getOrNull()
    }
}
