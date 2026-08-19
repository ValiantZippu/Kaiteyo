package ua.syt0r.kanji.presentation.screen.main.screen.learner_profile

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import ua.syt0r.kanji.core.knowledge.ProfilePresentation

// ============================================================
// LEARNER PROFILE — CONTRACT
// ------------------------------------------------------------
// The level-adaptation picker (spec §23–§24). One knowledge
// model; profiles adapt presentation only — furigana, romaji,
// translations, rare readings, explanation depth, sentence
// difficulty, graph complexity and the kanji-page card preset.
// A profile never deletes data — it only changes defaults.
// ============================================================

interface LearnerProfileScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>
        fun select(profile: LearnerProfile)

        /** Persists the Custom profile's presentation overrides (spec §23). */
        fun updateCustom(presentation: ProfilePresentation)

        fun reset()
    }

    data class ScreenState(
        val selected: LearnerProfile,
        val presentation: ProfilePresentation,
        /** Saved Custom overrides (null until the user edits them). */
        val custom: ProfilePresentation? = null,
        val loaded: Boolean = false
    )
}
