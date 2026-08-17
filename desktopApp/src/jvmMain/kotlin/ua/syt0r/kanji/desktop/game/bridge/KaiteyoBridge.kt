package ua.syt0r.kanji.desktop.game.bridge

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform
import ua.syt0r.kanji.core.japanese.getKanaReading
import ua.syt0r.kanji.core.japanese.isKana
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.activity.SignalContext
import ua.syt0r.kanji.desktop.engine.dictionary.SearchMode
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import ua.syt0r.kanji.desktop.model.ToastKind

/**
 * Desktop bridge implementation — routes game calls into the real Kaiteyo
 * services owned by [AppState]: the dictionary repository, the mining engine,
 * the activity/engagement tracker and the settings store.
 *
 * Study time stays honest (spec §67): only real discoveries/quests call
 * [recordActivity]; idling in the world never does.
 */
class KaiteyoBridge(private val state: AppState) : GameBridge {

    override fun lookup(headword: String): BridgeLookup? {
        val match = state.dictionary.lookupFlat(headword, SearchMode.All).firstOrNull() ?: return null
        val entry = match.entry
        val sense = entry.senses.firstOrNull()
        val reading = entry.readings.firstOrNull()?.reading.orEmpty()
            .ifBlank { sense?.primaryGloss.orEmpty() }
        return BridgeLookup(
            headword = entry.headword,
            reading = reading,
            meaning = sense?.glosses?.joinToString("; ").orEmpty(),
            dictionaryName = match.dictionary.name,
            jlpt = sense?.tags?.firstOrNull { it.startsWith("jlpt-") },
            tags = sense?.tags.orEmpty(),
            // Real entry data, not a fake summary (spec §17, §63): the full
            // sense list and the kanji components feed the in-game card.
            senses = entry.senses.take(4).map {
                BridgeSense(partOfSpeech = it.partOfSpeech, glosses = it.glosses)
            },
            kanji = entry.kanjiSpellings.take(4).map {
                BridgeKanji(
                    character = it.character,
                    onReadings = it.onReadings,
                    kunReadings = it.kunReadings,
                    meanings = it.meanings,
                    strokeCounts = it.strokeCounts,
                    radicals = it.radicals,
                    jlpt = it.jlpt,
                    grade = it.grade
                )
            },
            // Real pitch-accent data when the dictionary has it (spec §19).
            pitchAccents = entry.readings.firstOrNull()?.pitchAccents.orEmpty().map {
                BridgePitch(position = it.position, downstep = it.downstep)
            }
        )
    }

    override fun hasStudyMaterialFor(headword: String): Boolean =
        state.cards.any { it.character == headword }

    override fun mine(payload: BridgeMinePayload): Boolean {
        val card = state.mining.mine(
            MiningPayload(
                headword = payload.headword,
                reading = payload.reading,
                definition = payload.definition,
                sentence = payload.sentence,
                source = payload.source,
                sourceDetail = payload.sourceDetail,
                tags = payload.tags,
                notes = payload.notes
            )
        )
        return card != null
    }

    override fun speakJp(kanaText: String): Boolean {
        // Kaiteyo's voice engine is a Koin service (platformComponentsModule);
        // resolve it lazily so an absent voice (tests, first run) never breaks
        // the game — it just stays silent.
        val tts = runCatching { KoinPlatform.getKoin().get<KanaTtsManager>() }.getOrNull()
            ?: return false
        if (kanaText.isBlank()) return false
        // One clip per kana, played sequentially — each speak() waits for its
        // own clip to finish, so the line comes out at a natural pace. Non-kana
        // characters (punctuation) become a short breath instead of dead air.
        // A kana with no clip (edge cases in the voice data) is skipped rather
        // than crashing the line.
        return runBlocking {
            var spoken = false
            for (char in kanaText) {
                if (char.isKana()) {
                    val ok = runCatching { tts.speak(getKanaReading(char)) }.isSuccess
                    spoken = spoken || ok
                } else {
                    delay(90)
                }
            }
            spoken
        }
    }

    override fun savePhotoToDisk(photo: BridgePhoto): Boolean = runCatching {
        // A photo is a small JSON sidecar under ~/.kaiteyo/game/photos/ — the
        // album stays in the save file, and a copy can leave the game.
        val dir = java.io.File(System.getProperty("user.home"), ".kaiteyo/game/photos")
        dir.mkdirs()
        val safe = photo.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = java.io.File(dir, "$safe.json")
        val json = buildString {
            append("{\n  \"id\": \"").append(photo.id).append("\",\n")
            append("  \"title\": \"").append(photo.title.replace("\"", "\\\"")).append("\",\n")
            append("  \"category\": \"").append(photo.category).append("\",\n")
            append("  \"regionId\": \"").append(photo.regionId).append("\",\n")
            append("  \"locationId\": ").append(photo.locationId?.let { "\"$it\"" } ?: "null").append(",\n")
            append("  \"takenAt\": \"").append(photo.takenAt).append("\",\n")
            append("  \"tags\": [\n")
            photo.tags.forEachIndexed { index, tag ->
                append("    {\"headword\": \"").append(tag.headword).append("\", \"reading\": \"")
                    .append(tag.reading).append("\", \"meaning\": \"").append(tag.meaning).append("\"}")
                if (index < photo.tags.lastIndex) append(",")
                append("\n")
            }
            append("  ]\n}\n")
        }
        file.writeText(json)
        true
    }.getOrDefault(false)

    override fun recordActivity(kind: GameActivityKind, detail: String) {
        // Real interaction signal — keeps the engagement tracker honest.
        state.activity.recordSignal(SignalContext.Study)
        state.activityLog.record(
            ActivityCategory.Study,
            detail,
            details = "Game: ${kind.name}"
        )
    }

    override fun toast(message: String, kind: BridgeToastKind) {
        state.toastHost.show(message, kind = when (kind) {
            BridgeToastKind.Info -> ToastKind.Info
            BridgeToastKind.Success -> ToastKind.Success
            BridgeToastKind.Warning -> ToastKind.Warning
        })
    }

    override fun getSetting(key: String, default: String): String =
        state.settings.getString(key, default)

    override fun setSetting(key: String, value: String) {
        state.settings.set(key, value)
    }
}
