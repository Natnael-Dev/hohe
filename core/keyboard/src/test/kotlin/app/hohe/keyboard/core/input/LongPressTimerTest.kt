package app.hohe.keyboard.core.input

import app.hohe.keyboard.core.time.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LongPressTimerTest {
    private lateinit var clock: FakeClock

    @Before
    fun setUp() {
        clock = FakeClock()
    }

    @Test
    fun defaultDelayIs300Ms() {
        val timer = LongPressTimer(clock)
        assertEquals(300, timer.delayMs)
    }

    @Test
    fun delayClampingAndRoundingEdgeCases() {
        // §7 Edge cases: 0, 149, 2001, 305 -> 150, 150, 2000, 310
        assertEquals(150, LongPressTimer.normalizeDelay(0))
        assertEquals(150, LongPressTimer.normalizeDelay(149))
        assertEquals(2000, LongPressTimer.normalizeDelay(2001))
        assertEquals(310, LongPressTimer.normalizeDelay(305))

        val timer0 = LongPressTimer(clock, delayMs = 0)
        assertEquals(150, timer0.delayMs)

        val timer149 = LongPressTimer(clock, delayMs = 149)
        assertEquals(150, timer149.delayMs)

        val timer2001 = LongPressTimer(clock, delayMs = 2001)
        assertEquals(2000, timer2001.delayMs)

        val timer305 = LongPressTimer(clock, delayMs = 305)
        assertEquals(310, timer305.delayMs)
    }

    @Test
    fun kb024FiresAtExact150MsDelay() {
        val timer = LongPressTimer(clock, delayMs = 150)
        timer.down(keyId = 42, t = clock.nowMs())

        clock.advanceBy(149L)
        assertNull("Must never fire before delay", timer.tick(clock.nowMs()))

        clock.advanceBy(1L) // 150ms
        val fired = timer.tick(clock.nowMs())
        assertNotNull("Must fire at exact delay", fired)
        assertTrue(fired is LongPressFired.OpenMoreKeys)
        assertEquals(42, fired!!.keyId)

        // Fires once per press
        clock.advanceBy(100L)
        assertNull("Must not fire again during same press", timer.tick(clock.nowMs()))
    }

    @Test
    fun kb024FiresAtExact300MsDelay() {
        val timer = LongPressTimer(clock, delayMs = 300)
        timer.down(keyId = 10, t = clock.nowMs())

        clock.advanceBy(299L)
        assertNull(timer.tick(clock.nowMs()))

        clock.advanceBy(1L) // 300ms
        val fired = timer.tick(clock.nowMs())
        assertNotNull(fired)
        assertEquals(10, fired!!.keyId)

        clock.advanceBy(50L)
        assertNull(timer.tick(clock.nowMs()))
    }

    @Test
    fun kb024FiresAtExact2000MsDelay() {
        val timer = LongPressTimer(clock, delayMs = 2000)
        timer.down(keyId = 99, t = clock.nowMs())

        clock.advanceBy(1999L)
        assertNull(timer.tick(clock.nowMs()))

        clock.advanceBy(1L) // 2000ms
        val fired = timer.tick(clock.nowMs())
        assertNotNull(fired)
        assertEquals(99, fired!!.keyId)

        clock.advanceBy(100L)
        assertNull(timer.tick(clock.nowMs()))
    }

    @Test
    fun upBeforeDelayCancelsTimer() {
        val timer = LongPressTimer(clock, delayMs = 300)
        timer.down(keyId = 1, t = clock.nowMs())

        clock.advanceBy(200L)
        timer.up(clock.nowMs())

        clock.advanceBy(200L) // reaches 400ms
        assertNull("Must not fire after up", timer.tick(clock.nowMs()))
    }

    @Test
    fun multiTouchDownRestartsTimerForNewKey() {
        val timer = LongPressTimer(clock, delayMs = 300)
        timer.down(keyId = 1, t = clock.nowMs())

        clock.advanceBy(200L)
        // Second down without up
        timer.down(keyId = 2, t = clock.nowMs())

        clock.advanceBy(150L) // 350ms total, but only 150ms since key 2 down
        assertNull("First key must never fire, second not ready", timer.tick(clock.nowMs()))

        clock.advanceBy(150L) // 300ms since key 2 down
        val fired = timer.tick(clock.nowMs())
        assertNotNull(fired)
        assertEquals(2, fired!!.keyId)
    }

    @Test
    fun noPanelAutoMoreKeyFiresDirectCode() {
        val timer = LongPressTimer(clock, delayMs = 300)
        // e.g. space -> IME picker (-6) or shift -> caps lock (-2)
        timer.down(keyId = 32, t = clock.nowMs(), hasNoPanelAutoMoreKey = true, longPressCode = -6)

        clock.advanceBy(300L)
        val fired = timer.tick(clock.nowMs())
        assertNotNull(fired)
        assertTrue(fired is LongPressFired.DirectCode)
        val direct = fired as LongPressFired.DirectCode
        assertEquals(32, direct.keyId)
        assertEquals(-6, direct.code)
    }

    @Test
    fun tickWithoutDownReturnsNull() {
        val timer = LongPressTimer(clock, delayMs = 300)
        assertNull(timer.tick(clock.nowMs()))
    }

    @Test
    fun subsequentPressFiresSeparately() {
        val timer = LongPressTimer(clock, delayMs = 300)
        timer.down(keyId = 5, t = clock.nowMs())
        clock.advanceBy(300L)
        assertNotNull(timer.tick(clock.nowMs()))
        timer.up(clock.nowMs())

        clock.advanceBy(50L)
        timer.down(keyId = 6, t = clock.nowMs())
        clock.advanceBy(300L)
        val fired2 = timer.tick(clock.nowMs())
        assertNotNull(fired2)
        assertEquals(6, fired2!!.keyId)
    }
}
