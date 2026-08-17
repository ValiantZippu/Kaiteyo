package ua.syt0r.kanji.desktop.game.story

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

// ============================================================
// STORY SYSTEM (spec §54-55, §120)
// Optional small stories (a summer trip, a missing cat, a
// festival) with chapters → scenes → dialogue → choices → quests.
// ============================================================

@Serializable
data class Story(
    val id: String,
    val title: String,
    val titleJp: String = "",
    val description: String = "",
    val chapters: List<StoryChapter> = emptyList()
)

@Serializable
data class StoryChapter(
    val id: String,
    val title: String,
    val scenes: List<StoryScene> = emptyList()
)

@Serializable
data class StoryScene(
    val id: String,
    val dialogueId: String? = null,
    val locationId: String? = null,
    /** Quest triggered when this scene plays. */
    val questTrigger: String? = null,
    /** Node ids granted when this scene completes. */
    val grants: List<String> = emptyList(),
    /**
     * Branch points (spec §55): when present, the story waits for a choice
     * instead of advancing on its own. A choice can jump to any scene in the
     * story (or advance normally), trigger a different quest and set a flag
     * — quest order and relationships are data, not code.
     */
    val options: List<StoryChoice> = emptyList()
)

/**
 * One story branch (spec §55). [nextSceneId] jumps anywhere in the story;
 * blank means the branch **ends the story here** (e.g. heading home early) —
 * it never keeps walking the linear scene list.
 */
@Serializable
data class StoryChoice(
    val text: String,
    val textJp: String = "",
    val nextSceneId: String? = null,
    /** Quest triggered by picking this option. */
    val questTrigger: String? = null,
    /** Knowledge granted by picking this option. */
    val grants: List<String> = emptyList(),
    /** World-state flag set when picked (see [ua.syt0r.kanji.desktop.game.save.WorldStateData.flags]). */
    val setFlag: String = ""
)

/** Serializable story progress persisted in the save. */
@Serializable
data class StoryProgress(
    val storyId: String,
    val currentChapter: Int = 0,
    val currentScene: Int = 0,
    val completed: Boolean = false
)

/**
 * Drives one story: advances through chapters/scenes and reports scene
 * completions so the session can trigger quests and grant nodes. Lightweight
 * branching — choices alter dialogue, quest order and relationships, but no
 * massive branching is required (spec §55).
 */
class StoryEngine(
    private val stories: List<Story>
) {
    private val byId = stories.associateBy { it.id }

    val progress = mutableStateOf<List<StoryProgress>>(emptyList())

    var activeStory by mutableStateOf<Story?>(null)
        private set
    var activeChapter by mutableStateOf<StoryChapter?>(null)
        private set
    var activeScene by mutableStateOf<StoryScene?>(null)
        private set

    fun story(id: String): Story? = byId[id]

    fun start(storyId: String): StoryScene? {
        val story = byId[storyId] ?: return null
        lastChoice = null
        activeStory = story
        val chapter = story.chapters.firstOrNull() ?: return null
        activeChapter = chapter
        val scene = chapter.scenes.firstOrNull() ?: return null
        activeScene = scene
        return scene
    }

    /**
     * The most recent branch taken (for the session to apply its quest/flag
     * effects). Reset to null on every [start].
     */
    var lastChoice by mutableStateOf<StoryChoice?>(null)
        private set

    /** Advance to the next scene; returns it, or null when the story ends. */
    fun advance(): StoryScene? {
        val story = activeStory ?: return null
        val chapter = activeChapter ?: return null
        val scene = activeScene ?: return null
        val chapterIndex = story.chapters.indexOf(chapter)
        val sceneIndex = chapter.scenes.indexOf(scene)
        val nextScene = chapter.scenes.getOrNull(sceneIndex + 1)
        if (nextScene != null) {
            activeScene = nextScene
            return nextScene
        }
        val nextChapter = story.chapters.getOrNull(chapterIndex + 1)
        if (nextChapter != null) {
            activeChapter = nextChapter
            activeScene = nextChapter.scenes.firstOrNull()
            return activeScene
        }
        markComplete(story.id)
        return null
    }

    fun currentScene(): StoryScene? = activeScene

    /**
     * The player picked branch [index] on the active scene. Returns the
     * scene the story continues from (the choice's jump target, or the
     * linear next scene), or null when the story ends. The choice itself is
     * left in [lastChoice] for the session to apply quest/flag effects.
     */
    fun choose(index: Int): StoryScene? {
        val scene = activeScene ?: return null
        val choice = scene.options.getOrNull(index) ?: return null
        lastChoice = choice
        if (choice.nextSceneId != null) {
            activeScene = findScene(choice.nextSceneId)
            return activeScene
        }
        // No jump target: the branch ends the story here (e.g. "head home
        // early") — it must NOT keep walking the linear scene list.
        val activeId = activeStory?.id ?: return null
        markComplete(activeId)
        return null
    }

    /** Look up a scene by id anywhere in the active story. */
    private fun findScene(sceneId: String): StoryScene? {
        val story = activeStory ?: return null
        return story.chapters.asSequence()
            .flatMap { it.scenes.asSequence() }
            .firstOrNull { it.id == sceneId }
    }

    fun markComplete(storyId: String) {
        val current = progress.value.toMutableList()
        val index = current.indexOfFirst { it.storyId == storyId }
        val entry = if (index >= 0) current[index].copy(completed = true) else StoryProgress(storyId, completed = true)
        if (index >= 0) current[index] = entry else current.add(entry)
        progress.value = current
        activeStory = null
        activeChapter = null
        activeScene = null
    }

    fun snapshot(): List<StoryProgress> = progress.value

    fun restore(snapshot: List<StoryProgress>) {
        progress.value = snapshot
    }
}
