package ua.syt0r.kanji.presentation.screen.main.screen.learner_profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import ua.syt0r.kanji.core.knowledge.LearnerProfileCatalog
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.ProfilePresentation
import ua.syt0r.kanji.presentation.screen.main.screen.learner_profile.LearnerProfileScreenContract.ScreenState

class LearnerProfileScreenViewModel(
    private val viewModelScope: CoroutineScope,
    private val store: LearnerProfileStore
) : LearnerProfileScreenContract.ViewModel {

    private val _state = MutableStateFlow(ScreenState(LearnerProfile.Intermediate, LearnerProfileCatalog.defaultsFor(LearnerProfile.Intermediate)))
    override val state: StateFlow<ScreenState> = _state

    init {
        viewModelScope.launch {
            val preference = store.load()
            _state.value = ScreenState(
                selected = preference.profile,
                presentation = preference.effectivePresentation(),
                custom = preference.customPresentation,
                loaded = true
            )
        }
    }

    override fun select(profile: LearnerProfile) {
        // Selecting Custom once again keeps the user's saved overrides; a
        // fresh selection of Custom without overrides falls back to the
        // Intermediate-style defaults.
        val presentation = when (profile) {
            LearnerProfile.Custom ->
                _state.value.custom ?: LearnerProfileCatalog.defaultsFor(LearnerProfile.Custom)
            else -> LearnerProfileCatalog.defaultsFor(profile)
        }
        _state.value = _state.value.copy(selected = profile, presentation = presentation)
        viewModelScope.launch {
            store.saveProfile(profile)
        }
    }

    override fun updateCustom(presentation: ProfilePresentation) {
        // Editing the custom presentation also selects Custom (a custom
        // presentation only takes effect while Custom is active).
        _state.value = _state.value.copy(
            selected = LearnerProfile.Custom,
            presentation = presentation,
            custom = presentation
        )
        viewModelScope.launch {
            store.save(store.load().withCustomPresentation(presentation))
        }
    }

    override fun reset() {
        viewModelScope.launch {
            store.reset()
            _state.value = ScreenState(
                selected = LearnerProfile.Intermediate,
                presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.Intermediate),
                loaded = true
            )
        }
    }
}
