package app.hohe.keyboard.core.layout

/**
 * Repository of built-in immutable keyboard layouts.
 */
object Layouts {
    /**
     * Standard QWERTY layout matching KLA §2-3 and SPEC-002 Appendix A.
     */
    fun qwerty(): Layout {
        val row1Keys =
            listOf(
                letterKey('q', "1", listOf("1")),
                letterKey('w', "2", listOf("2")),
                letterKey('e', "3", listOf("3", "é", "è", "ê", "ë", "ē")),
                letterKey('r', "4", listOf("4")),
                letterKey('t', "5", listOf("5")),
                letterKey('y', "6", listOf("6")),
                letterKey('u', "7", listOf("7", "ú", "û", "ü", "ù", "ū")),
                letterKey('i', "8", listOf("8", "í", "î", "ï", "ī", "ì")),
                letterKey('o', "9", listOf("9", "ó", "ô", "ö", "ò", "œ", "ø", "ō", "õ")),
                letterKey('p', "0", listOf("0")),
            )
        val row1 = Row(keys = row1Keys, xOffsetPct = 0.5f)

        val row2Keys =
            listOf(
                letterKey('a', null, listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"), widthPct = 9.8f),
                letterKey('s', null, listOf("ß")),
                letterKey('d', null, emptyList()),
                letterKey('f', null, emptyList()),
                letterKey('g', null, emptyList()),
                letterKey('h', null, emptyList()),
                letterKey('j', null, emptyList()),
                letterKey('k', null, emptyList()),
                letterKey('l', null, emptyList()),
            )
        val row2 = Row(keys = row2Keys, xOffsetPct = 5.5f)

        val row3Keys =
            listOf(
                Key(
                    code = KeyCodes.SHIFT,
                    label = "Shift",
                    widthPct = 14.8f,
                    style = KeyStyle.SHIFT,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("CAPSLOCK", KeyCodes.CAPSLOCK)),
                ),
                letterKey('z', null, emptyList()),
                letterKey('x', null, emptyList()),
                letterKey('c', null, listOf("ç")),
                letterKey('v', null, emptyList()),
                letterKey('b', null, emptyList()),
                letterKey('n', null, listOf("ñ")),
                letterKey('m', null, emptyList()),
                Key(
                    code = KeyCodes.DELETE,
                    label = "⌫",
                    widthPct = 14.9f,
                    style = KeyStyle.DELETE,
                    flags = KeyFlags(repeatable = true),
                ),
            )
        val row3 = Row(keys = row3Keys, xOffsetPct = 0.5f)

        val periodMoreKeys =
            listOf(
                ",",
                "?",
                "!",
                "#",
                ")",
                "(",
                "/",
                ";",
                "'",
                "@",
                ":",
                "-",
                "\"",
                "+",
                "%",
                "&",
            ).map { MoreKey(it) }

        val row4Keys =
            listOf(
                Key(
                    code = KeyCodes.SYMBOLS,
                    label = "?123",
                    widthPct = 14.8f,
                    style = KeyStyle.FUNCTIONAL,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("EMOJI", KeyCodes.EMOJI_LONG_PRESS)),
                ),
                Key(
                    code = ','.code,
                    label = ",",
                    widthPct = 9.9f,
                    style = KeyStyle.FUNCTIONAL,
                    moreKeys = listOf(MoreKey("EMOJI", KeyCodes.COMMA_EMOJI)),
                ),
                Key(
                    code = KeyCodes.LANGUAGE,
                    label = "globe",
                    widthPct = 9.9f,
                    xPct = 25.2f,
                    style = KeyStyle.FUNCTIONAL,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("CHOOSER", KeyCodes.LANGUAGE_CHOOSER)),
                ),
                Key(
                    code = KeyCodes.SPACE,
                    label = "space",
                    widthPct = 39.5f,
                    style = KeyStyle.SPACE,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("IME_PICKER", KeyCodes.IME_PICKER)),
                ),
                Key(
                    code = '.'.code,
                    label = ".",
                    widthPct = 9.9f,
                    xPct = 74.7f,
                    style = KeyStyle.FUNCTIONAL,
                    moreKeys = periodMoreKeys,
                    moreKeysColumns = 8,
                ),
                Key(
                    code = KeyCodes.ENTER,
                    label = "Enter",
                    widthPct = 14.9f,
                    style = KeyStyle.ENTER,
                    flags = KeyFlags(enterAdaptive = true),
                    moreKeys = listOf(MoreKey("NEWLINE", KeyCodes.NEWLINE)),
                ),
            )
        val row4 = Row(keys = row4Keys, xOffsetPct = 0.5f)

        return Layout(
            id = LayoutSet.QWERTY,
            rows = listOf(row1, row2, row3, row4),
        )
    }

    /**
     * Standard SYMBOLS layout per KLA §4 and SPEC-002 Appendix A.
     */
    fun symbols(): Layout {
        val row1Keys =
            listOf(
                symbolDigitKey('1', listOf("¹", "½", "⅓", "¼", "⅕", "⅙", "⅐", "⅛", "⅑", "⅒", "₁")),
                symbolDigitKey('2', listOf("²", "⅔", "⅖", "₂")),
                symbolDigitKey('3', listOf("³", "¾", "⅗", "⅜", "₃")),
                symbolDigitKey('4', listOf("⁴", "⅘", "₄")),
                symbolDigitKey('5', listOf("⁵", "⅚", "⅝", "₅")),
                symbolDigitKey('6', listOf("⁶", "₆")),
                symbolDigitKey('7', listOf("⁷", "⅞", "₇")),
                symbolDigitKey('8', listOf("⁸", "₈")),
                symbolDigitKey('9', listOf("⁹", "₉")),
                symbolDigitKey('0', listOf("⁰", "ⁿ", "∅", "₀")),
            )
        val row1 = Row(keys = row1Keys, xOffsetPct = 0.5f)

        val row2Keys =
            listOf(
                symbolKey('@', emptyList()),
                symbolKey('#', listOf("№")),
                symbolKey('$', listOf("₹", "¢", "€", "£", "¥", "₱")),
                symbolKey('%', listOf("‰")),
                symbolKey('&', emptyList()),
                symbolKey('*', listOf("†", "‡", "★")),
                symbolKey('-', listOf("_", "–", "—", "·", "⁻", "₋")),
                symbolKey('=', listOf("≠", "≈", "∞", "⁼", "₌")),
                symbolKey('(', listOf("<", "{", "[", "⁽", "₍")),
                symbolKey(')', listOf(">", "}", "]", "⁾", "₎")),
            )
        val row2 = Row(keys = row2Keys, xOffsetPct = 0.5f)

        val row3Keys =
            listOf(
                Key(
                    code = KeyCodes.SHIFT_SYMBOLS,
                    label = "=\\<",
                    widthPct = 14.8f,
                    style = KeyStyle.FUNCTIONAL,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                ),
                symbolKey('!', listOf("¡", "✓")),
                symbolKey('"', listOf("„", "“", "”", "«", "»")),
                symbolKey('\'', listOf("‚", "‘", "’", "‹", "›")),
                symbolKey(':', emptyList()),
                symbolKey('+', listOf("±", "⁺", "₊")),
                symbolKey('/', emptyList()),
                symbolKey('?', listOf("¿")),
                Key(
                    code = KeyCodes.DELETE,
                    label = "⌫",
                    widthPct = 14.9f,
                    style = KeyStyle.DELETE,
                    flags = KeyFlags(repeatable = true),
                ),
            )
        val row3 = Row(keys = row3Keys, xOffsetPct = 0.5f)

        val row4 = symbolsBottomRow()

        return Layout(
            id = LayoutSet.SYMBOLS,
            rows = listOf(row1, row2, row3, row4),
        )
    }

    /**
     * Shifted symbols layout (SYMBOLS_SHIFT) per KLA §5 and SPEC-002 Appendix A.
     */
    fun symbolsShift(): Layout {
        val row1Keys =
            listOf(
                symbolKey('~', emptyList()),
                symbolKey('±', emptyList()),
                symbolKey('×', emptyList()),
                symbolKey('÷', listOf("√")),
                symbolKey('•', listOf("♪", "♥", "♠", "♦", "♣")),
                symbolKey('°', listOf("′", "″")),
                symbolKey('`', listOf("´")),
                symbolKey('_', emptyList()),
                symbolKey('{', emptyList()),
                symbolKey('}', emptyList()),
            )
        val row1 = Row(keys = row1Keys, xOffsetPct = 0.5f)

        val row2Keys =
            listOf(
                symbolKey('₹', emptyList()),
                symbolKey('£', emptyList()),
                symbolKey('€', emptyList()),
                symbolKey('¥', emptyList()),
                symbolKey('©', listOf("℅")),
                symbolKey('®', listOf("™")),
                symbolKey('^', listOf("↑", "↓", "←", "→")),
                symbolKey('π', listOf("√", "§", "∆", "Π", "¶", "∑", "Ω", "μ")),
                symbolKey('[', emptyList()),
                symbolKey(']', emptyList()),
            )
        val row2 = Row(keys = row2Keys, xOffsetPct = 0.5f)

        val row3Keys =
            listOf(
                Key(
                    code = KeyCodes.SYMBOLS,
                    label = "?123",
                    widthPct = 14.8f,
                    style = KeyStyle.FUNCTIONAL,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                ),
                symbolKey('¡', emptyList()),
                symbolKey('<', listOf("‹", "≤", "«", "⟨")),
                symbolKey('>', listOf("›", "≥", "»", "⟩")),
                symbolKey(';', emptyList()),
                symbolKey('|', emptyList()),
                symbolKey('\\', emptyList()),
                symbolKey('¿', listOf("π", "Π", "¶", "§", "∆")),
                Key(
                    code = KeyCodes.DELETE,
                    label = "⌫",
                    widthPct = 14.9f,
                    style = KeyStyle.DELETE,
                    flags = KeyFlags(repeatable = true),
                ),
            )
        val row3 = Row(keys = row3Keys, xOffsetPct = 0.5f)

        val row4 = symbolsBottomRow()

        return Layout(
            id = LayoutSet.SYMBOLS_SHIFT,
            rows = listOf(row1, row2, row3, row4),
        )
    }

    /**
     * Numeric keypad layout and field variants per KLA §6 and SPEC-002 Appendix A.
     */
    fun number(variant: NumberVariant = NumberVariant.NORMAL): Layout {
        val row1Keys =
            listOf(
                numKey('1'),
                numKey('2'),
                numKey('3'),
                Key(
                    code = '-'.code,
                    label = "-",
                    widthPct = 17.28f,
                    style = KeyStyle.FUNCTIONAL,
                    moreKeys = listOf(MoreKey("+")),
                ),
            )
        val row1 = Row(keys = row1Keys, xOffsetPct = 1.353f)

        val row2Key4 =
            when (variant) {
                NumberVariant.DATE -> Key(code = '.'.code, label = ".", widthPct = 17.28f, style = KeyStyle.FUNCTIONAL)
                NumberVariant.TIME, NumberVariant.DATETIME ->
                    Key(
                        code = '.'.code,
                        label = ".",
                        widthPct = 17.28f,
                        style = KeyStyle.FUNCTIONAL,
                        moreKeys = listOf(MoreKey("AM"), MoreKey("PM")),
                    )
                else -> Key(code = ','.code, label = ",", widthPct = 17.28f, style = KeyStyle.FUNCTIONAL)
            }
        val row2Keys =
            listOf(
                numKey('4'),
                numKey('5'),
                numKey('6'),
                row2Key4,
            )
        val row2 = Row(keys = row2Keys, xOffsetPct = 1.353f)

        val row3Keys =
            listOf(
                numKey('7'),
                numKey('8'),
                numKey('9'),
                Key(
                    code = KeyCodes.DELETE,
                    label = "⌫",
                    widthPct = 17.28f,
                    style = KeyStyle.DELETE,
                    flags = KeyFlags(repeatable = true),
                ),
            )
        val row3 = Row(keys = row3Keys, xOffsetPct = 1.353f)

        val row4Key1 =
            when (variant) {
                NumberVariant.DATE -> numKey('/')
                NumberVariant.TIME -> numKey(':')
                NumberVariant.DATETIME ->
                    Key(
                        code = '/'.code,
                        label = "/",
                        widthPct = 26.67f,
                        style = KeyStyle.LETTER,
                        moreKeys = listOf(MoreKey(":")),
                    )
                else -> numKey('.')
            }
        val row4Keys =
            listOf(
                row4Key1,
                numKey('0'),
                Key(
                    code = KeyCodes.SPACE,
                    label = "space",
                    widthPct = 26.67f,
                    style = KeyStyle.SPACE,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("IME_PICKER", KeyCodes.IME_PICKER)),
                ),
                Key(
                    code = KeyCodes.ENTER,
                    label = "Enter",
                    widthPct = 17.28f,
                    style = KeyStyle.ENTER,
                    flags = KeyFlags(enterAdaptive = true),
                ),
            )
        val row4 = Row(keys = row4Keys, xOffsetPct = 1.353f)

        return Layout(
            id = LayoutSet.NUMBER,
            rows = listOf(row1, row2, row3, row4),
            params = LayoutParams(horizontalGapPct = 1.353f, verticalGapDp = 6f),
        )
    }

    /**
     * 3x4 grid for password numeric fields per KLA §6 and SPEC-002 Appendix A.
     */
    fun numberPassword(): Layout {
        val row1 =
            Row(
                keys =
                    listOf(
                        Key(code = '1'.code, label = "1", widthPct = 33.33f),
                        Key(code = '2'.code, label = "2", widthPct = 33.33f),
                        Key(code = '3'.code, label = "3", widthPct = 33.33f),
                    ),
            )
        val row2 =
            Row(
                keys =
                    listOf(
                        Key(code = '4'.code, label = "4", widthPct = 33.33f),
                        Key(code = '5'.code, label = "5", widthPct = 33.33f),
                        Key(code = '6'.code, label = "6", widthPct = 33.33f),
                    ),
            )
        val row3 =
            Row(
                keys =
                    listOf(
                        Key(code = '7'.code, label = "7", widthPct = 33.33f),
                        Key(code = '8'.code, label = "8", widthPct = 33.33f),
                        Key(code = '9'.code, label = "9", widthPct = 33.33f),
                    ),
            )
        val row4 =
            Row(
                keys =
                    listOf(
                        Key(
                            code = KeyCodes.DELETE,
                            label = "⌫",
                            widthPct = 33.33f,
                            style = KeyStyle.DELETE,
                            flags = KeyFlags(repeatable = true),
                        ),
                        Key(
                            code = '0'.code,
                            label = "0",
                            hint = "+",
                            widthPct = 33.33f,
                            moreKeys = listOf(MoreKey("+")),
                        ),
                        Key(
                            code = KeyCodes.ENTER,
                            label = "Enter",
                            widthPct = 33.33f,
                            style = KeyStyle.ENTER,
                            flags = KeyFlags(enterAdaptive = true),
                        ),
                    ),
            )
        return Layout(
            id = LayoutSet.NUMBER_PASSWORD,
            rows = listOf(row1, row2, row3, row4),
            params = LayoutParams(horizontalGapPct = 1.353f, verticalGapDp = 6f),
        )
    }

    /**
     * Phone dialer layout per KLA §7 and SPEC-002 Appendix A.
     */
    fun phone(): Layout {
        val row1 =
            Row(
                keys =
                    listOf(
                        numKey('1'),
                        numKey('2'),
                        numKey('3'),
                        Key(
                            code = '-'.code,
                            label = "-",
                            widthPct = 17.28f,
                            style = KeyStyle.FUNCTIONAL,
                            moreKeys = listOf(MoreKey("+")),
                        ),
                    ),
                xOffsetPct = 1.353f,
            )
        val row2 =
            Row(
                keys =
                    listOf(
                        numKey('4'),
                        numKey('5'),
                        numKey('6'),
                        Key(code = '.'.code, label = ".", widthPct = 17.28f, style = KeyStyle.FUNCTIONAL),
                    ),
                xOffsetPct = 1.353f,
            )
        val row3 =
            Row(
                keys =
                    listOf(
                        numKey('7'),
                        numKey('8'),
                        numKey('9'),
                        Key(
                            code = KeyCodes.DELETE,
                            label = "⌫",
                            widthPct = 17.28f,
                            style = KeyStyle.DELETE,
                            flags = KeyFlags(repeatable = true),
                        ),
                    ),
                xOffsetPct = 1.353f,
            )
        val row4 =
            Row(
                keys =
                    listOf(
                        Key(
                            code = KeyCodes.SYMBOLS,
                            label = "＊＃",
                            widthPct = 26.67f,
                            style = KeyStyle.FUNCTIONAL,
                        ),
                        Key(
                            code = '0'.code,
                            label = "0",
                            widthPct = 26.67f,
                            moreKeys = listOf(MoreKey("+")),
                        ),
                        Key(
                            code = KeyCodes.SPACE,
                            label = "space",
                            widthPct = 26.67f,
                            style = KeyStyle.SPACE,
                            flags = KeyFlags(noPanelAutoMoreKey = true),
                            moreKeys = listOf(MoreKey("IME_PICKER", KeyCodes.IME_PICKER)),
                        ),
                        Key(
                            code = KeyCodes.ENTER,
                            label = "Enter",
                            widthPct = 17.28f,
                            style = KeyStyle.ENTER,
                            flags = KeyFlags(enterAdaptive = true),
                        ),
                    ),
                xOffsetPct = 1.353f,
            )
        return Layout(
            id = LayoutSet.PHONE,
            rows = listOf(row1, row2, row3, row4),
            params = LayoutParams(horizontalGapPct = 1.353f, verticalGapDp = 6f),
        )
    }

    /**
     * Phone symbols layout per KLA §7 and SPEC-002 Appendix A.
     */
    fun phoneSymbols(): Layout {
        val row1 =
            Row(
                keys =
                    listOf(
                        numKey('('),
                        numKey('/'),
                        numKey(')'),
                        Key(
                            code = '-'.code,
                            label = "-",
                            widthPct = 17.28f,
                            style = KeyStyle.FUNCTIONAL,
                            moreKeys = listOf(MoreKey("+")),
                        ),
                    ),
                xOffsetPct = 1.353f,
            )
        val row2 =
            Row(
                keys =
                    listOf(
                        numKey('N'),
                        Key(code = ','.code, label = "Pause", widthPct = 26.67f, style = KeyStyle.LETTER),
                        numKey(','),
                        Key(code = '.'.code, label = ".", widthPct = 17.28f, style = KeyStyle.FUNCTIONAL),
                    ),
                xOffsetPct = 1.353f,
            )
        val row3 =
            Row(
                keys =
                    listOf(
                        Key(code = '*'.code, label = "＊", widthPct = 26.67f, style = KeyStyle.LETTER),
                        Key(code = ';'.code, label = "Wait", widthPct = 26.67f, style = KeyStyle.LETTER),
                        numKey('#'),
                        Key(
                            code = KeyCodes.DELETE,
                            label = "⌫",
                            widthPct = 17.28f,
                            style = KeyStyle.DELETE,
                            flags = KeyFlags(repeatable = true),
                        ),
                    ),
                xOffsetPct = 1.353f,
            )
        val row4 =
            Row(
                keys =
                    listOf(
                        Key(
                            code = KeyCodes.NUMPAD,
                            label = "123",
                            widthPct = 26.67f,
                            style = KeyStyle.FUNCTIONAL,
                        ),
                        numKey('+'),
                        Key(
                            code = KeyCodes.SPACE,
                            label = "space",
                            widthPct = 26.67f,
                            style = KeyStyle.SPACE,
                            flags = KeyFlags(noPanelAutoMoreKey = true),
                            moreKeys = listOf(MoreKey("IME_PICKER", KeyCodes.IME_PICKER)),
                        ),
                        Key(
                            code = KeyCodes.ENTER,
                            label = "Enter",
                            widthPct = 17.28f,
                            style = KeyStyle.ENTER,
                            flags = KeyFlags(enterAdaptive = true),
                        ),
                    ),
                xOffsetPct = 1.353f,
            )
        return Layout(
            id = LayoutSet.PHONE_SYMBOLS,
            rows = listOf(row1, row2, row3, row4),
            params = LayoutParams(horizontalGapPct = 1.353f, verticalGapDp = 6f),
        )
    }

    private fun symbolsBottomRow(): Row {
        val keys =
            listOf(
                Key(
                    code = KeyCodes.ABC,
                    label = "ABC",
                    widthPct = 14.8f,
                    style = KeyStyle.FUNCTIONAL,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("EMOJI", KeyCodes.EMOJI_LONG_PRESS)),
                ),
                Key(
                    code = ','.code,
                    label = ",",
                    widthPct = 9.9f,
                    style = KeyStyle.FUNCTIONAL,
                    moreKeys = listOf(MoreKey("EMOJI", KeyCodes.COMMA_EMOJI)),
                ),
                Key(
                    code = KeyCodes.EMOJI,
                    label = "emoji",
                    widthPct = 9.9f,
                    style = KeyStyle.FUNCTIONAL,
                ),
                Key(
                    code = KeyCodes.SPACE,
                    label = "space",
                    widthPct = 29.7f,
                    xPct = 35.1f,
                    style = KeyStyle.SPACE,
                    flags = KeyFlags(noPanelAutoMoreKey = true),
                    moreKeys = listOf(MoreKey("IME_PICKER", KeyCodes.IME_PICKER)),
                ),
                Key(
                    code = KeyCodes.NUMPAD,
                    label = "123",
                    widthPct = 9.9f,
                    style = KeyStyle.FUNCTIONAL,
                ),
                Key(
                    code = '.'.code,
                    label = ".",
                    widthPct = 9.9f,
                    xPct = 74.7f,
                    style = KeyStyle.FUNCTIONAL,
                    moreKeys = listOf(MoreKey("…")),
                ),
                Key(
                    code = KeyCodes.ENTER,
                    label = "Enter",
                    widthPct = 14.9f,
                    style = KeyStyle.ENTER,
                    flags = KeyFlags(enterAdaptive = true),
                    moreKeys = listOf(MoreKey("NEWLINE", KeyCodes.NEWLINE)),
                ),
            )
        return Row(keys = keys, xOffsetPct = 0.5f)
    }

    private fun letterKey(
        char: Char,
        hint: String?,
        moreKeyLabels: List<String>,
        widthPct: Float = 9.9f,
    ): Key =
        Key(
            code = char.code,
            label = char.toString(),
            hint = hint,
            moreKeys = moreKeyLabels.map { MoreKey(it) },
            widthPct = widthPct,
            style = KeyStyle.LETTER,
            flags = KeyFlags(shiftAware = true),
        )

    private fun symbolDigitKey(
        char: Char,
        moreKeyLabels: List<String>,
    ): Key =
        Key(
            code = char.code,
            label = char.toString(),
            moreKeys = moreKeyLabels.map { MoreKey(it) },
            widthPct = 9.9f,
            style = KeyStyle.LETTER,
        )

    private fun symbolKey(
        char: Char,
        moreKeyLabels: List<String>,
    ): Key =
        Key(
            code = char.code,
            label = char.toString(),
            moreKeys = moreKeyLabels.map { MoreKey(it) },
            widthPct = 9.9f,
            style = KeyStyle.LETTER,
        )

    private fun numKey(char: Char): Key =
        Key(
            code = char.code,
            label = char.toString(),
            widthPct = 26.67f,
            style = KeyStyle.LETTER,
        )
}
