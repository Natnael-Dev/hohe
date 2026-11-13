package app.hohe.keyboard.core.input

import app.hohe.keyboard.core.time.Clock
import kotlin.math.roundToInt

/**
 * Event produced when a key has been held down for the long-press duration.
 */
sealed interface LongPressFired {
    val keyId: Int

    /**
     * Fired for keys configured with noPanelAutoMoreKey, directly emitting their long-press code.
     */
    data class DirectCode(
        override val keyId: Int,
        val code: Int,
    ) : LongPressFired

    /**
     * Fired for keys that display a more-keys popup panel.
     */
    data class OpenMoreKeys(
        override val keyId: Int,
    ) : LongPressFired
}

/**
 * Timer tracking key hold duration to trigger long-press actions.
 */
class LongPressTimer(
    private val clock: Clock,
    delayMs: Int = DEFAULT_DELAY_MS,
) {
    val delayMs: Int = normalizeDelay(delayMs)

    private var activeKeyId: Int? = null
    private var downTimeMs: Long = 0L
    private var hasNoPanel: Boolean = false
    private var longPressCode: Int = 0
    private var hasFired: Boolean = false

    /**
     * Records a key down event, resetting the timer for the given key.
     */
    fun down(
        keyId: Int,
        t: Long = clock.nowMs(),
        hasNoPanelAutoMoreKey: Boolean = false,
        longPressCode: Int = 0,
    ) {
        activeKeyId = keyId
        downTimeMs = t
        this.hasNoPanel = hasNoPanelAutoMoreKey
        this.longPressCode = longPressCode
        hasFired = false
    }

    /**
     * Cancels any pending long-press when the key is released.
     */
    fun up(t: Long = clock.nowMs()) {
        activeKeyId = null
        hasFired = false
    }

    /**
     * Checks if the active key has reached the long-press threshold.
     */
    fun tick(t: Long = clock.nowMs()): LongPressFired? {
        val keyId = activeKeyId ?: return null
        if (hasFired) return null

        if (t - downTimeMs >= delayMs) {
            hasFired = true
            return if (hasNoPanel) {
                LongPressFired.DirectCode(keyId, longPressCode)
            } else {
                LongPressFired.OpenMoreKeys(keyId)
            }
        }
        return null
    }

    companion object {
        const val DEFAULT_DELAY_MS: Int = 300
        const val MIN_DELAY_MS: Int = 150
        const val MAX_DELAY_MS: Int = 2000
        const val STEP_MS: Int = 10

        /**
         * Clamps and rounds raw delay values to steps of 10 within [MIN_DELAY_MS, MAX_DELAY_MS].
         */
        fun normalizeDelay(rawMs: Int): Int {
            val rounded = (rawMs / STEP_MS.toDouble()).roundToInt() * STEP_MS
            return rounded.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        }
    }
}
