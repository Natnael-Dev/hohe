package app.hohe.keyboard.core.time

/**
 * Clock source abstraction for time-driven keyboard state machines.
 */
interface Clock {
    /**
     * Returns the current time in milliseconds.
     */
    fun nowMs(): Long
}
