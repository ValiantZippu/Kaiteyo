# Skill — Scaffold a 4-File Screen

Pattern: `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/screen/main/screen/<feature>/`

Files:
1. `{Feature}ScreenContract.kt`
```kotlin
interface FeatureScreenContract {
    interface ViewModel {
        val state: StateFlow<State>
        fun onAction(action: Action)
    }
    data class State(val loading: Boolean = true, val items: List<Item> = emptyList(), val error: String? = null)
    sealed interface Action { data object Retry : Action }
}
```

2. `{Feature}ScreenViewModel.kt`
```kotlin
class FeatureScreenViewModel(repo: FeatureRepo) : FeatureScreenContract.ViewModel {
    private val _state = MutableStateFlow(State())
    override val state = _state.asStateFlow()
    override fun onAction(action: Action) { /* handle */ }
}
```

3. `{Feature}ScreenModule.kt`
```kotlin
val featureScreenModule = module {
    multiplatformViewModel<FeatureScreenContract.ViewModel> { FeatureScreenViewModel(get()) }
}
```

4. `{Feature}Screen.kt` / `{Feature}ScreenUI.kt`
```kotlin
@Composable fun FeatureScreen() {
    val vm = getMultiplatformViewModel<FeatureScreenContract.ViewModel>()
    val state by vm.state.collectAsState()
    ProvidePageIdentity("Feature > List") { /* UI */ }
}
```

Register:
- `core/src/commonMain/kotlin/ua/syt0r/kanji/di/AppModule.kt` → `screenModules += featureScreenModule`
- `.../screen/main/MainNavigation.kt` → `MainDestination.Feature` + `defaultMainDestinations` + serializer
- `.../presentation/common/ui/PageRegistry.kt` if used
- Strings: all three files

Verify: `:desktopApp:compileKotlinJvm` + check screen appears in NavShell/launchpad.

Reference: `AGENTS.md` §Screen pattern, `docs/development/AI_CONTEXT.md`
