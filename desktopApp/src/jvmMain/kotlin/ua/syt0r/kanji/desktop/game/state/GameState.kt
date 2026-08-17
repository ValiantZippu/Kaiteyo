package ua.syt0r.kanji.desktop.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.desktop.game.collection.CollectionManager
import ua.syt0r.kanji.desktop.game.learning.LearningManager
import ua.syt0r.kanji.desktop.game.photography.PhotoAlbum
import ua.syt0r.kanji.desktop.game.player.PlayerEntity
import ua.syt0r.kanji.desktop.game.quest.QuestManager
import ua.syt0r.kanji.desktop.game.settings.GameSettings
import ua.syt0r.kanji.desktop.game.save.GameStatsData
import ua.syt0r.kanji.desktop.game.save.WorldStateData
import ua.syt0r.kanji.desktop.game.story.StoryEngine
import ua.syt0r.kanji.desktop.game.time.WeatherKind
import ua.syt0r.kanji.desktop.game.time.WeatherSystem
import ua.syt0r.kanji.desktop.game.time.WorldClock

/**
 * The aggregate of every persisted/runtime game domain (spec §98) — state is
 * deliberately split, never one giant blob. The [GameSession] owns this and
 * snaps it into [SaveData] on save.
 */
class GameState(
    val player: PlayerEntity,
    val quests: QuestManager,
    val learning: LearningManager,
    val collections: CollectionManager,
    val album: PhotoAlbum,
    val story: StoryEngine,
    val clock: WorldClock,
    val weather: WeatherSystem,
    var settings: GameSettings
) {
    /** Runtime world-state (discoveries, location, flags). */
    var worldState: WorldStateData = WorldStateData()

    /** Activity-gated stats (spec §66-67). */
    var stats: GameStatsData = GameStatsData()

    // ------------------------------------------------------------
    // UI state — which panel is on screen right now
    // ------------------------------------------------------------
    var menuOpen by mutableStateOf(false)
    var mapOpen by mutableStateOf(false)
    var questLogOpen by mutableStateOf(false)
    var collectionOpen by mutableStateOf(false)
    var albumOpen by mutableStateOf(false)
    var knowledgeOpen by mutableStateOf(false)
    var dictionaryOpen by mutableStateOf(false)
    var peopleOpen by mutableStateOf(false)
    var characterOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var storyOpen by mutableStateOf(false)
    var savesOpen by mutableStateOf(false)
    var dialogueOpen by mutableStateOf(false)
    var travelOpen by mutableStateOf(false)
    var photoMode by mutableStateOf(false)
    var paused by mutableStateOf(false)
    /** In-world writing desk (kana tracing, spec §57-59). */
    var writingOpen by mutableStateOf(false)
    /** Knowledge node ids the desk wants traced, in order. */
    var writingTargets by mutableStateOf<List<String>>(emptyList())
    /** Festival stall ordering flow (spec §56). */
    var orderOpen by mutableStateOf(false)
    /** The stall currently being ordered from. */
    var orderStallId by mutableStateOf<String?>(null)

    /** Photo detail popup (photo id + category, from the album). */
    var photoDetail by mutableStateOf<String?>(null)

    /** Discovery popup queue (one at a time). */
    var discoveryQueue by mutableStateOf<List<ua.syt0r.kanji.desktop.game.learning.DiscoveryEvent>>(emptyList())

    /** The quest-completion popup. */
    var completedQuest by mutableStateOf<ua.syt0r.kanji.desktop.game.quest.Quest?>(null)

    fun closeAllPanels() {
        menuOpen = false
        mapOpen = false
        questLogOpen = false
        collectionOpen = false
        albumOpen = false
        knowledgeOpen = false
        dictionaryOpen = false
        peopleOpen = false
        characterOpen = false
        settingsOpen = false
        storyOpen = false
        savesOpen = false
        travelOpen = false
        dialogueOpen = false
        writingOpen = false
        orderOpen = false
        orderStallId = null
        photoDetail = null
    }

    /** The world clock's current weather (read-through for convenience). */
    fun weatherKind(): WeatherKind = weather.current

    fun isBusy(): Boolean =
        menuOpen || mapOpen || questLogOpen || collectionOpen || albumOpen ||
            knowledgeOpen || peopleOpen || characterOpen || settingsOpen || storyOpen ||
            savesOpen || dialogueOpen || travelOpen || writingOpen || orderOpen || paused
}
