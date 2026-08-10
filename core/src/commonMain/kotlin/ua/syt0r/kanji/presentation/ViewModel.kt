package ua.syt0r.kanji.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.definition.Definition
import org.koin.core.module.Module

/**
 * Registers a ViewModel in the Koin DI graph with platform-specific storage.
 *
 * @param scope The Koin definition that creates the ViewModel instance.
 */
inline fun <reified T> Module.multiplatformViewModel(
    crossinline scope: Definition<T>
) = platformMultiplatformViewModel(scope)

/** Platform-specific ViewModel registration. Implemented per target (Android, JVM, iOS). */
expect inline fun <reified T> Module.platformMultiplatformViewModel(
    crossinline scope: Definition<T>
)

/**
 * Lifecycle states that a [LifecycleAwareViewModel] can be in.
 * Set to [Visible] when the associated UI is on screen, [Hidden] when it is not.
 */
enum class LifecycleState { Visible, Hidden }

/**
 * A ViewModel that receives lifecycle callbacks (visible/hidden) from its
 * associated Composable via [getMultiplatformViewModel].
 */
interface LifecycleAwareViewModel {
    val lifecycleState: MutableStateFlow<LifecycleState>
}

/**
 * Retrieves a ViewModel instance from the Koin DI graph, injecting any
 * provided [args] and wiring up lifecycle tracking for [LifecycleAwareViewModel]s.
 *
 * @param args Optional arguments to pass to the ViewModel scope definition.
 */
@Composable
inline fun <reified T> getMultiplatformViewModel(vararg args: Any): T {
    val viewModel: T = platformGetMultiplatformViewModel(args)

    if (viewModel is LifecycleAwareViewModel) {
        DisposableEffect(Unit) {
            viewModel.lifecycleState.value = LifecycleState.Visible
            onDispose {
                viewModel.lifecycleState.value = LifecycleState.Hidden
            }
        }
    }

    return viewModel
}

@Composable
expect inline fun <reified T> platformGetMultiplatformViewModel(args: Array<out Any>): T
