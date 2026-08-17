package ua.syt0r.kanji.desktop.game.ui.menus

/**
 * A minimal focus model for menu navigation (spec §14-15, §105): a cursor
 * over a vertical list of items with wrap-around, driven by the game's
 * input actions so keyboard and gamepad share one path. Pure and testable —
 * the composable layer just renders the highlighted row and calls [select].
 */
class FocusNav(private val size: Int) {

    var index: Int = 0
        private set

    val focused: Int get() = index

    init {
        require(size > 0) { "FocusNav needs at least one item" }
    }

    /** Move up (wrap). */
    fun up() {
        index = (index - 1 + size) % size
    }

    /** Move down (wrap). */
    fun down() {
        index = (index + 1) % size
    }

    fun select(): Int = index
}
