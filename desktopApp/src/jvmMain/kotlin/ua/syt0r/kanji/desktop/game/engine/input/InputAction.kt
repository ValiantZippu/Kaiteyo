package ua.syt0r.kanji.desktop.game.engine.input

/**
 * Game-wide input actions. Game logic reads [InputAction]s — never physical
 * keys — so keyboard, mouse, controller, touch and future devices all drive
 * the same behaviour. Bindings map actions to keys/buttons ([ControlScheme]).
 */
enum class InputAction {
    /** Movement. */
    MoveUp,
    MoveDown,
    MoveLeft,
    MoveRight,
    Run,

    /** World interaction. */
    Interact,
    Jump,

    /** Camera / view. */
    SwitchCamera,
    PhotoMode,

    /** Menus and surfaces. */
    OpenMap,
    OpenQuests,
    OpenCollection,
    OpenMenu,
    Back,

    /** Photo mode controls. */
    PhotoCapture,
    ZoomIn,
    ZoomOut,

    /** Debug. */
    ToggleDebug,

    /** Item / cosmetic quick-use (future). */
    UseItem
}

/**
 * A physical input (keyboard key, mouse button, gamepad button, touch
 * gesture). The set is device-agnostic enough to rebind across all of them.
 */
enum class GameKey(val label: String) {
    // Keyboard letters
    A("A"), B("B"), C("C"), D("D"), E("E"), F("F"), G("G"), H("H"),
    I("I"), J("J"), K("K"), L("L"), M("M"), N("N"), O("O"), P("P"),
    Q("Q"), R("R"), S("S"), T("T"), U("U"), V("V"), W("W"), X("X"),
    Y("Y"), Z("Z"),
    // Digits
    Digit0("0"), Digit1("1"), Digit2("2"), Digit3("3"), Digit4("4"),
    Digit5("5"), Digit6("6"), Digit7("7"), Digit8("8"), Digit9("9"),
    // Navigation / control
    ArrowUp("↑"), ArrowDown("↓"), ArrowLeft("←"), ArrowRight("→"),
    Space("Space"), Enter("Enter"), Escape("Esc"), Tab("Tab"),
    Shift("Shift"), Ctrl("Ctrl"), Alt("Alt"), Backspace("Backspace"),
    // Function keys
    F1("F1"), F2("F2"), F3("F3"), F4("F4"), F5("F5"), F6("F6"),
    F7("F7"), F8("F8"), F9("F9"), F10("F10"), F11("F11"), F12("F12"),
    // Mouse
    MouseLeft("LMB"), MouseRight("RMB"), MouseMiddle("MMB"),
    // Gamepad (Xbox-style names; PS names map through the provider)
    GamepadDPadUp("DPad ↑"), GamepadDPadDown("DPad ↓"),
    GamepadDPadLeft("DPad ←"), GamepadDPadRight("DPad →"),
    GamepadA("A"), GamepadB("B"), GamepadX("X"), GamepadY("Y"),
    GamepadLeftStick("L-Stick"), GamepadRightStick("R-Stick"),
    GamepadLeftBumper("LB"), GamepadRightBumper("RB"),
    GamepadStart("Start"), GamepadBack("Back"),
    // Touch
    TouchTap("Tap"), TouchDoubleTap("Double tap"),
    TouchPinch("Pinch")
}
