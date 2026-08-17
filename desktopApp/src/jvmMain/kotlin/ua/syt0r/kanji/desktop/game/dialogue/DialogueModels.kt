package ua.syt0r.kanji.desktop.game.dialogue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

// ============================================================
// DIALOGUE SYSTEM (spec §31, §55, §120)
// Every line carries Japanese + reading + optional translation,
// and translation visibility adapts to the player's support level.
// ============================================================

@Serializable
data class Dialogue(
    val id: String,
    val lines: List<DialogueLine> = emptyList()
)

@Serializable
data class DialogueLine(
    val id: String,
    val speakerId: String = "",
    val speakerName: String = "",
    val jp: String,
    val reading: String = "",
    val translation: String = "",
    /**
     * Kid-mode variants (spec §68): simpler Japanese, kana reading and a
     * plainer translation shown when kids mode is on. Blank = fall back to
     * the main fields, so one line serves both audiences by default.
     */
    val kidJp: String = "",
    val kidReading: String = "",
    val kidTranslation: String = "",
    val audio: Boolean = false,
    /** Choice lines present options; normal lines continue to [nextId]. */
    val options: List<DialogueChoice> = emptyList(),
    val nextId: String? = null,
    /** Knowledge node ids this line exposes (listening/reading learning). */
    val learningTargets: List<String> = emptyList(),
    val effects: List<DialogueEffect> = emptyList()
) {
    /**
     * The line as shown to a kid-mode player: kid variants replace the main
     * text wherever they're authored, everything else stays identical.
     */
    fun withKidText(): DialogueLine = copy(
        jp = kidJp.ifBlank { jp },
        reading = kidReading.ifBlank { reading },
        translation = kidTranslation.ifBlank { translation }
    )
}

@Serializable
data class DialogueChoice(
    val text: String,
    val textJp: String = "",
    val nextId: String,
    /**
     * Optional knowledge-node id that must already be discovered for this
     * choice to appear (spec §13: knowledge-dependent responses). Blank =
     * always available. A dialogue keeps at least one ungated choice so the
     * branch never dead-ends.
     */
    val requiresKnowledge: String = ""
) {
    /** True when the player is allowed to see this choice. */
    fun isAvailable(knows: (String) -> Boolean): Boolean =
        requiresKnowledge.isBlank() || knows(requiresKnowledge)
}

/** Side effects a line can fire (quest grant, knowledge, story start…). */
@Serializable
data class DialogueEffect(
    val kind: DialogueEffectKind,
    val targetId: String = ""
)

@Serializable
enum class DialogueEffectKind {
    GrantQuest, StartStory, DiscoverKnowledge, SetFlag, GiveItem,
    OpenShop, MarkNpcMet, AdvanceStory
}

/** Receives dialogue effects — implemented by the game session. */
interface DialogueEffectHandler {
    fun handle(effect: DialogueEffect)
}

/**
 * Runs a dialogue: advances lines, presents choices, fires effects and
 * exposes the current line to the UI. Purely data-driven — a dialogue can
 * be authored entirely in JSON (spec §119-120).
 */
class DialogueRunner(
    private val dialogues: Map<String, Dialogue>,
    private val effectHandler: DialogueEffectHandler,
    /**
     * Which knowledge nodes the player has discovered (spec §13): gates
     * knowledge-dependent choices and lets advance() skip a line whose
     * options are all hidden — the branch never dead-ends.
     */
    private val knowledgeCheck: (String) -> Boolean = { true }
) {

    var activeDialogueId by mutableStateOf<String?>(null)
        private set

    var currentLine by mutableStateOf<DialogueLine?>(null)
        private set

    /** True when the dialogue is over and the UI should close the panel. */
    var finished by mutableStateOf(false)
        private set

    fun start(dialogueId: String) {
        val dialogue = dialogues[dialogueId] ?: return
        activeDialogueId = dialogueId
        finished = false
        val first = dialogue.lines.firstOrNull() ?: run {
            finished = true
            return
        }
        currentLine = first
        fireEffects(first)
    }

    fun advance() {
        val line = currentLine ?: return
        // A line with choices waits for one — unless every choice is hidden
        // by a knowledge gate (spec §13), in which case it continues on its
        // own so the dialogue never soft-locks.
        if (line.options.isNotEmpty() && availableChoices(knowledgeCheck).isNotEmpty()) return
        val nextId = line.nextId ?: run {
            end()
            return
        }
        val next = dialogues[activeDialogueId]?.lines?.firstOrNull { it.id == nextId } ?: run {
            end()
            return
        }
        currentLine = next
        fireEffects(next)
    }

    fun choose(index: Int) {
        val line = currentLine ?: return
        val choice = line.options.getOrNull(index) ?: return
        val next = dialogues[activeDialogueId]?.lines?.firstOrNull { it.id == choice.nextId } ?: run {
            end()
            return
        }
        currentLine = next
        fireEffects(next)
    }

    fun end() {
        currentLine = null
        activeDialogueId = null
        finished = true
    }

    val isActive: Boolean get() = activeDialogueId != null && currentLine != null

    /** Dialogue lines currently exposing knowledge (for listening practice). */
    fun exposedKnowledge(): List<String> = currentLine?.learningTargets.orEmpty()

    /**
     * The options a player can actually pick right now, with their original
     * indices (the runner selects by index into [DialogueLine.options]).
     * Knowledge-gated choices disappear until their word is discovered
     * (spec §13) — the panel renders exactly this list.
     */
    fun availableChoices(knows: (String) -> Boolean): List<Pair<Int, DialogueChoice>> =
        currentLine?.options.orEmpty()
            .mapIndexed { index, choice -> index to choice }
            .filter { (_, choice) -> choice.isAvailable(knows) }

    private fun fireEffects(line: DialogueLine) {
        for (effect in line.effects) {
            effectHandler.handle(effect)
        }
    }
}
