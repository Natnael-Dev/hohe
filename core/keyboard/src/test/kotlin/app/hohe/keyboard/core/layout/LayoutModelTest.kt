package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for layout model types, enums, flags, and special key codes.
 */
class LayoutModelTest {
    @Test
    fun testLayoutSetEnumValues() {
        val expected =
            listOf(
                "QWERTY",
                "SYMBOLS",
                "SYMBOLS_SHIFT",
                "NUMBER",
                "NUMBER_PASSWORD",
                "PHONE",
                "PHONE_SYMBOLS",
            )
        val actual = LayoutSet.values().map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testKeyStyleEnumValues() {
        val expected =
            listOf(
                "LETTER",
                "FUNCTIONAL",
                "SPACE",
                "ENTER",
                "SHIFT",
                "DELETE",
            )
        val actual = KeyStyle.values().map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testKeyFlagsDefaults() {
        val flags = KeyFlags()
        assertFalse(flags.repeatable)
        assertFalse(flags.noPanelAutoMoreKey)
        assertFalse(flags.enterAdaptive)
        assertFalse(flags.shiftAware)
    }

    @Test
    fun testKeyFlagsCustomValues() {
        val flags =
            KeyFlags(
                repeatable = true,
                noPanelAutoMoreKey = true,
                enterAdaptive = true,
                shiftAware = true,
            )
        assertTrue(flags.repeatable)
        assertTrue(flags.noPanelAutoMoreKey)
        assertTrue(flags.enterAdaptive)
        assertTrue(flags.shiftAware)
    }

    @Test
    fun testMoreKeyAutoCode() {
        val mk = MoreKey("a")
        assertEquals("a", mk.label)
        assertEquals('a'.code, mk.code)

        val special = MoreKey("caps", KeyCodes.CAPSLOCK)
        assertEquals("caps", special.label)
        assertEquals(KeyCodes.CAPSLOCK, special.code)
    }

    @Test
    fun testKeyCreationAndDefaults() {
        val key =
            Key(
                code = 'q'.code,
                label = "q",
                widthPct = 9.9f,
            )
        assertEquals('q'.code, key.code)
        assertEquals("q", key.label)
        assertNull(key.hint)
        assertTrue(key.moreKeys.isEmpty())
        assertEquals(9.9f, key.widthPct, 0.001f)
        assertNull(key.xPct)
        assertEquals(KeyStyle.LETTER, key.style)
        assertFalse(key.flags.repeatable)
        assertEquals(5, key.moreKeysColumns)
    }

    @Test
    fun testRowCreationAndDefaults() {
        val key = Key(code = 'a'.code, label = "a", widthPct = 9.8f)
        val row = Row(keys = listOf(key), xOffsetPct = 5.5f)
        assertEquals(1, row.keys.size)
        assertEquals(5.5f, row.xOffsetPct, 0.001f)
        assertEquals(1f, row.heightFactor, 0.001f)
    }

    @Test
    fun testLayoutParamsDefaults() {
        val params = LayoutParams()
        assertEquals(1.353f, params.horizontalGapPct, 0.001f)
        assertEquals(9f, params.verticalGapDp, 0.001f)
        assertEquals(9f, params.topPaddingDp, 0.001f)
        assertEquals(9f, params.bottomPaddingDp, 0.001f)
    }

    @Test
    fun testLayoutCreation() {
        val layout =
            Layout(
                id = LayoutSet.QWERTY,
                rows = emptyList(),
            )
        assertEquals(LayoutSet.QWERTY, layout.id)
        assertTrue(layout.rows.isEmpty())
        assertNotNull(layout.params)
    }

    @Test
    fun testKeyCodesValues() {
        assertEquals(-1, KeyCodes.SHIFT)
        assertEquals(-2, KeyCodes.CAPSLOCK)
        assertEquals(-3, KeyCodes.LANGUAGE_CHOOSER)
        assertEquals(-5, KeyCodes.DELETE)
        assertEquals(-6, KeyCodes.IME_PICKER)
        assertEquals(-31, KeyCodes.SYMBOLS)
        assertEquals(-30, KeyCodes.ABC)
        assertEquals(-16, KeyCodes.LANGUAGE)
        assertEquals(-11, KeyCodes.EMOJI)
        assertEquals(-22, KeyCodes.NUMPAD)
        assertEquals(-32, KeyCodes.SHIFT_SYMBOLS)
        assertEquals(10, KeyCodes.ENTER)
        assertEquals(-12, KeyCodes.NEWLINE)
        assertEquals(32, KeyCodes.SPACE)
        assertEquals(-110, KeyCodes.EMOJI_LONG_PRESS)
        assertEquals(-111, KeyCodes.COMMA_EMOJI)
        assertEquals(-112, KeyCodes.ENTER_EMOJI)
    }
}
