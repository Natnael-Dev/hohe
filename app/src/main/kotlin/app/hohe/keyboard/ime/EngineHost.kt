package app.hohe.keyboard.ime

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import app.hohe.keyboard.amharic.converter.AmharicConverter
import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.Candidate
import app.hohe.keyboard.core.engine.EmojiSuggester
import app.hohe.keyboard.core.engine.EnglishEngine
import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.SuggestionResult
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.ime.EngineProvider
import app.hohe.keyboard.core.ime.StripState
import app.hohe.keyboard.core.ime.SuggestScheduler

/**
 * Owns the single engine thread (language-engine-architecture §1). Only the latest request is
 * computed: submitting a new one drops any queued older one. Engines load lazily on that thread.
 */
class EngineHost(
    private val context: Context,
) : SuggestScheduler,
    EngineProvider {
    private val thread = HandlerThread("hohe-engine", Process.THREAD_PRIORITY_DEFAULT).apply { start() }
    private val engineHandler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private val token = Any()

    @Volatile private var amharicEngine: AmharicEngine? = null

    @Volatile private var englishEngine: EnglishEngine? = null

    @Volatile private var emojiSuggester: EmojiSuggester? = null

    override val amharic: AmharicEngine? get() = amharicEngine ?: ConverterOnlyAmharicEngine

    override val english: EnglishEngine? get() = englishEngine

    override val emoji: EmojiSuggester? get() = emojiSuggester

    /** Loads dictionaries off the main thread; typing works before this finishes. */
    fun preload() {
        engineHandler.post {
            if (amharicEngine == null) amharicEngine = EngineLoader.loadAmharic(context)
            if (englishEngine == null) englishEngine = EngineLoader.loadEnglish(context)
            if (emojiSuggester == null) emojiSuggester = EngineLoader.loadEmoji(context)
        }
    }

    override fun submit(
        work: () -> StripState,
        deliver: (StripState) -> Unit,
    ) {
        engineHandler.removeCallbacksAndMessages(token)
        val job =
            Runnable {
                val result = work()
                main.post { deliver(result) }
            }
        engineHandler.postAtTime(job, token, android.os.SystemClock.uptimeMillis())
    }

    /** Emoji panel categories from the bundled Unicode data, or a small built-in set. */
    fun emojiCategories(): List<app.hohe.keyboard.ime.panels.EmojiCategory> =
        EngineLoader.emojiCategories(context) ?: FallbackEmoji.CATEGORIES

    fun shutdown() {
        thread.quitSafely()
    }
}

/** Letter-by-letter Amharic used until (or if) the dictionary engine is unavailable (REQ-028). */
object ConverterOnlyAmharicEngine : AmharicEngine {
    override val dataVersion: String = "none"

    override fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult {
        val pick = if (req.usePersonalData) user.amharicPicks[req.input] else null
        val words = AmharicConverter.nBest(req.input, req.maxResults.coerceAtMost(5)).map { it.text }
        val ranked = (listOfNotNull(pick) + words).distinct()
        val cands =
            ranked.mapIndexed { i, w ->
                Candidate(w, if (w == pick) Source.USER_PICK else Source.FALLBACK_TRANSLIT, ranked.size - i)
            }
        return SuggestionResult(req.id, req.input, cands)
    }

    override fun transliterate(latin: String): String = AmharicConverter.convert(latin)
}
