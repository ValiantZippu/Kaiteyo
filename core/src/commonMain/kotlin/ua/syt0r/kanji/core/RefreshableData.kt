package ua.syt0r.kanji.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.presentation.LifecycleState
import kotlin.time.measureTime

sealed interface RefreshableData<T> {
    class Loading<T> : RefreshableData<T>
    data class Loaded<T>(val value: T) : RefreshableData<T>

    /**
     * The value provider threw. Emitted instead of crashing the collector,
     * so screens can render an error + retry instead of spinning forever.
     */
    data class Failed<T>(val error: Throwable? = null) : RefreshableData<T>
}

inline fun <reified T> refreshableDataFlow(
    dataChangeFlow: Flow<Unit>,
    lifecycleState: StateFlow<LifecycleState>,
    noinline valueProvider: suspend CoroutineScope.() -> T
): Flow<RefreshableData<T>> {
    var firstLoad = true
    return channelFlow {
        dataChangeFlow.onStart { emit(Unit) }
            .collectLatest {
                send(RefreshableData.Loading())
                // The very first load must never be gated on lifecycle visibility:
                // a screen that just opened has to render instantly. Only refreshes
                // triggered by data changes wait for the screen to be visible again.
                if (!firstLoad) waitForVisibility(lifecycleState)
                firstLoad = false

                val value: T
                try {
                    val loadingTime = measureTime { value = valueProvider.invoke(this) }
                    Logger.d("Loaded ${T::class.qualifiedName} data, loadingTime[$loadingTime]")
                } catch (t: Throwable) {
                    // Cancellation is not a load failure — rethrow so structured
                    // concurrency (e.g. leaving the screen) stays intact.
                    if (t is CancellationException) throw t
                    Logger.w("Failed to load ${T::class.qualifiedName} data: ${t.message}")
                    send(RefreshableData.Failed(t))
                    return@collectLatest
                }

                send(RefreshableData.Loaded(value))
            }
    }.distinctLoading()
}


@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified T> refreshableDataProducerFlow(
    dataChangeFlow: Flow<Unit>,
    lifecycleState: StateFlow<LifecycleState>,
    noinline producer: suspend ProducerScope<T>.() -> Unit
): Flow<RefreshableData<T>> {
    var firstLoad = true
    return dataChangeFlow
        .onStart { emit(Unit) }
        .flatMapLatest {
            channelFlow<T> {
                // First load renders immediately; refreshes wait for visibility.
                if (!firstLoad) waitForVisibility(lifecycleState)
                firstLoad = false
                try {
                    producer(this)
                } catch (t: Throwable) {
                    // Cancellation is not a load failure — rethrow so structured
                    // concurrency (e.g. leaving the screen) stays intact.
                    if (t is CancellationException) throw t
                    Logger.w("Failed to produce ${T::class.qualifiedName} data: ${t.message}")
                    throw ProducerFailure(t)
                }
            }
                .map<T, RefreshableData<T>> { RefreshableData.Loaded(it) }
                .onStart { emit(RefreshableData.Loading()) }
                .catch { error ->
                    val cause = (error as? ProducerFailure)?.errorCause ?: error
                    emit(RefreshableData.Failed(cause))
                }
        }
        .distinctLoading()
}

suspend fun waitForVisibility(lifecycleState: StateFlow<LifecycleState>) {
    lifecycleState.filter { it == LifecycleState.Visible }.first()
}

/**
 * Internal marker so a producer failure can be distinguished from a
 * genuinely unexpected flow error after it crosses the [map] boundary.
 * Public only because [refreshableDataProducerFlow] is inline.
 */
class ProducerFailure(val errorCause: Throwable) : Exception(errorCause)

fun <T> Flow<RefreshableData<T>>.distinctLoading(): Flow<RefreshableData<T>> {
    return distinctUntilChanged { old, new ->
        if (old::class == new::class && old::class == RefreshableData.Loading::class) true
        else old == new
    }
}
