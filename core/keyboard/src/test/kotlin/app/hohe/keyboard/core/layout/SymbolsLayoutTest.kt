package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Detailed tests for SYMBOLS and SYMBOLS_SHIFT layouts per KLA §4-5 and SPEC-002 Appendix A.
 */
class SymbolsLayoutTest {
    private val symbols = Layouts.symbols()
    private val symbolsShift = Layouts.symbolsShift()

    @Test
    fun testSymbolsDigitMoreKeys() {
        val row1 = symbols.rows[0]
        val key1 = row1.keys.first { it.label == "1" }
        assertEquals(listOf("¹", "½", "⅓", "¼", "⅕", "⅙", "⅐", "⅛", "⅑", "⅒", "₁"), key1.moreKeys.map { it.label })

        val key0 = row1.keys.first { it.label == "0" }
        assertEquals(listOf("⁰", "ⁿ", "∅", "₀"), key0.moreKeys.map { it.label })
    }

    @Test
    fun testSymbolsRow2MoreKeys() {
        val row2 = symbols.rows[1]
        val keyDollar = row2.keys.first { it.label == "$" }
        assertEquals(listOf("₹", "¢", "€", "£", "¥", "₱"), keyDollar.moreKeys.map { it.label })

        val keyHash = row2.keys.first { it.label == "#" }
        assertEquals(listOf("№"), keyHash.moreKeys.map { it.label })

        val keyPercent = row2.keys.first { it.label == "%" }
        assertEquals(listOf("‰"), keyPercent.moreKeys.map { it.label })

        val keyStar = row2.keys.first { it.label == "*" }
        assertEquals(listOf("†", "‡", "★"), keyStar.moreKeys.map { it.label })

        val keyMinus = row2.keys.first { it.label == "-" }
        assertEquals(listOf("_", "–", "—", "·", "⁻", "₋"), keyMinus.moreKeys.map { it.label })

        val keyEquals = row2.keys.first { it.label == "=" }
        assertEquals(listOf("≠", "≈", "∞", "⁼", "₌"), keyEquals.moreKeys.map { it.label })

        val keyOpenParen = row2.keys.first { it.label == "(" }
        assertEquals(listOf("<", "{", "[", "⁽", "₍"), keyOpenParen.moreKeys.map { it.label })

        val keyCloseParen = row2.keys.first { it.label == ")" }
        assertEquals(listOf(">", "}", "]", "⁾", "₎"), keyCloseParen.moreKeys.map { it.label })
    }

    @Test
    fun testSymbolsRow3MoreKeysAndShiftSymbolsKey() {
        val row3 = symbols.rows[2]
        val shiftSymbols = row3.keys[0]
        assertEquals(KeyCodes.SHIFT_SYMBOLS, shiftSymbols.code)
        assertEquals("=\\<", shiftSymbols.label)
        assertEquals(14.8f, shiftSymbols.widthPct, 0.001f)

        val keyExclamation = row3.keys.first { it.label == "!" }
        assertEquals(listOf("¡", "✓"), keyExclamation.moreKeys.map { it.label })

        val keyQuote = row3.keys.first { it.label == "\"" }
        assertEquals(listOf("„", "“", "”", "«", "»"), keyQuote.moreKeys.map { it.label })

        val keyApos = row3.keys.first { it.label == "'" }
        assertEquals(listOf("‚", "‘", "’", "‹", "›"), keyApos.moreKeys.map { it.label })

        val keyPlus = row3.keys.first { it.label == "+" }
        assertEquals(listOf("±", "⁺", "₊"), keyPlus.moreKeys.map { it.label })

        val keyQuestion = row3.keys.first { it.label == "?" }
        assertEquals(listOf("¿"), keyQuestion.moreKeys.map { it.label })
    }

    @Test
    fun testSymbolsShiftRow1MoreKeys() {
        val row1 = symbolsShift.rows[0]
        val keyDiv = row1.keys.first { it.label == "÷" }
        assertEquals(listOf("√"), keyDiv.moreKeys.map { it.label })

        val keyBullet = row1.keys.first { it.label == "•" }
        assertEquals(listOf("♪", "♥", "♠", "♦", "♣"), keyBullet.moreKeys.map { it.label })

        val keyDegree = row1.keys.first { it.label == "°" }
        assertEquals(listOf("′", "″"), keyDegree.moreKeys.map { it.label })

        val keyBacktick = row1.keys.first { it.label == "`" }
        assertEquals(listOf("´"), keyBacktick.moreKeys.map { it.label })
    }

    @Test
    fun testSymbolsShiftRow2MoreKeys() {
        val row2 = symbolsShift.rows[1]
        val keyCopy = row2.keys.first { it.label == "©" }
        assertEquals(listOf("℅"), keyCopy.moreKeys.map { it.label })

        val keyReg = row2.keys.first { it.label == "®" }
        assertEquals(listOf("™"), keyReg.moreKeys.map { it.label })

        val keyCaret = row2.keys.first { it.label == "^" }
        assertEquals(listOf("↑", "↓", "←", "→"), keyCaret.moreKeys.map { it.label })

        val keyPi = row2.keys.first { it.label == "π" }
        assertEquals(listOf("√", "§", "∆", "Π", "¶", "∑", "Ω", "μ"), keyPi.moreKeys.map { it.label })
    }

    @Test
    fun testSymbolsShiftRow3MoreKeys() {
        val row3 = symbolsShift.rows[2]
        val toSymbols = row3.keys[0]
        assertEquals(KeyCodes.SYMBOLS, toSymbols.code)
        assertEquals("?123", toSymbols.label)

        val keyLt = row3.keys.first { it.label == "<" }
        assertEquals(listOf("‹", "≤", "«", "⟨"), keyLt.moreKeys.map { it.label })

        val keyGt = row3.keys.first { it.label == ">" }
        assertEquals(listOf("›", "≥", "»", "⟩"), keyGt.moreKeys.map { it.label })

        val keyInvertedQ = row3.keys.first { it.label == "¿" }
        assertEquals(listOf("π", "Π", "¶", "§", "∆"), keyInvertedQ.moreKeys.map { it.label })
    }

    @Test
    fun testSymbolsWithNumberRow() {
        // F-006-04: number row on symbols
        val layout = LayoutTransforms.withNumberRow(symbols)
        assertEquals(5, layout.rows.size)
        val expectedRowLabels =
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("~", "±", "×", "÷", "•", "°", "`", "_", "{", "}"),
                listOf("@", "#", "$", "%", "&", "*", "-", "=", "(", ")"),
                listOf("=\\<", "!", "\"", "'", ":", "+", "/", "?", "⌫"),
                listOf("ABC", ",", "emoji", "space", "123", ".", "Enter"),
            )
        val actualRowLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedRowLabels, actualRowLabels)
    }

    @Test
    fun testSymbolsShiftWithNumberRow() {
        // F-006-04: number row on symbols_shift
        val layout = LayoutTransforms.withNumberRow(symbolsShift)
        assertEquals(5, layout.rows.size)
        val expectedRowLabels =
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("₹", "£", "€", "¥", "©", "®", "^", "π", "[", "]"),
                listOf("@", "#", "$", "%", "&", "*", "-", "=", "(", ")"),
                listOf("?123", "¡", "<", ">", ";", "|", "\\", "¿", "⌫"),
                listOf("ABC", ",", "emoji", "space", "123", ".", "Enter"),
            )
        val actualRowLabels = layout.rows.map { row -> row.keys.map { it.label } }
        assertEquals(expectedRowLabels, actualRowLabels)
    }
}
