package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for R8 longPressForSymbols hint transform per SPEC-002 Appendix A.
 */
class LongPressForSymbolsTest {
    private val qwerty = Layouts.qwerty()

    @Test
    fun testWithLongPressForSymbolsHints() {
        val transformed = LayoutTransforms.withLongPressForSymbols(qwerty)

        // Row 1 hints: q~ w± e× r÷ t• y° u` i_ o{ p}
        val row1Hints = transformed.rows[0].keys.map { it.hint }
        val expectedRow1 = listOf("~", "±", "×", "÷", "•", "°", "`", "_", "{", "}")
        assertEquals(expectedRow1, row1Hints)

        // Row 2 hints: a@ s# d$ f% g& h* j- k( l)
        val row2Hints = transformed.rows[1].keys.map { it.hint }
        val expectedRow2 = listOf("@", "#", "$", "%", "&", "*", "-", "(", ")")
        assertEquals(expectedRow2, row2Hints)

        // Row 3 hints: z! x" c' v: b+ n/ m? (letters only: index 1 to 7)
        val row3Letters = transformed.rows[2].keys.subList(1, 8)
        val row3Hints = row3Letters.map { it.hint }
        val expectedRow3 = listOf("!", "\"", "'", ":", "+", "/", "?")
        assertEquals(expectedRow3, row3Hints)
    }

    @Test
    fun testWithoutLongPressForSymbolsRestoresDefaults() {
        val transformed = LayoutTransforms.withLongPressForSymbols(qwerty)
        val restored = LayoutTransforms.withoutLongPressForSymbols(transformed)

        val row1Hints = restored.rows[0].keys.map { it.hint }
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), row1Hints)

        val row2Hints = restored.rows[1].keys.map { it.hint }
        assertEquals(List(9) { null }, row2Hints)
    }
}
