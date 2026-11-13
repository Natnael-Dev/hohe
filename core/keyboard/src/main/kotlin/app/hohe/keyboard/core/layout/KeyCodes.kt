package app.hohe.keyboard.core.layout

/**
 * Special functional key codes used across the keyboard engine.
 *
 * Printable characters, digits, and standard symbols use their Unicode code points.
 * Special functional or state-trigger keys use negative codes or standard control codes.
 */
object KeyCodes {
    /** Shift state modifier key. */
    const val SHIFT: Int = -1

    /** CapsLock lock key. */
    const val CAPSLOCK: Int = -2

    /** Language or layout chooser picker. */
    const val LANGUAGE_CHOOSER: Int = -3

    /** Backspace / delete key. */
    const val DELETE: Int = -5

    /** Input Method Editor system picker. */
    const val IME_PICKER: Int = -6

    /** Switch to symbols layout. */
    const val SYMBOLS: Int = -31

    /** Switch to ABC / Latin layout. */
    const val ABC: Int = -30

    /** Switch language toggle. */
    const val LANGUAGE: Int = -16

    /** Emoji panel toggle. */
    const val EMOJI: Int = -11

    /** Numeric keypad toggle. */
    const val NUMPAD: Int = -22

    /** Shifted symbols toggle (the =\< key on symbols layout). */
    const val SHIFT_SYMBOLS: Int = -32

    /** Standard enter action. */
    const val ENTER: Int = 10

    /** Newline character insertion. */
    const val NEWLINE: Int = -12

    /** Space character insertion. */
    const val SPACE: Int = 32

    /** Long-press trigger for emoji from symbols key. */
    const val EMOJI_LONG_PRESS: Int = -110

    /** Long-press trigger for emoji from comma key. */
    const val COMMA_EMOJI: Int = -111

    /** Long-press trigger for emoji from enter key. */
    const val ENTER_EMOJI: Int = -112
}
