package app.hohe.keyboard.ime

import app.hohe.keyboard.amharic.engine.HoheAmharicEngine
import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.SuggestionResult
import app.hohe.keyboard.core.engine.UserDataSnapshot
import java.nio.ByteBuffer

/** Builds the dictionary-backed Amharic engine; null keeps the letter-by-letter fallback. */
object AmharicLoader {
    fun load(buffer: ByteBuffer?): AmharicEngine? {
        val engine = HoheAmharicEngine.fromBuffer(buffer)
        if (engine.dataVersion == "none") return null
        return Locked(engine)
    }

    /**
     * The engine reuses scratch buffers, so calls are serialised. Normally only the engine thread
     * calls [suggest]; the main thread does so only when space arrives before a pending result.
     */
    private class Locked(
        private val delegate: AmharicEngine,
    ) : AmharicEngine {
        private val lock = Any()
        override val dataVersion: String get() = delegate.dataVersion

        override fun suggest(
            req: SuggestRequest,
            user: UserDataSnapshot,
        ): SuggestionResult = synchronized(lock) { delegate.suggest(req, user) }

        override fun transliterate(latin: String): String = delegate.transliterate(latin)
    }
}
