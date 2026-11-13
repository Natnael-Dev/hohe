package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests verifying QWERTY layout structure, key codes, labels, hints, more-keys,
 * and geometry parameters per KLA §2-3 and SPEC-002 Appendix A.
 */
class QwertyLayoutTest {
    private val qwerty = Layouts.qwerty()

    @Test
    fun testQwertyHasFourRows() {
        assertEquals(LayoutSet.QWERTY, qwerty.id)
        assertEquals(4, qwerty.rows.size)
    }

    @Test
    fun testRow1KeysAndHints() {
        val row1 = qwerty.rows[0]
        assertEquals(0.5f, row1.xOffsetPct, 0.001f)
        assertEquals(10, row1.keys.size)

        val labels = row1.keys.map { it.label }
        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), labels)

        val hints = row1.keys.map { it.hint }
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), hints)

        row1.keys.forEach { key ->
            assertEquals(9.9f, key.widthPct, 0.001f)
            assertEquals(KeyStyle.LETTER, key.style)
            assertTrue(key.flags.shiftAware)
        }
    }

    @Test
    fun testRow1MoreKeysAccents() {
        val row1 = qwerty.rows[0]
        val keyE = row1.keys.first { it.label == "e" }
        assertEquals(listOf("3", "é", "è", "ê", "ë", "ē"), keyE.moreKeys.map { it.label })

        val keyU = row1.keys.first { it.label == "u" }
        assertEquals(listOf("7", "ú", "û", "ü", "ù", "ū"), keyU.moreKeys.map { it.label })

        val keyI = row1.keys.first { it.label == "i" }
        assertEquals(listOf("8", "í", "î", "ï", "ī", "ì"), keyI.moreKeys.map { it.label })

        val keyO = row1.keys.first { it.label == "o" }
        assertEquals(listOf("9", "ó", "ô", "ö", "ò", "œ", "ø", "ō", "õ"), keyO.moreKeys.map { it.label })

        val keyQ = row1.keys.first { it.label == "q" }
        assertEquals(listOf("1"), keyQ.moreKeys.map { it.label })
    }

    @Test
    fun testRow2KeysAndAccents() {
        val row2 = qwerty.rows[1]
        assertEquals(5.5f, row2.xOffsetPct, 0.001f)
        assertEquals(9, row2.keys.size)

        val labels = row2.keys.map { it.label }
        assertEquals(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), labels)

        val keyA = row2.keys[0]
        assertEquals(9.8f, keyA.widthPct, 0.001f)
        assertEquals(listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"), keyA.moreKeys.map { it.label })

        val keyS = row2.keys[1]
        assertEquals(9.9f, keyS.widthPct, 0.001f)
        assertEquals(listOf("ß"), keyS.moreKeys.map { it.label })

        for (i in 2 until row2.keys.size) {
            assertEquals(9.9f, row2.keys[i].widthPct, 0.001f)
        }
    }

    @Test
    fun testRow3Keys() {
        val row3 = qwerty.rows[2]
        assertEquals(0.5f, row3.xOffsetPct, 0.001f)
        assertEquals(9, row3.keys.size)

        val shift = row3.keys.first()
        assertEquals(KeyCodes.SHIFT, shift.code)
        assertEquals(14.8f, shift.widthPct, 0.001f)
        assertEquals(KeyStyle.SHIFT, shift.style)
        assertTrue(shift.flags.noPanelAutoMoreKey)

        val delete = row3.keys.last()
        assertEquals(KeyCodes.DELETE, delete.code)
        assertEquals(14.9f, delete.widthPct, 0.001f)
        assertEquals(KeyStyle.DELETE, delete.style)
        assertTrue(delete.flags.repeatable)

        val middle = row3.keys.subList(1, 8).map { it.label }
        assertEquals(listOf("z", "x", "c", "v", "b", "n", "m"), middle)

        val keyC = row3.keys.first { it.label == "c" }
        assertEquals(listOf("ç"), keyC.moreKeys.map { it.label })

        val keyN = row3.keys.first { it.label == "n" }
        assertEquals(listOf("ñ"), keyN.moreKeys.map { it.label })
    }

    @Test
    fun testRow4Keys() {
        val row4 = qwerty.rows[3]
        assertEquals(0.5f, row4.xOffsetPct, 0.001f)
        assertEquals(6, row4.keys.size)

        val sym = row4.keys[0]
        assertEquals(KeyCodes.SYMBOLS, sym.code)
        assertEquals("?123", sym.label)
        assertEquals(14.8f, sym.widthPct, 0.001f)
        assertTrue(sym.flags.noPanelAutoMoreKey)

        val comma = row4.keys[1]
        assertEquals(','.code, comma.code)
        assertEquals(",", comma.label)
        assertEquals(9.9f, comma.widthPct, 0.001f)

        val lang = row4.keys[2]
        assertEquals(KeyCodes.LANGUAGE, lang.code)
        assertEquals(9.9f, lang.widthPct, 0.001f)
        assertEquals(25.2f, lang.xPct ?: 0f, 0.001f)
        assertTrue(lang.flags.noPanelAutoMoreKey)

        val space = row4.keys[3]
        assertEquals(KeyCodes.SPACE, space.code)
        assertEquals(39.5f, space.widthPct, 0.001f)
        assertEquals(KeyStyle.SPACE, space.style)
        assertTrue(space.flags.noPanelAutoMoreKey)

        val period = row4.keys[4]
        assertEquals('.'.code, period.code)
        assertEquals(".", period.label)
        assertEquals(9.9f, period.widthPct, 0.001f)
        assertEquals(74.7f, period.xPct ?: 0f, 0.001f)

        val enter = row4.keys[5]
        assertEquals(KeyCodes.ENTER, enter.code)
        assertEquals(14.9f, enter.widthPct, 0.001f)
        assertEquals(KeyStyle.ENTER, enter.style)
        assertTrue(enter.flags.enterAdaptive)
    }

    @Test
    fun testPeriodMoreKeysEightColumns() {
        val row4 = qwerty.rows[3]
        val period = row4.keys[4]
        assertEquals(8, period.moreKeysColumns)

        val expectedMoreKeys =
            listOf(
                ",",
                "?",
                "!",
                "#",
                ")",
                "(",
                "/",
                ";",
                "'",
                "@",
                ":",
                "-",
                "\"",
                "+",
                "%",
                "&",
            )
        assertEquals(expectedMoreKeys, period.moreKeys.map { it.label })
    }

    @Test
    fun testCommaAndEnterMoreKeys() {
        val row4 = qwerty.rows[3]
        val comma = row4.keys[1]
        assertEquals(KeyCodes.COMMA_EMOJI, comma.moreKeys.firstOrNull()?.code)

        val enter = row4.keys[5]
        val newlineMore = enter.moreKeys.firstOrNull { it.code == KeyCodes.NEWLINE }
        assertNotNull(newlineMore)
    }

    @Test
    fun testAutoMoreKeyCodesForDirectFire() {
        val row3 = qwerty.rows[2]
        val shift = row3.keys.first()
        assertEquals(KeyCodes.CAPSLOCK, shift.moreKeys.first().code)

        val row4 = qwerty.rows[3]
        val sym = row4.keys[0]
        assertEquals(KeyCodes.EMOJI_LONG_PRESS, sym.moreKeys.first().code)

        val lang = row4.keys[2]
        assertEquals(KeyCodes.LANGUAGE_CHOOSER, lang.moreKeys.first().code)

        val space = row4.keys[3]
        assertEquals(KeyCodes.IME_PICKER, space.moreKeys.first().code)
    }
}
