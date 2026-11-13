package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * AC-1: Every key, label, hint, more-key list and width in
 * QWERTY/SYMBOLS/SYMBOLS_SHIFT/NUMBER(+variants)/NUMBER_PASSWORD/PHONE/PHONE_SYMBOLS
 * equals KLA (100 %). Tested with one test per layout, asserting rows as label strings.
 */
class LayoutDataTest {
    @Test
    fun testQwertyLayoutRows() {
        val layout = Layouts.qwerty()
        assertEquals(LayoutSet.QWERTY, layout.id)
        val expected =
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "⌫"),
                listOf("?123", ",", "globe", "space", ".", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testSymbolsLayoutRows() {
        val layout = Layouts.symbols()
        assertEquals(LayoutSet.SYMBOLS, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "*", "-", "=", "(", ")"),
                listOf("=\\<", "!", "\"", "'", ":", "+", "/", "?", "⌫"),
                listOf("ABC", ",", "emoji", "space", "123", ".", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testSymbolsShiftLayoutRows() {
        val layout = Layouts.symbolsShift()
        assertEquals(LayoutSet.SYMBOLS_SHIFT, layout.id)
        val expected =
            listOf(
                listOf("~", "±", "×", "÷", "•", "°", "`", "_", "{", "}"),
                listOf("₹", "£", "€", "¥", "©", "®", "^", "π", "[", "]"),
                listOf("?123", "¡", "<", ">", ";", "|", "\\", "¿", "⌫"),
                listOf("ABC", ",", "emoji", "space", "123", ".", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testNumberLayoutNormalRows() {
        val layout = Layouts.number(NumberVariant.NORMAL)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", ","),
                listOf("7", "8", "9", "⌫"),
                listOf(".", "0", "space", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testNumberLayoutDateRows() {
        val layout = Layouts.number(NumberVariant.DATE)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf("/", "0", "space", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testNumberLayoutTimeRows() {
        val layout = Layouts.number(NumberVariant.TIME)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf(":", "0", "space", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testNumberLayoutDateTimeRows() {
        val layout = Layouts.number(NumberVariant.DATETIME)
        assertEquals(LayoutSet.NUMBER, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf("/", "0", "space", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testNumberPasswordLayoutRows() {
        val layout = Layouts.numberPassword()
        assertEquals(LayoutSet.NUMBER_PASSWORD, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testPhoneLayoutRows() {
        val layout = Layouts.phone()
        assertEquals(LayoutSet.PHONE, layout.id)
        val expected =
            listOf(
                listOf("1", "2", "3", "-"),
                listOf("4", "5", "6", "."),
                listOf("7", "8", "9", "⌫"),
                listOf("＊＃", "0", "space", "Enter"),
            )
        val actual = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expected, actual)
    }

    @Test
    fun testPhoneSymbolsLayoutRows() {
        val layout = Layouts.phoneSymbols()
        assertEquals(LayoutSet.PHONE_SYMBOLS, layout.id)
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
