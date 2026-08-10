package ua.syt0r.kanji.desktop.appstate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory

// ============================================
// WORKSPACE PANELS — persistent multitasking
// Reference panels (dictionary, browser, stats,
// decks, themes, search) can be docked or floated
// over any view — including an active review
// session — and the layout persists across runs.
// ============================================

enum class PanelKind(val label: String) {
    Dictionary("Dictionary"),
    DeckBrowser("Deck Browser"),
    ThemeStudio("Theme Studio"),
    Search("Search"),
    Media("Media"),
    Ocr("OCR")
}

enum class PanelPlacement { Dock, Floating }

/** A live panel instance. Mutable fields are state-backed so drags re-compose. */
class OpenPanel(
    val id: String,
    val kind: PanelKind,
    initialPlacement: PanelPlacement,
    initialX: Int = 0,
    initialY: Int = 0,
    initialWidth: Int = 460,
    initialHeight: Int = 480
) {
    var placement by mutableStateOf(initialPlacement)
    var x by mutableStateOf(initialX)
    var y by mutableStateOf(initialY)
    var width by mutableStateOf(initialWidth)
    var height by mutableStateOf(initialHeight)
}

@Serializable
private data class PanelDto(
    val kind: String,
    val placement: String,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 460,
    val height: Int = 480
)

private val panelJson = Json { ignoreUnknownKeys = true }

fun AppState.togglePanel(kind: PanelKind, placement: PanelPlacement = PanelPlacement.Dock) {
    val existing = openPanels.firstOrNull { it.kind == kind }
    if (existing != null) {
        openPanels.remove(existing)
        activityLog.record(ActivityCategory.System, "Closed ${kind.label} panel")
    } else {
        openPanels.add(OpenPanel(id = "${kind.name}-${openPanels.size}", kind = kind, initialPlacement = placement))
        activityLog.record(ActivityCategory.System, "Opened ${kind.label} panel")
    }
    persistWorkspacePanels()
}

fun AppState.closePanel(id: String) {
    val panel = openPanels.firstOrNull { it.id == id } ?: return
    openPanels.remove(panel)
    activityLog.record(ActivityCategory.System, "Closed ${panel.kind.label} panel")
    persistWorkspacePanels()
}

fun AppState.setPanelPlacement(id: String, placement: PanelPlacement) {
    val panel = openPanels.firstOrNull { it.id == id } ?: return
    panel.placement = placement
    persistWorkspacePanels()
}

fun AppState.movePanel(id: String, x: Int, y: Int) {
    val panel = openPanels.firstOrNull { it.id == id } ?: return
    panel.x = x.coerceAtLeast(0)
    panel.y = y.coerceAtLeast(0)
    persistWorkspacePanels()
}

fun AppState.resizePanel(id: String, width: Int, height: Int) {
    val panel = openPanels.firstOrNull { it.id == id } ?: return
    panel.width = width.coerceIn(280, 1200)
    panel.height = height.coerceIn(200, 1200)
    persistWorkspacePanels()
}

fun AppState.persistWorkspacePanels() {
    settings.set(
        "workspace.panels",
        panelJson.encodeToString(
            openPanels.map {
                PanelDto(it.kind.name, it.placement.name, it.x, it.y, it.width, it.height)
            }
        )
    )
}

fun AppState.loadWorkspacePanels() {
    val raw = settings.getString("workspace.panels")
    if (raw.isBlank()) return
    runCatching {
        val dtos = panelJson.decodeFromString<List<PanelDto>>(raw)
        dtos.forEach { dto ->
            val kind = PanelKind.entries.firstOrNull { it.name == dto.kind } ?: return@forEach
            val placement = PanelPlacement.entries.firstOrNull { it.name == dto.placement } ?: PanelPlacement.Dock
            openPanels.add(
                OpenPanel(
                    id = "${kind.name}-${openPanels.size}",
                    kind = kind,
                    initialPlacement = placement,
                    initialX = dto.x,
                    initialY = dto.y,
                    initialWidth = dto.width,
                    initialHeight = dto.height
                )
            )
        }
    }
}
