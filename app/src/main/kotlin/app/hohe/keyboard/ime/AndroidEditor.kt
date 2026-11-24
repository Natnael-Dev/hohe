package app.hohe.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import app.hohe.keyboard.core.field.EnterAction
import app.hohe.keyboard.core.ime.EditorPort
import java.text.BreakIterator

/**
 * The only class that calls [InputConnection] (application-architecture §3.1). It counts its own
 * pending edits so `onUpdateSelection` can tell our changes from external cursor moves.
 */
class AndroidEditor(
    private val service: InputMethodService,
) : EditorPort {
    private val ic: InputConnection? get() = service.currentInputConnection
    private var batchDepth = 0

    /**
     * Selection updates our own edits will cause. A batch counts once (editors report one update
     * per batch), and counts expire after [OWN_UPDATE_WINDOW_MS] so a lost update can never make
     * a later real cursor tap look like ours.
     */
    private var pendingOwnUpdates = 0
    private var lastEditAt = 0L
    private var batchEdited = false

    var selStart = 0
    var selEnd = 0

    private fun edited() {
        lastEditAt = android.os.SystemClock.uptimeMillis()
        if (batchDepth > 0) {
            batchEdited = true
        } else {
            pendingOwnUpdates++
        }
    }

    /** True if a selection update was caused by us (and consumes it). */
    fun consumeOwnSelectionUpdate(): Boolean {
        if (pendingOwnUpdates <= 0) return false
        if (android.os.SystemClock.uptimeMillis() - lastEditAt > OWN_UPDATE_WINDOW_MS) {
            pendingOwnUpdates = 0
            return false
        }
        pendingOwnUpdates--
        return true
    }

    fun resetTracking() {
        pendingOwnUpdates = 0
        batchEdited = false
    }

    override fun textBeforeCursor(n: Int): CharSequence = ic?.getTextBeforeCursor(n, 0) ?: ""

    override fun hasSelection(): Boolean = selStart != selEnd

    override fun setComposingText(text: String) {
        ic?.setComposingText(text, 1)
        edited()
    }

    override fun finishComposingText() {
        ic?.finishComposingText()
    }

    override fun commitText(text: String) {
        ic?.commitText(text, 1)
        edited()
    }

    override fun deleteBackward() {
        val conn = ic ?: return
        if (hasSelection()) {
            conn.commitText("", 1)
            edited()
            return
        }
        val before = conn.getTextBeforeCursor(GRAPHEME_LOOKBACK, 0)
        if (before.isNullOrEmpty()) {
            // Empty field or an editor that hides its text: let the app handle a real DEL key.
            conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            return
        }
        conn.deleteSurroundingText(lastGraphemeLength(before), 0)
        edited()
    }

    override fun deleteBefore(chars: Int) {
        if (chars <= 0) return
        ic?.deleteSurroundingText(chars, 0)
        edited()
    }

    override fun performEditorAction(action: EnterAction) {
        val id =
            when (action) {
                EnterAction.GO -> EditorInfo.IME_ACTION_GO
                EnterAction.SEARCH -> EditorInfo.IME_ACTION_SEARCH
                EnterAction.SEND -> EditorInfo.IME_ACTION_SEND
                EnterAction.NEXT -> EditorInfo.IME_ACTION_NEXT
                EnterAction.DONE -> EditorInfo.IME_ACTION_DONE
                EnterAction.PREVIOUS -> EditorInfo.IME_ACTION_PREVIOUS
                EnterAction.NEWLINE -> EditorInfo.IME_ACTION_UNSPECIFIED
            }
        ic?.performEditorAction(id)
    }

    override fun moveCursor(delta: Int) {
        val conn = ic ?: return
        val target =
            if (delta < 0) {
                val before = conn.getTextBeforeCursor(-delta, 0)?.length ?: 0
                selStart - before
            } else {
                val after = conn.getTextAfterCursor(delta, 0)?.length ?: 0
                selEnd + after
            }
        conn.setSelection(target, target)
        edited()
    }

    override fun beginBatch() {
        if (batchDepth++ == 0) ic?.beginBatchEdit()
    }

    override fun endBatch() {
        if (batchDepth > 0 && --batchDepth == 0) {
            ic?.endBatchEdit()
            if (batchEdited) {
                pendingOwnUpdates++
                batchEdited = false
            }
        }
    }

    companion object {
        private const val GRAPHEME_LOOKBACK = 24
        private const val OWN_UPDATE_WINDOW_MS = 400L

        /** Length in chars of the last user-perceived character (emoji ZWJ sequences, Ethiopic). */
        fun lastGraphemeLength(text: CharSequence): Int {
            if (text.isEmpty()) return 0
            val it = BreakIterator.getCharacterInstance()
            it.setText(text.toString())
            val end = it.last()
            val start = it.previous()
            return if (start == BreakIterator.DONE) end else end - start
        }
    }
}
