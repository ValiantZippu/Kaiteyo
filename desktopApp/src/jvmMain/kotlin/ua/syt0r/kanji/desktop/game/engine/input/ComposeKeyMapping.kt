package ua.syt0r.kanji.desktop.game.engine.input

import androidx.compose.ui.input.key.Key

/**
 * Maps a Compose [Key] to the game's device-agnostic [GameKey]. Shared by the
 * canvas (gameplay) and the rebind UI (capture) so both agree on what a key
 * is — one source of truth (spec §14-15).
 */
fun Key.toGameKey(): GameKey? = when (this) {
    Key.A -> GameKey.A; Key.B -> GameKey.B; Key.C -> GameKey.C; Key.D -> GameKey.D
    Key.E -> GameKey.E; Key.F -> GameKey.F; Key.G -> GameKey.G; Key.H -> GameKey.H
    Key.I -> GameKey.I; Key.J -> GameKey.J; Key.K -> GameKey.K; Key.L -> GameKey.L
    Key.M -> GameKey.M; Key.N -> GameKey.N; Key.O -> GameKey.O; Key.P -> GameKey.P
    Key.Q -> GameKey.Q; Key.R -> GameKey.R; Key.S -> GameKey.S; Key.T -> GameKey.T
    Key.U -> GameKey.U; Key.V -> GameKey.V; Key.W -> GameKey.W; Key.X -> GameKey.X
    Key.Y -> GameKey.Y; Key.Z -> GameKey.Z
    Key.Zero -> GameKey.Digit0; Key.One -> GameKey.Digit1; Key.Two -> GameKey.Digit2
    Key.Three -> GameKey.Digit3; Key.Four -> GameKey.Digit4; Key.Five -> GameKey.Digit5
    Key.Six -> GameKey.Digit6; Key.Seven -> GameKey.Digit7; Key.Eight -> GameKey.Digit8
    Key.Nine -> GameKey.Digit9
    Key.DirectionUp -> GameKey.ArrowUp; Key.DirectionDown -> GameKey.ArrowDown
    Key.DirectionLeft -> GameKey.ArrowLeft; Key.DirectionRight -> GameKey.ArrowRight
    Key.Spacebar -> GameKey.Space; Key.Enter -> GameKey.Enter; Key.Escape -> GameKey.Escape
    Key.Tab -> GameKey.Tab; Key.Backspace -> GameKey.Backspace
    Key.ShiftLeft -> GameKey.Shift; Key.ShiftRight -> GameKey.Shift
    Key.CtrlLeft -> GameKey.Ctrl; Key.CtrlRight -> GameKey.Ctrl
    Key.AltLeft -> GameKey.Alt; Key.AltRight -> GameKey.Alt
    Key.F1 -> GameKey.F1; Key.F2 -> GameKey.F2; Key.F3 -> GameKey.F3
    Key.F4 -> GameKey.F4; Key.F5 -> GameKey.F5; Key.F6 -> GameKey.F6
    Key.F7 -> GameKey.F7; Key.F8 -> GameKey.F8; Key.F9 -> GameKey.F9
    Key.F10 -> GameKey.F10; Key.F11 -> GameKey.F11; Key.F12 -> GameKey.F12
    else -> null
}
