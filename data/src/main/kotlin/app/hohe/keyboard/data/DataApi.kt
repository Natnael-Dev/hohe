package app.hohe.keyboard.data

import app.hohe.keyboard.core.engine.UserDataPort
import app.hohe.keyboard.core.engine.UserWord
import app.hohe.keyboard.core.settings.KeyboardSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Settings persistence (REQ-082, TASK-020). One process-wide instance, see [HoheData].
 * [settings] always holds a sanitized value; before the first disk read it holds the defaults.
 */
interface SettingsStore {
    val settings: StateFlow<KeyboardSettings>

    suspend fun update(transform: (KeyboardSettings) -> KeyboardSettings)

    /** Reset keyboard settings (Clear data screen, REQ-083). */
    suspend fun reset()
}

/** Categories on the Clear data screen (REQ-083, storage-architecture §6). */
enum class DataCategory {
    AMHARIC_PICKS,
    USER_WORDS,
    ENGLISH_HISTORY,
    CLIPBOARD,
    EMOJI_RECENTS,
}

data class ClipItem(
    val id: Long,
    val text: String,
    val pinned: Boolean,
    val createdAtMs: Long,
)

/**
 * Personal data (TASK-021). Writes are fire-and-forget on an IO thread unless `suspend`.
 * [snapshot] is cheap: it returns the latest immutable in-memory snapshot.
 */
interface PersonalDataStore : UserDataPort {
    val userWords: StateFlow<List<UserWord>>

    suspend fun addUserWord(word: UserWord)

    suspend fun deleteUserWord(word: UserWord)

    /** Recent (24 h) plus pinned items, newest first, pinned items first. */
    val clipboard: StateFlow<List<ClipItem>>

    /** Adds a clip unless [sensitive] or blank; de-duplicates by text. */
    fun addClip(
        text: String,
        sensitive: Boolean,
    )

    suspend fun setPinned(
        id: Long,
        pinned: Boolean,
    )

    suspend fun deleteClip(id: Long)

    /** Most recent first, at most 30. */
    val emojiRecents: StateFlow<List<String>>

    fun recordEmoji(emoji: String)

    /** Number of stored entries per category, for the Clear data screen. */
    suspend fun counts(): Map<DataCategory, Int>

    suspend fun clear(category: DataCategory)
}
