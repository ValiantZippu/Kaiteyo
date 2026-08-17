package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.engine.input.GameKey
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.JnaGamepadProvider
import ua.syt0r.kanji.desktop.game.engine.input.toGameKey
import ua.syt0r.kanji.desktop.game.learning.AssistanceLevel

/**
 * Game settings (spec §13, §15, §34-35, §109): assistance level (which
 * controls how much reading/translation the world shows), camera defaults,
 * input calibration, controller layout, control rebinding (keyboard +
 * gamepad), touch and dialogue voice. All values apply live and are saved
 * with the journey.
 */
@Composable
fun GameSettingsView(session: GameSession) {
    val settings = session.settings
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        SectionTitle("Learning assistance")
        // Kids mode (spec §68): a separate content + support layer, not just
        // a difficulty label. It pins the effective assistance to the Kids
        // level and swaps objects to their simpler kid vocabulary (kidTargets).
        OptionRow(
            label = "Kids mode — simpler words, full support",
            selected = settings.kidMode,
            onClick = {
                val turningOn = !settings.kidMode
                session.settings = settings.copy(
                    kidMode = turningOn,
                    assistanceLevel = if (turningOn) AssistanceLevel.Kids else settings.assistanceLevel
                )
            }
        )
        Text(
            text = "Objects teach their simpler vocabulary (水/お茶/ジュース over 飲み物) and reading + translation always show. Kids mode overrides the assistance level below.",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = DsType.Caption
        )
        AssistanceLevel.entries.forEach { level ->
            OptionRow(
                label = level.label,
                selected = settings.assistanceLevel == level,
                onClick = { session.settings = settings.copy(assistanceLevel = level) }
            )
        }
        SectionTitle("Camera")
        OptionRow(
            label = "Third person (default)",
            selected = settings.defaultCameraMode == ua.syt0r.kanji.desktop.game.engine.camera.CameraMode.ThirdPerson,
            onClick = {
                session.settings = settings.copy(
                    defaultCameraMode = ua.syt0r.kanji.desktop.game.engine.camera.CameraMode.ThirdPerson
                )
            }
        )
        OptionRow(
            label = "First person (default)",
            selected = settings.defaultCameraMode == ua.syt0r.kanji.desktop.game.engine.camera.CameraMode.FirstPerson,
            onClick = {
                session.settings = settings.copy(
                    defaultCameraMode = ua.syt0r.kanji.desktop.game.engine.camera.CameraMode.FirstPerson
                )
            }
        )
        OptionRow(
            label = "Invert mouse Y",
            selected = settings.invertY,
            onClick = { session.settings = settings.copy(invertY = !settings.invertY) }
        )
        SectionTitle("Voice")
        OptionRow(
            label = "Speak dialogue lines (kana voice)",
            selected = settings.dialogueTtsEnabled,
            onClick = { session.settings = settings.copy(dialogueTtsEnabled = !settings.dialogueTtsEnabled) }
        )
        SectionTitle("Audio")
        StepperRow(
            label = "Sound effects",
            value = settings.sfxVolume,
            onChange = { session.settings = settings.copy(sfxVolume = it) }
        )
        StepperRow(
            label = "Music / ambient",
            value = settings.musicVolume,
            onChange = { session.settings = settings.copy(musicVolume = it) }
        )
        OptionRow(
            label = "Ambient pad (per-region atmosphere)",
            selected = settings.ambientEnabled,
            onClick = { session.settings = settings.copy(ambientEnabled = !settings.ambientEnabled) }
        )
        SectionTitle("Accessibility")
        OptionRow(
            label = "Subtitles in dialogue",
            selected = settings.subtitlesEnabled,
            onClick = { session.settings = settings.copy(subtitlesEnabled = !settings.subtitlesEnabled) }
        )
        OptionRow(
            label = "Show hints",
            selected = settings.showHints,
            onClick = { session.settings = settings.copy(showHints = !settings.showHints) }
        )
        OptionRow(
            label = "Reduced motion",
            selected = settings.reducedMotion,
            onClick = { session.settings = settings.copy(reducedMotion = !settings.reducedMotion) }
        )
        SectionTitle("Controller")
        val gamepad = session.input.gamepad as? ua.syt0r.kanji.desktop.game.engine.input.JnaGamepadProvider
        Text(
            text = if (gamepad?.connected == true) "Controller connected — ${gamepad?.layout?.label ?: "?"} (hot-plug on)"
            else "No controller detected (XInput on Windows / joystick on Linux) — plug one in anytime",
            color = if (gamepad?.connected == true) Color(0xFFA5D6A7) else Color.White.copy(alpha = 0.5f),
            fontSize = DsType.Caption
        )
        ua.syt0r.kanji.desktop.game.engine.input.GamepadLayout.entries.forEach { layout ->
            OptionRow(
                label = "Layout: ${layout.label}",
                selected = gamepad?.layout == layout,
                onClick = {
                    gamepad?.layout = layout
                }
            )
        }
        SectionTitle("Controls")
        InputAction.entries.forEach { action ->
            RebindRow(session, action)
        }
        DsButton(
            text = "Reset control bindings",
            kind = DsButtonKind.Ghost,
            onClick = {
                session.input.resetBindings()
                session.settings = session.settings.copy(controlScheme = session.input.scheme)
            }
        )
        SectionTitle("Controller calibration")
        StepperRow(
            label = "Left stick dead zone",
            value = settings.inputCalibration.leftStickDeadZone,
            onChange = { value ->
                session.settings = settings.copy(
                    inputCalibration = settings.inputCalibration.copy(leftStickDeadZone = value)
                )
                session.input.calibration = session.settings.inputCalibration
            }
        )
        StepperRow(
            label = "Right stick dead zone",
            value = settings.inputCalibration.rightStickDeadZone,
            onChange = { value ->
                session.settings = settings.copy(
                    inputCalibration = settings.inputCalibration.copy(rightStickDeadZone = value)
                )
                session.input.calibration = session.settings.inputCalibration
            }
        )
        SectionTitle("Time")
        Text(
            text = "World: ${session.clock.hourLabel()} · ${session.clock.phase.label} · ${session.seasons.current.label} · ${session.weather.current.label}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )
        // Pacing presets (spec §40): a day in 12 min (fast), a day in 2 h
        // (standard), or real time — the setting is applied live each tick.
        listOf(
            "Fast (a day in 12 min)" to 0.5f,
            "Standard (a day in 2 h)" to 5f,
            "Real time (60 s / min)" to 60f
        ).forEach { (label, seconds) ->
            OptionRow(
                label = label,
                selected = kotlin.math.abs(settings.secondsPerWorldMinute - seconds) < 0.01f,
                onClick = { session.settings = settings.copy(secondsPerWorldMinute = seconds) }
            )
        }
        SectionTitle("Touch")
        OptionRow(
            label = "Touch controls (dynamic joystick + look drag)",
            selected = settings.touchControlsEnabled,
            onClick = { session.settings = settings.copy(touchControlsEnabled = !settings.touchControlsEnabled) }
        )
        Text(
            text = "Left side: move pad appears where you touch · right side: drag to look, tap to interact · contextual buttons bottom-right.",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = DsType.Caption
        )
    }
}

/** A rebindable action row: shows current keys, captures the next one. */
@Composable
private fun RebindRow(session: GameSession, action: InputAction) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val keys = session.input.scheme.keysFor(action)
    val focusRequester = remember { FocusRequester() }
    var listening by remember { mutableStateOf(false) }

    fun applyBind(key: GameKey) {
        session.input.bind(action, key)
        session.settings = session.settings.copy(controlScheme = session.input.scheme)
        listening = false
    }

    // Keyboard capture — the row takes focus and eats the next plain key.
    LaunchedEffect(listening) {
        if (listening) focusRequester.requestFocus()
    }
    // Gamepad capture — poll the provider's press queue while listening.
    LaunchedEffect(listening) {
        if (!listening) return@LaunchedEffect
        while (true) {
            val key = (session.input.gamepad as? JnaGamepadProvider)?.consumeNewButtonPress()
            if (key != null) {
                applyBind(key)
                return@LaunchedEffect
            }
            delay(120)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    listening -> Color(0xFFFFD54F).copy(alpha = 0.22f)
                    hovered -> Color.White.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (listening) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent true
                            if (event.isCtrlPressed || event.isMetaPressed) return@onKeyEvent true
                            val key = event.key.toGameKey()
                            when {
                                key == null -> true
                                key == GameKey.Escape -> {
                                    listening = false
                                    true
                                }
                                else -> {
                                    applyBind(key)
                                    true
                                }
                            }
                        }
                } else {
                    Modifier
                        .clickable(interactionSource = interaction, indication = null) { listening = true }
                        .hoverable(interaction)
                }
            )
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = action.displayName(),
            color = if (listening) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f),
            fontSize = DsType.Body,
            fontWeight = if (listening) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = if (listening) "Press a key or button… (Esc cancels)"
            else keys.joinToString(" / ") { it.label }.ifBlank { "—" },
            color = if (listening) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.45f),
            fontSize = DsType.Caption
        )
    }
}

/** Human-readable name for every rebindable action. */
private fun InputAction.displayName(): String = when (this) {
    InputAction.MoveUp -> "Move forward"
    InputAction.MoveDown -> "Move back"
    InputAction.MoveLeft -> "Move left"
    InputAction.MoveRight -> "Move right"
    InputAction.Run -> "Run"
    InputAction.Interact -> "Interact / talk"
    InputAction.Jump -> "Jump"
    InputAction.SwitchCamera -> "Switch camera"
    InputAction.PhotoMode -> "Photo mode"
    InputAction.OpenMap -> "Open map"
    InputAction.OpenQuests -> "Open quests"
    InputAction.OpenCollection -> "Open collection"
    InputAction.OpenMenu -> "Open menu"
    InputAction.Back -> "Back / cancel"
    InputAction.PhotoCapture -> "Take photo"
    InputAction.ZoomIn -> "Zoom in"
    InputAction.ZoomOut -> "Zoom out"
    InputAction.ToggleDebug -> "Toggle debug"
    InputAction.UseItem -> "Use item"
}

/** A − / value / + stepper for a 0..1 setting (volume, dead zones). */
@Composable
private fun StepperRow(label: String, value: Float, onChange: (Float) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (hovered) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = DsType.Body
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            StepButton("−") { onChange((value - 0.05f).coerceIn(0f, 1f)) }
            Text(
                text = "${(value * 100).toInt()}%",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Body,
                modifier = Modifier.padding(horizontal = DsSpacing.Xs)
            )
            StepButton("+") { onChange((value + 0.05f).coerceIn(0f, 1f)) }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Text(
        text = label,
        color = if (hovered) Color(0xFFFFD54F) else Color.White,
        fontSize = DsType.Body,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(if (hovered) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFFFFD54F),
        fontSize = DsType.Label,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = DsSpacing.Sm)
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> Color(0xFFFFD54F).copy(alpha = 0.16f)
                    hovered -> Color.White.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (selected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
        Text(
            text = if (selected) "●" else "○",
            color = if (selected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f),
            fontSize = DsType.Body
        )
    }
}
