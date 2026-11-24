package app.hohe.keyboard.core.settings

/** Typing language toggled by the language key (REQ-011). */
enum class TypingLanguage { AMHARIC, ENGLISH }

/** Typing style chosen in the chooser (REQ-052). */
enum class TypingStyle { TRANSLITERATION, ENGLISH, HANDWRITING, VOICE }

/** Built-in keyboard theme ids (REQ-070, REQ-016). */
enum class ThemeId {
    AUTO,
    DAYLIGHT,
    NIGHT,
    HIGHLAND,
    LAKE_TANA,
    BUNA,
    TEFF,
    SLATE,
    HC_YELLOW,
    HC_WHITE,
    PHOTO,
}

/** Handwriting speed ids and their recognition delay (settings-analysis §3.5). */
enum class HandwritingSpeed(
    val delayMs: Int,
) {
    VERY_SLOW(2000),
    SLOW(1500),
    MID_SLOW(1000),
    NORMAL(600),
    FAST(400),
    VERY_FAST(250),
}

/**
 * Every user setting with its default (REQ-004..016, 023, 026, 041, 061, 062, 070..073).
 * Persisted by `:data`; read by the IME as an immutable snapshot.
 */
data class KeyboardSettings(
    // Keyboard height and layout
    val heightMultiplier: Float = 1.0f,
    val keyTextSize: Float = 1.0f,
    val bottomSpaceDp: Int = 0,
    val keyBorders: Boolean = true,
    // Sound and vibration
    val sound: Boolean = false,
    val soundLevel: Int = 50,
    val vibrate: Boolean = true,
    /** -1 = system haptic default, otherwise duration in ms (0..100). */
    val vibrationMs: Int = -1,
    // Emojis, numbers and symbols
    val emojiRow: Boolean = true,
    val emojiSuggestions: Boolean = true,
    val numberRow: Boolean = false,
    val longPressForSymbols: Boolean = true,
    // Typing
    val longPressDelayMs: Int = 300,
    val selectSuggestionWithSpace: Boolean = true,
    val amharicPredictions: Boolean = true,
    val autoSpacePunctuation: Boolean = true,
    val autoCap: Boolean = true,
    val doubleSpacePeriod: Boolean = false,
    val keyPopup: Boolean = true,
    val nextWordSuggestions: Boolean = true,
    // Gestures
    val handwritingSpeed: HandwritingSpeed = HandwritingSpeed.NORMAL,
    val spaceCursorControl: Boolean = true,
    val swipeToDelete: Boolean = true,
    // Accessibility
    val highContrast: Boolean = false,
    val highContrastTheme: ThemeId = ThemeId.HC_YELLOW,
    // Outside the settings list
    val theme: ThemeId = ThemeId.AUTO,
    val photoBrightness: Float = 0.7f,
    val language: TypingLanguage = TypingLanguage.AMHARIC,
    val typingStyle: TypingStyle = TypingStyle.TRANSLITERATION,
    val clipboardEnabled: Boolean = true,
    val handwritingOptIn: Boolean = false,
) {
    /** Values clamped to their documented ranges; storage applies this on read. */
    fun sanitized(): KeyboardSettings =
        copy(
            heightMultiplier = heightMultiplier.coerceIn(HEIGHT_MIN, HEIGHT_MAX),
            keyTextSize = keyTextSize.coerceIn(TEXT_SIZE_STEPS.first(), TEXT_SIZE_STEPS.last()),
            bottomSpaceDp = bottomSpaceDp.coerceIn(0, BOTTOM_SPACE_MAX_DP),
            soundLevel = soundLevel.coerceIn(0, 100),
            vibrationMs = if (vibrationMs < 0) -1 else vibrationMs.coerceAtMost(100),
            longPressDelayMs = longPressDelayMs.coerceIn(LONG_PRESS_MIN_MS, LONG_PRESS_MAX_MS),
            photoBrightness = photoBrightness.coerceIn(0.2f, 1f),
            highContrastTheme =
                if (highContrastTheme == ThemeId.HC_WHITE) ThemeId.HC_WHITE else ThemeId.HC_YELLOW,
        )

    companion object {
        const val HEIGHT_MIN = 0.9f
        const val HEIGHT_MAX = 1.23f
        val HEIGHT_PRESETS = linkedMapOf("S" to 0.9f, "M" to 1.0f, "L" to 1.12f, "XL" to 1.23f)
        val TEXT_SIZE_STEPS = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.35f)
        const val BOTTOM_SPACE_MAX_DP = 48
        const val LONG_PRESS_MIN_MS = 150
        const val LONG_PRESS_MAX_MS = 2000
    }
}
