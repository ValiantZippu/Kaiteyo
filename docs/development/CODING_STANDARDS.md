# Kaiteyo (書いてよ) — Coding Standards

> These standards are **observed in the codebase**, not aspirational. Where a generic
> rule below conflicts with an existing pattern, follow the existing pattern and note
> the discrepancy.

## General principles

1. **Readability over cleverness** — code is read more than written
2. **Consistency** — match the surrounding file/module's patterns
3. **Minimalism** — the smallest correct change; prefer established libraries over
   custom code (`ENGINEERING_STANDARDS.md` §164)
4. **Type safety** — use Kotlin's type system to make illegal states unrepresentable
5. **Testability** — pure domain logic separated from UI so it can be unit-tested
6. **Token-driven UI** — never hardcode colors/spacing/radii; read `Ds*`/`Dimens` tokens

## Naming conventions

### Kotlin

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `ThemeManager`, `KaiteyoWindow` |
| Functions | camelCase | `resolveString`, `rememberMainNavigationState` |
| Properties | camelCase | `windowState`, `isMaximized` |
| Constants | UPPER_SNAKE_CASE | `ThemeTransitionMillis`, `SidebarElevation` |
| Composables | PascalCase | `KaiteyoTitleBar`, `DictionaryPopup` |
| State holders | PascalCase | `KaiteyoThemeState`, `GlowConfig` |
| ViewModels | `{Feature}ScreenViewModel` | `DeckDetailsScreenViewModel` |
| Contracts | `{Feature}ScreenContract` | `DeckDetailsScreenContract` |

### Files
- One primary class/interface per file; file name matches the primary name
- Compose screens follow the 4-file pattern (Contract / ViewModel / Module / Screen)
- Desktop `Ds*` components: one file per component family (`DsButtons.kt`, `DsCards.kt`)

## Formatting

- **Indentation**: 4 spaces (continuation: 8)
- **Braces**: `fun example() {`; `else`/`catch`/`finally` on the closing brace line
- **Spacing**: single space after keywords; no space before colon; space after colon;
  space after commas
- **Max line length**: 120 chars; wrap before operators
- **Imports**: no wildcards; grouped stdlib → Android/Compose → third-party → project,
  blank line between groups

## The screen pattern (core, all platforms)

Every feature screen in `core/.../screen/main/screen/<feature>/` uses 4 files:

```text
{Feature}ScreenContract.kt    # interface {Feature}ScreenContract { interface ViewModel ... }
{Feature}ScreenViewModel.kt   # class ... : Contract.ViewModel  (StateFlow-based)
{Feature}ScreenModule.kt      # Koin: multiplatformViewModel<Contract.ViewModel> { ... }
{Feature}Screen.kt|UI.kt      # @Composable, VM via getMultiplatformViewModel<Contract.ViewModel>()
```

- **Contract** — declares the `ViewModel` interface and any screen-level state types
  (`data class` state, `sealed interface` events/effects).
- **ViewModel** — exposes `StateFlow<State>`; receives a `viewModelScope`-style scope;
  never touches Compose.
- **Module** — `multiplatformViewModel<XContract.ViewModel> { XViewModel(...) }`;
  register the module in `di/AppModule.kt` `screenModules`.
- **Screen** — composable; collects state with `collectAsState()`; calls VM methods
  from callbacks.

New destinations: add a `MainDestination` to `MainNavigation.kt` and register it in
`defaultMainDestinations` (with its kotlinx.serialization configuration).

## Compose practices

### Composable signature

```kotlin
@Composable
fun MyComponent(
    param1: String,
    param2: Int = 0,
    modifier: Modifier = Modifier   // last, with default
)
```

### Modifier order

1. `size`, `width`, `height`, `fillMaxSize`…
2. `padding`
3. `background`, `border`, `clip`
4. `clickable`, `hoverable`, `scrollable` (interactive)
5. `align`, `weight`
6. `graphicsLayer`, `alpha`, `scale`
7. `testTag`, `semantics`

### State management

```kotlin
// Local UI state
var isExpanded by remember { mutableStateOf(false) }

// Derived state
val isValid by remember(input) { derivedStateOf { input.isNotEmpty() } }

// Animation state — always through config-aware helpers
val scale by animateFloatAsState(
    targetValue = if (isHovered) 1.05f else 1f,
    animationSpec = springAnim(),          // honors AnimationConfig
    label = "elementScale"
)
```

### Performance

- `remember` expensive computations; `derivedStateOf` derived values
- `key()`/stable keys in `LazyColumn`/`DsVirtualList`
- `@Stable` on state holders
- Animate `graphicsLayer`, not layout properties
- Respect `reducedMotion` and `AnimationSpeed` (see `docs/design/ANIMATION_SYSTEM.md`)

### Desktop suite

- Views: `@Composable fun XView(state: AppState)`; get state via
  `LocalAppState.current`/`rememberAppState()`
- Components: `Ds*` only (`DsButton`, `DsCard`, `DsDialog`, `DsTextField`…), tokens
  via `DsSpacing`, `DsRadius`, `DsType`, `DsMotion`, `DsElevation`, `DsSemantic`
- Panels/openers follow the existing view patterns (`DictionaryManagerView`,
  `ThemeStudioView`, `SettingsView` are canonical examples)

## Architecture

### Package structure (core screen)

```
screen/main/screen/<feature>/
├── {Feature}ScreenContract.kt
├── {Feature}ScreenViewModel.kt
├── {Feature}ScreenModule.kt
├── {Feature}Screen.kt / {Feature}ScreenUI.kt
├── data/          # configuration/screen data types
├── ui/            # feature-specific components
└── use_case/      # single-responsibility use cases (DeckDetails*UseCase)
```

### State flow (ViewModel)

```kotlin
// Contract
interface DeckDetailsScreenContract {
    interface ViewModel {
        val state: StateFlow<ScreenState>
        fun onEvent(event: ScreenEvent)
    }
    data class ScreenState(...)
    sealed interface ScreenEvent
}

// ViewModel
class DeckDetailsScreenViewModel(...) : DeckDetailsScreenContract.ViewModel {
    private val _state = MutableStateFlow(DeckDetailsScreenContract.ScreenState())
    override val state: StateFlow<DeckDetailsScreenContract.ScreenState> = _state.asStateFlow()
    ...
}
```

### Dependency injection

```kotlin
// Module
val featureScreenModule = module {
    multiplatformViewModel<FeatureScreenContract.ViewModel> {
        FeatureScreenViewModel(
            repository = get(),
            useCase = get()
        )
    }
}

// Composable
val viewModel = getMultiplatformViewModel<FeatureScreenContract.ViewModel>()
```

### expect/actual

Platform-specific behavior (backup archive, file pickers, sync transport, `main`
entry) uses `expect` declarations in `commonMain` with `actual`s in `jvmMain` /
`androidMain` / `iosMain`.

## Strings (i18n)

- Interface-based: `Strings` interface + `EnglishStrings` + `JapaneseStrings`
- Adding a string edits all three (interface + both impls) — compile enforces it
- Lookup: `resolveString { someString }`

## Documentation

- KDoc on public types/functions with `@param`/`@return` where non-obvious
- Inline comments explain **why**, not what
- Docs live in `docs/`; follow `DocumentationRules.md` (update map, no dead links,
  no placeholders)

## Testing

- Location: `core/src/commonTest/` (shared), `desktopApp/src/jvmTest/` (suite),
  `kjd/src/test/`
- Framework: kotlin.test (JUnit Platform)
- Naming: backtick sentences — ``fun `deinflect handles consecutive kana runs`()``
- Pure logic (scheduling, statistics, deinflection, search ranking, parsers) is
  tested; UI is not (yet)

## Git

- Commits: `type(scope): description` — one coherent change per commit
- Branches: `feature/…`, `fix/…`, `docs/…`, `refactor/…` off `develop`
- PRs to `develop`, squash-merged (see `GITHUB_WORKFLOW.md`)

## Related

- `AI_CONTEXT.md` — project facts, screen pattern registration, import rules
- `docs/design/UI_SYSTEM.md` — the component catalog
- `docs/engineering/ENGINEERING_STANDARDS.md` — the engineering contract
