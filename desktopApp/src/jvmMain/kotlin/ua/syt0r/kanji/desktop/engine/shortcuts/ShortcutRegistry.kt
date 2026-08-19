package ua.syt0r.kanji.desktop.engine.shortcuts

import kotlinx.serialization.Serializable

// ============================================
// SHORTCUT REGISTRY + DISPATCHER
// Keyboard-first everything. Shortcuts are
// declarative, rebindable, and dispatched by chord.
// ============================================

@Serializable
data class KeyChord(
    val key: String,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false
) {
    val label: String
        get() = buildString {
            if (ctrl) append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            if (meta) append("Meta+")
            append(key.take(1).uppercase() + key.drop(1))
        }

    fun matches(
        pressedKey: String,
        pressedCtrl: Boolean,
        pressedShift: Boolean,
        pressedAlt: Boolean,
        pressedMeta: Boolean
    ): Boolean {
        val keyMatch = pressedKey.equals(key, ignoreCase = true)
        return keyMatch && pressedCtrl == ctrl && pressedShift == shift &&
            pressedAlt == alt && pressedMeta == meta
    }

    companion object {
        fun fromLabel(label: String): KeyChord? = runCatching {
            val parts = label.split("+").map { it.trim() }
            var ctrl = false; var shift = false; var alt = false; var meta = false
            val mods = mutableListOf<String>()
            parts.forEach { p ->
                when (p.lowercase()) {
                    "ctrl", "control" -> ctrl = true
                    "shift" -> shift = true
                    "alt", "option" -> alt = true
                    "meta", "cmd", "super" -> meta = true
                    else -> mods.add(p)
                }
            }
            val key = mods.firstOrNull() ?: return@runCatching null
            KeyChord(key, ctrl, shift, alt, meta)
        }.getOrNull()
    }
}

@Serializable
data class ShortcutDef(
    val id: String,
    val name: String,
    val category: ShortcutCategory,
    val defaultChord: KeyChord,
    val description: String = "",
    val enabled: Boolean = true
) {
    var boundChord: KeyChord = defaultChord
        get() = if (field == KeyChord("", false)) defaultChord else field
        internal set
}

@Serializable
enum class ShortcutCategory { Review, Browser, Navigation, App, Search, Statistics, Media, Reading, Study, World }

/** Registry with rebinding, reset, conflict detection and export. */
class ShortcutRegistry(initial: List<ShortcutDef> = defaultShortcuts()) {

    private val _defs = LinkedHashMap<String, ShortcutDef>()
    val defs: List<ShortcutDef> get() = _defs.values.toList()

    init {
        initial.forEach { _defs[it.id] = it }
    }

    fun all(): List<ShortcutDef> = _defs.values.toList()

    fun get(id: String): ShortcutDef? = _defs[id]

    fun bind(id: String, chord: KeyChord): Result<Unit> = runCatching {
        val conflict = _defs.values.firstOrNull { it.enabled && it.boundChord == chord && it.id != id }
        if (conflict != null) error("Conflict with '${conflict.name}' (${chord.label})")
        val def = _defs[id] ?: error("Unknown shortcut $id")
        _defs[id] = def.copy().apply { boundChord = chord }
    }

    fun reset(id: String) {
        val def = _defs[id] ?: return
        _defs[id] = def.copy()
    }

    fun resetAll() {
        val originals = defaultShortcuts()
        _defs.clear()
        originals.forEach { _defs[it.id] = it }
    }

    fun enabled(): List<ShortcutDef> = _defs.values.filter { it.enabled }

    fun setEnabled(id: String, enabled: Boolean) {
        val def = _defs[id] ?: return
        _defs[id] = def.copy(enabled = enabled)
    }

    fun matches(chord: KeyChord): ShortcutDef? =
        _defs.values.firstOrNull { it.enabled && it.boundChord == chord }

    fun findByName(name: String): ShortcutDef? =
        _defs.values.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun export(): List<ShortcutDef> = defs

    companion object {
        fun from(defs: List<ShortcutDef>): ShortcutRegistry {
            val reg = ShortcutRegistry(emptyList())
            defs.forEach { reg._defs[it.id] = it }
            return reg
        }
    }
}

/** Dispatches a chord to a handler, with fallback "not found" reporting. */
class ShortcutDispatcher(private val registry: ShortcutRegistry) {

    private val handlers = mutableMapOf<String, () -> Unit>()

    fun register(id: String, handler: () -> Unit) {
        handlers[id] = handler
    }

    fun handle(
        pressedKey: String,
        ctrl: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
        meta: Boolean = false
    ): Boolean {
        val def = registry.matches(KeyChord(pressedKey, ctrl, shift, alt, meta)) ?: return false
        val handler = handlers[def.id] ?: return false
        handler()
        return true
    }

    fun unhandledDefaults(): List<ShortcutDef> =
        registry.all().filter { it.id !in handlers }
}

/** Default shortcut catalog (mirrors the built-in desktop experience). */
fun defaultShortcuts(): List<ShortcutDef> = listOf(
    ShortcutDef("command-palette", "Command Palette", ShortcutCategory.App, KeyChord("k", ctrl = true), "Open the command palette"),
    ShortcutDef("focus-search", "Focus Search", ShortcutCategory.Search, KeyChord("/", ctrl = true), "Focus the global search"),
    ShortcutDef("quick-switch", "Quick Switch", ShortcutCategory.App, KeyChord("p", ctrl = true), "Quick switch between views"),
    ShortcutDef("again", "Again", ShortcutCategory.Review, KeyChord("1"), "Mark card Again"),
    ShortcutDef("hard", "Hard", ShortcutCategory.Review, KeyChord("2"), "Mark card Hard"),
    ShortcutDef("good", "Good", ShortcutCategory.Review, KeyChord("3"), "Mark card Good"),
    ShortcutDef("easy", "Easy", ShortcutCategory.Review, KeyChord("4"), "Mark card Easy"),
    ShortcutDef("show-answer", "Show Answer", ShortcutCategory.Review, KeyChord(" ", shift = false), "Reveal the answer"),
    ShortcutDef("undo", "Undo", ShortcutCategory.Review, KeyChord("z", ctrl = true), "Undo last review action"),
    ShortcutDef("suspend", "Suspend Card", ShortcutCategory.Review, KeyChord("s"), "Suspend current card"),
    ShortcutDef("bury", "Bury Card", ShortcutCategory.Review, KeyChord("b"), "Bury current card"),
    ShortcutDef("skip", "Skip", ShortcutCategory.Review, KeyChord("enter", ctrl = true), "Skip current card"),
    ShortcutDef("retry", "Retry", ShortcutCategory.Review, KeyChord("r"), "Retry the current card"),
    ShortcutDef("preview", "Preview", ShortcutCategory.Browser, KeyChord("p", shift = false), "Preview card"),
    ShortcutDef("delete-card", "Delete Card", ShortcutCategory.Browser, KeyChord("delete"), "Delete selected card"),
    ShortcutDef("select-all", "Select All", ShortcutCategory.Browser, KeyChord("a", ctrl = true), "Select all cards"),
    ShortcutDef("grid-view", "Grid View", ShortcutCategory.Browser, KeyChord("g"), "Switch browser to grid"),
    ShortcutDef("list-view", "List View", ShortcutCategory.Browser, KeyChord("l"), "Switch browser to list"),
    ShortcutDef("open-dashboard", "Dashboard", ShortcutCategory.Navigation, KeyChord("1", alt = true), "Open dashboard"),
    ShortcutDef("open-browser", "Browser", ShortcutCategory.Navigation, KeyChord("2", alt = true), "Open browser"),
    ShortcutDef("open-review", "Review", ShortcutCategory.Navigation, KeyChord("3", alt = true), "Open review"),
    ShortcutDef("open-writing", "Writing Practice", ShortcutCategory.Navigation, KeyChord("5", alt = true), "Open kanji writing practice"),
    ShortcutDef("open-grammar", "Grammar Practice", ShortcutCategory.Navigation, KeyChord("6", alt = true), "Open grammar practice"),
    ShortcutDef("open-library", "Library", ShortcutCategory.Navigation, KeyChord("l", ctrl = true, shift = true), "Open the deck library"),
    ShortcutDef("open-stats", "Statistics", ShortcutCategory.Navigation, KeyChord("4", alt = true), "Open statistics"),
    ShortcutDef("open-settings", "Settings", ShortcutCategory.Navigation, KeyChord("comma", ctrl = true), "Open settings"),
    // Ctrl+T is reserved for the workspace tab system (new tab), so Theme
    // Studio lives on Ctrl+M.
    ShortcutDef("open-themes", "Theme Studio", ShortcutCategory.Navigation, KeyChord("m", ctrl = true), "Open theme studio"),
    ShortcutDef("open-history", "Activity Log", ShortcutCategory.Navigation, KeyChord("y", ctrl = true), "Open activity log"),
    ShortcutDef("open-transfer", "Import / Export", ShortcutCategory.Navigation, KeyChord("i", ctrl = true), "Open import/export"),
    ShortcutDef("toggle-nav", "Toggle Navigation", ShortcutCategory.App, KeyChord("n", ctrl = true, shift = true), "Cycle navigation through expanded / compact / bubble modes"),

    // ---- Media workspace ------------------------------------------
    // Transport hotkeys live in the media workspace's own configurable
    // catalog (Media → Settings → Keyboard shortcuts) so they are rebindable
    // without colliding with review/browser keys. Only the navigation chord
    // to open the workspace belongs to the global registry.
    ShortcutDef("open-media", "Media", ShortcutCategory.Navigation, KeyChord("v", alt = true), "Open the media workspace"),

    // ---- Remaining workspace destinations --------------------------
    // Every workspace view that has a dispatcher handler must also exist in
    // this catalog, or the chord can never be matched (registry.matches runs
    // against catalog entries only). These complete the keyboard map so
    // every destination is reachable without a mouse.
    ShortcutDef("open-exams", "Exams", ShortcutCategory.Study, KeyChord("e", ctrl = true, shift = true), "Open the exams workspace"),
    ShortcutDef("open-mistakes", "Mistakes", ShortcutCategory.Study, KeyChord("x", ctrl = true, shift = true), "Open the mistakes review"),
    ShortcutDef("open-dictionary", "Dictionary", ShortcutCategory.Navigation, KeyChord("d", ctrl = true, shift = true), "Open the dictionary manager"),
    ShortcutDef("open-mining", "Mining", ShortcutCategory.Study, KeyChord("m", ctrl = true, alt = true), "Open the mining workspace"),
    ShortcutDef("open-reading", "Reading", ShortcutCategory.Reading, KeyChord("r", ctrl = true, shift = true), "Open the reading workspace"),
    ShortcutDef("open-curriculum", "Curriculum", ShortcutCategory.Study, KeyChord("c", ctrl = true, shift = true), "Open the curriculum workspace"),
    ShortcutDef("open-graph", "Knowledge Graph", ShortcutCategory.Study, KeyChord("g", ctrl = true, shift = true), "Open the knowledge graph"),
    ShortcutDef("open-browser2", "Web Browser", ShortcutCategory.Navigation, KeyChord("b", ctrl = true, shift = true), "Open the learning browser"),
    ShortcutDef("open-ocr", "OCR", ShortcutCategory.Navigation, KeyChord("o", ctrl = true, shift = true), "Open the OCR workspace"),
    ShortcutDef("open-integrations", "Integrations", ShortcutCategory.Navigation, KeyChord("i", ctrl = true, shift = true), "Open the integrations workspace"),
    ShortcutDef("open-game", "Game", ShortcutCategory.World, KeyChord("f9"), "Open the game world"),
    // Ctrl+Shift+M is the AGENTS.md mining-dialog chord; Ctrl+Alt+M opens the
    // Mining workspace, keeping the two exclusive.
    ShortcutDef("mine-selection", "Mine Selection", ShortcutCategory.App, KeyChord("m", ctrl = true, shift = true), "Mine the currently selected text")
) + tabShortcuts()

/** Browser-style workspace tab shortcuts (Ctrl+T/W/Tab, Ctrl+1..9). */
private fun tabShortcuts(): List<ShortcutDef> = buildList {
    add(ShortcutDef("tab-new", "New Tab", ShortcutCategory.App, KeyChord("t", ctrl = true), "Open a new workspace tab"))
    add(ShortcutDef("tab-close", "Close Tab", ShortcutCategory.App, KeyChord("w", ctrl = true), "Close the active workspace tab"))
    add(ShortcutDef("tab-next", "Next Tab", ShortcutCategory.App, KeyChord("tab", ctrl = true), "Switch to the next workspace tab"))
    add(ShortcutDef("tab-previous", "Previous Tab", ShortcutCategory.App, KeyChord("tab", ctrl = true, shift = true), "Switch to the previous workspace tab"))
    add(ShortcutDef("tab-reopen", "Reopen Closed Tab", ShortcutCategory.App, KeyChord("t", ctrl = true, shift = true), "Reopen the last closed workspace tab"))
    (1..9).forEach { index ->
        add(ShortcutDef("tab-jump-$index", "Jump to Tab $index", ShortcutCategory.App, KeyChord("$index", ctrl = true), "Jump to workspace tab $index"))
    }
}
