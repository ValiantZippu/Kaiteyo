# Learnings — Gotchas AIs Discovered

> Append-only. Next AI reads this before coding.

- `gradle.properties`: `daemon=false` + 2GB heap — builds slow, never parallelize Gradle.
- `settings.gradle.kts` pins plugin versions literally — keep sync with `gradle/libs.versions.toml`.
- iOS `kotlin.native.ignoreDisabledTargets=true` expected on Windows — not a bug.
- `core/src/*/composeResources/files/` is managed by `PrepareAssetsTask` — files not in `AppAssets.kt` are deleted.
- `WindowDraggableArea` is `androidx.compose.foundation.window`, not `androidx.compose.ui.window`.
- `animateColorAsState` is `androidx.compose.animation`, `spring`/`tween` is `androidx.compose.animation.core`.
- `daemon` + 2GB cgroup OOM-kills Kotlin daemon mid-build — retry after killing daemons or enlarge heap.
