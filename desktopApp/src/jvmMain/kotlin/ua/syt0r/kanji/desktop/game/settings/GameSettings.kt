package ua.syt0r.kanji.desktop.game.settings

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.camera.CameraMode
import ua.syt0r.kanji.desktop.game.engine.input.ControlScheme
import ua.syt0r.kanji.desktop.game.engine.input.InputCalibration
import ua.syt0r.kanji.desktop.game.learning.AssistanceLevel

/**
 * Game settings (spec §13, §15, §34-35, §116). Persisted in the save file
 * (and mirrored to Kaiteyo settings where they overlap, e.g. reduced motion).
 */
@Serializable
data class GameSettings(
    // Camera (spec §13)
    val cameraSensitivity: Float = 1f,
    val invertY: Boolean = false,
    val fov: Float = 60f,
    val cameraDistance: Float = 1f,
    val cameraSmoothing: Float = 1f,
    val defaultCameraMode: CameraMode = CameraMode.ThirdPerson,

    // Input (spec §15)
    val controlScheme: ControlScheme = ControlScheme.default(),
    val inputCalibration: InputCalibration = InputCalibration(),

    // Learning (spec §34-35, §109)
    val assistanceLevel: AssistanceLevel = AssistanceLevel.Normal,
    val kidMode: Boolean = false,

    // Audio (spec §91-92) — surfaces exist, backends are TODO.
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.8f,
    val ambientEnabled: Boolean = true,
    /** Speak NPC lines aloud (kana-clip voice; replay button always shows). */
    val dialogueTtsEnabled: Boolean = true,

    // Accessibility (spec §116)
    val subtitlesEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val textSizeScale: Float = 1f,

    // UX
    val showHints: Boolean = true,
    val autoSaveMinutes: Int = 5,

    // Time (spec §40) — world-clock pacing. Real seconds per world minute:
    // 0.5 = fast (a day in 12 minutes, good for play sessions), 60 = real
    // time. The slice defaults to compressed so day/night is visible.
    val secondsPerWorldMinute: Float = 0.5f,

    // Touch (spec §16) — dynamic-origin joystick + look drag + contextual
    // buttons, PUBG/Genshin-style. Off by default on desktop; a touch device
    // would flip it on.
    val touchControlsEnabled: Boolean = false
)
