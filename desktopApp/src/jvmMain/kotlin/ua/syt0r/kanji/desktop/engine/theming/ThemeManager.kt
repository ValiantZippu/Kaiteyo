package ua.syt0r.kanji.desktop.engine.theming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import java.io.File
import java.io.IOException

// ============================================
// THEME MANAGER
// Owns the active theme and the user's theme
// library (custom + imported). Every mutation
// persists immediately to ~/.kaiteyo/themes/
// so the whole app live-updates through the
// active theme state.
// ============================================

class ThemeManager(
    private val themesDir: File = File(System.getProperty("user.home"), ".kaiteyo/themes")
) {

    private val presetsById: Map<String, KaiteyoTheme> = ThemePresets.all.associateBy { it.id }
    val presets: List<KaiteyoTheme> = ThemePresets.all

    val customThemes = mutableStateListOf<KaiteyoTheme>()

    var activeThemeId by mutableStateOf(ThemePresets.default.id)
        private set

    /** Bumped on every mutation so derived UI (library, preview) recomposes. */
    var revision by mutableStateOf(0)
        private set

    val activeTheme: KaiteyoTheme
        get() = byId(activeThemeId)

    val allThemes: List<KaiteyoTheme>
        get() = presets + customThemes

    init {
        load()
    }

    fun byId(id: String): KaiteyoTheme = customThemes.firstOrNull { it.id == id } ?: presetsById[id] ?: ThemePresets.default

    fun isPreset(id: String): Boolean = id in presetsById

    // ------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------

    fun applyTheme(id: String) {
        val exists = id in presetsById || customThemes.any { it.id == id }
        if (!exists) return
        activeThemeId = id
        saveActive()
        revision++
    }

    // ------------------------------------------------------------
    // Live editing — edits the active theme. If the active theme is
    // a built-in preset, it is promoted to a custom theme first so
    // the pristine preset stays intact and edits persist.
    // ------------------------------------------------------------

    fun updateActive(transform: (KaiteyoTheme) -> KaiteyoTheme) {
        val current = activeTheme
        val base = if (isPreset(current.id)) promoteToCustom(current) else current
        val updated = transform(base).copy(updatedAt = now())
        if (updated.id in customThemes.map { it.id }) {
            val idx = customThemes.indexOfFirst { it.id == updated.id }
            customThemes[idx] = updated
        } else {
            customThemes.add(updated)
        }
        activeThemeId = updated.id
        revision++
        save(updated)
        saveActive()
    }

    fun updateActiveColors(transform: (ThemeColors) -> ThemeColors) = updateActive { theme ->
        val old = theme.colors
        val colors = transform(old)
        // Keep the brand gradient in step with Primary / Secondary edits — unless
        // the user has explicitly recolored a stop away from those colors, in
        // which case their explicit stop wins.
        var gradient = theme.gradient
        if (gradient.enabled) {
            val stops = gradient.stops.toMutableList()
            if (colors.primary != old.primary && stops.isNotEmpty() && stops.first().color == old.primary) {
                stops[0] = stops[0].copy(color = colors.primary)
            }
            if (colors.secondary != old.secondary && stops.size > 1 && stops.last().color == old.secondary) {
                stops[stops.lastIndex] = stops.last().copy(color = colors.secondary)
            }
            if (stops != gradient.stops) gradient = gradient.copy(stops = stops)
        }
        theme.copy(colors = colors, gradient = gradient)
    }

    fun updateActiveGradient(transform: (ThemeGradient) -> ThemeGradient) =
        updateActive { it.copy(gradient = transform(it.gradient)) }

    fun updateActiveTypography(transform: (ThemeTypography) -> ThemeTypography) =
        updateActive { it.copy(typography = transform(it.typography)) }

    fun updateActiveScaling(transform: (ThemeScaling) -> ThemeScaling) =
        updateActive { it.copy(scaling = transform(it.scaling)) }

    fun updateActiveAnimation(transform: (ThemeAnimation) -> ThemeAnimation) =
        updateActive { it.copy(animation = transform(it.animation)) }

    fun updateActiveSpacing(transform: (ThemeSpacing) -> ThemeSpacing) =
        updateActive { it.copy(spacing = transform(it.spacing)) }

    fun updateActiveCorners(transform: (ThemeCorners) -> ThemeCorners) =
        updateActive { it.copy(corners = transform(it.corners)) }

    fun updateActiveEffects(transform: (ThemeEffects) -> ThemeEffects) =
        updateActive { it.copy(effects = transform(it.effects)) }

    fun updateActiveMeta(transform: (KaiteyoTheme) -> KaiteyoTheme) = updateActive(transform)

    private fun promoteToCustom(preset: KaiteyoTheme): KaiteyoTheme =
        preset.copy(
            id = "custom-${now().hashCode().toString(16).take(8)}",
            name = "${preset.name} (Custom)",
            source = "custom",
            createdAt = now(),
            updatedAt = now()
        )

    // ------------------------------------------------------------
    // Library CRUD
    // ------------------------------------------------------------

    fun duplicate(id: String, newName: String? = null): String {
        val base = byId(id)
        val copy = base.copy(
            id = "custom-${now().hashCode().toString(16).take(8)}",
            name = newName?.takeIf { it.isNotBlank() } ?: "${base.name} Copy",
            source = if (base.source == "custom") "custom" else "imported",
            favorite = false,
            createdAt = now(),
            updatedAt = now()
        )
        customThemes.add(copy)
        revision++
        save(copy)
        return copy.id
    }

    fun rename(id: String, name: String) {
        val idx = customThemes.indexOfFirst { it.id == id }
        if (idx < 0) return
        customThemes[idx] = customThemes[idx].copy(name = name.trim().ifBlank { customThemes[idx].name }, updatedAt = now())
        revision++
        save(customThemes[idx])
    }

    fun setDescription(id: String, description: String) {
        val idx = customThemes.indexOfFirst { it.id == id }
        if (idx < 0) return
        customThemes[idx] = customThemes[idx].copy(description = description, updatedAt = now())
        revision++
        save(customThemes[idx])
    }

    fun setAuthor(id: String, author: String) {
        val idx = customThemes.indexOfFirst { it.id == id }
        if (idx < 0) return
        customThemes[idx] = customThemes[idx].copy(author = author.trim().ifBlank { customThemes[idx].author }, updatedAt = now())
        revision++
        save(customThemes[idx])
    }

    fun toggleFavorite(id: String) {
        val idx = customThemes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            customThemes[idx] = customThemes[idx].copy(favorite = !customThemes[idx].favorite, updatedAt = now())
            revision++
            save(customThemes[idx])
            return
        }
        val preset = presetsById[id] ?: return
        val promoted = preset.copy(favorite = !preset.favorite, source = "custom", createdAt = now(), updatedAt = now())
        customThemes.add(promoted)
        revision++
        save(promoted)
    }

    /** Delete a custom theme. The active theme falls back to the default preset. */
    fun deleteTheme(id: String): Boolean {
        val idx = customThemes.indexOfFirst { it.id == id }
        if (idx < 0) return false
        val wasActive = activeThemeId == id
        customThemes.removeAt(idx)
        File(themesDir, "$id.json").delete()
        if (wasActive) activeThemeId = ThemePresets.default.id
        revision++
        saveActive()
        return true
    }

    /** Reset a custom theme back to its preset source values (or to the default). */
    fun resetTheme(id: String) {
        val sourcePreset = ThemePresets.all.firstOrNull { p -> id == p.id || id.startsWith(p.id + "-") }
        val target = sourcePreset ?: ThemePresets.default
        val reset = target.copy(
            id = id,
            name = if (isPreset(id)) target.name else byId(id).name,
            source = if (isPreset(id)) "preset" else "custom",
            createdAt = byId(id).createdAt,
            updatedAt = now()
        )
        val idx = customThemes.indexOfFirst { it.id == id }
        if (idx >= 0) customThemes[idx] = reset else customThemes.add(reset)
        revision++
        save(reset)
    }

    fun resetAll() {
        customThemes.clear()
        customThemesDirectory().listFiles()?.forEach { it.delete() }
        activeThemeId = ThemePresets.default.id
        revision++
        saveActive()
    }

    // ------------------------------------------------------------
    // Import / export
    // ------------------------------------------------------------

    fun exportJson(id: String): String = ThemeSerializer.export(byId(id))

    fun importJson(text: String): Boolean = try {
        val theme = ThemeSerializer.validate(text).getOrThrow()
        val clean = if (theme.id in presetsById) theme.copy(id = "imported-${theme.id}", source = "imported") else theme
        val existing = customThemes.indexOfFirst { it.id == clean.id }
        val imported = clean.copy(
            name = if (existing >= 0) "${clean.name} (Import)" else clean.name,
            source = "imported",
            updatedAt = now()
        )
        if (existing >= 0) customThemes[existing] = imported else customThemes.add(imported)
        revision++
        save(imported)
        true
    } catch (e: Exception) {
        false
    }

    fun exportAllJson(): String {
        val payload = allThemes.joinToString("\n") { ThemeSerializer.export(it) }
        return payload
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private fun customThemesDirectory(): File {
        val dir = File(themesDir, "custom")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun load() {
        runCatching {
            val activeFile = File(themesDir, "active.json")
            if (activeFile.exists()) {
                val saved = activeFile.readText().trim().removeSurrounding("\"")
                if (saved.isNotBlank() && (saved in presetsById || File(customThemesDirectory(), "$saved.json").exists())) {
                    activeThemeId = saved
                }
            }
            customThemesDirectory().listFiles()
                ?.filter { it.extension == "json" }
                ?.forEach { file ->
                    runCatching {
                        val theme = ThemeSerializer.import(file.readText()).getOrNull()
                        if (theme != null && theme.id !in presetsById) customThemes.add(theme)
                    }
                }
        }
    }

    private fun save(theme: KaiteyoTheme) {
        runCatching {
            customThemesDirectory().mkdirs()
            File(customThemesDirectory(), "${theme.id}.json").writeText(ThemeSerializer.export(theme))
        }
    }

    private fun saveActive() {
        runCatching {
            themesDir.mkdirs()
            File(themesDir, "active.json").writeText("\"$activeThemeId\"")
        }
    }

    private fun now(): String = Clock.System.now().toString()
}
