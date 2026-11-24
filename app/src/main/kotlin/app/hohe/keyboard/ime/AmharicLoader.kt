package app.hohe.keyboard.ime

import app.hohe.keyboard.core.engine.AmharicEngine
import java.nio.ByteBuffer

/** Builds the dictionary-backed Amharic engine; null keeps the letter-by-letter fallback. */
object AmharicLoader {
    fun load(buffer: ByteBuffer?): AmharicEngine? = null
}
