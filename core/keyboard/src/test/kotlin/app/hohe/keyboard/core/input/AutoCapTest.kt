package app.hohe.keyboard.core.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCapTest {
    @Test
    fun emptyFieldShouldCapitalize() {
        assertTrue(AutoCap.shouldCapitalize("", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize(null, enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun whitespaceOnlyFieldShouldCapitalize() {
        assertTrue(AutoCap.shouldCapitalize("   ", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("\t\t", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize(" \n ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterPeriodAndSpaceShouldCapitalize() {
        assertTrue(AutoCap.shouldCapitalize("Hello. ", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("Hello.   ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterPeriodAndNonBreakingSpaceShouldCapitalize() {
        // F-007-04: Non-breaking space \u00A0 must be treated as horizontal whitespace
        assertTrue(AutoCap.shouldCapitalize("Hello.\u00A0", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("Hello.\u00A0  ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterExclamationAndSpaceShouldCapitalize() {
        assertTrue(AutoCap.shouldCapitalize("Wow! ", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("Wait!  ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterQuestionMarkAndSpaceShouldCapitalize() {
        assertTrue(AutoCap.shouldCapitalize("Why? ", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("Really?   ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterEthiopicFullStopAndSpaceShouldCapitalize() {
        // '።' Ethiopic full stop
        assertTrue(AutoCap.shouldCapitalize("ሰላም። ", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("ጽሑፍ።   ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterEthiopicQuestionMarkAndSpaceShouldCapitalize() {
        // '፧' Ethiopic question mark
        assertTrue(AutoCap.shouldCapitalize("እንዴት፧ ", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("ይሆናል፧  ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun afterNewlineShouldCapitalize() {
        assertTrue(AutoCap.shouldCapitalize("Line one\n", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("Line one\r\n", enabled = true, fieldAllowsAutoCap = true))
        assertTrue(AutoCap.shouldCapitalize("Line one\n  ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun midSentenceShouldNotCapitalize() {
        assertFalse(AutoCap.shouldCapitalize("Hello ", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("Hello world", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("abc", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("Hello, ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun terminatorWithoutSpaceShouldNotCapitalize() {
        assertFalse(AutoCap.shouldCapitalize("Hello.", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("Hello!", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("Hello?", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("ሰላም።", enabled = true, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("እንዴት፧", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun disabledOrFieldNotAllowedNeverCapitalizes() {
        assertFalse(AutoCap.shouldCapitalize("", enabled = false, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("", enabled = true, fieldAllowsAutoCap = false))
        assertFalse(AutoCap.shouldCapitalize("", enabled = false, fieldAllowsAutoCap = false))
        assertFalse(AutoCap.shouldCapitalize("Hello. ", enabled = false, fieldAllowsAutoCap = true))
        assertFalse(AutoCap.shouldCapitalize("Hello. ", enabled = true, fieldAllowsAutoCap = false))
        // URI field
        assertFalse(AutoCap.shouldCapitalize("https://example.com/ ", enabled = true, fieldAllowsAutoCap = false))
    }

    @Test
    fun doctorAbbreviationEdgeCaseCapitalizesPerSpec() {
        // §7 Edge cases: Auto-cap on text ending in "Dr. " -> true (no abbreviation list in v1)
        assertTrue(AutoCap.shouldCapitalize("Dr. ", enabled = true, fieldAllowsAutoCap = true))
    }

    @Test
    fun autoCapModeWords() {
        assertTrue(AutoCap.shouldCapitalize("", mode = AutoCapMode.WORDS))
        assertTrue(AutoCap.shouldCapitalize("word ", mode = AutoCapMode.WORDS))
        assertFalse(AutoCap.shouldCapitalize("wor", mode = AutoCapMode.WORDS))
    }

    @Test
    fun autoCapModeCharacters() {
        assertTrue(AutoCap.shouldCapitalize("", mode = AutoCapMode.CHARACTERS))
        assertTrue(AutoCap.shouldCapitalize("abc", mode = AutoCapMode.CHARACTERS))
        assertTrue(AutoCap.shouldCapitalize("a", mode = AutoCapMode.CHARACTERS))
    }

    @Test
    fun autoCapModeNone() {
        assertFalse(AutoCap.shouldCapitalize("", mode = AutoCapMode.NONE))
        assertFalse(AutoCap.shouldCapitalize("Hello. ", mode = AutoCapMode.NONE))
    }
}
