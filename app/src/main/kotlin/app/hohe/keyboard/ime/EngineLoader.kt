package app.hohe.keyboard.ime

import android.content.Context
import android.os.Build
import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.EmojiSuggester
import app.hohe.keyboard.core.engine.EnglishEngine
import app.hohe.keyboard.english.HoheEnglishEngine
import app.hohe.keyboard.english.emoji.EmojiCatalog
import app.hohe.keyboard.english.emoji.EmojiData
import app.hohe.keyboard.english.emoji.HoheEmojiSuggester
import app.hohe.keyboard.ime.panels.EmojiCategory
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Creates the dictionary-backed engines from `assets/dict`. Every loader fails soft: a missing or
 * corrupt file returns null (or an empty engine) and typing falls back to the transliteration or
 * the typed word only.
 */
object EngineLoader {
    private const val EN_DICT = "dict/en_main.bin"
    private const val AM_DICT = "dict/am_words.bin"
    private const val EMOJI = "dict/emoji.tsv"

    @Volatile private var emojiData: EmojiData? = null

    /** Memory-maps an uncompressed asset (`noCompress += "bin"`); falls back to a heap copy. */
    internal fun mapAsset(
        context: Context,
        path: String,
    ): ByteBuffer? =
        try {
            context.assets.openFd(path).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { ch ->
                    ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.length)
                }
            }
        } catch (e: java.io.IOException) {
            try {
                context.assets.open(path).use { ByteBuffer.wrap(it.readBytes()) }
            } catch (e2: java.io.IOException) {
                null
            }
        }

    fun loadAmharic(context: Context): AmharicEngine? = AmharicLoader.load(mapAsset(context, AM_DICT))

    fun loadEnglish(context: Context): EnglishEngine? = HoheEnglishEngine.load(mapAsset(context, EN_DICT))

    fun loadEmoji(context: Context): EmojiSuggester? = HoheEmojiSuggester(emoji(context))

    /** Panel categories limited to emoji the running OS can draw. Null if the data is unusable. */
    fun emojiCategories(context: Context): List<EmojiCategory>? {
        val catalog = EmojiCatalog(emoji(context))
        val cats = catalog.categories(maxEmojiVersion())
        if (cats.isEmpty()) return null
        return cats.map { c -> EmojiCategory(c.id, c.emoji.firstOrNull() ?: "•", c.emoji) }
    }

    private fun emoji(context: Context): EmojiData =
        emojiData ?: synchronized(this) {
            emojiData ?: run {
                val d =
                    try {
                        context.assets.open(EMOJI).use { EmojiData.loadOrEmpty(it) }
                    } catch (e: java.io.IOException) {
                        EmojiData.EMPTY
                    }
                emojiData = d
                d
            }
        }

    /** Newest Unicode emoji version the platform emoji font supports, by API level. */
    fun maxEmojiVersion(sdk: Int = Build.VERSION.SDK_INT): Double =
        when {
            sdk >= 36 -> 16.0
            sdk >= 35 -> 15.1
            sdk >= 34 -> 15.0
            sdk >= 33 -> 14.0
            sdk >= 31 -> 13.1
            sdk >= 30 -> 13.0
            sdk >= 29 -> 12.0
            sdk >= 28 -> 11.0
            else -> 5.0
        }
}
