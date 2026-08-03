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
    val source: String
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

    var draft by mutableStateOf(MiningPayload())
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
     * Idempotent-ish: re-mining an identical headword updates it.
     */
    fun mine(payload: MiningPayload): DesktopCard {
        val definition = payload.definition
            .ifBlank { "(no definition)" }
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .take(400)

        val id = "mined-${payload.headword.hashCode().toUInt().toString(16)}-${payload.source.hashCode().toUInt().toString(16)}"
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
        recordMine(payload)
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

    private fun recordMine(payload: MiningPayload) {
        val rec = MinedRecord(
            id = "mine-${System.currentTimeMillis()}",
            headword = payload.headword,
            createdAt = Clock.System.now().toString(),
            source = payload.source
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