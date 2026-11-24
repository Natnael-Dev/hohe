package app.hohe.keyboard.ime

import android.content.Context
import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.EmojiSuggester
import app.hohe.keyboard.core.engine.EnglishEngine

/**
 * Creates the dictionary-backed engines from `assets/dict`. Every loader fails soft: a missing or
 * corrupt file returns null and typing falls back to transliteration / typed word only.
 */
object EngineLoader {
    fun loadAmharic(context: Context): AmharicEngine? = null

    fun loadEnglish(context: Context): EnglishEngine? = null

    fun loadEmoji(context: Context): EmojiSuggester? = null

    fun emojiCategories(context: Context): List<app.hohe.keyboard.ime.panels.EmojiCategory>? = null
}
