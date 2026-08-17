package ua.syt0r.kanji.desktop.game.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

// ============================================================
// COLLECTION (spec §46, §87-89)
// Stamps, postcards, phrases, locations, kanji. Collections
// reinforce exploration — never inventory-management hell.
// ============================================================

@Serializable
enum class CollectibleKind(val label: String) {
    Stamp("Stamp"),
    Postcard("Postcard"),
    Phrase("Phrase"),
    Location("Location"),
    Word("Word"),
    Kanji("Kanji"),
    Souvenir("Souvenir")
}

@Serializable
data class Collectible(
    val id: String,
    val kind: CollectibleKind,
    val title: String,
    val titleJp: String = "",
    val description: String = "",
    val locationId: String? = null,
    val knowledgeId: String? = null,
    val icon: String = ""
)

@Serializable
data class CollectionData(
    val unlocked: List<String> = emptyList()
)

/** Owns every collectible and what the player has found. */
class CollectionManager(
    catalogue: List<Collectible>
) {
    private val byId = catalogue.associateBy { it.id }
    private val unlockedIds = mutableSetOf<String>()

    var revision by mutableStateOf(0)
        private set

    val all: List<Collectible> get() = byId.values.sortedBy { it.title }

    fun collectible(id: String): Collectible? = byId[id]

    fun isUnlocked(id: String): Boolean = id in unlockedIds

    fun unlock(id: String): Boolean {
        if (byId[id] == null || !unlockedIds.add(id)) return false
        revision++
        return true
    }

    fun unlocked(): List<Collectible> = all.filter { it.id in unlockedIds }

    fun countByKind(kind: CollectibleKind): Pair<Int, Int> {
        val total = byId.values.count { it.kind == kind }
        val owned = unlockedIds.count { byId[it]?.kind == kind }
        return owned to total
    }

    fun snapshot(): CollectionData = CollectionData(unlocked = unlockedIds.sorted())

    fun restore(data: CollectionData) {
        unlockedIds.clear()
        unlockedIds.addAll(data.unlocked)
        revision++
    }
}
