package app.hohe.keyboard.core.input

import app.hohe.keyboard.core.time.Clock

/**
 * State of the keyboard shift modifier.
 */
enum class ShiftState {
    /** Shift is inactive; lowercase letters typed. */
    OFF,

    /** Shift is active for the next letter only. */
    ONE_SHOT,

    /** Caps lock is active; all letters typed uppercase until unlocked. */
    LOCKED,

    /** Auto-capitalization is active for the next letter; enters from auto-cap rules only. */
    AUTO,
}

/**
 * Action taken when shift is double-tapped within 300 ms.
 */
enum class ShiftDoubleTap {
    /** Lock caps lock. */
    LOCK,

    /** Double-tap does not lock; behaves as standard tap. */
    NONE,
}

/**
 * Shift state machine driven by key events, time, and auto-capitalization signals.
 */
class ShiftStateMachine(
    private val clock: Clock? = null,
    val doubleTapAction: ShiftDoubleTap = ShiftDoubleTap.LOCK,
) {
    var state: ShiftState = ShiftState.OFF
        private set

    val isUppercase: Boolean
        get() = state != ShiftState.OFF

    private var isDown: Boolean = false
    private var downTime: Long = 0L
    private var lastUpTime: Long = 0L

    /**
     * Called when the shift key is pressed down.
     */
    fun shiftDown(t: Long = clock?.nowMs() ?: 0L) {
        isDown = true
        downTime = t
    }

    /**
     * Periodic time update while keys are held.
     */
    fun tick(t: Long = clock?.nowMs() ?: 0L) {
        if (isDown && state != ShiftState.LOCKED && (t - downTime >= SHIFT_LOCK_HOLD_MS)) {
            state = ShiftState.LOCKED
        }
    }

    /**
     * Called when the shift key is released.
     */
    fun shiftUp(t: Long = clock?.nowMs() ?: 0L) {
        if (!isDown) return
        isDown = false

        val pressDuration = t - downTime
        if (pressDuration >= SHIFT_LOCK_HOLD_MS) {
            state = ShiftState.LOCKED
            lastUpTime = 0L
            return
        }

        // Tap release (< 1200 ms)
        when (state) {
            ShiftState.OFF -> {
                state = ShiftState.ONE_SHOT
                lastUpTime = t
            }
            ShiftState.ONE_SHOT -> {
                val isDoubleTap = lastUpTime > 0L && (downTime - lastUpTime <= DOUBLE_TAP_TIMEOUT_MS)
                if (isDoubleTap && doubleTapAction == ShiftDoubleTap.LOCK) {
                    state = ShiftState.LOCKED
                } else {
                    state = ShiftState.OFF
                }
                lastUpTime = 0L
            }
            ShiftState.AUTO, ShiftState.LOCKED -> {
                state = ShiftState.OFF
                lastUpTime = 0L
            }
        }
    }

    /**
     * Called when a letter key has been typed.
     */
    fun letterTyped() {
        if (state == ShiftState.ONE_SHOT || state == ShiftState.AUTO) {
            state = ShiftState.OFF
            lastUpTime = 0L
        }
    }

    /**
     * Called when the active layout changes (e.g. switching between letters and symbols).
     */
    fun layoutChanged() {
        state = ShiftState.OFF
        isDown = false
        lastUpTime = 0L
    }

    /**
     * Called by auto-capitalization logic to set or clear automatic capital state.
     */
    fun autoCapRequested(requested: Boolean) {
        if (requested) {
            if (state == ShiftState.OFF) {
                state = ShiftState.AUTO
            }
        } else {
            if (state == ShiftState.AUTO) {
                state = ShiftState.OFF
            }
        }
    }

    companion object {
        const val SHIFT_LOCK_HOLD_MS: Long = 1200L
        const val DOUBLE_TAP_TIMEOUT_MS: Long = 300L
    }
}
