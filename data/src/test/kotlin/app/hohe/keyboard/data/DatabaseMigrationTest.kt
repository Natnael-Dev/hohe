package app.hohe.keyboard.data

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hohe.keyboard.core.engine.UserWord
import app.hohe.keyboard.core.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Builds each database from a frozen copy of its v1 schema, then opens it with the current helper.
 * When a schema version is added, keep these v1 scripts unchanged and add the new expectations:
 * the data written here must survive every upgrade.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.deleteDatabase(UserWordsDb.NAME)
        context.deleteDatabase(HistoryDb.NAME)
    }

    private fun createV1(
        name: String,
        statements: List<String>,
    ) {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            statements.forEach(db::execSQL)
            db.version = 1
        }
    }

    @Test
    fun userWordsV1OpensWithCurrentSchema() {
        createV1(
            UserWordsDb.NAME,
            listOf(
                "CREATE TABLE am_user_word (id INTEGER PRIMARY KEY AUTOINCREMENT, latin TEXT NOT NULL, " +
                    "ethiopic TEXT NOT NULL, created_at INTEGER NOT NULL, UNIQUE(latin, ethiopic))",
                "INSERT INTO am_user_word (latin, ethiopic, created_at) VALUES ('selam', 'ሰላም', 1)",
            ),
        )
        val helper = UserWordsDb(context)
        try {
            val db = helper.readableDatabase
            assertEquals(UserWordsDb.VERSION, db.version)
            assertEquals(1L, DatabaseUtils.queryNumEntries(db, "am_user_word"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun historyV1OpensWithCurrentSchema() {
        createV1(
            HistoryDb.NAME,
            listOf(
                "CREATE TABLE am_pick (latin TEXT PRIMARY KEY NOT NULL, ethiopic TEXT NOT NULL, last_used INTEGER NOT NULL)",
                "CREATE INDEX am_pick_last_used ON am_pick(last_used)",
                "CREATE TABLE en_word (word TEXT PRIMARY KEY NOT NULL, count INTEGER NOT NULL, last_used INTEGER NOT NULL)",
                "CREATE INDEX en_word_last_used ON en_word(last_used)",
                "CREATE TABLE en_bigram (prev TEXT NOT NULL, next TEXT NOT NULL, count INTEGER NOT NULL, " +
                    "last_used INTEGER NOT NULL, PRIMARY KEY(prev, next))",
                "CREATE INDEX en_bigram_last_used ON en_bigram(last_used)",
                "CREATE TABLE clip_item (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL UNIQUE, " +
                    "pinned INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)",
                "CREATE TABLE emoji_recent (emoji TEXT PRIMARY KEY NOT NULL, last_used INTEGER NOT NULL)",
                "INSERT INTO am_pick VALUES ('selam', 'ሰላም', 1)",
                "INSERT INTO en_word VALUES ('hello', 3, 1)",
                "INSERT INTO en_bigram VALUES ('hello', 'world', 2, 1)",
                "INSERT INTO emoji_recent VALUES ('😀', 1)",
            ),
        )
        val helper = HistoryDb(context)
        try {
            val db = helper.readableDatabase
            assertEquals(HistoryDb.VERSION, db.version)
            assertEquals(1L, DatabaseUtils.queryNumEntries(db, "en_bigram"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun storeReadsV1DataIntoSnapshot() {
        historyV1OpensWithCurrentSchema()
        userWordsV1OpensWithCurrentSchema()
        val clock =
            object : Clock {
                override fun nowMs(): Long = 2L
            }
        val store = HohePersonalDataStore(UserWordsDb(context), HistoryDb(context), clock)
        try {
            runBlocking { store.awaitIdle() }
            val snap = store.snapshot()
            assertEquals(listOf(UserWord("selam", "ሰላም")), snap.userWords)
            assertEquals(mapOf("selam" to "ሰላም"), snap.amharicPicks)
            assertEquals(mapOf("hello" to 3), snap.englishUnigrams)
            assertEquals(mapOf("hello" to mapOf("world" to 2)), snap.englishBigrams)
            assertEquals(listOf("😀"), store.emojiRecents.value)
        } finally {
            store.close()
        }
    }
}
