package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for LayoutTransforms: forUrl, forEmail, and withNumberRow.
 */
class LayoutTransformsTest {
    private val qwerty = Layouts.qwerty()

    @Test
    fun testForUrlTransformsCommaToSlash() {
        val transformed = LayoutTransforms.forUrl(qwerty)
        val row4 = transformed.rows[3]
        val key = row4.keys[1]
        assertEquals('/', key.code.toChar())
        assertEquals("/", key.label)
        assertEquals(",", key.moreKeys.firstOrNull()?.label)
    }

    @Test
    fun testForEmailTransformsCommaToAt() {
        val transformed = LayoutTransforms.forEmail(qwerty)
        val row4 = transformed.rows[3]
        val key = row4.keys[1]
        assertEquals('@', key.code.toChar())
        assertEquals("@", key.label)
        assertEquals(",", key.moreKeys.firstOrNull()?.label)
    }

    /**
     * AC-4: Number row on/off transform matches KLA §2 (row 1 digits; hints removed from QWERTY row 1).
     */
    @Test
    fun testWithNumberRowStructure() {
        val withNumbers = LayoutTransforms.withNumberRow(qwerty)
        assertEquals(5, withNumbers.rows.size)

        // Row 0 is the number row
        val row0 = withNumbers.rows[0]
        assertEquals(10, row0.keys.size)
        assertEquals(0.5f, row0.xOffsetPct, 0.001f)
        val row0Labels = row0.keys.map { it.label }
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), row0Labels)

        // Row 1 is the original QWERTY row 1, with hints removed
        val row1 = withNumbers.rows[1]
        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), row1.keys.map { it.label })
        row1.keys.forEach { key ->
            assertNull("Key ${key.label} should have hint removed when number row is active", key.hint)
        }

        // Accents are still present on e, u, i, o in row 1, but without digit prefix
        val keyE = row1.keys.first { it.label == "e" }
        assertEquals(listOf("é", "è", "ê", "ë", "ē"), keyE.moreKeys.map { it.label })
    }

    @Test
    fun testWithNumberRowDigitMoreKeys() {
        val withNumbers = LayoutTransforms.withNumberRow(qwerty)
        val row0 = withNumbers.rows[0]

        val key0 = row0.keys.first { it.label == "0" }
        assertEquals(listOf("⁰", "ⁿ", "∅", "₀"), key0.moreKeys.map { it.label })

        val key1 = row0.keys.first { it.label == "1" }
        assertEquals(listOf("¹", "½", "⅓", "¼", "⅕", "⅙", "⅐", "⅛", "⅑", "⅒", "₁"), key1.moreKeys.map { it.label })
    }
}
