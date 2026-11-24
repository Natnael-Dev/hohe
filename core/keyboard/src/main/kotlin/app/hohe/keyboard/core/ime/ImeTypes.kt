package app.hohe.keyboard.core.ime

import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.field.EnterAction
import app.hohe.keyboard.core.input.ShiftState
import app.hohe.keyboard.core.layout.KeyboardPage
import app.hohe.keyboard.core.layout.Layout
import app.hohe.keyboard.core.settings.TypingLanguage

/**
 * The only way the controller touches the host text field. The Android implementation wraps
 * `InputConnection`; tests use an in-memory fake. All offsets are UTF-16 chars.
 */
interface EditorPort {
    fun textBeforeCursor(n: Int): CharSequence

    fun hasSelection(): Boolean

    fun setComposingText(text: String)

    fun finishComposingText()

    fun commitText(text: String)

    /** Deletes the selection, or one user-perceived character before the cursor. */
    fun deleteBackward()

    /** Deletes [chars] UTF-16 chars before the cursor. */
    fun deleteBefore(chars: Int)

    /** Runs a non-newline editor action (go, search, send, next, done, previous). */
    fun performEditorAction(action: EnterAction)

    /** Moves the cursor by [delta] characters (negative = left), collapsing any selection. */
    fun moveCursor(delta: Int)

    fun beginBatch()

    fun endBatch()
}

/** Requests the controller cannot fulfil itself; the service implements them. */
interface ImeHost {
    fun showImePicker()

    fun openPanel(panel: Panel)

    fun startVoiceInput()

    fun openSettings()

    /** Key feedback on press (sound/haptic); [code] distinguishes delete/space/enter. */
    fun keyFeedback(code: Int)
}

/** Non-key surfaces that replace the key area (keyboard-engine-architecture §8). */
enum class Panel { NONE, EMOJI, CLIPBOARD, CHOOSER }

enum class StripItemKind { PRIMARY, TYPED, CANDIDATE, EMOJI }

data class StripItem(
    val text: String,
    val kind: StripItemKind,
    val source: Source,
) {
    /** Letter-by-letter result, drawn with a dotted underline (keyboard-visual-specification §4). */
    val isFallback: Boolean get() = source == Source.FALLBACK_TRANSLIT
}

/** Top bar content: the toolbar, or suggestions for [forInput] ("" for next-word predictions). */
data class StripState(
    val items: List<StripItem> = emptyList(),
    val forInput: String = "",
) {
    val showsToolbar: Boolean get() = items.isEmpty()

    companion object {
        val TOOLBAR = StripState()
    }
}

/** Immutable snapshot the views draw from. */
data class KeyboardUiState(
    val layout: Layout,
    val page: KeyboardPage,
    val shift: ShiftState,
    val language: TypingLanguage,
    val amharicActive: Boolean,
    val enterAction: EnterAction,
    val strip: StripState,
    val panel: Panel,
    val incognito: Boolean,
    val voiceAvailable: Boolean,
)
