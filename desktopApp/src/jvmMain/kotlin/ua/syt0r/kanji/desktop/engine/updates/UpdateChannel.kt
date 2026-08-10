package ua.syt0r.kanji.desktop.engine.updates

// ============================================
// UPDATE CHANNELS
// Each channel reads its own feed file. The
// default is Stable; Beta/Nightly are opt-in.
// ============================================

enum class UpdateChannel(
    val displayName: String,
    val feedFileName: String
) {
    Stable("Stable", "update-stable.json"),
    Beta("Beta", "update-beta.json"),
    Nightly("Nightly", "update-nightly.json");

    companion object {
        fun fromName(name: String): UpdateChannel =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Stable
    }
}
