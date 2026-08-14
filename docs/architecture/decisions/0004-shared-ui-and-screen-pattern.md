# ADR-0004: Shared UI with the 4-File Screen Pattern + Koin DI

**Status**: Accepted

## Context

With a single Compose UI across desktop/Android/iOS, Kaiteyo needed a consistent way to
structure screens so ViewModels, Koin modules, and composables stay discoverable and
uniform — and so the desktop suite can reuse the same engine behind its own views.

## Decision

- Every feature screen in `core` follows a **4-file pattern** in
  `screen/main/screen/<feature>/`:
  1. `{Feature}ScreenContract.kt` — interface `{Feature}ScreenContract { interface ViewModel }`
  2. `{Feature}ScreenViewModel.kt` — implements the contract
  3. `{Feature}ScreenModule.kt` — Koin module registering the VM via
     `multiplatformViewModel<Contract.ViewModel>`
  4. `{Feature}Screen.kt` / `{Feature}ScreenUI.kt` — composables, obtaining the VM via
     `getMultiplatformViewModel<Contract.ViewModel>()`
- Every screen module is registered in `di/AppModule.kt` (`screenModules` list).
- **Koin** for dependency injection (`multiplatformViewModel` is an expect/actual pair in
  `presentation/ViewModel.kt`).
- State: `StateFlow` in ViewModels; `mutableStateOf`/`derivedStateOf` for local UI state;
  `CompositionLocal` for theme propagation.

## Alternatives

- ViewModel factories per platform (no DI) — rejected: verbose and inconsistent.
- Dagger/Hilt — rejected: heavy code generation; Koin is lightweight and KMP-friendly.

## Consequences

- New screens are cheap to add and follow a strict recipe.
- The desktop suite (`desktopApp/.../desktop/`) has its own view layer but shares core
  engines; it does not reuse the core screen pattern (it predates it and is
  self-contained — see ADR-0008).

## Implementation notes

- `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/screen/main/screen/`
- `core/src/commonMain/kotlin/ua/syt0r/kanji/di/AppModule.kt`
- `presentation/ViewModel.kt` (expect) + platform actuals.
