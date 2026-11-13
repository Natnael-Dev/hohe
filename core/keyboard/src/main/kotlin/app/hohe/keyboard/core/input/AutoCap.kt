package app.hohe.keyboard.core.input

/**
 * Capitalization modes mirroring standard IME input behavior.
 */
enum class AutoCapMode {
    /** No capitalization applied automatically. */
    NONE,

    /** Every character is capitalized. */
    CHARACTERS,

    /** The first character of each word is capitalized. */
    WORDS,

    /** The first character of each sentence is capitalized. */
    SENTENCES,
}

/**
 * Evaluates whether auto-capitalization should be applied given the text before the cursor.
 */
object AutoCap {
    private val SENTENCE_TERMINATORS = charArrayOf('.', '!', '?', '።', '፧')

    /**
     * Determines whether to capitalize based on text context, user setting, field policy, and mode.
     */
    fun shouldCapitalize(
        textBeforeCursor: CharSequence?,
        enabled: Boolean = true,
        fieldAllowsAutoCap: Boolean = true,
        mode: AutoCapMode = AutoCapMode.SENTENCES,
    ): Boolean {
        if (!enabled || !fieldAllowsAutoCap || mode == AutoCapMode.NONE) {
            return false
        }

        if (mode == AutoCapMode.CHARACTERS) {
            return true
        }

        if (textBeforeCursor == null || textBeforeCursor.isEmpty()) {
            return true
        }

        // Entirely whitespace
        var allWhitespace = true
        for (i in 0 until textBeforeCursor.length) {
            if (!textBeforeCursor[i].isWhitespace()) {
                allWhitespace = false
                break
            }
        }
        if (allWhitespace) {
            return true
        }

        if (mode == AutoCapMode.WORDS) {
            return textBeforeCursor.last().isWhitespace()
        }

        // Mode: SENTENCES
        // Scan backwards past horizontal whitespace (spaces and tabs)
        var i = textBeforeCursor.length - 1
        var spaceCount = 0
        while (i >= 0 && (textBeforeCursor[i] == ' ' || textBeforeCursor[i] == '\t')) {
            spaceCount++
            i--
        }

        if (i < 0) {
            return true
        }

        val charBeforeSpaces = textBeforeCursor[i]

        // True after newline (regardless of following spaces)
        if (charBeforeSpaces == '\n' || charBeforeSpaces == '\r') {
            return true
        }

        // True after sentence terminator followed by one or more spaces
        if (spaceCount > 0 && charBeforeSpaces in SENTENCE_TERMINATORS) {
            return true
        }

        return false
    }

    /**
     * Overload matching SPEC-002 R12 exact signature.
     */
    fun shouldCapitalize(
        textBeforeCursor: CharSequence?,
        enabled: Boolean,
        fieldAllowsAutoCap: Boolean,
    ): Boolean =
        shouldCapitalize(
            textBeforeCursor = textBeforeCursor,
            enabled = enabled,
            fieldAllowsAutoCap = fieldAllowsAutoCap,
            mode = AutoCapMode.SENTENCES,
        )
}
