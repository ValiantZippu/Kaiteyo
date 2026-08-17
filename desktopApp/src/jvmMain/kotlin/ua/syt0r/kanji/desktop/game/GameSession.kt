package ua.syt0r.kanji.desktop.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.collection.CollectionManager
import ua.syt0r.kanji.desktop.game.content.LoadedContent
import ua.syt0r.kanji.desktop.game.debug.DebugTools
import ua.syt0r.kanji.desktop.game.dialogue.DialogueEffect
import ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectHandler
import ua.syt0r.kanji.desktop.game.dialogue.DialogueRunner
import ua.syt0r.kanji.desktop.game.engine.GameEngine
import ua.syt0r.kanji.desktop.game.engine.Scene
import ua.syt0r.kanji.desktop.game.engine.camera.Camera
import ua.syt0r.kanji.desktop.game.engine.camera.CameraMode
import ua.syt0r.kanji.desktop.game.engine.camera.CameraRig
import ua.syt0r.kanji.desktop.game.engine.camera.CameraSettings
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.InputManager
import ua.syt0r.kanji.desktop.game.engine.render.RenderBackend
import ua.syt0r.kanji.desktop.game.interaction.Interactable
import ua.syt0r.kanji.desktop.game.interaction.InteractionBehavior
import ua.syt0r.kanji.desktop.game.interaction.InteractionResult
import ua.syt0r.kanji.desktop.game.interaction.InteractionSystem
import ua.syt0r.kanji.desktop.game.learning.AssistanceLevel
import ua.syt0r.kanji.desktop.game.learning.DiscoverySource
import ua.syt0r.kanji.desktop.game.learning.KnowledgeGraph
import ua.syt0r.kanji.desktop.game.learning.KnowledgeNode
import ua.syt0r.kanji.desktop.game.learning.LearningManager
import ua.syt0r.kanji.desktop.game.npc.NpcDirector
import ua.syt0r.kanji.desktop.game.photography.PhotoCamera
import ua.syt0r.kanji.desktop.game.photography.PhotoAlbum
import ua.syt0r.kanji.desktop.game.photography.PhotoSubject
import ua.syt0r.kanji.desktop.game.player.PlayerController
import ua.syt0r.kanji.desktop.game.player.PlayerEntity
import ua.syt0r.kanji.desktop.game.player.PlayerState
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.quest.QuestEvent
import ua.syt0r.kanji.desktop.game.quest.QuestManager
import ua.syt0r.kanji.desktop.game.quest.QuestRewardHandler
import ua.syt0r.kanji.desktop.game.render.WorldRenderer
import ua.syt0r.kanji.desktop.game.save.GameStatsData
import ua.syt0r.kanji.desktop.game.save.SaveData
import ua.syt0r.kanji.desktop.game.save.SaveManager
import ua.syt0r.kanji.desktop.game.save.WorldStateData
import ua.syt0r.kanji.desktop.game.settings.GameSettings
import ua.syt0r.kanji.desktop.game.state.GameState
import ua.syt0r.kanji.desktop.game.story.StoryEngine
import ua.syt0r.kanji.desktop.game.tts.DialogueTts
import ua.syt0r.kanji.desktop.game.audio.AmbientKind
import ua.syt0r.kanji.desktop.game.audio.GameAudio
import ua.syt0r.kanji.desktop.game.audio.Sfx
import ua.syt0r.kanji.desktop.game.time.WeatherSystem
import ua.syt0r.kanji.desktop.game.time.WorldClock
import ua.syt0r.kanji.desktop.game.time.SeasonSystem
import ua.syt0r.kanji.desktop.game.time.toSeasonAudio
import ua.syt0r.kanji.desktop.game.validation.ContentValidator
import ua.syt0r.kanji.desktop.game.world.GameWorld
import ua.syt0r.kanji.desktop.game.world.TileGrid
import ua.syt0r.kanji.desktop.game.world.WorldObject
import ua.syt0r.kanji.desktop.game.world.WorldStreamer
import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.geom.Rect

/**
 * One game session — the root that owns every system, drives the fixed-step
 * update, handles interactions/dialogue/quest rewards, streams the world and
 * saves/loads. Created once per Game view (a second session would be a
 * second journey).
 */
class GameSession(
    val bridge: GameBridge,
    val content: LoadedContent,
    saveDir: java.io.File? = null
) : DialogueEffectHandler, QuestRewardHandler {

    // ------------------------------------------------------------
    // Engine + world runtime
    // ------------------------------------------------------------
    val engine = GameEngine()
    val world: GameWorld = content.world
    /** Observable so settings UI (toggles, rebinds) reflects changes live. */
    var settings by mutableStateOf(GameSettings())

    /**
     * The assistance level actually in force (spec §68): kid mode pins the
     * world to the [AssistanceLevel.Kids] layer — full reading + translation
     * + simplified support — regardless of what the player picked manually.
     */
    val effectiveAssistance: AssistanceLevel
        get() = if (settings.kidMode) AssistanceLevel.Kids else settings.assistanceLevel
    val clock = WorldClock()
    val weather = WeatherSystem()
    /** World seasons (spec §42) — derived from the clock's day counter. */
    val seasons = SeasonSystem(clock)
    val input = InputManager()
    val camera = Camera()
    val cameraRig = CameraRig(camera, CameraSettings())
    val debug = DebugTools()
    val validator = ContentValidator()
    val saveManager: SaveManager = if (saveDir != null) SaveManager(saveDir) else SaveManager()

    lateinit var tileGrid: TileGrid
        private set

    val npcDirector: NpcDirector
    val interaction = InteractionSystem()
    val quests: QuestManager
    val learning: LearningManager
    val collections: CollectionManager
    val album = PhotoAlbum()
    val story: StoryEngine
    val photoCamera = PhotoCamera()
    val streamer = WorldStreamer(world)
    /** Spoken dialogue (spec §61-62) — kana clips through the bridge. */
    val tts = DialogueTts(bridge)
    /** Procedural SFX + ambient pad (spec §91-92) — zero audio assets. */
    val audio = GameAudio()
    /** Stall ordering flow (spec §56). */
    val order = ua.syt0r.kanji.desktop.game.activity.OrderSession()

    val player: PlayerEntity
    val playerController = PlayerController()
    val dialogue: DialogueRunner
    val renderer = WorldRenderer(this)
    val state: GameState

    /** The interactable the player can currently activate. */
    var currentInteractable by mutableStateOf<Interactable?>(null)
        private set

    var activeSeconds by mutableStateOf(0L)
        private set

    private var autosaveTimer = 0f

    /** The knowledge graph the game navigates (spec §73-75). */
    val knowledgeGraph: KnowledgeGraph = content.knowledgeGraph

    init {
        // Validate content before anything runs (spec §123-126).
        validator.validate(content)
        // The saved scheme/calibration drive the live input manager.
        input.scheme = settings.controlScheme
        input.calibration = settings.inputCalibration

        val defaultSpawn = world.defaultSpawn

        quests = QuestManager(content.quests).apply {
            rewardHandler = this@GameSession
            initProgress()
        }
        learning = LearningManager(knowledgeGraph, bridge)
        collections = CollectionManager(content.collectibles)
        story = StoryEngine(content.stories)
        dialogue = DialogueRunner(
            content.dialogues.associateBy { it.id },
            this,
            knowledgeCheck = { learning.isDiscovered(it) }
        )

        player = PlayerEntity(
            PlayerState(
                regionId = defaultSpawn.regionId,
                cellId = defaultSpawn.cellId,
                position = defaultSpawn.position
            ),
            engine.entities.add(
                ua.syt0r.kanji.desktop.game.engine.Entity("player", defaultSpawn.position.toVec2()).apply {
                    size = Vec2(24f, 32f)
                    solid = true
                }
            )
        )

        npcDirector = NpcDirector(content.npcs)

        state = GameState(
            player = player,
            quests = quests,
            learning = learning,
            collections = collections,
            album = album,
            story = story,
            clock = clock,
            weather = weather,
            settings = settings
        )

        enterRegion(defaultSpawn.regionId, defaultSpawn.cellId)
        engine.currentScene = WorldScene()
        // Restore a previous journey when one exists.
        loadFromSave(saveManager.load())
        camera.snapTo(player.entity.position)
        quests.refreshAvailability()
        maybeAutoStartQuest()
        // Controller support is opt-in per session: connect if a device is
        // present (XInput on Windows, evdev joystick on Linux).
        input.connectGamepad()
    }

    /**
     * Switch the runtime to a region/cell: rebuild the tile grid, camera
     * bounds, region-scoped NPC population and interactables. Used by the
     * initial spawn, train travel, save-load and new game — a second region
     * is content, not code (spec §5-6).
     */
    private fun enterRegion(regionId: String, cellId: String) {
        val cell = world.cell(cellId) ?: error("Region '$regionId' cell missing: $cellId")
        tileGrid = TileGrid(cell)
        camera.bounds = world.regionBounds(regionId)
        // Streaming first: the residency set decides which cells' NPCs exist
        // (spec §96) — leaving a region unloads it, arriving loads it.
        streamer.update(cellId, player.entity.position)
        npcDirector.clear()
        npcDirector.spawn(engine.entities, clock.minuteOfDay, streamer.loaded)
        rebuildInteractables()
    }

    // ------------------------------------------------------------
    // Update loop
    // ------------------------------------------------------------

    /** Called once per host frame by the canvas. */
    fun advance(frameDelta: Float) {
        engine.advance(frameDelta)
    }

    /** Fixed-step tick — internal so tests can drive the loop directly. */
    internal fun tick(dt: Float) {
        syncAudioSettings()
        val busy = state.isBusy()
        debug.updateFrame(
            fps = if (dt > 0f) 1f / engine.loop.fixedDelta else 0f,
            frameTimeMs = dt * 1000f,
            loadedCells = streamer.loaded.size,
            entityCount = engine.entities.entities.size,
            cellId = player.state.cellId,
            regionId = player.state.regionId,
            activeQuests = quests.activeQuests().size
        )

        input.beginFrame()
        input.poll()

        if (busy) {
            // Menus/dialogue freeze the world; the clock keeps real time but
            // no study time accrues and nothing moves.
            clock.tick(dt)
            weather.tick(dt)
            syncSeason()
            handleMenuActions()
            exposeDialogueLearning()
            return
        }

        clock.tick(dt)
        weather.tick(dt)
        syncSeason()
        // Time-window population: festival NPCs appear at dusk, shops close
        // at night. When the population changes, re-register interactables.
        if (npcDirector.tick(
                engine.entities,
                clock.minuteOfDay,
                engine.loop.fixedDelta,
                weather.current.name,
                seasons.current.name
            )
        ) {
            rebuildInteractables()
        }
        // Moving NPCs change position — keep their talk hitboxes live so the
        // world stays honest while the stall-holder walks to their stall.
        if (npcDirector.allNpcs().any { it.isMoving }) {
            refreshNpcInteractables()
        }

        if (state.photoMode) {
            // The viewfinder is a mode of its own: mouse-look moves the
            // focus, capture takes the shot, Back/Esc/C exits.
            updatePhotoMode(dt)
            camera.decayShake(dt)
            return
        }

        playerController.update(player, tileGrid, input, dt, solidRects())

        // Active-time accounting (spec §67): only real play counts — the
        // player must have moved recently or interacted; menus/AFK never add.
        if (player.entity.velocity.lengthSquared() > 1f) {
            activeSeconds += 1
        }

        // Camera collision (spec §30): resolve the follow point against the
        // cell's solid buildings so the lens never ends up inside one.
        cameraRig.follow(player.entity.position, dt, solidRects())
        camera.decayShake(dt)

        updateLocationDiscovery()
        // Keep residency tight: load the neighbourhood, evict the far cells.
        streamer.update(player.state.cellId, player.entity.position)
        streamer.unloadDistant()
        currentInteractable = interaction.nearest(player.entity.position)
        queuePendingDiscoveries()
        maybeAutoStartQuest()

        handleWorldActions()

        // Autosave (spec §97) — quiet and versioned.
        autosaveTimer += dt
        if (autosaveTimer >= settings.autoSaveMinutes * 60f) {
            autosaveTimer = 0f
            save()
        }
    }

    private fun handleWorldActions() {
        val inputState = input.state
        if (inputState.wasPressedThisFrame(InputAction.Interact)) {
            val result = interaction.interact(player.entity.position)
            handleInteraction(result)
        }
        if (inputState.wasPressedThisFrame(InputAction.PhotoMode)) {
            togglePhotoMode()
        }
        if (inputState.wasPressedThisFrame(InputAction.SwitchCamera)) {
            camera.mode = if (camera.mode == CameraMode.ThirdPerson) CameraMode.FirstPerson else CameraMode.ThirdPerson
            bridge.toast("Camera: ${camera.mode.name}", BridgeToastKind.Info)
        }
        if (inputState.wasPressedThisFrame(InputAction.ZoomIn)) cameraRig.zoomBy(0.5f)
        if (inputState.wasPressedThisFrame(InputAction.ZoomOut)) cameraRig.zoomBy(-0.5f)
        if (inputState.wasPressedThisFrame(InputAction.ToggleDebug)) debug.toggle()
        if (!state.photoMode) {
            if (inputState.wasPressedThisFrame(InputAction.OpenMenu)) state.menuOpen = true
            if (inputState.wasPressedThisFrame(InputAction.OpenMap)) state.mapOpen = true
            if (inputState.wasPressedThisFrame(InputAction.OpenQuests)) state.questLogOpen = true
            if (inputState.wasPressedThisFrame(InputAction.OpenCollection)) state.collectionOpen = true
        }
    }

    /** Menu-key handling while a panel is open. */
    private fun handleMenuActions() {
        if (input.state.wasPressedThisFrame(InputAction.Back) || input.state.wasPressedThisFrame(InputAction.OpenMenu)) {
            state.closeAllPanels()
            state.photoMode = false
        }
    }

    /** Listening practice (spec §61): NPC lines expose their words once. */
    private var lastDialogueLineId = ""

    private fun exposeDialogueLearning() {
        if (!dialogue.isActive) {
            lastDialogueLineId = ""
            return
        }
        val line = dialogue.currentLine ?: return
        if (line.id != lastDialogueLineId) {
            lastDialogueLineId = line.id
            // Listening quests (spec §21, §61) complete through presented lines.
            quests.reportEvent(QuestEvent.Listen(line.id))
            discoverAll(line.learningTargets, DiscoverySource.Npc)
            // Relationship depth (spec §53): talking about an NPC's favorite
            // topic deepens the bond — same words, deeper friendship.
            if (line.speakerId.isNotBlank()) {
                val npc = npcDirector.npc(line.speakerId)
                if (npc != null && npc.relationship.favoriteTopics.any { it in line.learningTargets }) {
                    npc.relationship.affinity++
                }
            }
        }
    }

    /** Track weather changes so weather quests can complete (spec §41). */
    private var lastWeather: String = weather.current.name

    /** Keep the season in sync with the clock and report changes (quests). */
    private fun syncSeason() {
        // Pacing is a live setting (spec §40): fast while playing, real
        // time if the player prefers.
        clock.secondsPerWorldMinute = settings.secondsPerWorldMinute
        if (seasons.sync()) {
            // The season leans on the weather (spec §42): winter snows,
            // spring rains — seasonal quests become reachable.
            weather.seasonalWeather = seasons.seasonalWeather()
            quests.reportEvent(QuestEvent.SeasonChange(seasons.current))
            // The ambient pad is coloured by the season (spec §42, §91-92):
            // spring birdsong, summer cicadas, autumn leaves, winter wind.
            audio.setSeason(seasons.current.toSeasonAudio)
            bridge.toast("The season changed — ${seasons.current.label}!", BridgeToastKind.Info)
        }
        if (weather.current.name != lastWeather) {
            lastWeather = weather.current.name
            quests.reportEvent(QuestEvent.WeatherChange(weather.current))
        }
    }

    // ------------------------------------------------------------
    // Debug tools (spec §121-122) — season/weather/time forcing
    // ------------------------------------------------------------

    /** Jump the world to [season] — dev/test helper, gated behind [debug]. */
    fun debugForceSeason(season: ua.syt0r.kanji.desktop.game.time.Season) {
        val current = seasons.current
        if (current == season) {
            bridge.toast("Already ${season.label}", BridgeToastKind.Info)
            return
        }
        // Set the day counter to the first day of the requested season.
        val cycle = seasons.cycleDays
        clock.day = 1 + season.ordinal * cycle
        seasons.sync()
        weather.seasonalWeather = seasons.seasonalWeather()
        quests.reportEvent(QuestEvent.SeasonChange(season))
        npcDirector.tick(
            engine.entities, clock.minuteOfDay, engine.loop.fixedDelta,
            weather.current.name, season.name
        )
        rebuildInteractables()
        bridge.toast("Season forced: ${season.label}", BridgeToastKind.Success)
    }

    /** Force a weather kind — dev/test helper, gated behind [debug]. */
    fun debugForceWeather(weatherKind: ua.syt0r.kanji.desktop.game.time.WeatherKind) {
        weather.setWeather(weatherKind)
        lastWeather = weatherKind.name
        quests.reportEvent(QuestEvent.WeatherChange(weatherKind))
        npcDirector.tick(
            engine.entities, clock.minuteOfDay, engine.loop.fixedDelta,
            weatherKind.name, seasons.current.name
        )
        rebuildInteractables()
        bridge.toast("Weather forced: ${weatherKind.label}", BridgeToastKind.Success)
    }

    /** Jump the clock to a phase boundary — dev/test helper. */
    fun debugSetTime(minuteOfDay: Int) {
        clock.minuteOfDay = minuteOfDay.coerceIn(0, 1439)
        npcDirector.tick(
            engine.entities, clock.minuteOfDay, engine.loop.fixedDelta,
            weather.current.name, seasons.current.name
        )
        rebuildInteractables()
        bridge.toast("Time set: ${clock.hourLabel()}", BridgeToastKind.Info)
    }

    /**
     * Teleport the player to a world position — dev/test helper (spec §121).
     * Repopulates the visible region so the destination looks right.
     */
    fun debugTeleport(x: Float, y: Float) {
        player.entity.teleport(ua.syt0r.kanji.desktop.game.engine.geom.Vec2(x, y))
        player.entity.velocity = ua.syt0r.kanji.desktop.game.engine.geom.Vec2.Zero
        val cell = world.cell(player.state.cellId)
        if (cell != null) {
            streamer.update(player.state.cellId, player.entity.position)
            npcDirector.tick(
                engine.entities, clock.minuteOfDay, engine.loop.fixedDelta,
                weather.current.name, seasons.current.name
            )
            rebuildInteractables()
        }
        camera.snapTo(player.entity.position)
        bridge.toast("Teleported to (${x.toInt()}, ${y.toInt()})", BridgeToastKind.Info)
    }

    /** Teleport to a named location's anchor — dev/test helper. */
    fun debugTeleportToLocation(locationId: String) {
        val location = world.location(locationId)
        if (location == null) {
            bridge.toast("Unknown location: $locationId", BridgeToastKind.Info)
            return
        }
        debugTeleport(location.anchor.x, location.anchor.y)
        bridge.toast("Teleported to ${location.name}", BridgeToastKind.Success)
    }

    /** Live audio mirror of [settings] + region ambient (cheap, 60 Hz). */
    private fun syncAudioSettings() {
        audio.configure(settings.sfxVolume, settings.musicVolume, settings.ambientEnabled)
        audio.setSeason(seasons.current.toSeasonAudio)
        audio.setAmbient(ambientForRegion())
    }

    private fun ambientForRegion(): AmbientKind? = when (world.region(player.state.regionId)?.theme) {
        "historic" -> AmbientKind.HistoricTown
        "beach" -> AmbientKind.Beach
        "station" -> AmbientKind.Station
        else -> AmbientKind.SeasideTown
    }

    /** Whether the phase/season/weather-gated object behind [result] is open. */
    private fun objectGateOpen(result: InteractionResult): Boolean {
        val objectId = when (result) {
            is InteractionResult.ReadObject -> result.objectId
            is InteractionResult.BuyDrink -> result.objectId
            is InteractionResult.SitBench -> result.objectId
            is InteractionResult.PhotoSpot -> result.objectId
            is InteractionResult.WriteKana -> result.objectId
            else -> return true
        }
        val interactable = interaction.interactable("obj:$objectId")
        if (interactable == null) return true
        return interactable.isOpenAt(clock.phase.name) &&
            interactable.isOpenInSeason(seasons.current.name) &&
            interactable.isOpenInWeather(weather.current.name)
    }

    /** HUD helper — is the current prompt's object open (spec §40)? */
    fun isCurrentInteractableOpen(): Boolean {
        val target = currentInteractable ?: return true
        return target.isOpenAt(clock.phase.name) &&
            target.isOpenInSeason(seasons.current.name) &&
            target.isOpenInWeather(weather.current.name)
    }

    internal fun handleInteraction(result: InteractionResult) {
        if (!objectGateOpen(result)) {
            audio.play(Sfx.Boop)
            val gate = when {
                currentInteractable?.isOpenInSeason(seasons.current.name) != true -> "not this season"
                currentInteractable?.isOpenInWeather(weather.current.name) != true -> "not in this weather"
                else -> "at this time — try again later"
            }
            bridge.toast("Closed $gate", BridgeToastKind.Info)
            return
        }
        when (result) {
            is InteractionResult.StartDialogue -> {
                audio.play(Sfx.Blip)
                val npc = npcDirector.npc(result.npcId)
                if (npc == null) {
                    bridge.toast("Nobody here right now…", BridgeToastKind.Info)
                    return
                }
                npc.relationship.met = true
                npc.relationship.talkedCount++
                quests.reportEvent(QuestEvent.TalkToNpc(result.npcId))
                val dialogueId = npc.definition.dialogueId
                if (dialogueId != null) {
                    dialogue.start(dialogueId)
                    state.dialogueOpen = true
                } else {
                    bridge.toast("${npc.definition.name} waves hello. (${npc.definition.idleLineJp})", BridgeToastKind.Info)
                }
            }
            is InteractionResult.ReadObject -> {
                state.worldState = state.worldState.copy(discoveredObjects = (state.worldState.discoveredObjects + result.objectId).distinct())
                quests.reportEvent(QuestEvent.ReadSign(result.objectId))
                if (result.label.isNotBlank()) {
                    bridge.toast("「${result.label}」 ${result.learningTargets.size} new words", BridgeToastKind.Success)
                }
                discoverAll(result.learningTargets, DiscoverySource.Object)
            }
            is InteractionResult.BuyDrink -> buyDrink(result.objectId)
            is InteractionResult.BuyFood -> buyFood(result.objectId)
            is InteractionResult.OrderFood -> openOrder(result.objectId)
            is InteractionResult.SitBench -> {
                state.dialogueOpen = true
                dialogue.start("sit-bench")
            }
            is InteractionResult.OpenTravel -> {
                audio.play(Sfx.Blip)
                state.travelOpen = true
            }
            is InteractionResult.PhotoSpot -> {
                audio.play(Sfx.Blip)
                // A photo spot teaches its subject before you shoot (spec
                // §19): 海の写真を撮ろう at the beach teaches 海, and the
                // shot itself tags whatever ends up in frame.
                world.allObjects().firstOrNull { it.id == result.objectId }
                    ?.let { discoverAll(effectiveTargets(it), DiscoverySource.Object) }
                togglePhotoMode()
            }
            is InteractionResult.WriteKana -> {
                openWriting(result.targets)
            }
            is InteractionResult.None -> {
                audio.play(Sfx.Boop)
                bridge.toast("Nothing to interact with here", BridgeToastKind.Info)
            }
        }
    }

    private fun buyDrink(objectId: String) {
        // The vending machine teaches through use (spec §9-10): its own
        // learning targets (飲み物/水/お茶/ジュース) are discovered here.
        audio.play(Sfx.Coin)
        val obj = world.allObjects().firstOrNull { it.id == objectId }
        discoverAll(obj?.let { effectiveTargets(it) }.orEmpty(), DiscoverySource.Object)
        shake(1f)
        val drinks = listOf("DrinkWater", "DrinkTea", "DrinkJuice")
        val picked = drinks.random()
        giveItem(picked)
        quests.reportEvent(QuestEvent.BuyItem("drink"))
        bridge.recordActivity(GameActivityKind.WordDiscovered, "Bought a drink from the vending machine")
        bridge.toast("Buying 飲み物 … got a drink!", BridgeToastKind.Success)
    }

    /**
     * Open the stall's Japanese menu (spec §56). The menu is data-driven:
     * items are (id, nameJp, reading, meaning, knowledgeId) pairs — the
     * player orders from Japanese, not from a translation.
     */
    fun openOrder(objectId: String) {
        val obj = world.allObjects().firstOrNull { it.id == objectId }
        val menu = stallMenuFor(obj?.id.orEmpty())
        if (menu.isEmpty()) {
            bridge.toast("この屋台は閉まっています — this stall is closed", BridgeToastKind.Info)
            audio.play(Sfx.Boop)
            return
        }
        order.open(objectId, menu)
        state.orderStallId = objectId
        state.orderOpen = true
        audio.play(Sfx.Blip)
    }

    /** Menu for a stall object; content mirrors the knowledge nodes (JSON). */
    private fun stallMenuFor(stallId: String): List<ua.syt0r.kanji.desktop.game.activity.MenuItem> {
        val base = when (stallId) {
            "stall-takoyaki" -> listOf(
                menuItem("takoyaki", "たこ焼き", "takoyaki", "octopus balls", "takoyaki"),
                menuItem("soda", "ラムネ", "ramune", "Japanese lemon soda", "ramune")
            )
            "stall-taiyaki" -> listOf(
                menuItem("taiyaki", "たい焼き", "taiyaki", "fish-shaped cake", "taiyaki"),
                menuItem("yakisoba", "焼きそば", "yakisoba", "fried noodles", "yakisoba")
            )
            else -> emptyList()
        }
        return base
    }

    private fun menuItem(
        id: String,
        nameJp: String,
        reading: String,
        meaning: String,
        knowledgeId: String
    ) = ua.syt0r.kanji.desktop.game.activity.MenuItem(
        id = id,
        nameJp = nameJp,
        reading = reading,
        meaning = meaning,
        price = 300,
        knowledgeId = knowledgeId
    )

    /** The player picked an item off the stall menu — complete the order. */
    fun chooseOrderItem(itemId: String) {
        val stallId = state.orderStallId ?: return
        val item = order.order(itemId) ?: return
        audio.play(Sfx.Coin)
        // The item's words are learned through use (spec §9-10, §56).
        item.knowledgeId?.let { discoverWord(it, DiscoverySource.Object) }
        giveItem(item.id)
        quests.reportEvent(QuestEvent.OrderFood(item.id, stallId))
        bridge.recordActivity(GameActivityKind.WordDiscovered, "Ordered ${item.nameJp} from the stall")
        bridge.toast("注文完了！ ${item.nameJp} (${item.meaning}) — order complete!", BridgeToastKind.Success)
    }

    /** Leave the stall (order stays recorded, menu closes). */
    fun closeOrder() {
        order.close()
        state.orderOpen = false
        state.orderStallId = null
    }

    /** Buy from a festival stall — teaches through use (spec §9-10, §56). */
    private fun buyFood(objectId: String) {
        audio.play(Sfx.Coin)
        val obj = world.allObjects().firstOrNull { it.id == objectId }
        discoverAll(obj?.let { effectiveTargets(it) }.orEmpty(), DiscoverySource.Object)
        shake(1f)
        giveItem(obj?.label?.ifBlank { "Festival food" } ?: "Festival food")
        quests.reportEvent(QuestEvent.BuyItem("takoyaki"))
        bridge.recordActivity(GameActivityKind.WordDiscovered, "Bought ${obj?.label ?: "festival food"} from a stall")
        bridge.toast("Bought ${obj?.label ?: "festival food"} from the stall!", BridgeToastKind.Success)
    }

    private fun updatePhotoMode(dt: Float) {
        if (!state.photoMode) return
        val look = input.state.lookDelta
        photoCamera.focus = Vec2(
            photoCamera.focus.x + look.x * 3f,
            photoCamera.focus.y + look.y * 3f
        )
        if (input.state.wasPressedThisFrame(InputAction.PhotoCapture)) {
            capturePhoto()
        }
        if (input.state.wasPressedThisFrame(InputAction.Back) ||
            input.state.wasPressedThisFrame(InputAction.OpenMenu) ||
            input.state.wasPressedThisFrame(InputAction.PhotoMode)
        ) {
            state.photoMode = false
            photoCamera.exit()
            camera.mode = settings.defaultCameraMode
        }
    }

    /** Public capture entry (photo-mode button). */
    fun capturePhotoPublic() {
        if (state.photoMode) capturePhoto()
    }

    private fun capturePhoto() {
        val region = photoCamera.frameRect()
        val subjects = photoSubjectsIn(region)
        val photo = photoCamera.capture(
            frameObjects = subjects,
            locationId = state.worldState.discoveredLocations.lastOrNull(),
            nowIso = Clock.System.now().toString(),
            regionId = player.state.regionId
        )
        album.add(photo)
        audio.play(Sfx.Shutter)
        shake(3f)
        state.stats = state.stats.copy(photosTaken = state.stats.photosTaken + 1)
        quests.reportEvent(QuestEvent.TakePhoto(1))
        bridge.recordActivity(GameActivityKind.PhotoTaken, "Took a photo with ${photo.tags.size} tagged words")
        // Photo tags are discoveries (spec §45).
        for (tag in photo.tags) {
            discoverWord(tag.knowledgeId, DiscoverySource.Photo)
        }
        bridge.toast(
            if (photo.tags.isEmpty()) "Photo saved (no vocabulary found in frame)"
            else "Photo saved — found ${photo.tags.size} vocabulary items!",
            if (photo.tags.isEmpty()) BridgeToastKind.Info else BridgeToastKind.Success
        )
    }

    /** Export a photo (with its vocabulary tags) to the user's disk. */
    fun savePhotoToDisk(photo: ua.syt0r.kanji.desktop.game.photography.Photo) {
        val ok = bridge.savePhotoToDisk(
            ua.syt0r.kanji.desktop.game.bridge.BridgePhoto(
                id = photo.id,
                title = photo.title,
                category = photo.category.name,
                regionId = photo.regionId,
                locationId = photo.locationId,
                takenAt = photo.takenAt,
                tags = photo.tags.map {
                    ua.syt0r.kanji.desktop.game.bridge.BridgePhotoTag(
                        headword = it.headword,
                        reading = it.reading,
                        meaning = it.meaning
                    )
                }
            )
        )
        audio.play(if (ok) Sfx.Shutter else Sfx.Boop)
        bridge.toast(
            if (ok) "Photo saved to ~/.kaiteyo/game/photos/"
            else "Couldn't save the photo to disk.",
            if (ok) BridgeToastKind.Success else BridgeToastKind.Warning
        )
    }

    /** Remove a photo from the album (photos are moments, not inventory). */
    fun deletePhoto(photoId: String) {
        val removed = album.photos.firstOrNull { it.id == photoId } ?: return
        state.albumOpen = false
        // Remove from the live album via a snapshot-free approach: rebuild.
        album.restore(ua.syt0r.kanji.desktop.game.photography.AlbumData(photos = album.photos.filterNot { it.id == photoId }))
        state.photoDetail = null
        audio.play(Sfx.Blip)
        bridge.toast("Deleted photo ${removed.title}", BridgeToastKind.Info)
    }

    private fun photoSubjectsIn(region: Rect): List<PhotoSubject> {
        val subjects = mutableListOf<PhotoSubject>()
        for (obj in world.allObjects()) {
            val pos = obj.position.toVec2()
            if (region.contains(pos)) {
                val node = knowledgeGraph.nodeByHeadword(obj.label)
                subjects.add(
                    PhotoSubject(
                        id = obj.id,
                        position = pos,
                        label = obj.label,
                        knowledgeNode = node
                    )
                )
            }
        }
        for (npc in npcDirector.allNpcs()) {
            val pos = npc.entity.position
            if (region.contains(pos)) {
                val node = knowledgeGraph.nodeByHeadword(npc.definition.name)
                subjects.add(
                    PhotoSubject(
                        id = npc.id,
                        position = pos,
                        label = npc.definition.name,
                        knowledgeNode = node
                    )
                )
            }
        }
        return subjects
    }

    private fun updateLocationDiscovery() {
        val region = world.region(player.state.regionId) ?: return
        for (location in region.locations) {
            if (location.id in state.worldState.discoveredLocations) continue
            val anchor = location.anchor.toVec2()
            if (anchor.distanceTo(player.entity.position) <= location.radius) {
                state.worldState = state.worldState.copy(
                    discoveredLocations = (state.worldState.discoveredLocations + location.id).distinct()
                )
                state.stats = state.stats.copy(locationsDiscovered = state.worldState.discoveredLocations.size)
                collections.unlock("loc:${location.id}")
                quests.reportEvent(QuestEvent.DiscoverLocation(location.id))
                quests.reportEvent(QuestEvent.ReachLocation(location.id))
                bridge.recordActivity(GameActivityKind.LocationDiscovered, "Discovered ${location.name} (${location.nameJp})")
                bridge.toast("Discovered: ${location.name} ${location.nameJp}", BridgeToastKind.Success)
                discoverAll(location.learningTargets, DiscoverySource.Environment)
            }
        }
    }

    // ------------------------------------------------------------
    // Dialogue effects (implemented by the session — the engine only emits)
    // ------------------------------------------------------------

    override fun handle(effect: DialogueEffect) {
        when (effect.kind) {
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.GrantQuest ->
                quests.start(effect.targetId)
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.StartStory ->
                story.start(effect.targetId)
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.DiscoverKnowledge ->
                discoverWord(effect.targetId, DiscoverySource.Npc)
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.SetFlag ->
                state.worldState = state.worldState.copy(flags = state.worldState.flags + (effect.targetId to "true"))
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.GiveItem ->
                giveItem(effect.targetId)
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.MarkNpcMet -> {
                // NPCs are marked met on first talk already; idempotent here.
            }
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.OpenShop -> {
                bridge.toast("The shop is open — quests and drinks live here.", BridgeToastKind.Info)
            }
            ua.syt0r.kanji.desktop.game.dialogue.DialogueEffectKind.AdvanceStory ->
                story.advance()
        }
    }

    // ------------------------------------------------------------
    // Quest rewards (implemented by the session)
    // ------------------------------------------------------------

    override fun onQuestComplete(quest: Quest) {
        state.completedQuest = quest
        audio.play(Sfx.Chime)
        player.state = player.state.addXp(quest.rewards.xp)
        state.stats = state.stats.copy(
            questsCompleted = state.stats.questsCompleted + 1,
            xpEarned = state.stats.xpEarned + quest.rewards.xp
        )
        for (item in quest.rewards.items) {
            giveItem(item)
        }
        for (cosmetic in quest.rewards.cosmetics) {
            player.state = player.state.copy(cosmetics = (player.state.cosmetics + cosmetic).distinct())
        }
        for (stamp in quest.rewards.stamps) {
            collections.unlock(stamp)
        }
        // Unlock ids: quests (availability handled by the manager), locations,
        // stations ("station:…") and knowledge nodes.
        for (unlock in quest.rewards.unlocks) {
            when {
                unlock.startsWith("station:") -> {
                    state.worldState = state.worldState.copy(
                        flags = state.worldState.flags + ("travel:${unlock.removePrefix("station:")}" to "true")
                    )
                }
                world.location(unlock) != null -> {
                    state.worldState = state.worldState.copy(
                        discoveredLocations = (state.worldState.discoveredLocations + unlock).distinct()
                    )
                    collections.unlock("loc:$unlock")
                }
                else -> {} // quest ids unlock through refreshAvailability()
            }
        }
        for (knowledgeId in quest.rewards.knowledge) {
            discoverWord(knowledgeId, DiscoverySource.Quest)
        }
        bridge.recordActivity(GameActivityKind.QuestCompleted, "Completed quest \"${quest.title}\"")
        bridge.toast("Quest complete: ${quest.title}!", BridgeToastKind.Success)
        quests.refreshAvailability()
    }

    // ------------------------------------------------------------
    // Interactable construction (content → runtime)
    // ------------------------------------------------------------

    private fun rebuildInteractables() {
        interaction.clear()
        val cell = world.cell(player.state.cellId) ?: return
        for (obj in cell.objects) {
            registerObjectInteractable(obj)
        }
        refreshNpcInteractables()
        interaction.rebuild()
    }

    /** Re-register NPC talk hitboxes at their live positions. */
    private fun refreshNpcInteractables() {
        for (npc in npcDirector.allNpcs()) {
            interaction.register(
                Interactable(
                    id = "talk:${npc.id}",
                    position = npc.entity.position,
                    radius = 56f,
                    promptJp = "話す",
                    promptEn = "Talk",
                    behavior = InteractionBehavior.Talk(npc.id)
                )
            )
        }
    }

    private fun registerObjectInteractable(obj: WorldObject) {
        val behavior: InteractionBehavior? = when (obj.interactableId) {
            null -> null
            "read" -> InteractionBehavior.ReadObject(obj.id, obj.label, effectiveTargets(obj))
            "buy-drink" -> InteractionBehavior.BuyDrink(obj.id)
            "buy-food" -> InteractionBehavior.BuyFood(obj.id)
            "order-food" -> InteractionBehavior.OrderFood(obj.id)
            "sit" -> InteractionBehavior.SitBench(obj.id)
            "photo-spot" -> InteractionBehavior.PhotoSpot(obj.id)
            "write" -> InteractionBehavior.WriteKana(obj.id, effectiveTargets(obj))
            else -> if (obj.interactableId.startsWith("station:")) {
                InteractionBehavior.OpenStation(obj.interactableId.removePrefix("station:"))
            } else {
                InteractionBehavior.Inspect(obj.id, effectiveTargets(obj))
            }
        }
        if (behavior == null) return
        interaction.register(
            Interactable(
                id = "obj:${obj.id}",
                position = obj.position.toVec2(),
                radius = 64f,
                promptJp = obj.label.ifBlank { "調べる" },
                promptEn = "Inspect",
                behavior = behavior,
                worldObject = obj,
                availablePhases = obj.availablePhases,
                availableSeasons = obj.availableSeasons,
                availableWeather = obj.availableWeather
            )
        )
    }

    /**
     * Give the player an item and report it to collection quests (spec §8).
     * Every item gain funnels through here so Collect objectives see real
     * inventory changes — the same single path as discoveries.
     */
    private fun giveItem(itemId: String) {
        player.state = player.state.addItem(itemId)
        quests.reportEvent(QuestEvent.CollectItem(itemId))
    }

    /**
     * The vocabulary an object teaches right now (spec §7): in kid mode the
     * object's simpler [WorldObject.kidTargets] layer replaces the default
     * one — the environment adapts to the player instead of the reverse.
     */
    private fun effectiveTargets(obj: WorldObject): List<String> =
        if (settings.kidMode && obj.kidTargets.isNotEmpty()) obj.kidTargets else obj.learningTargets

    /**
     * Small camera kick for satisfying moments (photo shutter, discovery,
     * purchase). Honored off entirely with reduced motion (spec §116).
     */
    private fun shake(mag: Float) {
        if (settings.reducedMotion) return
        camera.addShake(
            Vec2(
                (Math.random() * 2 - 1).toFloat() * mag,
                (Math.random() * 2 - 1).toFloat() * mag
            )
        )
    }

    /** Discover one word and progress word-learning objectives (single path). */
    private fun discoverWord(target: String, source: DiscoverySource) {
        if (learning.discover(target, effectiveAssistance, source, player.entity.position) != null) {
            // A little kick so a new word landing feels like something.
            shake(1.5f)
            // Word-learning objectives progress through real discoveries.
            quests.reportEvent(QuestEvent.LearnWord(target))
        }
    }

    private fun discoverAll(targets: List<String>, source: DiscoverySource) {
        for (target in targets) {
            discoverWord(target, source)
        }
    }

    /**
     * Guided momentum (spec §36): when nothing is active and a quest is
     * available, surface it — the slice never leaves the player stuck.
     */
    private fun maybeAutoStartQuest() {
        if (quests.activeQuests().isEmpty()) {
            quests.availableQuests().firstOrNull()?.let { quests.start(it.id) }
        }
    }

    // ------------------------------------------------------------
    // Story (spec §54-55) — the UI drives chapters/scenes through the
    // engine; scene effects (dialogue, quest, knowledge) land here.
    // ------------------------------------------------------------

    /** Begin a story and play its first scene. */
    fun startStory(storyId: String) {
        playStoryScene(story.start(storyId))
    }

    /** Advance the active story to its next scene (or finish it). */
    fun advanceStory() {
        val next = story.advance()
        playStoryScene(next)
        if (next == null) {
            bridge.toast("Story complete!", BridgeToastKind.Success)
        }
    }

    /**
     * The player picked branch [index] on the active story scene (spec §55).
     * The choice's own effects — a different quest, extra knowledge, a world
     * flag — land before the continuation scene plays, so picking "watch the
     * fireworks to the end" genuinely changes what happens next.
     */
    fun chooseStory(index: Int) {
        val next = story.choose(index)
        val choice = story.lastChoice
        choice?.questTrigger?.let { questId ->
            if (quests.start(questId)) {
                bridge.toast("New quest: ${quests.quest(questId)?.title ?: questId}", BridgeToastKind.Success)
            }
        }
        choice?.grants?.forEach { discoverWord(it, DiscoverySource.Npc) }
        choice?.setFlag?.takeIf { it.isNotBlank() }?.let { flag ->
            state.worldState = state.worldState.copy(flags = state.worldState.flags + (flag to "true"))
        }
        playStoryScene(next)
        if (next == null) {
            bridge.toast("Story complete!", BridgeToastKind.Success)
        }
    }

    private fun playStoryScene(scene: ua.syt0r.kanji.desktop.game.story.StoryScene?) {
        if (scene == null) return
        // Dialogue first — it is the scene's presentation. Close any open
        // panel so the dialogue is the focus; it closes itself when done.
        scene.dialogueId?.let { id ->
            dialogue.start(id)
            state.dialogueOpen = true
            state.storyOpen = false
        }
        // Then its world effects.
        scene.questTrigger?.let { questId ->
            if (quests.start(questId)) {
                bridge.toast("New quest: ${quests.quest(questId)?.title ?: questId}", BridgeToastKind.Success)
            }
        }
        for (grant in scene.grants) {
            discoverWord(grant, DiscoverySource.Npc)
        }
    }

    // ------------------------------------------------------------
    // Save slots (spec §97) — one journey per slot
    // ------------------------------------------------------------

    var activeSlot by mutableStateOf("slot-1")
        private set

    /** Save the current journey into [slot] and make it active. */
    fun saveToSlot(slot: String) {
        activeSlot = slot
        save()
    }

    /** Start a completely fresh journey in [slot]. */
    fun newJourneyIn(slot: String) {
        activeSlot = slot
        newGame()
        bridge.toast("New journey in $slot", BridgeToastKind.Info)
    }

    /** Load the journey in [slot] (or start fresh if empty). */
    fun loadSlot(slot: String) {
        activeSlot = slot
        val saved = saveManager.load(slot)
        if (saved == null) {
            newGame()
            bridge.toast("New journey in $slot", BridgeToastKind.Info)
        } else {
            loadFromSave(saved)
            bridge.toast("Loaded $slot", BridgeToastKind.Success)
        }
    }

    fun deleteSlot(slot: String) {
        saveManager.delete(slot)
        if (activeSlot == slot) activeSlot = "slot-1"
    }

    fun slotSavedAt(slot: String): String? = saveManager.load(slot)?.savedAt

    /** Whether [slot] has an existing journey. */
    fun slotExists(slot: String): Boolean = saveManager.load(slot) != null

    // ------------------------------------------------------------
    // Travel (spec §47-48)
    // ------------------------------------------------------------

    fun isTravelUnlocked(stationId: String): Boolean {
        // The home station (where the journey starts) is always open — you
        // arrived there. Everything else unlocks through quest rewards.
        val station = world.station(stationId) ?: return false
        if (station.cellId == world.defaultSpawn.cellId) return true
        return state.worldState.flags["travel:$stationId"] == "true"
    }

    fun travelTo(stationId: String) {
        val station = world.station(stationId)
        if (station == null) {
            bridge.toast("That station doesn't exist yet.", BridgeToastKind.Warning)
            return
        }
        if (!isTravelUnlocked(stationId)) {
            bridge.toast("The train to ${station.name} isn't available yet — keep exploring!", BridgeToastKind.Info)
            return
        }
        // The train is a real world transition: arrive at the destination
        // station's cell and rebuild the region runtime (spec §47-48).
        val destRegionId = world.regionIdForCell(station.cellId)
        if (destRegionId == null) {
            bridge.toast("The train to ${station.name} is still on the rails… (region not built yet)", BridgeToastKind.Info)
            return
        }
        quests.reportEvent(QuestEvent.RideTrain(stationId))
        audio.play(Sfx.Train)
        bridge.recordActivity(GameActivityKind.LocationDiscovered, "Rode the train to ${station.name} (${station.nameJp})")
        bridge.toast("Boarding the train to ${station.name} (${station.nameJp})…", BridgeToastKind.Success)

        player.state = player.state.copy(
            regionId = destRegionId,
            cellId = station.cellId,
            position = station.position
        )
        player.applyStateToEntity()
        state.worldState = state.worldState.copy(
            regionId = destRegionId,
            cellId = station.cellId
        )
        enterRegion(destRegionId, station.cellId)
        camera.snapTo(player.entity.position)
        clock.advanceMinutes(station.arrivalDelayMinutes)
        bridge.toast("Arrived at ${station.name}  ${station.nameJp}", BridgeToastKind.Success)
        // Guided momentum: destinations can carry their own arrival story
        // (data-driven — see Station.arrivalStoryId).
        station.arrivalStoryId?.let { storyId ->
            if (story.story(storyId) != null && story.progress.value.none { it.storyId == storyId }) {
                startStory(storyId)
            }
        }
    }

    // ------------------------------------------------------------
    // In-world writing (spec §57-59) — kana tracing at the writing desk
    // ------------------------------------------------------------

    /** Index into [GameState.writingTargets] for the current trace target. */
    var writingIndex by mutableStateOf(0)
        private set

    /** Open the writing desk for [targets] (knowledge node ids, in order). */
    fun openWriting(targets: List<String>) {
        if (targets.isEmpty()) {
            bridge.toast("Nothing to write here yet", BridgeToastKind.Info)
            return
        }
        state.writingTargets = targets
        writingIndex = 0
        state.writingOpen = true
        audio.play(Sfx.Blip)
    }

    /** The knowledge node the desk currently wants traced. */
    fun currentWritingTarget(): KnowledgeNode? {
        val id = state.writingTargets.getOrNull(writingIndex) ?: return null
        return knowledgeGraph.node(id)
    }

    /** A trace passed: reward it, then move to the next target. */
    fun completeWriting(targetId: String) {
        discoverWord(targetId, DiscoverySource.Writing)
        quests.reportEvent(QuestEvent.WriteKana(targetId))
        player.state = player.state.addXp(6)
        state.stats = state.stats.copy(xpEarned = state.stats.xpEarned + 6)
        audio.play(Sfx.WriteOk)
        bridge.recordActivity(
            GameActivityKind.WritingCompleted,
            "Traced ${currentWritingTarget()?.headword ?: targetId}"
        )
        bridge.toast("Well written!  ${currentWritingTarget()?.headword ?: ""}", BridgeToastKind.Success)
        if (!advanceWritingTarget()) {
            bridge.toast("Writing practice complete!", BridgeToastKind.Success)
        }
    }

    /** A trace didn't pass — the desk stays open, gently. */
    fun failedWriting() {
        audio.play(Sfx.Boop)
    }

    /** Move to the next target; false when the desk is done. */
    private fun advanceWritingTarget(): Boolean {
        writingIndex++
        if (writingIndex >= state.writingTargets.size) {
            state.writingOpen = false
            return false
        }
        return true
    }

    // ------------------------------------------------------------
    // Photo mode entry
    // ------------------------------------------------------------

    /** Collision rects of solid world objects in the current cell. */
    private fun solidRects(): List<Rect> {
        val cell = world.cell(player.state.cellId) ?: return emptyList()
        return cell.objects
            .filter { it.solid }
            .map { obj ->
                Rect(
                    obj.position.x - obj.size.x / 2f,
                    obj.position.y - obj.size.y,
                    obj.size.x,
                    obj.size.y
                )
            }
    }

    fun togglePhotoMode() {
        state.photoMode = !state.photoMode
        if (state.photoMode) {
            photoCamera.enter()
            photoCamera.focus = player.entity.position
            camera.mode = CameraMode.FirstPerson
            camera.zoom = camera.zoom.coerceAtLeast(1.2f)
        } else {
            photoCamera.exit()
            camera.mode = settings.defaultCameraMode
        }
    }

    // ------------------------------------------------------------
    // Discovery popups (queue shown by the UI, one at a time)
    // ------------------------------------------------------------

    private fun queuePendingDiscoveries() {
        val event = learning.pendingDiscovery ?: return
        if (state.discoveryQueue.none { it.node.id == event.node.id }) {
            state.discoveryQueue = state.discoveryQueue + event
            audio.play(Sfx.Sparkle)
        }
        learning.clearPendingDiscovery()
    }

    fun dismissDiscovery() {
        state.discoveryQueue = state.discoveryQueue.drop(1)
    }

    /** Mine the currently shown discovery into Kaiteyo (spec §65). */
    fun mineDiscovery(nodeId: String) {
        if (learning.mine(nodeId)) {
            dismissDiscovery()
        }
    }

    // ------------------------------------------------------------
    // Save / load
    // ------------------------------------------------------------

    fun save() {
        player.syncPositionToState()
        state.stats = state.stats.copy(activeSeconds = activeSeconds)
        val data = SaveData(
            savedAt = Clock.System.now().toString(),
            player = player.state,
            quests = quests.snapshot(),
            knowledge = learning.snapshot(),
            collection = collections.snapshot(),
            world = state.worldState,
            story = story.snapshot(),
            album = album.snapshot(),
            settings = settings,
            stats = state.stats
        )
        saveManager.save(data, activeSlot)
    }

    private fun loadFromSave(saved: SaveData?) {
        if (saved == null) return
        player.state = saved.player
        player.applyStateToEntity()
        quests.restore(saved.quests)
        learning.restore(saved.knowledge)
        collections.restore(saved.collection)
        state.worldState = saved.world
        story.restore(saved.story)
        album.restore(saved.album)
        settings = saved.settings
        state.settings = settings
        // Apply the journey's own control scheme (rebinds survive a reload).
        input.scheme = settings.controlScheme
        input.calibration = settings.inputCalibration
        state.stats = saved.stats
        activeSeconds = saved.stats.activeSeconds
        clock.minuteOfDay = saved.world.minuteOfDay
        clock.day = saved.world.day
        weather.setWeather(saved.world.weather)
        seasons.sync()
        // A save can live in any region (journeys continue where they left
        // off) — rebuild the region runtime around the saved location.
        enterRegion(saved.player.regionId, saved.player.cellId)
        camera.snapTo(player.entity.position)
    }

    fun newGame() {
        val defaultSpawn = world.defaultSpawn
        player.state = PlayerState(
            regionId = defaultSpawn.regionId,
            cellId = defaultSpawn.cellId,
            position = defaultSpawn.position
        )
        player.applyStateToEntity()
        quests.initProgress()
        quests.refreshAvailability()
        learning.restore(ua.syt0r.kanji.desktop.game.learning.LearningManager.LearningState())
        collections.restore(ua.syt0r.kanji.desktop.game.collection.CollectionData())
        album.restore(ua.syt0r.kanji.desktop.game.photography.AlbumData())
        story.restore(emptyList())
        state.worldState = WorldStateData()
        state.stats = GameStatsData()
        settings = GameSettings()
        state.settings = settings
        input.scheme = settings.controlScheme
        input.calibration = settings.inputCalibration
        activeSeconds = 0L
        clock.minuteOfDay = 9 * 60
        clock.day = 1
        enterRegion(defaultSpawn.regionId, defaultSpawn.cellId)
        camera.snapTo(player.entity.position)
    }

    fun quitToKaiteyo() {
        save()
        state.closeAllPanels()
        tts.stop()
        audio.shutdown()
        input.disconnectGamepad()
    }

    // ------------------------------------------------------------
    // World scene (the engine's active scene)
    // ------------------------------------------------------------

    private inner class WorldScene : Scene {
        override val id = "world"

        override fun update(engine: GameEngine, dt: Float) {
            this@GameSession.tick(dt)
        }

        override fun render(engine: GameEngine, backend: RenderBackend) {
            renderer.render(backend, camera)
        }
    }
}
