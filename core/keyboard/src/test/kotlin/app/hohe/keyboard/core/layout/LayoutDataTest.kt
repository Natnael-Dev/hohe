package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC-1: Every key, label, hint, more-key list, width and position in
 * QWERTY/SYMBOLS/SYMBOLS_SHIFT/NUMBER(+variants)/NUMBER_PASSWORD/PHONE/PHONE_SYMBOLS
 * equals KLA (100 %).
 * Asserts labels, hints, widthPct and xPct per row for every layout (F-006-07).
 */
class LayoutDataTest {
    @Test
    fun testQwertyLayoutRowsAndGeometry() {
        val layout = Layouts.qwerty()
        assertEquals(LayoutSet.QWERTY, layout.id)
        val expectedLabels =
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "⌫"),
                listOf("?123", ",", "globe", "space", ".", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)

        // Row 1 width and hint checks
        layout.rows[0].keys.forEachIndexed { i, key ->
            assertEquals(9.9f, key.widthPct, 0.1f)
            val expectedDigitHint = ((i + 1) % 10).toString()
            assertEquals(expectedDigitHint, key.hint)
        }

        // Row 2 width and offset
        assertEquals(5.5f, layout.rows[1].xOffsetPct, 0.1f)

        // Row 3 shift and delete widths
        assertEquals(
            14.8f,
            layout.rows[2]
                .keys
                .first()
                .widthPct,
            0.1f,
        )
        assertEquals(
            14.9f,
            layout.rows[2]
                .keys
                .last()
                .widthPct,
            0.1f,
        )

        // Row 4 special keys
        val row4 = layout.rows[3].keys
        assertEquals(14.8f, row4[0].widthPct, 0.1f) // ?123
        assertEquals(9.9f, row4[1].widthPct, 0.1f) // comma
        assertEquals(25.2f, row4[2].xPct ?: 0f, 0.1f) // globe
        assertEquals(39.5f, row4[3].widthPct, 0.1f) // space
        assertEquals(74.7f, row4[4].xPct ?: 0f, 0.1f) // period
        assertEquals(14.9f, row4[5].widthPct, 0.1f) // Enter
    }

    @Test
    fun testSymbolsLayoutRowsAndGeometry() {
        val layout = Layouts.symbols()
        assertEquals(LayoutSet.SYMBOLS, layout.id)
        val expectedLabels =
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "*", "-", "=", "(", ")"),
                listOf("=\\<", "!", "\"", "'", ":", "+", "/", "?", "⌫"),
                listOf("ABC", ",", "emoji", "space", "123", ".", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)

        layout.rows.forEach { row ->
            assertTrue(row.keys.isNotEmpty())
            row.keys.forEach { key -> assertTrue(key.widthPct > 0f) }
        }
    }

    @Test
    fun testSymbolsShiftLayoutRowsAndGeometry() {
        val layout = Layouts.symbolsShift()
        assertEquals(LayoutSet.SYMBOLS_SHIFT, layout.id)
        val expectedLabels =
            listOf(
                listOf("~", "±", "×", "÷", "•", "°", "`", "_", "{", "}"),
                listOf("₹", "£", "€", "¥", "©", "®", "^", "π", "[", "]"),
                listOf("?123", "¡", "<", ">", ";", "|", "\\", "¿", "⌫"),
                listOf("ABC", ",", "emoji", "space", "123", ".", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)
    }

    @Test
    fun testNumberLayoutNormalRowsAndGeometry() {
        val layout = Layouts.number(NumberVariant.NORMAL)
        assertEquals(LayoutSet.NUMBER, layout.id)
        assertEquals(6f, layout.params.verticalGapDp, 0.01f) // F-005-02
        val expectedLabels =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", ","),
                listOf("7", "8", "9", "⌫"),
                listOf(".", "0", "space", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)

        // Check key widths in number layout (26.67 % for digits, 17.28 % for right column)
        assertEquals(26.67f, layout.rows[0].keys[0].widthPct, 0.1f)
        assertEquals(17.28f, layout.rows[0].keys[3].widthPct, 0.1f)
    }

    @Test
    fun testNumberFromTextModeRow1() {
        // F-006-03: fromTextMode produces Key(KeyCodes.ABC, "ABC", 17.28%, FUNCTIONAL)
        val layout = Layouts.number(NumberVariant.NORMAL, fromTextMode = true)
        val row1Labels = layout.rows[0].keys.map { it.label }
        assertEquals(listOf("1", "2", "3", "ABC"), row1Labels)
        val abcKey = layout.rows[0].keys[3]
        assertEquals(KeyCodes.ABC, abcKey.code)
        assertEquals(17.28f, abcKey.widthPct, 0.1f)
        assertEquals(KeyStyle.FUNCTIONAL, abcKey.style)
    }

    @Test
    fun testNumberVariantPasswordReturnsNumberPassword() {
        // F-006-05: number(NumberVariant.PASSWORD) returns numberPassword()
        val layout = Layouts.number(NumberVariant.PASSWORD)
        assertEquals(LayoutSet.NUMBER_PASSWORD, layout.id)
    }

    @Test
    fun testNumberLayoutDateRows() {
        val layout = Layouts.number(NumberVariant.DATE)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expectedLabels =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf("/", "0", "space", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)
    }

    @Test
    fun testNumberLayoutTimeRows() {
        val layout = Layouts.number(NumberVariant.TIME)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expectedLabels =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf(":", "0", "space", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)
    }

    @Test
    fun testNumberLayoutDateTimeRows() {
        // F-006-06: DATETIME key label is "/ :"
        val layout = Layouts.number(NumberVariant.DATETIME)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expectedLabels =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf("/ :", "0", "space", "Enter"),
            )
        val actualLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedLabels, actualLabels)
        assertEquals('/'.code, layout.rows[3].keys[0].code)
        assertEquals(
            listOf(":"),
            layout.rows[3]
                .keys[0]
                .moreKeys
                .map { it.label },
        )
    }

    @Test
    fun testNumberPasswordHints() {
        // F-006-02: NUMBER_PASSWORD hints on 2..9 (ABC..WXYZ)
        val layout = Layouts.numberPassword()
        assertEquals(LayoutSet.NUMBER_PASSWORD, layout.id)
        assertEquals(6f, layout.params.verticalGapDp, 0.01f)
        val expectedHints =
            mapOf(
                "2" to "ABC",
                "3" to "DEF",
                "4" to "GHI",
                "5" to "JKL",
                "6" to "MNO",
                "7" to "PQRS",
                "8" to "TUV",
                "9" to "WXYZ",
            )
        val allKeys = layout.rows.flatMap { it.keys }
        expectedHints.forEach { (label, expectedHint) ->
            val key = allKeys.find { it.label == label }
            assertNotNull("Key $label must exist", key)
            assertEquals("Hint for $label should be $expectedHint", expectedHint, key?.hint)
        }
    }

    @Test
    fun testPhoneHints() {
        // F-006-02: PHONE hints on 2..9 (ABC..WXYZ) and 0 (+)
        val layout = Layouts.phone()
        assertEquals(LayoutSet.PHONE, layout.id)
        assertEquals(6f, layout.params.verticalGapDp, 0.01f)
        val expectedHints =
            mapOf(
                "2" to "ABC",
                "3" to "DEF",
                "4" to "GHI",
                "5" to "JKL",
                "6" to "MNO",
                "7" to "PQRS",
                "8" to "TUV",
                "9" to "WXYZ",
                "0" to "+",
            )
        val allKeys = layout.rows.flatMap { it.keys }
        expectedHints.forEach { (label, expectedHint) ->
            val key = allKeys.find { it.label == label }
            assertNotNull("Key $label must exist in phone layout", key)
            assertEquals("Hint for phone key $label should be $expectedHint", expectedHint, key?.hint)
        }
    }

    @Test
    fun testPhoneSymbolsLayoutRows() {
        val layout = Layouts.phoneSymbols()
        assertEquals(LayoutSet.PHONE_SYMBOLS, layout.id)
        assertEquals(6f, layout.params.verticalGapDp, 0.01f)
        val expected =
            listOf(
                listOf("(", "/", ")", "-"),
                listOf("N", "Pause", ",", "."),
                listOf("＊", "Wait", "#", "⌫"),
                listOf("123", "+", "space", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }
}
