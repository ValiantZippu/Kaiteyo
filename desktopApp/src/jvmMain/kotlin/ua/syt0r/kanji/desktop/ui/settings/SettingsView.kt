package ua.syt0r.kanji.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsCategoryBadge
import ua.syt0r.kanji.desktop.designsystem.DsNumericField
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.settings.SettingCategory
import ua.syt0r.kanji.desktop.engine.settings.SettingDef
import ua.syt0r.kanji.desktop.engine.settings.SettingType
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// SETTINGS
// Categorized, searchable settings. Every value
// reads from the typed SettingsEngine and persists
// live; the UI mirrors engine state with a simple
// version counter so edits recompose instantly.
// ============================================

@Composable
fun SettingsView(state: AppState) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(0) }

    // Refresh the mirrored snapshot whenever the engine changes.
    val snapshot = remember(version) { state.settings.snapshot() }
    val defs = remember(version) {
        if (query.isBlank()) state.settings.defs.filter { it.searchable }
        else state.settings.search(query)
    }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search settings…"
        )

        if (defs.isEmpty()) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Xl)) {
                    Text("No settings match", color = sc.textSecondary, fontSize = DsType.Body)
                }
            }
        } else {
            val grouped = defs.groupBy { it.category }
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Xl)
            ) {
                grouped.keys.forEach { category ->
                    SettingsSection(
                        category = category,
                        defs = grouped.getValue(category),
                        snapshot = snapshot,
                        version = version,
                        onChanged = { state.settings.set(it.first, it.second); version++ },
                        onResetCategory = {
                            state.settings.resetCategory(category)
                            version++
                            state.activityLog.record(ActivityCategory.Settings, "Reset ${category.name} settings")
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    DsButton(
                        text = "Reset all settings",
                        icon = Icons.Default.RestartAlt,
                        kind = DsButtonKind.Ghost,
                        onClick = {
                            state.settings.resetAll()
                            version++
                            state.activityLog.record(ActivityCategory.Settings, "Reset all settings")
                            state.toastHost.show("All settings restored to defaults", kind = ToastKind.Info)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    category: SettingCategory,
    defs: List<SettingDef>,
    snapshot: Map<String, String>,
    version: Int,
    onChanged: (Pair<String, Any>) -> Unit,
    onResetCategory: () -> Unit
) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsSectionHeader(
                title = category.name,
                subtitle = "${defs.size} setting${if (defs.size == 1) "" else "s"}",
                action = {
                    DsButton(
                        text = "Reset",
                        kind = DsButtonKind.Ghost,
                        onClick = onResetCategory,
                        compact = true
                    )
                }
            )
            defs.forEach { def ->
                SettingRow(def, snapshot, version, onChanged)
            }
        }
    }
}

@Composable
private fun SettingRow(
    def: SettingDef,
    snapshot: Map<String, String>,
    version: Int,
    onChanged: (Pair<String, Any>) -> Unit
) {
    val sc = surfaceColors()
    val current = snapshot[def.key] ?: def.normalizedDefault

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = def.name,
                    color = sc.textPrimary,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.Medium
                )
                if (def.description.isNotBlank()) {
                    Text(
                        text = def.description,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            Spacer(Modifier.width(DsSpacing.Md))
            when (def.type) {
                SettingType.Boolean -> DsToggle(
                    checked = current.toBooleanStrictOrNull() ?: false,
                    onCheckedChange = { onChanged(def.key to it) }
                )
                SettingType.Enum -> DsSelect(
                    selected = current,
                    options = def.options.ifEmpty { listOf(current) },
                    onSelected = { onChanged(def.key to it) },
                    labelOf = { it },
                    modifier = Modifier.width(220.dp)
                )
                SettingType.Int -> DsNumericField(
                    value = current.toIntOrNull() ?: 0,
                    onValueChange = { onChanged(def.key to it) },
                    label = null
                )
                SettingType.Float -> {
                    val text = remember(def.key, version) { mutableStateOf(current) }
                    DsTextField(
                        value = text.value,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }.take(6)
                            text.value = filtered
                            onChanged(def.key to (filtered.toFloatOrNull() ?: 0f))
                        },
                        singleLine = true
                    )
                }
                SettingType.String, SettingType.List -> {
                    val text = remember(def.key, version) { mutableStateOf(current) }
                    DsTextField(
                        value = text.value,
                        onValueChange = { text.value = it; onChanged(def.key to it) },
                        singleLine = true
                    )
                }
            }
        }
    }
}
