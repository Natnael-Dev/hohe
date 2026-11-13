package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ConverterRowTest(
    private val id: String,
    private val input: String,
    private val expected: String,
    private val next: List<String>,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {1}")
        fun data(): Collection<Array<Any>> {
            val rows = ConverterGoldenTest.loadRows()
            return rows.map { arrayOf(it.id, it.input, it.expected, it.next) }
        }
    }

    @Test
    fun testRowSlot0() {
        assertEquals("Row $id input '$input'", expected, AmharicConverter.convert(input))
    }
}
