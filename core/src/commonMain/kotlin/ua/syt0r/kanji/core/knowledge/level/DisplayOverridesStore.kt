package ua.syt0r.kanji.core.knowledge.level

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.knowledge.ProfilePresentation
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// DISPLAY OVERRIDES — per-user presentation overrides
// ------------------------------------------------------------
// A small persisted store for presentation flags the user can
// override regardless of the active learner profile (spec §24:
// romaji "always overridable, never destroys data"). A null
// value means "follow the profile" — the profile's own default
// keeps applying until the user explicitly overrides it, and
// clearing the override restores the profile default.
// ============================================================

@Serializable
data class RomajiOverridePreference(
    /** null = follow the learner profile's showRomaji default. */
    val override: Boolean? = null
)

class DisplayOverridesStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): RomajiOverridePreference {
        val raw = preferences.romajiOverrideJson.get()
        if (raw.isBlank()) return RomajiOverridePreference()
        return runCatching {
            Json.decodeFromString<RomajiOverridePreference>(raw)
        }.getOrDefault(RomajiOverridePreference())
    }

    suspend fun setRomaji(enabled: Boolean) {
        preferences.romajiOverrideJson.set(Json.encodeToString(RomajiOverridePreference(override = enabled)))
    }

    /** Returns to profile-default behavior. */
    suspend fun clearRomajiOverride() {
        preferences.romajiOverrideJson.set("")
    }
}
