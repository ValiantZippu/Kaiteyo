package ua.syt0r.kanji.desktop.game.validation

import ua.syt0r.kanji.desktop.game.content.LoadedContent
import ua.syt0r.kanji.desktop.game.quest.ObjectiveKind
import ua.syt0r.kanji.desktop.game.world.GameNodeType

/**
 * Content validation (spec §123-126): every id, quest dependency, dialogue
 * link, knowledge reference and travel destination is checked before the
 * game runs. Runs at load time; failures surface in the debug overlay and
 * the dev log — broken content can never silently ship.
 */
class ContentValidator {

    data class Issue(val severity: Severity, val message: String) {
        enum class Severity { Error, Warning }
    }

    val issues = mutableListOf<Issue>()

    val errors: List<Issue> get() = issues.filter { it.severity == Issue.Severity.Error }
    val warnings: List<Issue> get() = issues.filter { it.severity == Issue.Severity.Warning }

    fun validate(content: LoadedContent): ContentValidator {
        issues.clear()
        validateDuplicateIds(content)
        validateQuests(content)
        validateDialogue(content)
        validateKnowledge(content)
        validateTravel(content)
        validateCollectibles(content)
        validateNodes(content)
        validateNpcs(content)
        validateStories(content)
        return this
    }

    private fun validateDuplicateIds(content: LoadedContent) {
        val allIds = buildList {
            content.quests.forEach { add("quest:${it.id}") }
            content.npcs.forEach { add("npc:${it.id}") }
            content.dialogues.forEach { add("dialogue:${it.id}") }
            content.knowledgeGraph.nodes.forEach { add("knowledge:${it.id}") }
            content.world.allLocations().forEach { add("location:${it.id}") }
            content.world.travel.stations.forEach { add("station:${it.id}") }
            content.world.allObjects().forEach { add("object:${it.id}") }
            content.collectibles.forEach { add("collectible:${it.id}") }
        }
        val seen = mutableSetOf<String>()
        for (id in allIds) {
            if (!seen.add(id)) {
                issues += Issue(Issue.Severity.Error, "Duplicate content id: $id")
            }
        }
    }

    private fun validateQuests(content: LoadedContent) {
        val questIds = content.quests.map { it.id }.toSet()
        val locationIds = content.world.allLocations().map { it.id }.toSet()
        val objectIds = content.world.allObjects().map { it.id }.toSet()
        val npcIds = content.npcs.map { it.id }.toSet()
        val knowledgeIds = content.knowledgeGraph.nodes.map { it.id }.toSet()
        val dialogueIds = content.dialogues.map { it.id }.toSet()
        val stationIds = content.world.travel.stations.map { it.id }.toSet()

        for (quest in content.quests) {
            for (prereq in quest.prerequisites) {
                // Prerequisites may reference quest ids or world-node ids.
                if (prereq !in questIds && !prereq.startsWith("loc:") && !prereq.startsWith("station:")) {
                    issues += Issue(Issue.Severity.Error, "Quest '${quest.id}' has unknown prerequisite '$prereq'")
                }
            }
            if (quest.locationId != null && quest.locationId !in locationIds) {
                issues += Issue(Issue.Severity.Error, "Quest '${quest.id}' references missing location '${quest.locationId}'")
            }
            if (quest.dialogueId != null && quest.dialogueId !in dialogueIds) {
                issues += Issue(Issue.Severity.Error, "Quest '${quest.id}' references missing dialogue '${quest.dialogueId}'")
            }
            for (target in quest.learningTargets) {
                if (target !in knowledgeIds) {
                    issues += Issue(Issue.Severity.Error, "Quest '${quest.id}' learning target '$target' is not in the knowledge graph")
                }
            }
            for (objective in quest.objectives) {
                val target = objective.targetId
                val ok = when (objective.kind) {
                    ObjectiveKind.ReachLocation, ObjectiveKind.DiscoverLocation -> target in locationIds
                    ObjectiveKind.InteractObject, ObjectiveKind.ReadSign -> target in objectIds
                    ObjectiveKind.TalkToNpc -> target in npcIds
                    ObjectiveKind.LearnWord -> target in knowledgeIds
                    ObjectiveKind.BuyItem -> target.isNotBlank()
                    ObjectiveKind.RideTrain -> target in stationIds
                    ObjectiveKind.Listen -> target.isNotBlank()
                    ObjectiveKind.WriteKana -> target in knowledgeIds
                    ObjectiveKind.OrderFood -> target.isNotBlank()
                    // Collect targets are item ids (blank = any item) — free
                    // strings validated by the player's item catalogue.
                    ObjectiveKind.Collect -> true
                    // Season/weather targets are enum names; the manager
                    // matches them case-insensitively at runtime.
                    ObjectiveKind.Season, ObjectiveKind.Weather ->
                        target.isNotBlank()
                    ObjectiveKind.TakePhoto, ObjectiveKind.Custom -> true
                }
                if (!ok) {
                    issues += Issue(
                        Issue.Severity.Error,
                        "Quest '${quest.id}' objective '${objective.id}' references missing target '$target'"
                    )
                }
            }

        // Circular prerequisites (spec §124).
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun hasCycle(id: String): Boolean {
            if (id in visiting) return true
            if (id in visited) return false
            visiting.add(id)
            val quest = content.quests.firstOrNull { it.id == id } ?: return false
            for (prereq in quest.prerequisites) {
                if (prereq in questIds && hasCycle(prereq)) {
                    issues += Issue(Issue.Severity.Error, "Circular quest prerequisite involving '$id'")
                    return true
                }
            }
            visiting.remove(id)
            visited.add(id)
            return false
        }
        for (quest in content.quests) hasCycle(quest.id)
        }
    }

    private fun validateNpcs(content: LoadedContent) {
        for (npc in content.npcs) {
            if (content.world.cell(npc.cellId) == null) {
                issues += Issue(Issue.Severity.Error, "NPC '${npc.id}' references missing cell '${npc.cellId}'")
            }
            // Schedule + chained-route windows must not collide (spec §39, §52)
            // and route points must stay inside the NPC's own cell bounds.
            val cell = content.world.cell(npc.cellId)
            val inCell = { p: ua.syt0r.kanji.desktop.game.world.WorldPoint ->
                cell == null ||
                    (p.x >= cell.bounds.x && p.x <= cell.bounds.x + cell.bounds.width &&
                        p.y >= cell.bounds.y && p.y <= cell.bounds.y + cell.bounds.height)
            }
            val windows = buildList {
                npc.schedule.forEach { add(it.fromMinutes..it.toMinutes) }
                npc.routes.forEach { add(it.fromMinutes..it.toMinutes) }
            }
            for (i in windows.indices) {
                for (j in i + 1 until windows.size) {
                    if (windows[i].first <= windows[j].last && windows[j].first <= windows[i].last) {
                        issues += Issue(Issue.Severity.Warning, "NPC '${npc.id}' has overlapping schedule/route windows")
                    }
                }
            }
            npc.schedule.forEach { entry ->
                if (!inCell(entry.position)) {
                    issues += Issue(Issue.Severity.Warning, "NPC '${npc.id}' schedule point outside cell '${npc.cellId}' bounds")
                }
            }
            npc.routes.forEach { route ->
                route.points.forEach { p ->
                    if (!inCell(p)) {
                        issues += Issue(Issue.Severity.Warning, "NPC '${npc.id}' route point outside cell '${npc.cellId}' bounds")
                    }
                }
            }
            if (npc.dialogueId != null && content.dialogues.none { it.id == npc.dialogueId }) {
                issues += Issue(Issue.Severity.Error, "NPC '${npc.id}' references missing dialogue '${npc.dialogueId}'")
            }
        }
    }

    private fun validateDialogue(content: LoadedContent) {
        val dialogueById = content.dialogues.associateBy { it.id }
        val npcIds = content.npcs.map { it.id }.toSet()
        val knowledgeIds = content.knowledgeGraph.nodes.map { it.id }.toSet()
        for (dialogue in content.dialogues) {
            val lineIds = dialogue.lines.map { it.id }.toSet()
            for (line in dialogue.lines) {
                if (line.speakerId.isNotBlank() && line.speakerId !in npcIds && line.speakerId != "player" && line.speakerId != "narrator") {
                    issues += Issue(Issue.Severity.Warning, "Dialogue '${dialogue.id}' line '${line.id}' speaker '${line.speakerId}' unknown")
                }
                line.nextId?.let { next ->
                    if (next !in lineIds) {
                        issues += Issue(Issue.Severity.Error, "Dialogue '${dialogue.id}' line '${line.id}' has missing next '$next'")
                    }
                }
                for (choice in line.options) {
                    if (choice.nextId !in lineIds) {
                        issues += Issue(Issue.Severity.Error, "Dialogue '${dialogue.id}' choice '${choice.text}' has missing target '${choice.nextId}'")
                    }
                    if (choice.requiresKnowledge.isNotBlank() && choice.requiresKnowledge !in knowledgeIds) {
                        issues += Issue(Issue.Severity.Error, "Dialogue '${dialogue.id}' choice '${choice.text}' requires missing knowledge '${choice.requiresKnowledge}'")
                    }
                }
                // Every choice is knowledge-gated → the line would soft-lock:
                // a player who knows nothing could never pick anything, and
                // advance() has to skip the line. At least one ungated option
                // must remain (spec §13).
                if (line.options.isNotEmpty() && line.options.all { it.requiresKnowledge.isNotBlank() }) {
                    issues += Issue(
                        Issue.Severity.Error,
                        "Dialogue '${dialogue.id}' line '${line.id}' gates every choice behind knowledge — keep at least one ungated option"
                    )
                }
                // Kid variants are per-audience text; a kid translation without
                // kid Japanese is almost certainly a content slip.
                if (line.kidTranslation.isNotBlank() && line.kidJp.isBlank()) {
                    issues += Issue(
                        Issue.Severity.Warning,
                        "Dialogue '${dialogue.id}' line '${line.id}' has a kid translation but no kid Japanese"
                    )
                }
            }
        }
    }

    private fun validateKnowledge(content: LoadedContent) {
        val graph = content.knowledgeGraph
        val nodeIds = graph.nodes.map { it.id }.toSet()
        for (node in graph.nodes) {
            if (node.lookupKey().isBlank()) {
                issues += Issue(Issue.Severity.Error, "Knowledge node '${node.id}' has no kaiteyoKey/headword to look up")
            }
            for (dep in node.dependsOn) {
                if (dep !in nodeIds) {
                    issues += Issue(Issue.Severity.Error, "Knowledge node '${node.id}' depends on missing '$dep'")
                }
            }
            for (kanji in node.kanjiIds) {
                if (kanji !in nodeIds) {
                    issues += Issue(Issue.Severity.Error, "Knowledge node '${node.id}' references missing kanji '$kanji'")
                }
            }
        }
        // World objects and locations must teach real nodes — the kid-mode
        // layers (spec §7, §68) included, so a typo'd kidTargets can never
        // silently teach nothing.
        for (obj in content.world.allObjects()) {
            for (target in obj.learningTargets + obj.kidTargets) {
                if (target !in nodeIds) {
                    issues += Issue(
                        Issue.Severity.Error,
                        "Object '${obj.id}' learning target '$target' is not in the knowledge graph"
                    )
                }
            }
        }
        for (location in content.world.allLocations()) {
            for (target in location.learningTargets) {
                if (target !in nodeIds) {
                    issues += Issue(
                        Issue.Severity.Error,
                        "Location '${location.id}' learning target '$target' is not in the knowledge graph"
                    )
                }
            }
        }
    }

    private fun validateTravel(content: LoadedContent) {
        val network = content.world.travel
        val stationIds = network.stations.map { it.id }.toSet()
        val lineIds = network.lines.map { it.id }.toSet()
        for (line in network.lines) {
            for (stationId in line.stations) {
                if (stationId !in stationIds) {
                    issues += Issue(Issue.Severity.Error, "Line '${line.id}' references missing station '$stationId'")
                }
            }
        }
        for (edge in network.edges) {
            if (edge.fromStationId !in stationIds || edge.toStationId !in stationIds) {
                issues += Issue(Issue.Severity.Error, "Travel edge references missing station")
            }
            if (edge.lineId !in lineIds) {
                issues += Issue(Issue.Severity.Error, "Travel edge references missing line '${edge.lineId}'")
            }
        }
    }

    private fun validateCollectibles(content: LoadedContent) {
        val knowledgeIds = content.knowledgeGraph.nodes.map { it.id }.toSet()
        val locationIds = content.world.allLocations().map { it.id }.toSet()
        for (collectible in content.collectibles) {
            collectible.knowledgeId?.let {
                if (it !in knowledgeIds) {
                    issues += Issue(Issue.Severity.Error, "Collectible '${collectible.id}' references missing knowledge '$it'")
                }
            }
            collectible.locationId?.let {
                if (it !in locationIds) {
                    issues += Issue(Issue.Severity.Error, "Collectible '${collectible.id}' references missing location '$it'")
                }
            }
        }
    }

    private fun validateStories(content: LoadedContent) {
        val questIds = content.quests.map { it.id }.toSet()
        val knowledgeIds = content.knowledgeGraph.nodes.map { it.id }.toSet()
        for (story in content.stories) {
            val sceneIds = story.chapters.flatMap { it.scenes }.map { it.id }.toSet()
            for (scene in story.chapters.flatMap { it.scenes }) {
                scene.questTrigger?.let { trigger ->
                    if (trigger !in questIds) {
                        issues += Issue(Issue.Severity.Error, "Story '${story.id}' scene '${scene.id}' triggers missing quest '$trigger'")
                    }
                }
                for (grant in scene.grants) {
                    if (grant !in knowledgeIds) {
                        issues += Issue(Issue.Severity.Error, "Story '${story.id}' scene '${scene.id}' grants missing knowledge '$grant'")
                    }
                }
                for (choice in scene.options) {
                    choice.nextSceneId?.let { target ->
                        if (target !in sceneIds) {
                            issues += Issue(Issue.Severity.Error, "Story '${story.id}' choice '${choice.text}' jumps to missing scene '$target'")
                        }
                    }
                    choice.questTrigger?.let { trigger ->
                        if (trigger !in questIds) {
                            issues += Issue(Issue.Severity.Error, "Story '${story.id}' choice '${choice.text}' triggers missing quest '$trigger'")
                        }
                    }
                    for (grant in choice.grants) {
                        if (grant !in knowledgeIds) {
                            issues += Issue(Issue.Severity.Error, "Story '${story.id}' choice '${choice.text}' grants missing knowledge '$grant'")
                        }
                    }
                }
            }
        }
    }

    private fun validateNodes(content: LoadedContent) {
        val worldGraph = content.world.worldGraph
        val locationIds = content.world.allLocations().map { it.id }.toSet()
        for (node in worldGraph.nodes) {
            if (node.type == GameNodeType.LOCATION && node.locationId !in locationIds) {
                issues += Issue(Issue.Severity.Error, "World node '${node.id}' references missing location '${node.locationId}'")
            }
        }
    }
}
