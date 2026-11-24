package app.hohe.keyboard.core.theme

import app.hohe.keyboard.core.settings.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardThemeTest {
    @Test
    fun autoFollowsSystemDarkMode() {
        assertEquals(KeyboardThemes.NIGHT, KeyboardThemes.resolve(ThemeId.AUTO, systemDark = true))
        assertEquals(KeyboardThemes.DAYLIGHT, KeyboardThemes.resolve(ThemeId.AUTO, systemDark = false))
    }

    @Test
    fun highContrastOverridesSelectedTheme() {
        val t = KeyboardThemes.resolve(ThemeId.BUNA, false, highContrast = true, highContrastTheme = ThemeId.HC_WHITE)
        assertEquals(ThemeId.HC_WHITE, t.id)
    }

    @Test
    fun contrastMatchesPaletteDocument() {
        // color-palette.md: enter label on enter, Daylight = 6.50:1
        val r = KeyboardThemes.contrast(KeyboardThemes.DAYLIGHT.enterText, KeyboardThemes.DAYLIGHT.enter)
        assertEquals(6.50, r, 0.01)
    }

    @Test
    fun everyThemeMeetsTextContrast() {
        for (t in KeyboardThemes.ALL) {
            assertTrue("${t.id} letter text", KeyboardThemes.contrast(t.text, t.key) >= 4.5)
            assertTrue("${t.id} fn text", KeyboardThemes.contrast(t.fnText, t.fnKey) >= 4.5)
            assertTrue("${t.id} enter text", KeyboardThemes.contrast(t.enterText, t.enter) >= 4.5)
            assertTrue("${t.id} hint", KeyboardThemes.contrast(t.hint, t.key) >= 3.0)
            assertTrue("${t.id} slot-0 chip", KeyboardThemes.contrast(t.onPill, t.pill) >= 4.5)
            assertTrue("${t.id} space label", KeyboardThemes.contrast(t.spaceLabel, t.key) >= 4.5)
        }
    }

    @Test
    fun mixEndpointsReturnInputs() {
        val a = KeyboardThemes.DAYLIGHT.key
        val b = KeyboardThemes.DAYLIGHT.text
        assertEquals(a, KeyboardThemes.mix(a, b, 0f))
        assertEquals(b, KeyboardThemes.mix(a, b, 1f))
    }
}
