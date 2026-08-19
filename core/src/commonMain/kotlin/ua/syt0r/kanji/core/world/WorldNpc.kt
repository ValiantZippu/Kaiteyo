package ua.syt0r.kanji.core.world

import kotlin.random.Random

// ============================================================
// WORLD — NPC FRAMEWORK & INTERACTIONS
// ------------------------------------------------------------
// NPCs populate the world. Each NPC has a schedule (where they
// are at what time of day), dialogue lines, and interactions.
// NPCs are data-driven — no behavior is hardcoded per character.
// ============================================================

/**
 * NPC gender/persona metadata (kept neutral — NPCs are just
 * characters with dialogue).
 */
enum class NpcRole(val label: String) {
    Shopkeeper("Shopkeeper"),
    Student("Student"),
    Commuter("Commuter"),
    Tourist("Tourist"),
    TemplePriest("Temple priest"),
    StationStaff("Station staff"),
    Fisher("Fisher"),
    Surfer("Surfer"),
    Elder("Elder"),
    Child("Child")
}

/**
 * An NPC in the world.
 */
data class Npc(
    val id: String,
    val name: String,
    val japaneseName: String,
    val role: NpcRole,
    val homePosition: WorldPosition,
    /** Schedule: hour (0-23) → position. Linear interpolation between. */
    val schedule: Map<Int, WorldPosition>,
    val dialogue: List<NpcDialogue> = emptyList(),
    /** Interactions available. */
    val interactions: List<NpcInteraction> = emptyList(),
    val currentPosition: WorldPosition = homePosition,
    val isMoving: Boolean = false,
    val facingDegrees: Float = 0f,
    val activeDialogueId: String? = null
)

/**
 * A dialogue line an NPC can say.
 */
data class NpcDialogue(
    val id: String,
    val japanese: String,
    val translation: String,
    /** Condition for this line (null = always available). */
    val condition: NpcDialogueCondition = NpcDialogueCondition.Always
)

/**
 * Conditions for dialogue availability.
 */
enum class NpcDialogueCondition(val label: String) {
    Always("Always"),
    Daytime("Daytime"),
    Nighttime("Nighttime"),
    Rainy("When raining"),
    Sunny("When sunny"),
    FirstMeeting("First meeting"),
    AfterLearning("After learning N kanji"),
    QuestComplete("After quest complete")
}

/**
 * An interaction the player can perform with an NPC.
 */
data class NpcInteraction(
    val id: String,
    val label: String,
    val type: NpcInteractionType,
    /** Optional quest/learning payload. */
    val payload: String? = null
)

/**
 * Interaction types.
 */
enum class NpcInteractionType(val label: String) {
    Talk("Talk"),
    Shop("Shop"),
    Quest("Quest"),
    Learn("Learn"),
    Trade("Trade"),
    Greet("Greet"),
    Info("Info")
}

/**
 * NPC system — updates NPC positions per their schedule and
 * manages interactions.
 */
class NpcSystem(
    private val npcLimit: Int = 60
) : WorldSystem {

    private val npcs = mutableListOf<Npc>()

    val activeNpcs: List<Npc> get() = npcs.toList()

    fun addNpc(npc: Npc) {
        if (npcs.size < npcLimit) npcs.add(npc)
    }

    fun removeNpc(id: String) {
        npcs.removeAll { it.id == id }
    }

    fun clear() = npcs.clear()

    fun npcById(id: String): Npc? = npcs.firstOrNull { it.id == id }

    /** Returns NPCs within [radiusMeters] of a position. */
    fun npcsNear(position: WorldPosition, radiusMeters: Double): List<Npc> =
        npcs.filter { it.currentPosition.horizontalDistanceTo(position) <= radiusMeters }

    override suspend fun onStart(runtime: WorldRuntime) {}

    override suspend fun onStop(runtime: WorldRuntime) {
        npcs.clear()
    }

    override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
        val hour = (runtime.timeOfDay.value * 24).toInt().coerceIn(0, 23)
        for (i in npcs.indices) {
            val npc = npcs[i]
            // Find the nearest schedule waypoint for this hour.
            val scheduled = npc.schedule[hour] ?: npc.homePosition
            val distance = npc.currentPosition.horizontalDistanceTo(scheduled)
            val step = 2.0 * deltaSeconds // NPC walk speed ~2 m/s

            if (distance > 0.5) {
                val delta = scheduled - npc.currentPosition
                val factor = (step / distance).coerceAtMost(1.0)
                val newPos = npc.currentPosition + delta * factor
                npcs[i] = npc.copy(
                    currentPosition = newPos,
                    isMoving = distance > 1.0
                )
            } else {
                npcs[i] = npc.copy(isMoving = false)
            }
        }
    }
}

/**
 * NPC builder — a tiny fluent DSL for defining NPCs.
 */
class NpcBuilder(private val id: String, private val name: String) {
    private var japaneseName: String = name
    private var role: NpcRole = NpcRole.Commuter
    private var home: WorldPosition = WorldPosition.Zero
    private val schedule = mutableMapOf<Int, WorldPosition>()
    private val dialogue = mutableListOf<NpcDialogue>()
    private val interactions = mutableListOf<NpcInteraction>()

    fun japaneseName(name: String) = apply { japaneseName = name }
    fun role(role: NpcRole) = apply { this.role = role }
    fun home(position: WorldPosition) = apply { home = position; schedule.putIfAbsent(0, position) }
    fun at(hour: Int, position: WorldPosition) = apply { schedule[hour] = position }
    fun says(japanese: String, translation: String, condition: NpcDialogueCondition = NpcDialogueCondition.Always) = apply {
        dialogue.add(NpcDialogue("$id-d${dialogue.size}", japanese, translation, condition))
    }
    fun can(type: NpcInteractionType, label: String) = apply {
        interactions.add(NpcInteraction("$id-i${interactions.size}", label, type))
    }

    fun build(): Npc = Npc(
        id = id,
        name = name,
        japaneseName = japaneseName,
        role = role,
        homePosition = home,
        schedule = schedule,
        dialogue = dialogue,
        interactions = interactions
    )
}

/**
 * Preset Kamakura NPCs for the vertical slice.
 */
object KamakuraNpcs {

    /** Builds a small cast around Kamakura Station. */
    fun buildAll(): List<Npc> {
        val station = KamakuraLocations.KAMAKURA_STATION.position
        val beach = KamakuraLocations.YUIGAHAMA_BEACH.position
        val shrine = KamakuraLocations.TSUROKA_HACHIMANGU.position

        val stationStaff = NpcBuilder("staff-1", "Tanaka")
            .japaneseName("田中")
            .role(NpcRole.StationStaff)
            .home(station)
            .at(0, station)
            .at(8, station)
            .at(17, station)
            .says("鎌倉駅へようこそ！", "Welcome to Kamakura Station!")
            .says("江ノ電は次、和田塚です。", "The Enoden's next stop is Wadazuka.")
            .can(NpcInteractionType.Info, "Ask about trains")
            .build()

        val surfer = NpcBuilder("surfer-1", "Kenji")
            .japaneseName("ケンジ")
            .role(NpcRole.Surfer)
            .home(beach)
            .at(0, beach)
            .at(6, beach)
            .at(10, shrine)
            .at(14, beach)
            .says("今日の波は最高だ！", "The waves are amazing today!")
            .can(NpcInteractionType.Talk, "Chat about surfing")
            .build()

        val elder = NpcBuilder("elder-1", "Yamamoto")
            .japaneseName("山本")
            .role(NpcRole.Elder)
            .home(shrine)
            .at(0, shrine)
            .at(9, shrine)
            .at(12, shrine)
            .at(18, shrine)
            .says("鶴岡八幡宮は鎌倉の心です。", "Tsurugaoka Hachimangu is the heart of Kamakura.")
            .says("若い頃はここでよく遊びました。", "I used to play here when I was young.", NpcDialogueCondition.Daytime)
            .can(NpcInteractionType.Learn, "Ask about Kamakura history")
            .build()

        return listOf(stationStaff, surfer, elder)
    }
}
