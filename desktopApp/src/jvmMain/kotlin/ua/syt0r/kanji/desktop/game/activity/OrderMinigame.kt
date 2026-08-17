package ua.syt0r.kanji.desktop.game.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ============================================================
// ORDER MINIGAME (spec §56) — ordering food at a stall
// A real-world-like micro-flow: the stall's menu appears in
// Japanese, the player picks an item, the order completes and
// teaches the item's words. No arbitrary arcade scoring.
// ============================================================

/** One purchasable item on a stall's menu. */
data class MenuItem(
    val id: String,
    val nameJp: String,
    val reading: String,
    val meaning: String,
    /** In-game currency? No — the price is a fun number, not a grind gate. */
    val price: Int = 0,
    /** Knowledge node discovered when this item is ordered. */
    val knowledgeId: String? = null
)

/** The state of one ordering session at a stall. */
class OrderSession {

    /** Menu of the stall currently open (empty when no order is active). */
    var items by mutableStateOf<List<MenuItem>>(emptyList())
        private set

    var stallId by mutableStateOf<String?>(null)
        private set

    /** The item just ordered — drives the thanks state in the panel. */
    var lastOrdered by mutableStateOf<MenuItem?>(null)
        private set

    val isActive: Boolean get() = stallId != null

    /** Open the ordering flow for a stall with [menu]. */
    fun open(stallId: String, menu: List<MenuItem>) {
        this.stallId = stallId
        this.items = menu
        this.lastOrdered = null
    }

    /** Pick an item: records it and closes the menu (one order per visit). */
    fun order(itemId: String): MenuItem? {
        val item = items.firstOrNull { it.id == itemId } ?: return null
        lastOrdered = item
        return item
    }

    fun close() {
        stallId = null
        items = emptyList()
        lastOrdered = null
    }
}
