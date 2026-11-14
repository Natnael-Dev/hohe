package app.hohe.keyboard.core.layout

// TASK-030: Amharic punctuation and Birr (ብር) transforms will be implemented here.

/**
 * Pure transform functions for deriving keyboard layout variations.
 */
object LayoutTransforms {
    private val symbolsHintsRow1 =
        mapOf(
            "q" to "~",
            "w" to "±",
            "e" to "×",
            "r" to "÷",
            "t" to "•",
            "y" to "°",
            "u" to "`",
            "i" to "_",
            "o" to "{",
            "p" to "}",
        )

    private val symbolsHintsRow2 =
        mapOf(
            "a" to "@",
            "s" to "#",
            "d" to "$",
            "f" to "%",
            "g" to "&",
            "h" to "*",
            "j" to "-",
            "k" to "(",
            "l" to ")",
        )

    private val symbolsHintsRow3 =
        mapOf(
            "z" to "!",
            "x" to "\"",
            "c" to "'",
            "v" to ":",
            "b" to "+",
            "n" to "/",
            "m" to "?",
        )

    private val symbolHintsByLabel = symbolsHintsRow1 + symbolsHintsRow2 + symbolsHintsRow3

    private val defaultDigitHintsRow1 =
        mapOf(
            "q" to "1",
            "w" to "2",
            "e" to "3",
            "r" to "4",
            "t" to "5",
            "y" to "6",
            "u" to "7",
            "i" to "8",
            "o" to "9",
            "p" to "0",
        )

    /**
     * Transforms the comma key into a forward-slash key for URL input fields.
     */
    fun forUrl(layout: Layout): Layout = transformCommaKey(layout, '/', "/")

    /**
     * Transforms the comma key into an @ symbol key for email input fields.
     */
    fun forEmail(layout: Layout): Layout = transformCommaKey(layout, '@', "@")

    /**
     * Adds a dedicated number row above row 1, removing digit hints from letter keys.
     */
    fun withNumberRow(layout: Layout): Layout {
        if (layout.rows.size > 4) {
            return layout
        }
        return when (layout.id) {
            LayoutSet.QWERTY -> transformQwertyWithNumberRow(layout)
            LayoutSet.SYMBOLS -> Layouts.symbolsWithNumberRow()
            LayoutSet.SYMBOLS_SHIFT -> Layouts.symbolsShiftWithNumberRow()
            else -> layout
        }
    }

    private fun transformQwertyWithNumberRow(layout: Layout): Layout {
        val numberRowKeys =
            listOf(
                digitKey('1', listOf("¹", "½", "⅓", "¼", "⅕", "⅙", "⅐", "⅛", "⅑", "⅒", "₁")),
                digitKey('2', listOf("²", "⅔", "⅖", "₂")),
                digitKey('3', listOf("³", "¾", "⅗", "⅜", "₃")),
                digitKey('4', listOf("⁴", "⅘", "₄")),
                digitKey('5', listOf("⁵", "⅚", "⅝", "₅")),
                digitKey('6', listOf("⁶", "₆")),
                digitKey('7', listOf("⁷", "⅞", "₇")),
                digitKey('8', listOf("⁸", "₈")),
                digitKey('9', listOf("⁹", "₉")),
                digitKey('0', listOf("⁰", "ⁿ", "∅", "₀")),
            )
        val numberRow = Row(keys = numberRowKeys, xOffsetPct = 0.5f)

        val row1 = layout.rows[0]
        val transformedRow1Keys =
            row1.keys.map { key ->
                val cleanedMoreKeys = key.moreKeys.filter { it.label !in "0".."9" }
                val newHint = if (key.hint != null && key.hint in "0".."9") null else key.hint
                key.copy(hint = newHint, moreKeys = cleanedMoreKeys)
            }
        val transformedRow1 = row1.copy(keys = transformedRow1Keys)

        val newRows = mutableListOf<Row>()
        newRows.add(numberRow)
        newRows.add(transformedRow1)
        for (i in 1 until layout.rows.size) {
            newRows.add(layout.rows[i])
        }

        return layout.copy(rows = newRows)
    }

    /**
     * Transforms letter key hints in QWERTY to show long-press symbol hints (Appendix A).
     */
    fun withLongPressForSymbols(layout: Layout): Layout {
        if (layout.id != LayoutSet.QWERTY) {
            return layout
        }
        val newRows =
            layout.rows.map { row ->
                val newKeys =
                    row.keys.map { key ->
                        val symbolHint = symbolHintsByLabel[key.label]
                        if (symbolHint != null) {
                            key.copy(hint = symbolHint)
                        } else {
                            key
                        }
                    }
                row.copy(keys = newKeys)
            }
        return layout.copy(rows = newRows)
    }

    /**
     * Restores default letter hints (digit hints on row 1, null on rows 2 and 3).
     */
    fun withoutLongPressForSymbols(layout: Layout): Layout {
        if (layout.id != LayoutSet.QWERTY) {
            return layout
        }
        val hasNumberRow = layout.rows.size > 4
        val newRows =
            layout.rows.map { row ->
                val newKeys =
                    row.keys.map { key ->
                        if (key.label in symbolHintsByLabel) {
                            val restoredDigit = defaultDigitHintsRow1[key.label]
                            if (restoredDigit != null && !hasNumberRow) {
                                key.copy(hint = restoredDigit)
                            } else {
                                key.copy(hint = null)
                            }
                        } else {
                            key
                        }
                    }
                row.copy(keys = newKeys)
            }
        return layout.copy(rows = newRows)
    }

    private fun digitKey(
        digit: Char,
        moreKeyLabels: List<String>,
    ): Key =
        Key(
            code = digit.code,
            label = digit.toString(),
            widthPct = 9.9f,
            style = KeyStyle.LETTER,
            moreKeys = moreKeyLabels.map { MoreKey(it) },
        )

    private fun transformCommaKey(
        layout: Layout,
        newCode: Char,
        newLabel: String,
    ): Layout {
        if (layout.id != LayoutSet.QWERTY) return layout
        val newRows =
            layout.rows.map { row ->
                val newKeys =
                    row.keys.map { key ->
                        if (key.label == "," || key.code == ','.code) {
                            key.copy(
                                code = newCode.code,
                                label = newLabel,
                                moreKeys = listOf(MoreKey(",")),
                            )
                        } else {
                            key
                        }
                    }
                row.copy(keys = newKeys)
            }
        return layout.copy(rows = newRows)
    }
}
