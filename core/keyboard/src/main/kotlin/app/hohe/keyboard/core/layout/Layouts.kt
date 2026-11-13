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
}
