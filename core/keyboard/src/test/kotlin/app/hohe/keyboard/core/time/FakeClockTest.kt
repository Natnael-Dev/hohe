package app.hohe.keyboard.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FakeClockTest {
    @Test
    fun defaultInitialTimeIsZero() {
        val clock = FakeClock()
        assertEquals(0L, clock.nowMs())
    }

    @Test
    fun customInitialTimeAndAdvanceBy() {
        val clock = FakeClock(100L)
        assertEquals(100L, clock.nowMs())
        clock.advanceBy(50L)
        assertEquals(150L, clock.nowMs())
    }

    @Test
    fun setAndNegativeValidation() {
        val clock = FakeClock()
        clock.set(500L)
        assertEquals(500L, clock.nowMs())
        assertThrows(IllegalArgumentException::class.java) {
            clock.advanceBy(-1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            clock.set(-1L)
        }
    }
}
