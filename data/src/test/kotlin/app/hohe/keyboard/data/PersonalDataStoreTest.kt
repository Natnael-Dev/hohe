package app.hohe.keyboard.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hohe.keyboard.core.engine.UserWord
import app.hohe.keyboard.core.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class TestClock(
    var now: Long = 1_000_000L,
) : Clock {
    override fun nowMs(): Long = now
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PersonalDataStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = TestClock()
    private val limits =
        PersonalDataLimits(
            amharicPicks = 3,
            englishUnigrams = 3,
            englishBigrams = 2,
            unpinnedClips = 3,
            emojiRecents = 3,
        )
    private val open = mutableListOf<HohePersonalDataStore>()

    @Before
    fun setUp() {
        context.deleteDatabase(UserWordsDb.NAME)
        context.deleteDatabase(HistoryDb.NAME)
    }

    @After
    fun tearDown() {
        open.forEach { it.close() }
    }

    private fun newStore(l: PersonalDataLimits = limits): HohePersonalDataStore {
        open.forEach { it.close() }
        open.clear()
        return HohePersonalDataStore(UserWordsDb(context), HistoryDb(context), clock, l)
            .also { open += it }
            .also { runBlocking { it.awaitIdle() } }
    }

    private fun HohePersonalDataStore.idle() = runBlocking { awaitIdle() }

    private fun tick(ms: Long = 10) {
        clock.now += ms
    }

    @Test
    fun usesTwoDatabaseFilesSoBackupCanSplitThem() {
        val store = newStore()
        runBlocking { store.addUserWord(UserWord("selam", "ሰላም")) }
        store.recordAmharicPick("selam", "ሰላም")
        store.idle()
        assertTrue(context.getDatabasePath("userwords.db").exists())
        assertTrue(context.getDatabasePath("history.db").exists())
    }

    @Test
    fun userWordsRoundTripDeDuplicateAndSurviveReopen() =
        runBlocking {
            val store = newStore()
            store.addUserWord(UserWord(" Selam ", "ሰላም"))
            tick()
            store.addUserWord(UserWord("selam", "ሰላም"))
            store.addUserWord(UserWord("tena", "ጤና"))
            store.addUserWord(UserWord("", "ባዶ"))
            val expected = listOf(UserWord("tena", "ጤና"), UserWord("selam", "ሰላም"))
            assertEquals(expected, store.userWords.value)
            assertEquals(expected, store.snapshot().userWords)

            val reopened = newStore()
            assertEquals(expected, reopened.userWords.value)
            reopened.deleteUserWord(UserWord("tena", "ጤና"))
            assertEquals(listOf(UserWord("selam", "ሰላም")), reopened.snapshot().userWords)
        }

    @Test
    fun amharicPickKeepsLastPickPerLowercaseKey() {
        val store = newStore()
        store.recordAmharicPick("Selam", "ሰላም")
        tick()
        store.recordAmharicPick("selam", "ሠላም")
        store.recordAmharicPick("  ", "ሰ")
        store.idle()
        assertEquals(mapOf("selam" to "ሠላም"), store.snapshot().amharicPicks)
        assertEquals(mapOf("selam" to "ሠላም"), newStore().snapshot().amharicPicks)
    }

    @Test
    fun amharicPicksAreCappedLeastRecentlyUsedFirst() {
        val store = newStore()
        for (k in listOf("a", "b", "c")) {
            store.recordAmharicPick(k, "x$k")
            tick()
        }
        store.recordAmharicPick("a", "xa")
        tick()
        store.recordAmharicPick("d", "xd")
        store.idle()
        assertEquals(setOf("a", "c", "d"), store.snapshot().amharicPicks.keys)
    }

    @Test
    fun englishCommitsLowercaseCountAndBuildBigrams() {
        val store = newStore()
        store.recordEnglishCommit("Hello", null)
        tick()
        store.recordEnglishCommit("World", "Hello")
        tick()
        store.recordEnglishCommit("world", "hello")
        store.recordEnglishCommit("123", "hello")
        store.recordEnglishCommit("  ", null)
        store.recordEnglishCommit("!!", null)
        store.idle()
        val snap = store.snapshot()
        assertEquals(mapOf("hello" to 1, "world" to 2), snap.englishUnigrams)
        assertEquals(mapOf("hello" to mapOf("world" to 2)), snap.englishBigrams)
    }

    @Test
    fun englishPreviousWithoutLettersRecordsNoBigram() {
        val store = newStore()
        store.recordEnglishCommit("go", "42")
        store.idle()
        assertEquals(mapOf("go" to 1), store.snapshot().englishUnigrams)
        assertTrue(store.snapshot().englishBigrams.isEmpty())
    }

    @Test
    fun englishUnigramsAndBigramsAreCapped() {
        val store = newStore()
        var prev: String? = null
        for (w in listOf("one", "two", "three", "four")) {
            store.recordEnglishCommit(w, prev)
            prev = w
            tick()
        }
        store.idle()
        val snap = store.snapshot()
        assertEquals(setOf("two", "three", "four"), snap.englishUnigrams.keys)
        assertEquals(mapOf("two" to mapOf("three" to 1), "three" to mapOf("four" to 1)), snap.englishBigrams)
    }

    @Test
    fun sensitiveAndBlankClipsAreIgnored() {
        val store = newStore()
        store.addClip("hunter2", sensitive = true)
        store.addClip("   ", sensitive = false)
        store.addClip("x".repeat(10_001), sensitive = false)
        store.idle()
        assertTrue(store.clipboard.value.isEmpty())
        assertEquals(0, runBlocking { store.counts() }[DataCategory.CLIPBOARD])
    }

    @Test
    fun clipsDeDuplicateAndReAddMovesToTop() {
        val store = newStore()
        store.addClip("alpha", false)
        store.addClip("beta", false)
        store.addClip("alpha", false)
        store.idle()
        assertEquals(listOf("alpha", "beta"), store.clipboard.value.map { it.text })
    }

    @Test
    fun unpinnedClipsExpireAfter24HoursAndPinnedOnesStay() =
        runBlocking {
            val store = newStore()
            store.addClip("keep", false)
            store.addClip("old", false)
            store.idle()
            val keep = store.clipboard.value.first { it.text == "keep" }
            store.setPinned(keep.id, true)
            assertEquals(listOf("keep", "old"), store.clipboard.value.map { it.text })

            tick(24L * 60 * 60 * 1000 + 1)
            store.addClip("new", false)
            store.idle()
            assertEquals(listOf("keep", "new"), store.clipboard.value.map { it.text })
            assertTrue(
                store.clipboard.value
                    .first()
                    .pinned,
            )

            // Unpinning restarts the 24 h life, so the item is kept as a recent clip.
            store.setPinned(keep.id, false)
            assertEquals(
                setOf("keep", "new"),
                store.clipboard.value
                    .map { it.text }
                    .toSet(),
            )
            assertFalse(store.clipboard.value.any { it.pinned })
        }

    @Test
    fun rePinnedClipKeepsPinWhenCopiedAgain() =
        runBlocking {
            val store = newStore()
            store.addClip("a", false)
            store.idle()
            store.setPinned(
                store.clipboard.value
                    .single()
                    .id,
                true,
            )
            store.addClip("a", false)
            store.idle()
            assertTrue(
                store.clipboard.value
                    .single()
                    .pinned,
            )
        }

    @Test
    fun unpinnedClipsAreCappedPinnedAreNot() =
        runBlocking {
            val store = newStore()
            store.addClip("p", false)
            store.idle()
            store.setPinned(
                store.clipboard.value
                    .single()
                    .id,
                true,
            )
            for (t in listOf("1", "2", "3", "4")) {
                tick()
                store.addClip(t, false)
            }
            store.idle()
            assertEquals(listOf("p", "4", "3", "2"), store.clipboard.value.map { it.text })
            store.deleteClip(
                store.clipboard.value
                    .first { it.text == "3" }
                    .id,
            )
            assertEquals(listOf("p", "4", "2"), store.clipboard.value.map { it.text })
        }

    @Test
    fun emojiRecentsMostRecentFirstCappedAndDeDuplicated() {
        val store = newStore()
        for (e in listOf("😀", "😂", "😀", "👍", "🙏")) store.recordEmoji(e)
        store.idle()
        assertEquals(listOf("🙏", "👍", "😀"), store.emojiRecents.value)
        assertEquals(listOf("🙏", "👍", "😀"), newStore().emojiRecents.value)
    }

    @Test
    fun countsAndClearEveryCategory() =
        runBlocking {
            val store = newStore()
            store.addUserWord(UserWord("selam", "ሰላም"))
            store.recordAmharicPick("tena", "ጤና")
            store.recordEnglishCommit("hi", null)
            store.recordEnglishCommit("there", "hi")
            store.addClip("clip", false)
            store.recordEmoji("😀")
            store.awaitIdle()
            assertEquals(
                mapOf(
                    DataCategory.AMHARIC_PICKS to 1,
                    DataCategory.USER_WORDS to 1,
                    DataCategory.ENGLISH_HISTORY to 2,
                    DataCategory.CLIPBOARD to 1,
                    DataCategory.EMOJI_RECENTS to 1,
                ),
                store.counts(),
            )

            for (category in DataCategory.entries) store.clear(category)

            assertTrue(store.counts().values.all { it == 0 })
            val snap = store.snapshot()
            assertTrue(snap.userWords.isEmpty())
            assertTrue(snap.amharicPicks.isEmpty())
            assertTrue(snap.englishUnigrams.isEmpty())
            assertTrue(snap.englishBigrams.isEmpty())
            assertTrue(store.userWords.value.isEmpty())
            assertTrue(store.clipboard.value.isEmpty())
            assertTrue(store.emojiRecents.value.isEmpty())
        }

    @Test
    fun clearOneCategoryLeavesOthers() =
        runBlocking {
            val store = newStore()
            store.addUserWord(UserWord("selam", "ሰላም"))
            store.recordAmharicPick("tena", "ጤና")
            store.awaitIdle()
            store.clear(DataCategory.AMHARIC_PICKS)
            assertTrue(store.snapshot().amharicPicks.isEmpty())
            assertEquals(listOf(UserWord("selam", "ሰላም")), store.snapshot().userWords)
        }

    @Test
    fun snapshotIsStableBetweenWritesAndReloadedOnOpen() {
        val store = newStore()
        store.recordEnglishCommit("hello", null)
        store.idle()
        val first = store.snapshot()
        assertSame(first, store.snapshot())
        assertEquals(mapOf("hello" to 1), newStore().snapshot().englishUnigrams)
    }

    @Test
    fun fireAndForgetWritesDoNotRunOnCallerThread() {
        val caller = Thread.currentThread()
        var writer: Thread? = null
        val probe =
            object : Clock {
                override fun nowMs(): Long {
                    writer = Thread.currentThread()
                    return 1L
                }
            }
        val store =
            HohePersonalDataStore(UserWordsDb(context), HistoryDb(context), probe, limits).also { open += it }
        store.recordEmoji("😀")
        store.idle()
        assertTrue(writer != null && writer !== caller)
    }
}
