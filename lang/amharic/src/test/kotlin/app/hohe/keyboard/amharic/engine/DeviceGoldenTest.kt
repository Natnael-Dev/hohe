package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Device tests A1-A7 (keyboard-device-test-results.md §3) against a small in-test dictionary
 * whose frequencies are synthetic. This checks the ranking logic, not corpus coverage; see
 * [CommittedAssetTest] for the same cases on the shipped dictionary.
 */
class DeviceGoldenTest {
    private val engine = HoheAmharicEngine(TestDicts.dict())

    private fun bar(
        input: String,
        user: UserDataSnapshot = UserDataSnapshot.EMPTY,
    ) = engine.suggest(SuggestRequest(1, input), user).candidates.map { it.text }

    @Test
    fun a1SelShowsSelamFirst() {
        val b = bar("sel")
        assertEquals("ሰላም", b[0])
        assertTrue(b.containsAll(listOf("ሰል", "ሴል")))
    }

    @Test
    fun a2SelamWithoutHistory() {
        val b = bar("selam")
        assertEquals("ሰላም", b[0])
        assertTrue(b.contains("ሠላም"))
    }

    @Test
    fun a2SelamWithHistoryPromotesPick() {
        assertEquals("ሠላም", bar("selam", UserDataSnapshot(amharicPicks = mapOf("selam" to "ሠላም")))[0])
    }

    @Test
    fun a3Melkam() {
        val b = bar("melkam")
        assertEquals("መልካም", b[0])
        assertEquals("መልካሙን", b[1])
    }

    @Test
    fun a4EndetFromDictionary() {
        assertEquals("እንዴት", bar("endet")[0])
    }

    @Test
    fun a5EndetAfterPick() {
        assertEquals("አንዴት", bar("endet", UserDataSnapshot(amharicPicks = mapOf("endet" to "አንዴት")))[0])
    }

    @Test
    fun a6DexterFallsBackToTransliteration() {
        val r = engine.suggest(SuggestRequest(1, "dexter"), UserDataSnapshot.EMPTY).candidates
        assertEquals("ደሽተር", r[0].text)
        assertEquals(Source.FALLBACK_TRANSLIT, r[0].source)
        assertTrue(r.map { it.text }.contains("ደሽጠር"))
    }

    @Test
    fun a7Abebe() {
        val b = bar("abebe")
        assertEquals("አበበ", b[0])
        assertEquals("አበበች", b[1])
    }

    @Test
    fun amesegnalehuReachesDictionary() {
        assertEquals("አመሰግናለሁ", bar("amesegnalehu")[0])
        assertEquals("አመሰግናለሁ", bar("amesegn")[0])
    }
}
