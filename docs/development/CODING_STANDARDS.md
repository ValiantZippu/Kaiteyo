# Kaiteyo (書いてよ) — Coding Standards

## General Principles

1. **Readability over cleverness** — Write code that is easy to understand
2. **Consistency** — Follow existing patterns in the codebase
3. **Minimalism** — Less code is better code
4. **Type safety** — Use Kotlin's type system to prevent errors
5. **Testability** — Write code that can be tested

## Naming Conventions

### Kotlin
| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `ThemeManager`, `KaiteyoWindow` |
| Functions | camelCase | `animateColorAsState`, `resolveString` |
| Properties | camelCase | `windowState`, `isMaximized` |
| Constants | UPPER_SNAKE_CASE | `MAX_WIDTH`, `DEFAULT_RADIUS` |
| Composables | PascalCase | `KaiteyoTitleBar`, `FloatingControlButton` |
| State holders | PascalCase | `KaiteyoThemeState`, `GlowConfig` |
| Type aliases | PascalCase | `ColorToken`, `DpValue` |

### Files
- One class/interface per file (except for small related types)
- File name matches the primary class name
- Use `.kt` extension

## Formatting

### Indentation
- 4 spaces (no tabs)
- Continuation indent: 8 spaces

### Braces
- Opening brace on same line: `fun example() {`
- Closing brace on its own line
- `else`, `catch`, `finally` on same line as closing brace

### Spacing
- Single space after keywords: `if (condition)`, `for (item in list)`
- No space before colon: `val x: Int`
- Space after colon: `val x: Int`
- No space before comma: `fun example(a: Int, b: Int)`
- Space after comma: `fun example(a: Int, b: Int)`

### Maximum Line Length
- 120 characters
- Break before operators when wrapping

### Imports
- No wildcard imports (use explicit imports)
- Group imports by:
  1. Kotlin stdlib
  2. Android/Compose
  3. Third-party libraries
  4. Project imports
- Blank line between groups

## Compose Practices

### Composable Functions
```kotlin
@Composable
fun MyComponent(
    param1: String,
    param2: Int = 0,
    modifier: Modifier = Modifier
) {
    // Implementation
}
```

### Modifier Order
1. `size`, `width`, `height`, `fillMaxSize`, etc.
2. `padding`, `margin`
3. `background`, `border`, `clip`
4. `clickable`, `hoverable`, `scrollable`
5. `align`, `weight`
6. `graphicsLayer`, `alpha`, `scale`
7. `testTag`, `semantics`

### State Management
```kotlin
// Local UI state
var isExpanded by remember { mutableStateOf(false) }

// Derived state
val isValid by remember(input) { derivedStateOf { input.isNotEmpty() } }

// Animation state
val scale by animateFloatAsState(
    targetValue = if (isHovered) 1.1f else 1f,
    animationSpec = spring(dampingRatio = 0.5f),
    label = "elementScale"
)
```

### Performance
- Use `remember` to cache expensive computations
- Use `derivedStateOf` to derive state without recomposition
- Use `key()` in `LazyColumn` for stable item identity
- Avoid creating new objects in composition (use `remember`)
- Use `@Stable` annotation on state holders
- Keep composable functions small and focused

## Architecture

### Package Structure
```
feature/
├── FeatureScreen.kt        # Screen composable
├── FeatureContract.kt      # State/Event contracts
├── FeatureViewModel.kt     # ViewModel
└── components/             # Feature-specific components
```

### State Flow
```kotlin
// Contract
data class FeatureState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val error: String? = null
)

sealed class FeatureEvent {
    data object Load : FeatureEvent()
    data class SelectItem(val id: String) : FeatureEvent()
}

// ViewModel
class FeatureViewModel(
    private val repository: Repository
) : ViewModel() {
    private val _state = MutableStateFlow(FeatureState())
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    fun onEvent(event: FeatureEvent) {
        when (event) {
            is FeatureEvent.Load -> loadData()
            is FeatureEvent.SelectItem -> selectItem(event.id)
        }
    }
}
```

### Dependency Injection
```kotlin
// Module definition
val featureModule = module {
    factory { FeatureViewModel(get()) }
    factory { FeatureRepository(get()) }
}

// Injection in composable
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    // UI
}
```

## Documentation

### KDoc Comments
```kotlin
/**
 * A floating window control button with hover animations.
 *
 * @param icon The text character to display (e.g., "×", "─", "□")
 * @param onClick Callback when button is clicked
 * @param glowColor Color for hover glow effect
 * @param size Button size in dp
 */
@Composable
private fun FloatingControlButton(
    icon: String,
    onClick: () -> Unit,
    glowColor: Color,
    size: Dp = 32.dp
)
```

### Inline Comments
- Explain WHY, not WHAT
- Use `//` for single-line comments
- Use `/* */` for multi-line comments
- Avoid obvious comments (`// increment counter`)

## Testing

### Unit Tests
```kotlin
class FeatureViewModelTest {
    @Test
    fun `load data should update state`() = runTest {
        val viewModel = FeatureViewModel(mockRepository)
        viewModel.onEvent(FeatureEvent.Load)
        assertTrue(viewModel.state.value.isLoading)
    }
}
```

### Test Naming
- Use backtick names: ``fun `description of test`()``
- Follow pattern: `subject_action_expectedResult`

## Git

### Commit Messages
```
type(scope): description

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `style`, `chore`

### Branch Names
```
type/description
```
Examples: `feature/floating-sidebar`, `fix/window-drag`, `docs/architecture`
