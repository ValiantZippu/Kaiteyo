package ua.syt0r.kanji.desktop.ui.shortcuts

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.shortcuts.KeyChord
import ua.syt0r.kanji.desktop.engine.shortcuts.ShortcutCategory
import ua.syt0r.kanji.desktop.engine.shortcuts.ShortcutDef
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// SHORTCUTS
// Every command, grouped by category, with live
// chords, enable toggles and conflict-aware
// rebinding through the shortcut registry.
// ============================================

@Composable
fun ShortcutsView(state: AppState) {
    val sc = surfaceColors()
    var version by remember { mutableStateOf(0) }
    var rebindTarget by remember { mutableStateOf<ShortcutDef?>(null) }
    val registry = state.shortcutRegistry
    val defs = registry.defs

    Column(
        Modifier.fillMaxSize().padding(DsSpacing.Lg).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xl)
    ) {
        DsSectionHeader(
            title = resolveSuiteString { keyboardShortcutsTitle },
            subtitle = "${defs.count { it.enabled }} ${resolveSuiteString { shortcutsEnabledOf }} ${defs.size}",
            action = {
                DsButton(
                    text = resolveSuiteString { resetAll },

                    kind = DsButtonKind.Ghost,
                    onClick = {
                        registry.resetAll()
                        version++
                        state.activityLog.record(ActivityCategory.Settings, "Reset all shortcuts")
                        state.toastHost.show("Shortcuts restored to defaults", kind = ToastKind.Info)
                    },
                    compact = true
                )
            }
        )

        ShortcutCategory.entries.forEach { category ->
            val categoryDefs = defs.filter { it.category == category }
            if (categoryDefs.isEmpty()) return@forEach
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(category.name, color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                    categoryDefs.forEach { def ->
                        ShortcutRow(
                            def = def,
                            onToggle = { enabled ->
                                registry.setEnabled(def.id, enabled)
                                version++
                            },
                            onRebind = { rebindTarget = def },
                            onReset = {
                                registry.reset(def.id)
                                version++
                            }
                        )
                    }
                }
            }
        }
    }

    rebindTarget?.let { def ->
        DsPromptDialog(
            title = "Rebind '${def.name}'",
            placeholder = resolveSuiteString { rebindPlaceholder },
            initialValue = def.boundChord.label,
            onConfirm = { raw ->
                val chord = KeyChord.fromLabel(raw)
                if (chord == null) {
                    state.toastHost.show("Could not parse '${raw}' as a chord", kind = ToastKind.Error)
                } else {
                    registry.bind(def.id, chord)
                        .onSuccess {
                            version++
                            state.activityLog.record(ActivityCategory.Settings, "Rebound '${def.name}' to ${chord.label}")
                            state.toastHost.show("'${def.name}' → ${chord.label}", kind = ToastKind.Success)
                        }
                        .onFailure { e ->
                            state.toastHost.show(e.message ?: "Rebind failed", kind = ToastKind.Error)
                        }
                }
                rebindTarget = null
            },
            onDismiss = { rebindTarget = null }
        )
    }
}

@Composable
private fun ShortcutRow(
    def: ShortcutDef,
    onToggle: (Boolean) -> Unit,
    onRebind: () -> Unit,
    onReset: () -> Unit
) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(def.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            if (def.description.isNotBlank()) {
                Text(def.description, color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
        Spacer(Modifier.width(DsSpacing.Md))
        KeyBadge(label = def.boundChord.label)
        Spacer(Modifier.width(DsSpacing.Sm))
        DsButton(text = "Rebind", kind = DsButtonKind.Ghost, onClick = onRebind, compact = true)
        DsButton(text = "Reset", kind = DsButtonKind.Ghost, onClick = onReset, compact = true)
        DsToggle(
            checked = def.enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun KeyBadge(label: String) {
    val sc = surfaceColors()
    val ac = ua.syt0r.kanji.desktop.designsystem.accent()
    Text(
        text = label,
        color = ac.primary,
        fontSize = DsType.Label,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(ac.primary.copy(alpha = 0.12f))
            .padding(horizontal = DsSpacing.Sm, vertical = 3.dp)
    )
}
