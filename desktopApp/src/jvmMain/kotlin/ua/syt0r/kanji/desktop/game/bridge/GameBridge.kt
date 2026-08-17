package ua.syt0r.kanji.desktop.game.bridge

/**
 * The engine bridge (spec §102): the *only* channel between the game and
 * Kaiteyo core. The game never touches dictionary repositories, mining
 * tables or settings files directly — everything goes through this interface,
 * so the game stays swappable and the core stays protected.
 *
 * [KaiteyoBridge] is the desktop implementation over [AppState]; a future
 * engine integration (Orx/libGDX) receives the same surface.
 */
interface GameBridge {

    // ------------------------------------------------------------
    // Dictionary (spec §60, §63)
    // ------------------------------------------------------------

    /** Look up a headword in Kaiteyo's enabled dictionaries. */
    fun lookup(headword: String): BridgeLookup?

    /** Whether the user already has study material for this headword. */
    fun hasStudyMaterialFor(headword: String): Boolean

    // ------------------------------------------------------------
    // Mining (spec §65 — game discoveries mine like every other source)
    // ------------------------------------------------------------

    /** Mine a discovery into Kaiteyo/Anki through the shared mining pipeline. */
    fun mine(payload: BridgeMinePayload): Boolean

    // ------------------------------------------------------------
    // TTS (spec §61-62, §91-92 — spoken Japanese, kana-clip based)
    // ------------------------------------------------------------

    /**
     * Speak a Japanese (kana) string aloud through Kaiteyo's voice engine.
     * Returns false when no voice is available. The default no-op keeps
     * engine-agnostic implementations (and test fakes) honest.
     */
    fun speakJp(kanaText: String): Boolean = false

    // ------------------------------------------------------------
    // Photography (spec §43-45 — the album can leave the game)
    // ------------------------------------------------------------

    /**
     * Save a photo (with its vocabulary tags) to the user's disk as a small
     * JSON sidecar under the Kaiteyo data dir. Returns false when the write
     * fails — the game never crashes over a photo export.
     */
    fun savePhotoToDisk(photo: BridgePhoto): Boolean = false

    // ------------------------------------------------------------
    // Stats / activity (spec §66-67 — meaningful activity only)
    // ------------------------------------------------------------

    /** Record a meaningful game activity (never AFK time). */
    fun recordActivity(kind: GameActivityKind, detail: String)

    // ------------------------------------------------------------
    // UX
    // ------------------------------------------------------------

    fun toast(message: String, kind: BridgeToastKind = BridgeToastKind.Info)

    // ------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------

    fun getSetting(key: String, default: String): String

    fun setSetting(key: String, value: String)
}

/**
 * A resolved dictionary hit, flattened for the game. Carries the entry's
 * senses (POS + glosses) and kanji detail (readings, strokes, radicals) so
 * the discovery card can show a real mini entry — spec §17, §63.
 */
data class BridgeLookup(
    val headword: String,
    val reading: String,
    val meaning: String,
    val dictionaryName: String,
    val jlpt: String? = null,
    val tags: List<String> = emptyList(),
    /** The entry's senses: part of speech + glosses (spec §63). */
    val senses: List<BridgeSense> = emptyList(),
    /** Kanji components of the entry: readings, strokes, radicals (spec §15). */
    val kanji: List<BridgeKanji> = emptyList(),
    /** Pitch-accent markers for the primary reading (spec §19), Yomitan style. */
    val pitchAccents: List<BridgePitch> = emptyList()
)

/**
 * One pitch-accent marker (spec §19): [position] 0 = heiban (平板, no
 * downstep); otherwise the accent sits on that mora. Shown only when the
 * dictionary actually carries it — never fabricated.
 */
data class BridgePitch(
    val position: Int,
    val downstep: Int? = null
)

/** One sense of an entry, flattened from [ua.syt0r.kanji.desktop.engine.dictionary.DictionarySense]. */
data class BridgeSense(
    val partOfSpeech: List<String> = emptyList(),
    val glosses: List<String> = emptyList()
)

/** Kanji detail flattened from [ua.syt0r.kanji.desktop.engine.dictionary.KanjiSpelling]. */
data class BridgeKanji(
    val character: String,
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val strokeCounts: List<Int> = emptyList(),
    val radicals: List<String> = emptyList(),
    val jlpt: Int? = null,
    val grade: Int? = null
)

/** A mine request — mirrors the suite's MiningPayload shape. */
data class BridgeMinePayload(
    val headword: String,
    val reading: String = "",
    val definition: String = "",
    val sentence: String = "",
    val source: String = "game",
    val sourceDetail: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = ""
)

/** A photo snapshot passed to the bridge for disk export. */
data class BridgePhoto(
    val id: String,
    val title: String,
    val category: String,
    val regionId: String,
    val locationId: String?,
    val takenAt: String,
    val tags: List<BridgePhotoTag>
)

data class BridgePhotoTag(
    val headword: String,
    val reading: String,
    val meaning: String
)

/** Meaningful game activities counted by Kaiteyo stats. */
enum class GameActivityKind {
    WordDiscovered,
    KanjiDiscovered,
    QuestCompleted,
    SentenceRead,
    DialogueListened,
    PhotoTaken,
    LocationDiscovered,
    WritingCompleted
}

enum class BridgeToastKind {
    Info, Success, Warning
}
