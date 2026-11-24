package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.amharic.dict.Fidel
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the shipped `app/src/main/assets/dict/am_words.bin` (TASK-025 output). */
class CommittedAssetTest {
    private val dict = TestDicts.assetDict()

    @Test
    fun assetLoadsWithinBudget() {
        assertTrue("missing ${TestDicts.ASSET.absolutePath}", TestDicts.ASSET.isFile)
        assertNotNull("asset failed validation", dict)
        assertTrue("size ${TestDicts.ASSET.length()}", TestDicts.ASSET.length() <= 5L * 1024 * 1024)
        assertEquals("CC-BY-SA-4.0", dict!!.licenceId)
        assertTrue(dict.dataVersion.startsWith("am-"))
        assertTrue(dict.attribution.contains("Wikipedia") && dict.attribution.contains("Yimam"))
        assertTrue(dict.wordCount in 60_000..120_000)
    }

    @Test
    fun everyWordIsFiledUnderItsOwnSkeleton() {
        // guards against drift between Fidel.kt and the builder's SkeletonClasses copy
        val d = dict!!
        for (s in 0 until d.skeletonCount) {
            val skel = d.skeleton(s)
            if (s > 0) assertTrue(d.skeleton(s - 1) < skel)
            for (i in d.firstWord(s) until d.firstWord(s + 1)) {
                val w = d.word(i)
                assertEquals(w, skel, Fidel.skeletonOf(w))
            }
        }
    }

    private fun bar(input: String): List<String> {
        val e = HoheAmharicEngine(dict)
        return e.suggest(SuggestRequest(1, input), UserDataSnapshot.EMPTY).candidates.map { it.text }
    }

    /**
     * Device tests on the real corpus data (corpus-dependent). A2/A5 history and the exact
     * A7 slot-2 word (አበበች) depend on user picks or Desh's vocabulary, not on our corpus.
     */
    @Test
    fun deviceCasesOnShippedDictionary() {
        assertEquals("A1", "ሰላም", bar("sel")[0])
        assertEquals("A2 (no history)", "ሰላም", bar("selam")[0])
        assertTrue("A2 variant", bar("selam").contains("ሠላም"))
        assertEquals("A3", "መልካም", bar("melkam")[0])
        assertTrue("A3 completion", bar("melkam").contains("መልካሙን"))
        assertEquals("A4", "እንዴት", bar("endet")[0])
        assertTrue("A5 alternative present", bar("endet").contains("አንዴት"))
        assertEquals("A6", "ደሽተር", bar("dexter")[0])
        assertEquals("A7", "አበበ", bar("abebe")[0])
    }

    @Test
    fun commonChatWordsOnShippedDictionary() {
        val expected =
            mapOf(
                "amesegnalehu" to "አመሰግናለሁ",
                "new" to "ነው",
                "betam" to "በጣም",
                "ishi" to "እሺ",
                "gize" to "ጊዜ",
                "addis" to "አዲስ",
                "konjo" to "ቆንጆ",
            )
        for ((latin, word) in expected) assertEquals(latin, word, bar(latin)[0])
    }
}
