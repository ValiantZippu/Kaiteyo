package ua.syt0r.kanji.core.knowledge

// ============================================================
// FREQUENCY SYSTEM
// ------------------------------------------------------------
// Frequency is normalized metadata, never decoration. A kanji's
// raw KANJIDIC rank maps onto five labeled bands. Bands always
// carry a text label + numeric rank so information never relies
// on color alone (accessibility).
// ============================================================

enum class FrequencyBand(
    val label: String,
    val jpLabel: String,
    /** Inclusive rank ranges the band covers; [Int.MAX_VALUE] = open top. */
    val range: IntRange
) {
    VeryCommon("Very common", "非常によく使う", 1..500),
    Common("Common", "よく使う", 501..1000),
    Moderate("Moderate", "普通", 1001..2000),
    Uncommon("Uncommon", "あまり使わない", 2001..3500),
    Rare("Rare", "まれ", 3501..Int.MAX_VALUE);

    companion object {
        fun forRank(rank: Int?): FrequencyBand? {
            if (rank == null || rank <= 0) return null
            return entries.firstOrNull { rank in it.range }
        }
    }
}

/** "#183" / "Unranked" — the numeric rank is always available next to a band. */
fun frequencyRankLabel(rank: Int?): String =
    if (rank == null || rank <= 0) "Unranked" else "#$rank"

/**
 * A frequency datum with its source labeled. The bundled kanji frequency
 * comes from KANJIDIC; word frequency sources are a roadmap item — the model
 * exists now so a real dataset can be attached later without rework.
 */
enum class FrequencySource(val label: String) {
    Kanjidic("KANJIDIC"),
    Unknown("Unknown")
}
