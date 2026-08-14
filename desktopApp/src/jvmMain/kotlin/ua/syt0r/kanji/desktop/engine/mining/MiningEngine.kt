package ua.syt0r.kanji.desktop.engine.mining

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryEntry
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryMatch
import ua.syt0r.kanji.desktop.engine.dictionary.MinedDictionaryData
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.ToastKind
import java.io.File

// ============================================
// KAITEYO MINING ENGINE
// A complete mining workflow: sources (dictionary,
// browser, subtitle, OCR, clipboard, media) feed a
// uniform card-creation pipeline. Cards land in the
// AppState card pool with full source/tag/note data
// so they can be studied immediately.
// ============================================

/** Everything a mined card needs, independent of its source. */
data class MiningPayload(
    val headword: String,
    val reading: String = "",
    val definition: String = "",
    val sentence: String = "",
    val screenshotPath: String? = null,
    val audioPath: String? = null,
    val videoPath: String? = null,
    val timestamp: Double? = null,
    val source: String = "manual",
    val sourceDetail: String = "",
    val tags: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val notes: String = "",
    val deckId: String = DesktopCard.DEFAULT_DECK_ID,
    val example: String = "",
    val pitchAccent: List<MinedDictionaryData> = emptyList()
)

/** Preset template used by the power-user mining workflow. */
@Serializable
data class MiningTemplate(
    val id: String,
    val name: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val deckId: String = DesktopCard.DEFAULT_DECK_ID,
    val source: String = "template"
)

@Serializable
private data class MiningStateDto(
    val recentSources: List<String> = emptyList(),
    val templates: List<MiningTemplate> = emptyList(),
    val mines: List<MinedRecord> = emptyList()
)

/** A record of a completed mine (activity feed + repeat protection). */
@Serializable
data class MinedRecord(
    val id: String,
    val headword: String,
    val createdAt: String,
    val source: String,
    val cardId: String = ""
)

enum class MiningSource(val label: String) {
    Dictionary("Dictionary"),
    Browser("Browser"),
    Video("Video"),
    Subtitle("Subtitle"),
    Ocr("OCR"),
    Clipboard("Clipboard"),
    Reader("Reader"),
    Image("Image"),
    Audio("Audio"),
    Api("Integration API")
}

class MiningEngine(val state: AppState) {

    val sourceOptions: List<MiningSource> = MiningSource.entries

    var draft by mutableStateOf(MiningPayload(headword = ""))
    var miningDialogOpen by mutableStateOf(false)
    var targetCardId by mutableStateOf<String?>(null)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateFile: File get() = File(System.getProperty("user.home"), ".kaiteyo/mining-state.json")

    val recentSources = mutableStateListOf<String>()
    val templates = mutableStateListOf<MiningTemplate>()
    val minedRecords = mutableStateListOf<MinedRecord>()

    init {
        load()
    }

    // ------------------------------------------------------------
    // Card creation (core)
    // ------------------------------------------------------------

    /**
     * Create a study card from a mining payload.
     * Duplicate policy (create / skip / update) is applied per the
     * settings; identical sentence+target pairs are detected across the
     * existing card pool.
     */
    fun mine(payload: MiningPayload): DesktopCard {
        val definition = payload.definition
            .ifBlank { "(no definition)" }
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .take(400)

        // Duplicate detection: sentence + target word is the default key.
        val duplicate = if (payload.sentence.isNotBlank()) {
            state.cards.firstOrNull {
                it.character == payload.headword &&
                    (it.note.contains(payload.sentence.take(60)) || it.note.contains("Sentence: ${payload.sentence.take(60)}"))
            }
        } else null
        if (duplicate != null) {
            when (state.settings.getString("media.mine-duplicate-policy", "create")) {
                "skip" -> {
                    state.toastHost.show("Duplicate sentence already mined — skipped", kind = ToastKind.Info)
                    return duplicate
                }
                "update" -> {
                    val updated = duplicate.copy(
                        meaning = definition,
                        onReadings = payload.reading.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: duplicate.onReadings,
                        tags = (duplicate.tags + payload.tags + listOf("re-mined")).distinct()
                    )
                    state.updateCard(updated)
                    state.toastHost.show("Updated existing card for \"${payload.headword}\"", kind = ToastKind.Success)
                    return updated
                }
            }
        }

        val id = "mined-${payload.headword.hashCode().toUInt().toString(16)}-${payload.source.hashCode().toUInt().toString(16)}-${(payload.timestamp ?: 0.0).toLong()}"
        val now = Clock.System.now()
        val card = DesktopCard(
            id = id,
            character = payload.headword,
            meaning = definition,
            onReadings = payload.reading.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
            kunReadings = emptyList(),
            tags = buildList {
                addAll(payload.tags)
                add("mined")
                add("source:${payload.source}")
            }.distinct(),
            flags = payload.flags,
            note = buildString {
                if (payload.sentence.isNotBlank()) append("Sentence: ").append(payload.sentence).append("\n")
                if (payload.example.isNotBlank()) append("Example: ").append(payload.example).append("\n")
                if (payload.sourceDetail.isNotBlank()) append("Source: ").append(payload.sourceDetail).append("\n")
                if (payload.notes.isNotBlank()) append("Notes: ").append(payload.notes).append("\n")
                if (payload.screenshotPath != null) append("Screenshot: ").append(payload.screenshotPath).append("\n")
                if (payload.audioPath != null) append("Audio: ").append(payload.audioPath).append("\n")
                if (payload.videoPath != null) append("Video: ").append(payload.videoPath).append("\n")
                if (payload.timestamp != null) append("Timestamp: ").append(payload.timestamp).append("\n")
            }.trim(),
            favorite = false,
            status = SrsStatus.New,
            deckId = payload.deckId.ifBlank { DesktopCard.DEFAULT_DECK_ID },
            createdAt = now
        )
        state.addCard(card)
        state.activityLog.record(
            ActivityCategory.Study,
            "Mined \"${payload.headword}\" from ${payload.source}",
            details = definition,
            cardIds = listOf(card.id)
        )
        recordMine(payload, card.id)

        // External transports (GameSentenceMiner / AnkiConnect) when enabled —
        // Kaiteyo mining never depends on them.
        state.miningIntegration.forward(payload).forEach { result ->
            result.onSuccess {
                state.activityLog.record(ActivityCategory.Study, "Forwarded \"${payload.headword}\" to an external integration")
            }.onFailure {
                state.toastHost.show("External forward failed: ${it.message}", kind = ToastKind.Warning)
            }
        }

        // OS notification when the app is in the background (opt-in).
        state.media.notifyMined(payload.headword)

        // Media source link: cards mined from a subtitle keep a pointer back
        // to the exact anime moment (Media → Card → Media round-trip).
        if (payload.source == "subtitle" && state.media.currentItem != null) {
            state.media.recordMiningEvent(card, payload)
        }

        state.toastHost.show("Mined \"${payload.headword}\" → study it in Review", kind = ToastKind.Success)
        return card
    }

    /** Convenience: mine straight from a dictionary match. */
    fun mineFromDictionary(match: DictionaryMatch): DesktopCard {
        val entry = match.entry
        return mine(
            MiningPayload(
                headword = entry.headword,
                reading = entry.readings.firstOrNull()?.reading.orEmpty(),
                definition = entry.senses.joinToString("\n") { s -> s.glosses.joinToString("; ") },
                source = "dictionary",
                sourceDetail = match.dictionary.name,
                tags = buildList {
                    add("dict:${match.dictionary.name}")
                    entry.senses.firstOrNull()?.partOfSpeech?.firstOrNull()?.let { add("pos:$it") }
                },
                example = entry.senses.firstOrNull()?.primaryGloss.orEmpty()
            )
        )
    }

    /** Convert a dictionary entry into a mining payload so the dialog can pre-fill. */
    fun payloadForEntry(entry: DictionaryEntry, dictionaryName: String = ""): MiningPayload {
        val reading = entry.readings.firstOrNull()?.reading.orEmpty()
        val definition = entry.senses.joinToString("\n") { s -> s.glosses.joinToString("; ") }
        return MiningPayload(
            headword = entry.headword,
            reading = reading,
            definition = definition,
            source = "dictionary",
            sourceDetail = dictionaryName,
            tags = entry.senses.firstOrNull()?.partOfSpeech?.firstOrNull()?.let { listOf("pos:$it") } ?: emptyList(),
            example = entry.senses.firstOrNull()?.primaryGloss.orEmpty()
        )
    }

    // ------------------------------------------------------------
    // Dialog workflow
    // ------------------------------------------------------------

    fun openMining(payload: MiningPayload? = null) {
        draft = payload ?: draft
        miningDialogOpen = true
    }

    fun closeMining() {
        miningDialogOpen = false
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private fun recordMine(payload: MiningPayload, cardId: String = "") {
        // Every completed mine — from any source — feeds the mining statistics
        // store (per-day + per-source counters) so Statistics and the Dashboard
        // reflect real volume, not just the capped in-memory record feed.
        state.miningStatistics.recordMine(payload.source)
        val rec = MinedRecord(
            id = "mine-${System.currentTimeMillis()}",
            headword = payload.headword,
            createdAt = Clock.System.now().toString(),
            source = payload.source,
            cardId = cardId
        )
        minedRecords.add(0, rec)
        while (minedRecords.size > 200) minedRecords.removeAt(minedRecords.lastIndex)
        if (payload.source !in recentSources) {
            recentSources.add(0, payload.source)
            while (recentSources.size > 20) recentSources.removeAt(recentSources.lastIndex)
        }
        save()
    }

    private fun load() {
        if (!stateFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<MiningStateDto>(stateFile.readText())
            recentSources.clear(); recentSources.addAll(dto.recentSources)
            templates.clear(); templates.addAll(dto.templates)
            minedRecords.clear(); minedRecords.addAll(dto.mines)
        }
    }

    private fun save() {
        runCatching {
            stateFile.writeText(
                json.encodeToString(
                    MiningStateDto(recentSources.toList(), templates.toList(), minedRecords.toList())
                )
            )
        }
    }
}