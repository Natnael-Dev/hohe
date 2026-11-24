package app.hohe.keyboard.core.layout

import app.hohe.keyboard.core.field.FieldPolicy
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.TypingLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LayoutFactoryTest {
    private val text = FieldPolicy.classify(1, 0)

    private fun Layout.keyByCode(code: Int) = rows.flatMap { it.keys }.firstOrNull { it.code == code }

    @Test
    fun amharicPeriodOffersEthiopicFullStopFirst() {
        val l = LayoutFactory.build(KeyboardPage.LETTERS, text, KeyboardSettings(), TypingLanguage.AMHARIC)
        val period = l.keyByCode('.'.code)!!
        assertEquals("።", period.moreKeys.first().label)
        assertEquals(8, period.moreKeysColumns)
    }

    @Test
    fun englishPeriodKeepsLatinPunctuation() {
        val l = LayoutFactory.build(KeyboardPage.LETTERS, text, KeyboardSettings(), TypingLanguage.ENGLISH)
        assertEquals(
            ",",
            l
                .keyByCode('.'.code)!!
                .moreKeys
                .first()
                .label,
        )
    }

    @Test
    fun dollarOffersBirrFirst() {
        val l = LayoutFactory.build(KeyboardPage.SYMBOLS, text, KeyboardSettings(), TypingLanguage.AMHARIC)
        val dollar = l.keyByCode('$'.code)!!
        assertEquals(LayoutFactory.BIRR, dollar.moreKeys.first().label)
        assertEquals(0, dollar.moreKeys.first().code)
    }

    @Test
    fun longPressForSymbolsPutsHintFirst() {
        val l = LayoutFactory.build(KeyboardPage.LETTERS, text, KeyboardSettings(), TypingLanguage.ENGLISH)
        val q = l.keyByCode('q'.code)!!
        assertEquals("~", q.hint)
        assertEquals("~", q.moreKeys.first().label)
    }

    @Test
    fun numberRowAddsARow() {
        val on = LayoutFactory.build(KeyboardPage.LETTERS, text, KeyboardSettings(numberRow = true), TypingLanguage.ENGLISH)
        assertEquals(5, on.rows.size)
        assertNotNull(on.keyByCode('1'.code))
    }

    @Test
    fun numericFieldsGetTheNumberPad() {
        val num = LayoutFactory.build(KeyboardPage.LETTERS, FieldPolicy.classify(2, 0), KeyboardSettings(), TypingLanguage.AMHARIC)
        assertEquals(LayoutSet.NUMBER, num.id)
        val phone = LayoutFactory.build(KeyboardPage.LETTERS, FieldPolicy.classify(3, 0), KeyboardSettings(), TypingLanguage.AMHARIC)
        assertEquals(LayoutSet.PHONE, phone.id)
    }

    @Test
    fun emailFieldsShowAtOnComma() {
        val email = FieldPolicy.classify(1 or 0x20, 0)
        val l = LayoutFactory.build(KeyboardPage.LETTERS, email, KeyboardSettings(), TypingLanguage.AMHARIC)
        assertNotNull(l.keyByCode('@'.code))
    }
}
