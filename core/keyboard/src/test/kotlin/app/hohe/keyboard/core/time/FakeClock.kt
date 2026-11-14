package app.hohe.keyboard.core.time

/**
 * Deterministic test clock with manual time advancement.
 */
class FakeClock(
    private var currentMs: Long = 0L,
) : Clock {
    override fun nowMs(): Long = currentMs

    fun advanceBy(deltaMs: Long) {
        require(deltaMs >= 0L) { "Time delta must be non-negative: $deltaMs" }
        currentMs += deltaMs
    }

    fun set(timeMs: Long) {
        require(timeMs >= 0L) { "Time must be non-negative: $timeMs" }
        currentMs = timeMs
    }
}
