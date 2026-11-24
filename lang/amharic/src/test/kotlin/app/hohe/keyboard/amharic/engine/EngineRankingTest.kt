package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.amharic.converter.AmharicConverter
import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.engine.UserWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class EngineRankingTest {
    private val engine = HoheAmharicEngine(TestDicts.dict())

    private fun suggest(
        input: String,
        user: UserDataSnapshot = UserDataSnapshot.EMPTY,
        personal: Boolean = true,
        max: Int = 12,
        e: HoheAmharicEngine = engine,
    ) = e.suggest(SuggestRequest(id = 7, input = input, usePersonalData = personal, maxResults = max), user)

    private val user =
        UserDataSnapshot(
            userWords = listOf(UserWord("selam", "ሰላምታ"), UserWord("other", "ሌላ")),
            amharicPicks = mapOf("selam" to "ሠላም"),
        )

    @Test
    fun orderIsUserWordThenPickThenDictionaryThenFallback() {
        val r = suggest("selam", user)
        assertEquals(7L, r.id)
        assertEquals("selam", r.forInput)
        val c = r.candidates
        assertEquals("ሰላምታ", c[0].text)
        assertEquals(Source.USER_WORD, c[0].source)
        assertEquals("ሠላም", c[1].text)
        assertEquals(Source.USER_PICK, c[1].source)
        assertEquals("ሰላም", c[2].text)
        assertEquals(Source.DICTIONARY, c[2].source)
        val firstFallback = c.indexOfFirst { it.source == Source.FALLBACK_TRANSLIT }
        assertTrue(c.drop(firstFallback).all { it.source == Source.FALLBACK_TRANSLIT })
        assertEquals("candidates de-duplicated", c.size, c.map { it.text }.toSet().size)
        assertTrue("scores descend", c.zipWithNext().all { (a, b) -> a.score > b.score })
    }

    @Test
    fun personalDataIgnoredWhenDisabled() {
        val c = suggest("selam", user, personal = false).candidates
        assertTrue(c.none { it.source == Source.USER_WORD || it.source == Source.USER_PICK })
        assertEquals("ሰላም", c[0].text)
    }

    @Test
    fun pickThatIsAlsoADictionaryWordIsNotRepeated() {
        val c = suggest("selam", UserDataSnapshot(amharicPicks = mapOf("selam" to "ሰላም"))).candidates
        assertEquals("ሰላም", c[0].text)
        assertEquals(Source.USER_PICK, c[0].source)
        assertEquals(1, c.count { it.text == "ሰላም" })
    }

    @Test
    fun exactMatchBeatsComparableCompletion() {
        // አበበች is a completion of `abebe` but only ~4x rarer, so the exact አበበ stays first
        val c = suggest("abebe").candidates.map { it.text }
        assertEquals("አበበ", c[0])
        assertTrue(c.indexOf("አበበች") > 0)
    }

    @Test
    fun dominantCompletionLeads() {
        // D-018: ሰላም is 1000x more frequent than the exact ሰል
        val c = suggest("sel").candidates
        assertEquals("ሰላም", c[0].text)
        assertTrue(c.any { it.text == "ሰል" })
    }

    @Test
    fun unknownWordGetsTransliterationFirst() {
        val c = suggest("dexter").candidates
        assertEquals(AmharicConverter.convert("dexter"), c[0].text)
        assertEquals(Source.FALLBACK_TRANSLIT, c[0].source)
    }

    @Test
    fun neverEmptyForLetters() {
        for (s in listOf("a", "q", "zzzzzz", "xyzw", "e", "ng", "rrr")) {
            assertFalse(s, suggest(s).candidates.isEmpty())
        }
    }

    @Test
    fun emptyOrNonLetterInputGivesNothing() {
        assertTrue(suggest("").candidates.isEmpty())
        assertTrue(suggest("123").candidates.isEmpty())
        assertTrue(suggest("'").candidates.isEmpty())
    }

    @Test
    fun inputIsLowercased() {
        assertEquals(suggest("selam").candidates, suggest("Selam").candidates)
    }

    @Test
    fun maxResultsIsRespected() {
        assertEquals(1, suggest("sel", max = 1).candidates.size)
        assertEquals(3, suggest("sel", max = 3).candidates.size)
        assertTrue(suggest("sel", max = 0).candidates.isEmpty())
    }

    @Test
    fun missingDictionaryFailsSoftToTransliteration() {
        val none = HoheAmharicEngine(null)
        assertEquals("none", none.dataVersion)
        val c = suggest("selam", e = none).candidates
        assertEquals(AmharicConverter.convert("selam"), c[0].text)
        assertTrue(c.all { it.source == Source.FALLBACK_TRANSLIT })
        assertEquals("none", HoheAmharicEngine.fromBuffer(ByteBuffer.wrap(ByteArray(64))).dataVersion)
        assertEquals("none", HoheAmharicEngine.fromStream(null).dataVersion)
        assertEquals("test-1", HoheAmharicEngine.fromStream(TestDicts.bytes().inputStream()).dataVersion)
    }

    @Test
    fun userDataStillWorksWithoutDictionary() {
        val c = suggest("selam", user, e = HoheAmharicEngine(null)).candidates
        assertEquals("ሰላምታ", c[0].text)
        assertEquals("ሠላም", c[1].text)
    }

    @Test
    fun transliterateDelegatesToConverterAndIsThreadSafe() {
        assertEquals(AmharicConverter.convert("selam"), engine.transliterate("selam"))
        val inputs = listOf("selam", "melkam", "endet", "dexter", "abebe", "amesegnalehu")
        val expected = inputs.map { AmharicConverter.convert(it) }
        val errors =
            java.util.concurrent.atomic
                .AtomicInteger()
        val threads =
            (1..4).map {
                Thread {
                    repeat(2000) { k ->
                        val i = k % inputs.size
                        if (engine.transliterate(inputs[i]) != expected[i]) errors.incrementAndGet()
                    }
                }
            }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(0, errors.get())
    }

    @Test
    fun homophoneVariantsAreCapped() {
        val words = mapOf("ሀገር" to 900L, "ሐገር" to 50L, "ኀገር" to 40L, "ሃገር" to 30L)
        val e = HoheAmharicEngine(TestDicts.dict(words))
        val dictHits = suggest("hager", e = e).candidates.filter { it.source == Source.DICTIONARY }.map { it.text }
        // ሀ/ሐ/ኀ + ገር fold to one form: at most 2 shown; ሃገር (4th order) is a different form
        assertEquals(listOf("ሀገር", "ሐገር"), dictHits.filter { it != "ሃገር" })
        assertTrue(dictHits.contains("ሃገር"))
    }
}
