package ua.syt0r.kanji.desktop.game.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.game.collection.Collectible
import ua.syt0r.kanji.desktop.game.dialogue.Dialogue
import ua.syt0r.kanji.desktop.game.learning.KnowledgeGraph
import ua.syt0r.kanji.desktop.game.learning.KnowledgeLink
import ua.syt0r.kanji.desktop.game.learning.KnowledgeNode
import ua.syt0r.kanji.desktop.game.npc.NpcDefinition
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.story.Story
import ua.syt0r.kanji.desktop.game.world.GameNode
import ua.syt0r.kanji.desktop.game.world.GameNodeType
import ua.syt0r.kanji.desktop.game.world.GameWorld
import ua.syt0r.kanji.desktop.game.world.Location
import ua.syt0r.kanji.desktop.game.world.Region
import ua.syt0r.kanji.desktop.game.world.SpawnPoint
import ua.syt0r.kanji.desktop.game.world.Station
import ua.syt0r.kanji.desktop.game.world.TravelNetwork
import ua.syt0r.kanji.desktop.game.world.WorldGraph

/**
 * Everything the game needs from content files. Content is versioned with
 * the release (`contentVersion`), so saves and content can be validated
 * against each other (spec §127).
 */
data class LoadedContent(
    val contentVersion: Int,
    val world: GameWorld,
    val quests: List<Quest>,
    val npcs: List<NpcDefinition>,
    val dialogues: List<Dialogue>,
    val knowledgeGraph: KnowledgeGraph,
    val stories: List<Story>,
    val collectibles: List<Collectible>
)

/**
 * Loads every game content file from the classpath (`src/jvmMain/resources/game/`).
 * Content is data — regions, quests, NPCs, dialogue and vocabulary are added
 * as JSON without touching the engine (spec §78).
 */
object WorldContentLoader {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): LoadedContent {
        val worldManifest = loadFile<WorldManifest>("/game/world.json")
        val npcs = loadFile<NpcFile>("/game/npcs.json").npcs
        val quests = loadFile<QuestFile>("/game/quests.json").quests
        val dialogues = loadFile<DialogueFile>("/game/dialogue.json").dialogues
        val knowledgeFile = loadFile<KnowledgeFile>("/game/knowledge.json")
        val stories = loadFile<StoryFile>("/game/stories.json").stories
        val collectibles = loadFile<CollectibleFile>("/game/collectibles.json").collectibles

        val regions = worldManifest.regions
        val travel = worldManifest.travel
        val nodes = buildWorldNodes(regions, quests, travel.stations)
        val world = GameWorld(
            regions = regions,
            travel = travel,
            worldGraph = WorldGraph(nodes),
            defaultSpawn = worldManifest.defaultSpawn
        )
        val graph = KnowledgeGraph(knowledgeFile.nodes, knowledgeFile.links)

        return LoadedContent(
            contentVersion = worldManifest.contentVersion,
            world = world,
            quests = quests,
            npcs = npcs,
            dialogues = dialogues,
            knowledgeGraph = graph,
            stories = stories,
            collectibles = collectibles
        )
    }

    /**
     * Derive the world graph nodes from content (spec §76): one node per
     * location, station and quest, with prerequisites/rewards wired from the
     * content files themselves.
     */
    private fun buildWorldNodes(
        regions: List<Region>,
        quests: List<Quest>,
        stations: List<Station>
    ): List<GameNode> {
        val nodes = mutableListOf<GameNode>()
        for (region in regions) {
            for (location in region.locations) {
                nodes += GameNode(
                    id = "loc:${location.id}",
                    type = GameNodeType.LOCATION,
                    title = location.name,
                    description = location.description,
                    locationId = location.id,
                    learningTargets = location.learningTargets,
                    rewards = location.unlocks
                )
            }
        }
        for (station in stations) {
            nodes += GameNode(
                id = "station:${station.id}",
                type = GameNodeType.TRAVEL,
                title = station.name,
                description = "Travel hub for ${station.name}",
                locationId = station.cellId,
                learningTargets = station.learningTargets,
                rewards = listOf("travel:${station.id}")
            )
        }
        for (quest in quests) {
            nodes += GameNode(
                id = "quest:${quest.id}",
                type = GameNodeType.QUEST,
                title = quest.title,
                description = quest.description,
                prerequisites = quest.prerequisites.map { "quest:$it" },
                children = quest.rewards.unlocks.map { "loc:$it" },
                locationId = quest.locationId,
                learningTargets = quest.learningTargets,
                rewards = quest.rewards.unlocks
            )
        }
        return nodes
    }

    private inline fun <reified T> loadFile(path: String): T {
        val stream = WorldContentLoader::class.java.getResourceAsStream(path)
            ?: error("Game content file missing from resources: $path")
        return json.decodeFromString(stream.bufferedReader().use { it.readText() })
    }
}

// ------------------------------------------------------------
// File envelopes
// ------------------------------------------------------------

@Serializable
data class WorldManifest(
    val contentVersion: Int = 1,
    val defaultSpawn: SpawnPoint,
    val regions: List<Region> = emptyList(),
    val travel: TravelNetwork = TravelNetwork()
)

@Serializable
data class NpcFile(val npcs: List<NpcDefinition> = emptyList())

@Serializable
data class QuestFile(val quests: List<Quest> = emptyList())

@Serializable
data class DialogueFile(val dialogues: List<Dialogue> = emptyList())

@Serializable
data class KnowledgeFile(
    val nodes: List<KnowledgeNode> = emptyList(),
    val links: List<KnowledgeLink> = emptyList()
)

@Serializable
data class StoryFile(val stories: List<Story> = emptyList())

@Serializable
data class CollectibleFile(val collectibles: List<Collectible> = emptyList())
