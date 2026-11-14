package app.hohe.keyboard.core.field

/**
 * Functional classification of an input field based on its Android input type and options.
 */
enum class FieldClass {
    /** Password fields; no learning, suggestions, or clipboard capture. */
    PASSWORD,

    /** Numeric, phone, and date/time fields. */
    NUMERIC,

    /** URL and email address fields. */
    ADDRESS,

    /** Private/incognito fields requesting no personalized learning (DICTIONARY_ONLY, or TYPED_ONLY with NO_SUGGESTIONS (D-035)). */
    INCOGNITO,

    /** Fields explicitly requesting no suggestion strip. */
    NO_SUGGEST,

    /** Standard text fields. */
    NORMAL,
}

/**
 * Suggestion strip presentation mode.
 */
enum class SuggestMode {
    /** No suggestions shown. */
    OFF,

    /** Only the verbatim typed text is displayed in the strip. */
    TYPED_ONLY,

    /** Dictionary candidates only; no user personalized learning history. */
    DICTIONARY_ONLY,

    /** Suggestions restricted to English only (e.g. for URLs and emails). */
    ENGLISH_ONLY,

    /** Full dictionary and personalized learning suggestions active. */
    ON,
}

/**
 * Base keyboard layout kind required by a field.
 */
enum class LayoutKind {
    /** Standard QWERTY layout. */
    QWERTY,

    /** Numeric pin/digits layout. */
    NUMBER,

    /** Phone dialer layout. */
    PHONE,

    /** Number password keypad. */
    NUMBER_PASSWORD,
}

/**
 * Action triggered by the keyboard enter key.
 */
enum class EnterAction {
    /** Inserts a newline character. */
    NEWLINE,

    /** Submits or navigates to the target (action ID 2). */
    GO,

    /** Performs a search query (action ID 3). */
    SEARCH,

    /** Sends a message (action ID 4). */
    SEND,

    /** Moves focus to the next field (action ID 5). */
    NEXT,

    /** Concludes input / closes keyboard (action ID 6). */
    DONE,

    /** Moves focus to the previous field (action ID 7). */
    PREVIOUS,
}

/**
 * Punctuation variant displayed on the comma key.
 */
enum class CommaVariant {
    /** Standard comma punctuation. */
    NORMAL,

    /** Forward slash for URLs. */
    URL,

    /** At sign for email addresses. */
    EMAIL,
}

/**
 * Privacy, layout, and input capabilities policy for a focused input field.
 */
data class FieldPolicy(
    val fieldClass: FieldClass,
    val suggestions: SuggestMode,
    val learning: Boolean,
    val clipboardCapture: Boolean,
    val amharicLookup: Boolean,
    val voiceKey: Boolean,
    val composing: Boolean,
    val autoCap: Boolean,
    val autoSpace: Boolean,
    val layout: LayoutKind,
    val enterAction: EnterAction,
    val commaVariant: CommaVariant,
) {
    companion object {
        internal const val TYPE_MASK_CLASS: Int = 0xF
        internal const val TYPE_CLASS_TEXT: Int = 1
        internal const val TYPE_CLASS_NUMBER: Int = 2
        internal const val TYPE_CLASS_PHONE: Int = 3
        internal const val TYPE_CLASS_DATETIME: Int = 4

        internal const val TYPE_MASK_VARIATION: Int = 0xFF0
        internal const val TEXT_VARIATION_URI: Int = 0x10
        internal const val TEXT_VARIATION_EMAIL_ADDRESS: Int = 0x20
        internal const val TEXT_VARIATION_PASSWORD: Int = 0x80
        internal const val TEXT_VARIATION_VISIBLE_PASSWORD: Int = 0x90
        internal const val TEXT_VARIATION_WEB_EMAIL_ADDRESS: Int = 0xD0
        internal const val TEXT_VARIATION_WEB_PASSWORD: Int = 0xE0
        internal const val NUMBER_VARIATION_PASSWORD: Int = 0x10
        internal const val DATETIME_VARIATION_DATE: Int = 0x10
        internal const val DATETIME_VARIATION_TIME: Int = 0x20

        internal const val TYPE_TEXT_FLAG_NO_SUGGESTIONS: Int = 0x80000

        internal const val IME_MASK_ACTION: Int = 0xFF
        internal const val IME_FLAG_NO_ENTER_ACTION: Int = 0x40000000
        internal const val IME_FLAG_NO_PERSONALIZED_LEARNING: Int = 0x1000000

        /**
         * Classifies field capabilities from Android InputType and imeOptions integer masks.
         */
        fun classify(
            inputType: Int,
            imeOptions: Int,
        ): FieldPolicy {
            val inputClass = inputType and TYPE_MASK_CLASS
            val variation = inputType and TYPE_MASK_VARIATION
            val noPersonalizedLearning = (imeOptions and IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
            val noSuggestionsFlag = (inputType and TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0

            val enterAction =
                if ((imeOptions and IME_FLAG_NO_ENTER_ACTION) != 0) {
                    EnterAction.NEWLINE
                } else {
                    when (imeOptions and IME_MASK_ACTION) {
                        2 -> EnterAction.GO
                        3 -> EnterAction.SEARCH
                        4 -> EnterAction.SEND
                        5 -> EnterAction.NEXT
                        6 -> EnterAction.DONE
                        7 -> EnterAction.PREVIOUS
                        else -> EnterAction.NEWLINE
                    }
                }

            // 1. PASSWORD (Precedence 1)
            val isTextPassword =
                inputClass == TYPE_CLASS_TEXT &&
                    (
                        variation == TEXT_VARIATION_PASSWORD ||
                            variation == TEXT_VARIATION_VISIBLE_PASSWORD ||
                            variation == TEXT_VARIATION_WEB_PASSWORD
                    )
            val isNumberPassword = inputClass == TYPE_CLASS_NUMBER && variation == NUMBER_VARIATION_PASSWORD

            if (isTextPassword || isNumberPassword) {
                return FieldPolicy(
                    fieldClass = FieldClass.PASSWORD,
                    suggestions = SuggestMode.OFF,
                    learning = false,
                    clipboardCapture = false,
                    amharicLookup = false,
                    voiceKey = false,
                    composing = false,
                    autoCap = false,
                    autoSpace = false,
                    layout = if (isNumberPassword) LayoutKind.NUMBER_PASSWORD else LayoutKind.QWERTY,
                    enterAction = enterAction,
                    commaVariant = CommaVariant.NORMAL,
                )
            }

            // 2. NUMERIC (Precedence 2)
            val isNumeric =
                inputClass == TYPE_CLASS_NUMBER ||
                    inputClass == TYPE_CLASS_PHONE ||
                    inputClass == TYPE_CLASS_DATETIME

            if (isNumeric) {
                return FieldPolicy(
                    fieldClass = FieldClass.NUMERIC,
                    suggestions = SuggestMode.OFF,
                    learning = false,
                    clipboardCapture = false,
                    amharicLookup = false,
                    voiceKey = false,
                    composing = false,
                    autoCap = false,
                    autoSpace = false,
                    layout = if (inputClass == TYPE_CLASS_PHONE) LayoutKind.PHONE else LayoutKind.NUMBER,
                    enterAction = enterAction,
                    commaVariant = CommaVariant.NORMAL,
                )
            }

            // 3. ADDRESS (Precedence 3)
            val isUri = inputClass == TYPE_CLASS_TEXT && variation == TEXT_VARIATION_URI
            val isEmail =
                inputClass == TYPE_CLASS_TEXT &&
                    (
                        variation == TEXT_VARIATION_EMAIL_ADDRESS ||
                            variation == TEXT_VARIATION_WEB_EMAIL_ADDRESS
                    )

            if (isUri || isEmail) {
                return FieldPolicy(
                    fieldClass = FieldClass.ADDRESS,
                    suggestions = SuggestMode.ENGLISH_ONLY,
                    learning = false,
                    clipboardCapture = false,
                    amharicLookup = false,
                    voiceKey = true,
                    composing = true,
                    autoCap = false,
                    autoSpace = isEmail,
                    layout = LayoutKind.QWERTY,
                    enterAction = enterAction,
                    commaVariant = if (isUri) CommaVariant.URL else CommaVariant.EMAIL,
                )
            }

            // 4. INCOGNITO (Precedence 4)
            if (noPersonalizedLearning) {
                return FieldPolicy(
                    fieldClass = FieldClass.INCOGNITO,
                    suggestions =
                        when {
                            inputType == 0 -> SuggestMode.OFF
                            noSuggestionsFlag -> SuggestMode.TYPED_ONLY
                            else -> SuggestMode.DICTIONARY_ONLY
                        },
                    learning = false,
                    clipboardCapture = false,
                    amharicLookup = true,
                    voiceKey = true,
                    composing = inputType != 0,
                    autoCap = true,
                    autoSpace = true,
                    layout = LayoutKind.QWERTY,
                    enterAction = enterAction,
                    commaVariant = CommaVariant.NORMAL,
                )
            }

            // 5. NO_SUGGEST (Precedence 5)
            if (noSuggestionsFlag) {
                return FieldPolicy(
                    fieldClass = FieldClass.NO_SUGGEST,
                    suggestions = SuggestMode.TYPED_ONLY,
                    learning = false,
                    clipboardCapture = true,
                    amharicLookup = true,
                    voiceKey = true,
                    composing = true,
                    autoCap = true,
                    autoSpace = true,
                    layout = LayoutKind.QWERTY,
                    enterAction = enterAction,
                    commaVariant = CommaVariant.NORMAL,
                )
            }

            // 6. NORMAL (Precedence 6)
            return FieldPolicy(
                fieldClass = FieldClass.NORMAL,
                suggestions = if (inputType == 0) SuggestMode.OFF else SuggestMode.ON,
                learning = !noPersonalizedLearning,
                clipboardCapture = true,
                amharicLookup = true,
                voiceKey = true,
                composing = inputType != 0,
                autoCap = true,
                autoSpace = true,
                layout = LayoutKind.QWERTY,
                enterAction = enterAction,
                commaVariant = CommaVariant.NORMAL,
            )
        }
    }
}
