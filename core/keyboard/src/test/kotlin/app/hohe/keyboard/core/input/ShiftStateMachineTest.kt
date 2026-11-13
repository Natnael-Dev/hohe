package app.hohe.keyboard.core.input

import app.hohe.keyboard.core.time.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShiftStateMachineTest {
    private lateinit var clock: FakeClock
    private lateinit var sm: ShiftStateMachine

    @Before
    fun setUp() {
        clock = FakeClock()
        sm = ShiftStateMachine(clock)
    }

    @Test
    fun initialStateIsOffAndNotUppercase() {
        assertEquals(ShiftState.OFF, sm.state)
        assertFalse(sm.isUppercase)
    }

    @Test
    fun shiftTapTransitionsOffToOneShot() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(100L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.ONE_SHOT, sm.state)
        assertTrue(sm.isUppercase)
    }

    @Test
    fun kb020ShiftTapThenTwoLettersTransitionsToOneShotThenOff() {
        // KB-020: Shift tap, then a, b -> Ab
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(80L)
        sm.shiftUp(clock.nowMs())
        assertTrue("First letter should be uppercase", sm.isUppercase)

        sm.letterTyped()
        assertEquals(ShiftState.OFF, sm.state)
        assertFalse("Second letter should be lowercase", sm.isUppercase)

        sm.letterTyped()
        assertEquals(ShiftState.OFF, sm.state)
        assertFalse(sm.isUppercase)
    }

    @Test
    fun kb021ShiftLongPressLocksAndStaysLockedAcrossMultipleLetters() {
        // KB-021: Shift long-press, abc -> ABC, lock stays
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(1200L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.LOCKED, sm.state)
        assertTrue(sm.isUppercase)

        sm.letterTyped()
        assertEquals(ShiftState.LOCKED, sm.state)
        assertTrue(sm.isUppercase)

        sm.letterTyped()
        assertEquals(ShiftState.LOCKED, sm.state)
        assertTrue(sm.isUppercase)

        sm.letterTyped()
        assertEquals(ShiftState.LOCKED, sm.state)
        assertTrue(sm.isUppercase)
    }

    @Test
    fun kb021HoldingShiftTransitionsToLockedDuringTick() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(600L)
        sm.tick(clock.nowMs())
        assertEquals(ShiftState.OFF, sm.state)

        clock.advanceBy(600L) // total 1200ms
        sm.tick(clock.nowMs())
        assertEquals(ShiftState.LOCKED, sm.state)
        assertTrue(sm.isUppercase)

        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.LOCKED, sm.state)
    }

    @Test
    fun kb022ShiftDoubleTapWithin300MsLocks() {
        // First tap
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.ONE_SHOT, sm.state)

        // Second tap within 300ms of first up
        clock.advanceBy(150L)
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.LOCKED, sm.state)
        assertTrue(sm.isUppercase)
    }

    @Test
    fun shiftDoubleTapAtExact300MsLocks() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.ONE_SHOT, sm.state)

        clock.advanceBy(300L)
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.LOCKED, sm.state)
    }

    @Test
    fun shiftDoubleTapDisabledGoesToOff() {
        val noneSm = ShiftStateMachine(clock, doubleTapAction = ShiftDoubleTap.NONE)
        noneSm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        noneSm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.ONE_SHOT, noneSm.state)

        clock.advanceBy(100L)
        noneSm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        noneSm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.OFF, noneSm.state)
        assertFalse(noneSm.isUppercase)
    }

    @Test
    fun secondTapAfter300MsTogglesOneShotToOff() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.ONE_SHOT, sm.state)

        // More than 300ms after first up
        clock.advanceBy(350L)
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.OFF, sm.state)
        assertFalse(sm.isUppercase)
    }

    @Test
    fun lockedTappedTransitionsToOff() {
        // Lock via long press
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(1200L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.LOCKED, sm.state)

        clock.advanceBy(500L)
        // Tap to unlock
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(80L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.OFF, sm.state)
        assertFalse(sm.isUppercase)
    }

    @Test
    fun autoCapRequestedTransitionsOffToAuto() {
        sm.autoCapRequested(true)
        assertEquals(ShiftState.AUTO, sm.state)
        assertTrue(sm.isUppercase)

        // Typing a letter resets AUTO to OFF
        sm.letterTyped()
        assertEquals(ShiftState.OFF, sm.state)
        assertFalse(sm.isUppercase)
    }

    @Test
    fun autoCapTappedTransitionsToOff() {
        sm.autoCapRequested(true)
        assertEquals(ShiftState.AUTO, sm.state)

        sm.shiftDown(clock.nowMs())
        clock.advanceBy(60L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.OFF, sm.state)
    }

    @Test
    fun autoCapRequestedFalseCancelsAutoOnly() {
        sm.autoCapRequested(true)
        assertEquals(ShiftState.AUTO, sm.state)

        sm.autoCapRequested(false)
        assertEquals(ShiftState.OFF, sm.state)

        // Does not cancel ONE_SHOT
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.ONE_SHOT, sm.state)

        sm.autoCapRequested(false)
        assertEquals(ShiftState.ONE_SHOT, sm.state)

        // Does not cancel LOCKED
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(1250L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.LOCKED, sm.state)

        sm.autoCapRequested(false)
        assertEquals(ShiftState.LOCKED, sm.state)
    }

    @Test
    fun autoCapRequestedDoesNotOverrideOneShotOrLocked() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(50L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.ONE_SHOT, sm.state)

        sm.autoCapRequested(true)
        assertEquals(ShiftState.ONE_SHOT, sm.state)
    }

    @Test
    fun layoutChangedResetsToOff() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(1200L)
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.LOCKED, sm.state)

        sm.layoutChanged()
        assertEquals(ShiftState.OFF, sm.state)
        assertFalse(sm.isUppercase)
    }

    @Test
    fun shiftUpWithoutDownIsIgnored() {
        sm.shiftUp(clock.nowMs())
        assertEquals(ShiftState.OFF, sm.state)
    }

    @Test
    fun holdJustUnder1200MsIsTreatedAsTap() {
        sm.shiftDown(clock.nowMs())
        clock.advanceBy(1199L)
        sm.shiftUp(clock.nowMs())

        assertEquals(ShiftState.ONE_SHOT, sm.state)
    }
}
