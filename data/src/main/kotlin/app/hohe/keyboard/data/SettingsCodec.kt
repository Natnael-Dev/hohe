package app.hohe.keyboard.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.hohe.keyboard.core.settings.KeyboardSettings

/**
 * Maps [KeyboardSettings] to Preferences DataStore keys. Key names are stable persisted format:
 * never rename one without a migration. Names reuse the original app's keys where the value type
 * matches (settings-analysis §3–4); enums are stored by name and unknown names read as the default.
 */
internal object SettingsCodec {
    private val HEIGHT = floatPreferencesKey("keyboard_size_multiplier")
    private val KEY_TEXT_SIZE = floatPreferencesKey("key_font_size_multiplier")
    private val BOTTOM_SPACE = intPreferencesKey("keyboard_bottom_padding")
    private val KEY_BORDERS = booleanPreferencesKey("key_border")
    private val SOUND = booleanPreferencesKey("sound")
    private val SOUND_LEVEL = intPreferencesKey("sound_level")
    private val VIBRATE = booleanPreferencesKey("vibrate")
    private val VIBRATION_MS = intPreferencesKey("vibrate_level")
    private val EMOJI_ROW = booleanPreferencesKey("emoji_row")
    private val EMOJI_SUGGESTIONS = booleanPreferencesKey("emoji_suggestions_enabled")
    private val NUMBER_ROW = booleanPreferencesKey("number_row")
    private val LONG_PRESS_SYMBOLS = booleanPreferencesKey("long_press_for_symbols")
    private val LONG_PRESS_DELAY = intPreferencesKey("longpress_timeout")
    private val SELECT_WITH_SPACE = booleanPreferencesKey("auto_replace")
    private val AMHARIC_PREDICTIONS = booleanPreferencesKey("smart_prediction")
    private val AUTO_SPACE_PUNCT = booleanPreferencesKey("insert_space")
    private val AUTO_CAP = booleanPreferencesKey("auto_cap")
    private val DOUBLE_SPACE_PERIOD = booleanPreferencesKey("pref_key_use_double_space_period")
    private val KEY_POPUP = booleanPreferencesKey("popup_on")
    private val NEXT_WORD = booleanPreferencesKey("next_word_suggestions")
    private val HANDWRITING_SPEED = stringPreferencesKey("handwriting_speed_name")
    private val SPACE_CURSOR = booleanPreferencesKey("pref_space_track_pad")
    private val SWIPE_DELETE = booleanPreferencesKey("pref_delete_swipe")
    private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_mode")
    private val HIGH_CONTRAST_THEME = stringPreferencesKey("high_contrast_theme")
    private val THEME = stringPreferencesKey("theme_selected")
    private val PHOTO_BRIGHTNESS = floatPreferencesKey("photo_brightness")
    private val LANGUAGE = stringPreferencesKey("typing_language")
    private val TYPING_STYLE = stringPreferencesKey("typing_style")
    private val CLIPBOARD = booleanPreferencesKey("clipboard_enabled")
    private val HANDWRITING_OPT_IN = booleanPreferencesKey("handwriting_opt_in")

    /** All keys this codec writes; used by tests to check every field has a key. */
    val allKeys: List<Preferences.Key<*>> =
        listOf(
            HEIGHT,
            KEY_TEXT_SIZE,
            BOTTOM_SPACE,
            KEY_BORDERS,
            SOUND,
            SOUND_LEVEL,
            VIBRATE,
            VIBRATION_MS,
            EMOJI_ROW,
            EMOJI_SUGGESTIONS,
            NUMBER_ROW,
            LONG_PRESS_SYMBOLS,
            LONG_PRESS_DELAY,
            SELECT_WITH_SPACE,
            AMHARIC_PREDICTIONS,
            AUTO_SPACE_PUNCT,
            AUTO_CAP,
            DOUBLE_SPACE_PERIOD,
            KEY_POPUP,
            NEXT_WORD,
            HANDWRITING_SPEED,
            SPACE_CURSOR,
            SWIPE_DELETE,
            HIGH_CONTRAST,
            HIGH_CONTRAST_THEME,
            THEME,
            PHOTO_BRIGHTNESS,
            LANGUAGE,
            TYPING_STYLE,
            CLIPBOARD,
            HANDWRITING_OPT_IN,
        )

    fun decode(p: Preferences): KeyboardSettings {
        val d = KeyboardSettings()
        return KeyboardSettings(
            heightMultiplier = p[HEIGHT] ?: d.heightMultiplier,
            keyTextSize = p[KEY_TEXT_SIZE] ?: d.keyTextSize,
            bottomSpaceDp = p[BOTTOM_SPACE] ?: d.bottomSpaceDp,
            keyBorders = p[KEY_BORDERS] ?: d.keyBorders,
            sound = p[SOUND] ?: d.sound,
            soundLevel = p[SOUND_LEVEL] ?: d.soundLevel,
            vibrate = p[VIBRATE] ?: d.vibrate,
            vibrationMs = p[VIBRATION_MS] ?: d.vibrationMs,
            emojiRow = p[EMOJI_ROW] ?: d.emojiRow,
            emojiSuggestions = p[EMOJI_SUGGESTIONS] ?: d.emojiSuggestions,
            numberRow = p[NUMBER_ROW] ?: d.numberRow,
            longPressForSymbols = p[LONG_PRESS_SYMBOLS] ?: d.longPressForSymbols,
            longPressDelayMs = p[LONG_PRESS_DELAY] ?: d.longPressDelayMs,
            selectSuggestionWithSpace = p[SELECT_WITH_SPACE] ?: d.selectSuggestionWithSpace,
            amharicPredictions = p[AMHARIC_PREDICTIONS] ?: d.amharicPredictions,
            autoSpacePunctuation = p[AUTO_SPACE_PUNCT] ?: d.autoSpacePunctuation,
            autoCap = p[AUTO_CAP] ?: d.autoCap,
            doubleSpacePeriod = p[DOUBLE_SPACE_PERIOD] ?: d.doubleSpacePeriod,
            keyPopup = p[KEY_POPUP] ?: d.keyPopup,
            nextWordSuggestions = p[NEXT_WORD] ?: d.nextWordSuggestions,
            handwritingSpeed = enumOr(p[HANDWRITING_SPEED], d.handwritingSpeed),
            spaceCursorControl = p[SPACE_CURSOR] ?: d.spaceCursorControl,
            swipeToDelete = p[SWIPE_DELETE] ?: d.swipeToDelete,
            highContrast = p[HIGH_CONTRAST] ?: d.highContrast,
            highContrastTheme = enumOr(p[HIGH_CONTRAST_THEME], d.highContrastTheme),
            theme = enumOr(p[THEME], d.theme),
            photoBrightness = p[PHOTO_BRIGHTNESS] ?: d.photoBrightness,
            language = enumOr(p[LANGUAGE], d.language),
            typingStyle = enumOr(p[TYPING_STYLE], d.typingStyle),
            clipboardEnabled = p[CLIPBOARD] ?: d.clipboardEnabled,
            handwritingOptIn = p[HANDWRITING_OPT_IN] ?: d.handwritingOptIn,
        ).sanitized()
    }

    fun encode(
        s: KeyboardSettings,
        p: MutablePreferences,
    ) {
        p[HEIGHT] = s.heightMultiplier
        p[KEY_TEXT_SIZE] = s.keyTextSize
        p[BOTTOM_SPACE] = s.bottomSpaceDp
        p[KEY_BORDERS] = s.keyBorders
        p[SOUND] = s.sound
        p[SOUND_LEVEL] = s.soundLevel
        p[VIBRATE] = s.vibrate
        p[VIBRATION_MS] = s.vibrationMs
        p[EMOJI_ROW] = s.emojiRow
        p[EMOJI_SUGGESTIONS] = s.emojiSuggestions
        p[NUMBER_ROW] = s.numberRow
        p[LONG_PRESS_SYMBOLS] = s.longPressForSymbols
        p[LONG_PRESS_DELAY] = s.longPressDelayMs
        p[SELECT_WITH_SPACE] = s.selectSuggestionWithSpace
        p[AMHARIC_PREDICTIONS] = s.amharicPredictions
        p[AUTO_SPACE_PUNCT] = s.autoSpacePunctuation
        p[AUTO_CAP] = s.autoCap
        p[DOUBLE_SPACE_PERIOD] = s.doubleSpacePeriod
        p[KEY_POPUP] = s.keyPopup
        p[NEXT_WORD] = s.nextWordSuggestions
        p[HANDWRITING_SPEED] = s.handwritingSpeed.name
        p[SPACE_CURSOR] = s.spaceCursorControl
        p[SWIPE_DELETE] = s.swipeToDelete
        p[HIGH_CONTRAST] = s.highContrast
        p[HIGH_CONTRAST_THEME] = s.highContrastTheme.name
        p[THEME] = s.theme.name
        p[PHOTO_BRIGHTNESS] = s.photoBrightness
        p[LANGUAGE] = s.language.name
        p[TYPING_STYLE] = s.typingStyle.name
        p[CLIPBOARD] = s.clipboardEnabled
        p[HANDWRITING_OPT_IN] = s.handwritingOptIn
    }

    private inline fun <reified E : Enum<E>> enumOr(
        name: String?,
        default: E,
    ): E = if (name == null) default else enumValues<E>().firstOrNull { it.name == name } ?: default
}
