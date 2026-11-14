package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Detailed tests for NUMBER (and variants), NUMBER_PASSWORD, PHONE, and PHONE_SYMBOLS layouts
 * per KLA §6-7 and SPEC-002 Appendix A.
 */
class NumberAndPhoneLayoutTest {
    @Test
    fun testNumberLayoutKeyWidths() {
        val numberLayout = Layouts.number(NumberVariant.NORMAL)
        assertEquals(4, numberLayout.rows.size)

        // Digit keys in number layout are 26.67 %p wide
        val row1 = numberLayout.rows[0]
        assertEquals(26.67f, row1.keys[0].widthPct, 0.01f)
        assertEquals(26.67f, row1.keys[1].widthPct, 0.01f)
        assertEquals(26.67f, row1.keys[2].widthPct, 0.01f)

        // Fill-right fourth key
        val keyMinus = row1.keys[3]
        assertEquals("-", keyMinus.label)
        assertEquals(listOf("+"), keyMinus.moreKeys.map { it.label })
    }

    @Test
    fun testNumberLayoutDateTimeVariants() {
        val timeLayout = Layouts.number(NumberVariant.TIME)
        val periodKeyTime = timeLayout.rows[1].keys[3]
        assertEquals(".", periodKeyTime.label)
        assertEquals(listOf("AM", "PM"), periodKeyTime.moreKeys.map { it.label })

        val colonKeyTime = timeLayout.rows[3].keys[0]
        assertEquals(":", colonKeyTime.label)

        val dateLayout = Layouts.number(NumberVariant.DATE)
        val slashKeyDate = dateLayout.rows[3].keys[0]
        assertEquals("/", slashKeyDate.label)

        val dateTimeLayout = Layouts.number(NumberVariant.DATETIME)
        val slashKeyDateTime = dateTimeLayout.rows[3].keys[0]
        assertEquals("/ :", slashKeyDateTime.label)
        assertEquals(listOf(":"), slashKeyDateTime.moreKeys.map { it.label })
    }

    @Test
    fun testNumberPasswordGrid() {
        val pwLayout = Layouts.numberPassword()
        assertEquals(4, pwLayout.rows.size)

        // 3x4 grid of 33.33 % keys
        pwLayout.rows.forEach { row ->
            assertEquals(3, row.keys.size)
            row.keys.forEach { key ->
                assertEquals(33.33f, key.widthPct, 0.01f)
            }
        }

        // Row 4: Delete, 0 (+ hint), Enter
        val row4 = pwLayout.rows[3]
        assertEquals(KeyCodes.DELETE, row4.keys[0].code)
        assertEquals('0'.code, row4.keys[1].code)
        assertEquals("+", row4.keys[1].hint)
        assertEquals(KeyCodes.ENTER, row4.keys[2].code)
    }

    @Test
    fun testPhoneLayoutStructure() {
        val phone = Layouts.phone()
        assertEquals(4, phone.rows.size)

        // Row 1: 1, 2ABC, 3DEF, -(+)
        val row1 = phone.rows[0]
        assertEquals(listOf("1", "2", "3", "-"), row1.keys.map { it.label })
        assertEquals(listOf("+"), row1.keys[3].moreKeys.map { it.label })

        // Row 2: 4GHI, 5JKL, 6MNO, .
        val row2 = phone.rows[1]
        assertEquals(listOf("4", "5", "6", "."), row2.keys.map { it.label })

        // Row 3: 7PQRS, 8TUV, 9WXYZ, Delete
        val row3 = phone.rows[2]
        assertEquals(listOf("7", "8", "9", "⌫"), row3.keys.map { it.label })

        // Row 4: ＊＃, 0(+), Space, Enter
        val row4 = phone.rows[3]
        assertEquals(listOf("＊＃", "0", "space", "Enter"), row4.keys.map { it.label })
        assertEquals(listOf("+"), row4.keys[1].moreKeys.map { it.label })
    }

    @Test
    fun testPhoneSymbolsLayoutStructure() {
        val phoneSymbols = Layouts.phoneSymbols()
        assertEquals(4, phoneSymbols.rows.size)

        // Row 1: ( / ) -(+)
        val row1 = phoneSymbols.rows[0]
        assertEquals(listOf("(", "/", ")", "-"), row1.keys.map { it.label })

        // Row 2: N, Pause(,), ",", .
        val row2 = phoneSymbols.rows[1]
        assertEquals(listOf("N", "Pause", ",", "."), row2.keys.map { it.label })
        val pauseKey = row2.keys[1]
        assertEquals(','.code, pauseKey.code)

        // Row 3: ＊, Wait(;), #, Delete
        val row3 = phoneSymbols.rows[2]
        assertEquals(listOf("＊", "Wait", "#", "⌫"), row3.keys.map { it.label })
        val waitKey = row3.keys[1]
        assertEquals(';'.code, waitKey.code)

        // Row 4: 123, +, Space, Enter
        val row4 = phoneSymbols.rows[3]
        assertEquals(listOf("123", "+", "space", "Enter"), row4.keys.map { it.label })
    }
}
