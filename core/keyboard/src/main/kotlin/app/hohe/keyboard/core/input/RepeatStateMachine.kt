package app.hohe.keyboard.core.input

import app.hohe.keyboard.core.time.Clock

/**
 * Event fired when a held repeatable key triggers one or more repeat actions.
 */
data class RepeatFired(
    val keyId: Int,
    val count: Int,
)

/**
 * State machine managing repeated key actions when a key is held down.
 */
class RepeatStateMachine(
    private val clock: Clock,
    val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    private var activeKeyId: Int? = null
    private var downTimeMs: Long = 0L
    private var isRepeatable: Boolean = false
    private var firedCount: Int = 0

    /**
     * Starts tracking repeat for the given key.
     */
    fun down(
        keyId: Int,
        t: Long = clock.nowMs(),
        repeatable: Boolean = true,
    ) {
        activeKeyId = keyId
        downTimeMs = t
        isRepeatable = repeatable
        firedCount = 0
    }

    /**
     * Stops tracking repeat when the key is released.
     */
    fun up(t: Long = clock.nowMs()) {
        activeKeyId = null
        isRepeatable = false
        firedCount = 0
    }

    /**
     * Cancels any active repeat.
     */
    fun cancel() {
        activeKeyId = null
        isRepeatable = false
        firedCount = 0
    }

    /**
     * Periodic time update evaluating repeat triggers.
     */
    fun tick(t: Long = clock.nowMs()): RepeatFired? {
        val keyId = activeKeyId ?: return null
        if (!isRepeatable) return null

        val elapsed = t - downTimeMs
        if (elapsed < initialDelayMs) return null

        val elapsedPastInitial = elapsed - initialDelayMs
        val totalDue = 1 + (elapsedPastInitial / intervalMs).toInt()
        val toFire = totalDue - firedCount
        if (toFire > 0) {
            firedCount = totalDue
            return RepeatFired(keyId = keyId, count = toFire)
        }
        return null
    }

    companion object {
        const val DEFAULT_INITIAL_DELAY_MS: Long = 400L
        const val DEFAULT_INTERVAL_MS: Long = 50L
    }
}
