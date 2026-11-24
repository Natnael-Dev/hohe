package app.hohe.keyboard.data

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.engine.UserWord
import app.hohe.keyboard.core.time.Clock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/** Retention limits (storage-architecture §1, TASK-021). Tests pass smaller values. */
internal data class PersonalDataLimits(
    val amharicPicks: Int = 10_000,
    val englishUnigrams: Int = 20_000,
    val englishBigrams: Int = 20_000,
    val unpinnedClips: Int = 50,
    val clipTtlMs: Long = 24L * 60 * 60 * 1000,
    val maxClipChars: Int = 10_000,
    val emojiRecents: Int = 30,
    val maxWordChars: Int = 48,
)

/**
 * [PersonalDataStore] on framework SQLite ([UserWordsDb], [HistoryDb]).
 *
 * Threading: every database access runs on one private IO thread, so writes are ordered and never
 * block the caller. Fire-and-forget writes ([recordAmharicPick], [recordEnglishCommit], [addClip],
 * [recordEmoji]) mark the affected data dirty and a coalesced publish rebuilds it; `suspend` writes
 * publish before they return. [snapshot] reads an atomic reference and never touches the disk.
 *
 * Privacy: nothing here logs words or clipboard text; failures log the exception class only.
 * The caller decides whether learning is allowed (password / incognito fields, REQ-017).
 */
class HohePersonalDataStore internal constructor(
    private val userWordsDb: UserWordsDb,
    private val historyDb: HistoryDb,
    private val clock: Clock,
    private val limits: PersonalDataLimits = PersonalDataLimits(),
) : PersonalDataStore {
    private val executor =
        Executors.newSingleThreadExecutor { r -> Thread(r, "hohe-personal-data").apply { isDaemon = true } }
    private val dispatcher: ExecutorCoroutineDispatcher = executor.asCoroutineDispatcher()
    private val scope =
        CoroutineScope(
            SupervisorJob() + dispatcher +
                CoroutineExceptionHandler { _, e -> Log.w(TAG, "personal data write failed: ${e.javaClass.simpleName}") },
        )

    private val snapshotRef = AtomicReference(UserDataSnapshot.EMPTY)
    private val userWordsState = MutableStateFlow<List<UserWord>>(emptyList())
    private val clipboardState = MutableStateFlow<List<ClipItem>>(emptyList())
    private val emojiState = MutableStateFlow<List<String>>(emptyList())

    // Confined to the IO thread.
    private var dirty = 0
    private var publishQueued = false
    private var clipExpiryJob: Job? = null

    override val userWords: StateFlow<List<UserWord>> = userWordsState.asStateFlow()
    override val clipboard: StateFlow<List<ClipItem>> = clipboardState.asStateFlow()
    override val emojiRecents: StateFlow<List<String>> = emojiState.asStateFlow()

    init {
        scope.launch {
            purgeClips(history(), clock.nowMs())
            publish(ALL)
        }
    }

    override fun snapshot(): UserDataSnapshot = snapshotRef.get()

    // ---- Amharic ----

    override fun recordAmharicPick(
        latin: String,
        ethiopic: String,
    ) {
        val key = latin.trim().lowercase(Locale.ROOT)
        val value = ethiopic.trim()
        if (key.isEmpty() || value.isEmpty() || key.length > limits.maxWordChars) return
        scope.launch {
            val db = history()
            db.inTransaction {
                val cv = ContentValues(3)
                cv.put("latin", key)
                cv.put("ethiopic", value)
                cv.put("last_used", clock.nowMs())
                db.insertWithOnConflict("am_pick", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                evictOldest(db, "am_pick", limits.amharicPicks)
            }
            schedulePublish(PICKS)
        }
    }

    override suspend fun addUserWord(word: UserWord) {
        val latin = word.latin.trim().lowercase(Locale.ROOT)
        val ethiopic = word.ethiopic.trim()
        if (latin.isEmpty() || ethiopic.isEmpty()) return
        withContext(dispatcher) {
            val cv = ContentValues(3)
            cv.put("latin", latin)
            cv.put("ethiopic", ethiopic)
            cv.put("created_at", clock.nowMs())
            userWordsDb.writableDatabase
                .insertWithOnConflict("am_user_word", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            publish(USER_WORDS)
        }
    }

    override suspend fun deleteUserWord(word: UserWord) {
        withContext(dispatcher) {
            userWordsDb.writableDatabase.delete(
                "am_user_word",
                "latin = ? AND ethiopic = ?",
                arrayOf(word.latin.trim().lowercase(Locale.ROOT), word.ethiopic.trim()),
            )
            publish(USER_WORDS)
        }
    }

    // ---- English ----

    override fun recordEnglishCommit(
        word: String,
        previous: String?,
    ) {
        val w = normalizeEnglish(word) ?: return
        val prev = previous?.let(::normalizeEnglish)
        scope.launch {
            val db = history()
            val now = clock.nowMs()
            db.inTransaction {
                bump(db, "en_word", "word = ?", arrayOf(w), now) {
                    put("word", w)
                }
                evictOldest(db, "en_word", limits.englishUnigrams)
                if (prev != null) {
                    bump(db, "en_bigram", "prev = ? AND next = ?", arrayOf(prev, w), now) {
                        put("prev", prev)
                        put("next", w)
                    }
                    evictOldest(db, "en_bigram", limits.englishBigrams)
                }
            }
            schedulePublish(if (prev != null) UNIGRAMS or BIGRAMS else UNIGRAMS)
        }
    }

    private fun normalizeEnglish(raw: String): String? {
        val w = raw.trim().lowercase(Locale.ROOT)
        return w.takeIf { it.length <= limits.maxWordChars && it.any(Char::isLetter) }
    }

    // ---- Clipboard ----

    override fun addClip(
        text: String,
        sensitive: Boolean,
    ) {
        if (sensitive || text.isBlank() || text.length > limits.maxClipChars) return
        scope.launch {
            val db = history()
            val now = clock.nowMs()
            db.inTransaction {
                val wasPinned =
                    DatabaseUtils.longForQuery(
                        db,
                        "SELECT COALESCE(MAX(pinned), 0) FROM clip_item WHERE text = ?",
                        arrayOf(text),
                    )
                // Delete + insert gives the clip a new, highest id so it sorts first.
                db.delete("clip_item", "text = ?", arrayOf(text))
                val cv = ContentValues(3)
                cv.put("text", text)
                cv.put("pinned", wasPinned)
                cv.put("created_at", now)
                db.insertOrThrow("clip_item", null, cv)
                purgeClips(db, now)
            }
            schedulePublish(CLIPS)
        }
    }

    override suspend fun setPinned(
        id: Long,
        pinned: Boolean,
    ) {
        withContext(dispatcher) {
            val db = history()
            val now = clock.nowMs()
            db.inTransaction {
                val cv = ContentValues(2)
                cv.put("pinned", if (pinned) 1 else 0)
                // An unpinned item starts a fresh 24 h life instead of expiring at once.
                if (!pinned) cv.put("created_at", now)
                db.update("clip_item", cv, "id = ?", arrayOf(id.toString()))
                purgeClips(db, now)
            }
            publish(CLIPS)
        }
    }

    override suspend fun deleteClip(id: Long) {
        withContext(dispatcher) {
            history().delete("clip_item", "id = ?", arrayOf(id.toString()))
            publish(CLIPS)
        }
    }

    private fun purgeClips(
        db: SQLiteDatabase,
        now: Long,
    ) {
        db.delete("clip_item", "pinned = 0 AND created_at < ?", arrayOf((now - limits.clipTtlMs).toString()))
        db.execSQL(
            "DELETE FROM clip_item WHERE pinned = 0 AND id NOT IN (SELECT id FROM clip_item " +
                "WHERE pinned = 0 ORDER BY created_at DESC, id DESC LIMIT ${limits.unpinnedClips})",
        )
    }

    // ---- Emoji ----

    override fun recordEmoji(emoji: String) {
        val e = emoji.trim()
        if (e.isEmpty() || e.length > MAX_EMOJI_CHARS) return
        scope.launch {
            val db = history()
            db.inTransaction {
                val cv = ContentValues(2)
                cv.put("emoji", e)
                cv.put("last_used", clock.nowMs())
                // REPLACE deletes then inserts, so the row gets a new, highest rowid.
                db.insertWithOnConflict("emoji_recent", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                db.execSQL(
                    "DELETE FROM emoji_recent WHERE rowid NOT IN (SELECT rowid FROM emoji_recent " +
                        "ORDER BY last_used DESC, rowid DESC LIMIT ${limits.emojiRecents})",
                )
            }
            schedulePublish(EMOJI)
        }
    }

    // ---- Clear data screen ----

    override suspend fun counts(): Map<DataCategory, Int> =
        withContext(dispatcher) {
            val h = history()
            purgeClips(h, clock.nowMs())
            publish(CLIPS)
            val words = userWordsDb.readableDatabase
            DataCategory.entries.associateWith { category ->
                when (category) {
                    DataCategory.AMHARIC_PICKS -> DatabaseUtils.queryNumEntries(h, "am_pick")
                    DataCategory.USER_WORDS -> DatabaseUtils.queryNumEntries(words, "am_user_word")
                    DataCategory.ENGLISH_HISTORY -> DatabaseUtils.queryNumEntries(h, "en_word")
                    DataCategory.CLIPBOARD -> DatabaseUtils.queryNumEntries(h, "clip_item")
                    DataCategory.EMOJI_RECENTS -> DatabaseUtils.queryNumEntries(h, "emoji_recent")
                }.toInt()
            }
        }

    override suspend fun clear(category: DataCategory) {
        withContext(dispatcher) {
            when (category) {
                DataCategory.AMHARIC_PICKS -> {
                    history().delete("am_pick", null, null)
                    publish(PICKS)
                }
                DataCategory.USER_WORDS -> {
                    userWordsDb.writableDatabase.delete("am_user_word", null, null)
                    publish(USER_WORDS)
                }
                DataCategory.ENGLISH_HISTORY -> {
                    val db = history()
                    db.inTransaction {
                        db.delete("en_word", null, null)
                        db.delete("en_bigram", null, null)
                    }
                    publish(UNIGRAMS or BIGRAMS)
                }
                // Recent and pinned items together; the enum has one clipboard row.
                DataCategory.CLIPBOARD -> {
                    history().delete("clip_item", null, null)
                    publish(CLIPS)
                }
                DataCategory.EMOJI_RECENTS -> {
                    history().delete("emoji_recent", null, null)
                    publish(EMOJI)
                }
            }
        }
    }

    // ---- Publishing ----

    /** Waits for queued writes and publishes their results. For tests and orderly shutdown. */
    internal suspend fun awaitIdle() {
        withContext(dispatcher) { publish(0) }
    }

    /** Stops the IO thread and closes both databases. The store is unusable afterwards. */
    internal fun close() {
        scope.cancel()
        executor.execute {
            userWordsDb.close()
            historyDb.close()
        }
        executor.shutdown()
    }

    private fun schedulePublish(bits: Int) {
        dirty = dirty or bits
        if (!publishQueued) {
            publishQueued = true
            scope.launch { publish(0) }
        }
    }

    private fun publish(bits: Int) {
        val todo = dirty or bits
        dirty = 0
        publishQueued = false
        if (todo == 0) return
        if (todo and (USER_WORDS or PICKS or UNIGRAMS or BIGRAMS) != 0) {
            val old = snapshotRef.get()
            val words = if (todo and USER_WORDS != 0) readUserWords() else old.userWords
            if (todo and USER_WORDS != 0) userWordsState.value = words
            snapshotRef.set(
                UserDataSnapshot(
                    userWords = words,
                    amharicPicks = if (todo and PICKS != 0) readPicks() else old.amharicPicks,
                    englishUnigrams = if (todo and UNIGRAMS != 0) readUnigrams() else old.englishUnigrams,
                    englishBigrams = if (todo and BIGRAMS != 0) readBigrams() else old.englishBigrams,
                ),
            )
        }
        if (todo and CLIPS != 0) {
            val clips = readClips(clock.nowMs())
            clipboardState.value = clips
            scheduleClipExpiry(clips)
        }
        if (todo and EMOJI != 0) emojiState.value = readEmoji()
    }

    private fun scheduleClipExpiry(clips: List<ClipItem>) {
        clipExpiryJob?.cancel()
        clipExpiryJob = null
        val oldest = clips.filterNot { it.pinned }.minOfOrNull { it.createdAtMs } ?: return
        val waitMs = (oldest + limits.clipTtlMs - clock.nowMs()).coerceAtLeast(0) + 1
        clipExpiryJob =
            scope.launch {
                delay(waitMs)
                clipExpiryJob = null
                purgeClips(history(), clock.nowMs())
                publish(CLIPS)
            }
    }

    private fun readUserWords(): List<UserWord> =
        userWordsDb.readableDatabase
            .rawQuery("SELECT latin, ethiopic FROM am_user_word ORDER BY created_at DESC, id DESC", null)
            .use { c ->
                val out = ArrayList<UserWord>(c.count)
                while (c.moveToNext()) out.add(UserWord(c.getString(0), c.getString(1)))
                Collections.unmodifiableList(out)
            }

    private fun readPicks(): Map<String, String> =
        history().rawQuery("SELECT latin, ethiopic FROM am_pick", null).use { c ->
            val out = HashMap<String, String>(c.count * 2)
            while (c.moveToNext()) out[c.getString(0)] = c.getString(1)
            Collections.unmodifiableMap(out)
        }

    private fun readUnigrams(): Map<String, Int> =
        history().rawQuery("SELECT word, count FROM en_word", null).use { c ->
            val out = HashMap<String, Int>(c.count * 2)
            while (c.moveToNext()) out[c.getString(0)] = c.getInt(1)
            Collections.unmodifiableMap(out)
        }

    private fun readBigrams(): Map<String, Map<String, Int>> =
        history().rawQuery("SELECT prev, next, count FROM en_bigram", null).use { c ->
            val out = HashMap<String, HashMap<String, Int>>()
            while (c.moveToNext()) out.getOrPut(c.getString(0)) { HashMap() }[c.getString(1)] = c.getInt(2)
            Collections.unmodifiableMap(out.mapValues { Collections.unmodifiableMap(it.value) })
        }

    private fun readClips(now: Long): List<ClipItem> =
        history()
            .rawQuery(
                "SELECT id, text, pinned, created_at FROM clip_item WHERE pinned = 1 OR created_at >= ? " +
                    "ORDER BY pinned DESC, created_at DESC, id DESC",
                arrayOf((now - limits.clipTtlMs).toString()),
            ).use { c ->
                val out = ArrayList<ClipItem>(c.count)
                while (c.moveToNext()) {
                    out.add(ClipItem(c.getLong(0), c.getString(1), c.getInt(2) != 0, c.getLong(3)))
                }
                Collections.unmodifiableList(out)
            }

    private fun readEmoji(): List<String> =
        history()
            .rawQuery(
                "SELECT emoji FROM emoji_recent ORDER BY last_used DESC, rowid DESC LIMIT ${limits.emojiRecents}",
                null,
            ).use { c ->
                val out = ArrayList<String>(c.count)
                while (c.moveToNext()) out.add(c.getString(0))
                Collections.unmodifiableList(out)
            }

    // ---- SQL helpers ----

    private fun history(): SQLiteDatabase = historyDb.writableDatabase

    /** Increments `count` for the row matching [where], inserting it with count 1 if absent. */
    private inline fun bump(
        db: SQLiteDatabase,
        table: String,
        where: String,
        args: Array<String>,
        now: Long,
        keyValues: ContentValues.() -> Unit,
    ) {
        val updated =
            db.compileStatement("UPDATE $table SET count = count + 1, last_used = ? WHERE $where").use { st ->
                st.bindLong(1, now)
                args.forEachIndexed { i, arg -> st.bindString(i + 2, arg) }
                st.executeUpdateDelete()
            }
        if (updated == 0) {
            val cv = ContentValues(4)
            cv.keyValues()
            cv.put("count", 1)
            cv.put("last_used", now)
            db.insertOrThrow(table, null, cv)
        }
    }

    /** Deletes the least recently used rows of [table] beyond [cap]. */
    private fun evictOldest(
        db: SQLiteDatabase,
        table: String,
        cap: Int,
    ) {
        val excess = DatabaseUtils.queryNumEntries(db, table) - cap
        if (excess > 0) {
            db.execSQL(
                "DELETE FROM $table WHERE rowid IN " +
                    "(SELECT rowid FROM $table ORDER BY last_used ASC, rowid ASC LIMIT $excess)",
            )
        }
    }

    private inline fun SQLiteDatabase.inTransaction(block: () -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    internal companion object {
        private const val TAG = "HoheData"
        private const val MAX_EMOJI_CHARS = 32

        private const val USER_WORDS = 1
        private const val PICKS = 2
        private const val UNIGRAMS = 4
        private const val BIGRAMS = 8
        private const val CLIPS = 16
        private const val EMOJI = 32
        private const val ALL = 63

        fun create(
            context: Context,
            clock: Clock = WallClock,
        ): HohePersonalDataStore {
            val app = context.applicationContext
            return HohePersonalDataStore(UserWordsDb(app), HistoryDb(app), clock)
        }
    }
}

internal object WallClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
