package app.hohe.keyboard.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Framework SQLite schemas for personal data (storage-architecture §3). Two files so backup rules
 * can include one and leave out the other:
 * - [UserWordsDb] `userwords.db`: user-authored Amharic words, backed up (D-025).
 * - [HistoryDb] `history.db`: picks, English history, clipboard, emoji recents; never backed up.
 *
 * Every schema change bumps the version and adds an `onUpgrade` step plus a migration test
 * (no persisted-format change without a migration).
 */
internal class UserWordsDb(
    context: Context,
    name: String? = NAME,
) : SQLiteOpenHelper(context, name, null, VERSION) {
    init {
        // Rollback journal (no WAL): the backed-up file is complete on its own between writes.
        setWriteAheadLoggingEnabled(false)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE am_user_word (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "latin TEXT NOT NULL, " +
                "ethiopic TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "UNIQUE(latin, ethiopic))",
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        // v1 is the first schema. Add `if (oldVersion < 2) { ... }` steps here; never drop data.
    }

    companion object {
        const val NAME = "userwords.db"
        const val VERSION = 1
    }
}

internal class HistoryDb(
    context: Context,
    name: String? = NAME,
) : SQLiteOpenHelper(context, name, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE am_pick (latin TEXT PRIMARY KEY NOT NULL, ethiopic TEXT NOT NULL, " +
                "last_used INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX am_pick_last_used ON am_pick(last_used)")
        db.execSQL(
            "CREATE TABLE en_word (word TEXT PRIMARY KEY NOT NULL, count INTEGER NOT NULL, " +
                "last_used INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX en_word_last_used ON en_word(last_used)")
        db.execSQL(
            "CREATE TABLE en_bigram (prev TEXT NOT NULL, next TEXT NOT NULL, " +
                "count INTEGER NOT NULL, last_used INTEGER NOT NULL, PRIMARY KEY(prev, next))",
        )
        db.execSQL("CREATE INDEX en_bigram_last_used ON en_bigram(last_used)")
        db.execSQL(
            "CREATE TABLE clip_item (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT NOT NULL UNIQUE, pinned INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE emoji_recent (emoji TEXT PRIMARY KEY NOT NULL, " +
                "last_used INTEGER NOT NULL)",
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        // v1 is the first schema. Add `if (oldVersion < 2) { ... }` steps here.
    }

    companion object {
        const val NAME = "history.db"
        const val VERSION = 1
    }
}
