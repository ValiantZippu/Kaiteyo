package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

/**
 * Provided by the Settings Center shell so descriptors rendered inside search
 * results (outside the category page) can still trigger navigation.
 */
val LocalSettingsNavigation = compositionLocalOf<MainNavigationState?> { null }

// ============================================
// SETTINGS CENTER DATA MODEL
// Settings are modeled as typed descriptors so
// the category pages and the instant search
// share one source of truth.
// ============================================

/**
 * A single searchable setting. [render] produces the full interactive row
 * (title, description and control) and is used both on the category page and
 * inside search results — so a matched setting is always fully functional.
 */
class SettingDescriptor(
    val id: String,
    val title: String,
    val description: String = "",
    val keywords: List<String> = emptyList(),
    val render: @Composable () -> Unit
) {

    /** Searchable text: title + description + keywords + id. */
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val haystack = buildString {
            append(title.lowercase())
            append(' ')
            append(description.lowercase())
            append(' ')
            append(id.lowercase())
            keywords.forEach { append(' '); append(it.lowercase()) }
        }
        return query.split(' ').filter { it.isNotBlank() }
            .all { haystack.contains(it) }
    }

}

// ============================================
// PREFERENCE BINDING
// Wraps a SuspendedProperty into live Compose
// state: reads the persisted value once, writes
// through on change, and mirrors external
// modifications (e.g. other screens).
// ============================================

class SettingBinding<T>(
    initial: T,
    private val scope: CoroutineScope,
    private val write: suspend (T) -> Unit
) {

    var value by mutableStateOf(initial)
        private set

    fun set(newValue: T) {
        if (newValue == value) return
        value = newValue
        scope.launch { runCatching { write(newValue) } }
    }

    fun update(transform: (T) -> T) = set(transform(value))

    fun resetTo(defaultValue: T) = set(defaultValue)

}

@Composable
fun <T : Any> rememberSetting(
    property: SuspendedProperty<T>,
    scope: CoroutineScope = rememberCoroutineScope()
): SettingBinding<T> {
    val binding = remember(property, scope) {
        SettingBinding(
            initial = runBlocking { property.get() },
            scope = scope,
            write = { property.set(it) }
        )
    }
    LaunchedEffect(property) {
        property.onModified.collect { external ->
            if (external != binding.value) binding.set(external)
        }
    }
    return binding
}
