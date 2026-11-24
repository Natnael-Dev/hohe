package app.hohe.keyboard.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardSettingsTest {
    @Test
    fun defaultsMatchProductScope() {
        val s = KeyboardSettings()
        assertEquals(1.0f, s.heightMultiplier)
        assertFalse(s.sound)
        assertEquals(50, s.soundLevel)
        assertTrue(s.vibrate)
        assertTrue(s.emojiRow)
        assertFalse(s.numberRow)
        assertTrue(s.longPressForSymbols)
        assertEquals(300, s.longPressDelayMs)
        assertTrue(s.selectSuggestionWithSpace)
        assertTrue(s.autoSpacePunctuation)
        assertFalse(s.doubleSpacePeriod)
        assertTrue(s.keyPopup)
        assertTrue(s.spaceCursorControl)
        assertTrue(s.swipeToDelete)
        assertFalse(s.highContrast)
        assertEquals(TypingLanguage.AMHARIC, s.language)
        assertFalse(s.handwritingOptIn)
    }

    @Test
    fun sanitizedClampsOutOfRangeValues() {
        val s =
            KeyboardSettings(
                heightMultiplier = 3f,
                keyTextSize = 0.1f,
                bottomSpaceDp = 500,
                soundLevel = -4,
                vibrationMs = -20,
                longPressDelayMs = 10,
                highContrastTheme = ThemeId.NIGHT,
            ).sanitized()
        assertEquals(KeyboardSettings.HEIGHT_MAX, s.heightMultiplier)
        assertEquals(0.8f, s.keyTextSize)
        assertEquals(KeyboardSettings.BOTTOM_SPACE_MAX_DP, s.bottomSpaceDp)
        assertEquals(0, s.soundLevel)
        assertEquals(-1, s.vibrationMs)
        assertEquals(KeyboardSettings.LONG_PRESS_MIN_MS, s.longPressDelayMs)
        assertEquals(ThemeId.HC_YELLOW, s.highContrastTheme)
    }

    @Test
    fun sanitizedKeepsValidValues() {
        val s = KeyboardSettings(longPressDelayMs = 2000, vibrationMs = 30, highContrastTheme = ThemeId.HC_WHITE)
        assertEquals(s, s.sanitized())
    }
}
