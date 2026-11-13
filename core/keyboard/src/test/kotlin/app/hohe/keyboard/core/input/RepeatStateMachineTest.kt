package app.hohe.keyboard.core.input

import app.hohe.keyboard.core.time.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepeatStateMachineTest {
    private lateinit var clock: FakeClock
    private lateinit var repeatSm: RepeatStateMachine

    @Before
    fun setUp() {
        clock = FakeClock()
        repeatSm = RepeatStateMachine(clock)
    }

    @Test
    fun defaultDelaysAre400And50Ms() {
        assertEquals(400L, repeatSm.initialDelayMs)
        assertEquals(50L, repeatSm.intervalMs)
    }

    @Test
    fun nonRepeatableKeyNeverRepeats() {
        repeatSm.down(keyId = 65, t = clock.nowMs(), repeatable = false)
        clock.advanceBy(1000L)
        assertNull(repeatSm.tick(clock.nowMs()))
    }

    @Test
    fun tickBeforeInitialDelayReturnsNull() {
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true) // Delete key
        clock.advanceBy(399L)
        assertNull("Must not fire repeat before 400ms", repeatSm.tick(clock.nowMs()))
    }

    @Test
    fun tickAtExact400MsFiresFirstRepeat() {
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true)
        clock.advanceBy(400L)
        val fired = repeatSm.tick(clock.nowMs())
        assertNotNull(fired)
        assertEquals(-5, fired!!.keyId)
        assertEquals(1, fired.count)

        // Immediate next tick within interval returns null
        clock.advanceBy(20L)
        assertNull(repeatSm.tick(clock.nowMs()))
    }

    @Test
    fun subsequentTicksFireAtInterval() {
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true)
        clock.advanceBy(400L)
        assertEquals(1, repeatSm.tick(clock.nowMs())?.count)

        clock.advanceBy(50L) // 450ms
        val fired2 = repeatSm.tick(clock.nowMs())
        assertNotNull(fired2)
        assertEquals(1, fired2!!.count)

        clock.advanceBy(100L) // 550ms (2 intervals elapsed)
        val fired3 = repeatSm.tick(clock.nowMs())
        assertNotNull(fired3)
        assertEquals(2, fired3!!.count)
    }

    @Test
    fun kb030HoldBackspace2SecondsDeletesAtLeast30Chars() {
        // KB-030: Hold backspace 2 s -> repeat starts 400 ms, >= 30 chars deleted
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true)

        var totalRepeats = 0
        // Sample every 10 ms for 2000 ms (total 2 seconds)
        for (step in 1..200) {
            clock.advanceBy(10L)
            repeatSm.tick(clock.nowMs())?.let {
                totalRepeats += it.count
            }
        }

        // At 2000 ms:
        // Initial delay 400 ms -> 1600 ms remaining.
        // 1600 / 50 = 32 intervals.
        // 1 initial repeat + 32 interval repeats = 33 repeats!
        assertTrue("Expected >= 30 repeats, got $totalRepeats", totalRepeats >= 30)
        assertEquals(33, totalRepeats)
    }

    @Test
    fun upCancelsRepeat() {
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true)
        clock.advanceBy(300L)
        repeatSm.up(clock.nowMs())

        clock.advanceBy(300L) // 600ms total
        assertNull("Must not fire after up", repeatSm.tick(clock.nowMs()))
    }

    @Test
    fun cancelResetsRepeat() {
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true)
        clock.advanceBy(450L)
        assertNotNull(repeatSm.tick(clock.nowMs()))

        repeatSm.cancel()
        clock.advanceBy(200L)
        assertNull("Must not fire after cancel", repeatSm.tick(clock.nowMs()))
    }

    @Test
    fun multiTouchDownRestartsRepeatForNewKey() {
        repeatSm.down(keyId = -5, t = clock.nowMs(), repeatable = true)
        clock.advanceBy(350L)

        // New key down without up
        repeatSm.down(keyId = 32, t = clock.nowMs(), repeatable = true)

        clock.advanceBy(100L) // 450ms from first key, but only 100ms from second key
        assertNull(repeatSm.tick(clock.nowMs()))

        clock.advanceBy(300L) // 400ms from second key
        val fired = repeatSm.tick(clock.nowMs())
        assertNotNull(fired)
        assertEquals(32, fired!!.keyId)
    }
}
