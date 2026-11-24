package app.hohe.keyboard.english

import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.field.FieldClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class HoheEnglishEngineTest {
    private val engine = TestData.engine

    private fun texts(
        input: String,
        user: UserDataSnapshot = UserDataSnapshot.EMPTY,
        personal: Boolean = true,
        field: FieldClass = FieldClass.NORMAL,
        proximity: Boolean = false,
    ): List<String> =
        engine
            .suggest(
                SuggestRequest(
                    id = 1,
                    input = input,
                    proximity = if (proximity) QwertyProximity.forWord(input) else null,
                    usePersonalData = personal,
                    field = field,
                ),
                user,
            ).candidates
            .map { it.text }

    @Test
    fun dictionaryHeaderAndContent() {
        val d = TestData.dictionary
        assertEquals("en-aosp54.1", d.dataVersion)
        assertEquals("Apache-2.0", d.licenceId)
        assertEquals(150_000, d.wordCount)
        assertEquals(222, d.frequency("the"))
        assertTrue("hello" in d)
        assertFalse("possibly offensive entries are skipped", "sex" in d)
        assertFalse("not_a_word entries are skipped", "acomodate" in d)
        assertEquals("en-aosp54.1", engine.dataVersion)
    }

    @Test
    fun heloSuggestsHelloFirst() {
        assertEquals("hello", texts("helo").first())
    }

    @Test
    fun tehSuggestsTheFirst() {
        assertEquals("the", texts("teh").first())
    }

    @Test
    fun typedWordIsNeverIncluded() {
        for (w in listOf("helo", "teh", "the", "hello", "tomor")) assertFalse(w, w in texts(w))
    }

    @Test
    fun proximitySubstitutionRanksHelloForHwllo() {
        assertEquals("hello", texts("hwllo", proximity = true).first())
        // Without explicit proximity the built-in QWERTY map gives the same result.
        assertEquals("hello", texts("hwllo").first())
    }

    @Test
    fun nearbyKeyBeatsFarKey() {
        // 'w' is next to 'e' on QWERTY, 'p' is not: "hwllo" is closer to hello than "hpllo".
        val near = engine.suggest(SuggestRequest(1, "hwllo"), UserDataSnapshot.EMPTY).candidates.first { it.text == "hello" }
        val far = engine.suggest(SuggestRequest(1, "hpllo"), UserDataSnapshot.EMPTY).candidates.firstOrNull { it.text == "hello" }
        assertTrue(far == null || near.score > far.score)
    }

    @Test
    fun completesPrefixes() {
        assertEquals("tomorrow", texts("tomor").first())
        assertTrue(texts("t").containsAll(listOf("the", "to")))
    }

    @Test
    fun capitalisationFollowsTypedWord() {
        assertEquals("Hello", texts("Helo").first())
        assertEquals("THE", texts("TEH").first())
        assertFalse("The" in texts("The"))
    }

    @Test
    fun caseOnlyVariantsHiddenWhenTypedWordIsKnown() {
        assertFalse("US" in texts("us"))
        assertTrue("I" in texts("i"))
    }

    @Test
    fun learnedWordIsSuggestedOnlyWithPersonalData() {
        val user = UserDataSnapshot(englishUnigrams = mapOf("hohe" to 3))
        val withUser = engine.suggest(SuggestRequest(1, "hoh"), user).candidates
        assertTrue(withUser.any { it.text == "hohe" && it.source == Source.USER_WORD })
        assertFalse("hohe" in texts("hoh", user, personal = false))
        assertFalse("hohe" in texts("hoh", user, field = FieldClass.INCOGNITO))
        // Typos of a learned word are corrected too.
        assertTrue("hohe" in texts("hoeh", user))
    }

    @Test
    fun userHistoryBoostsDictionaryWords() {
        val base = texts("hel")
        val user = UserDataSnapshot(englishUnigrams = mapOf("helm" to 50))
        val boosted = texts("hel", user)
        assertTrue(boosted.indexOf("helm") < base.indexOf("helm").let { if (it < 0) Int.MAX_VALUE else it })
        assertEquals("helm", boosted.first())
    }

    @Test
    fun blockedFieldsGetNothing() {
        assertTrue(texts("helo", field = FieldClass.PASSWORD).isEmpty())
        assertTrue(texts("helo", field = FieldClass.NO_SUGGEST).isEmpty())
        assertTrue(texts("").isEmpty())
    }

    @Test
    fun failsSoftWithoutDictionary() {
        val none = HoheEnglishEngine.load(null as ByteBuffer?)
        assertEquals(HoheEnglishEngine.NO_DATA, none.dataVersion)
        assertTrue(none.suggest(SuggestRequest(1, "helo"), UserDataSnapshot.EMPTY).candidates.isEmpty())

        val corrupt = TestData.enBytes.copyOf()
        corrupt[corrupt.size - 5] = (corrupt[corrupt.size - 5] + 1).toByte()
        val bad = HoheEnglishEngine.load(ByteBuffer.wrap(corrupt))
        assertEquals(HoheEnglishEngine.NO_DATA, bad.dataVersion)
        assertEquals("none", HoheEnglishEngine.load(ByteArray(10).inputStream()).dataVersion)

        // Learned words still work without a dictionary.
        val user = UserDataSnapshot(englishUnigrams = mapOf("hohe" to 1))
        assertEquals(listOf("hohe"), bad.suggest(SuggestRequest(1, "hoh"), user).candidates.map { it.text })
    }

    @Test
    fun loadsFromStream() {
        val e = HoheEnglishEngine.load(TestData.enBytes.inputStream())
        assertEquals("en-aosp54.1", e.dataVersion)
    }

    @Test
    fun nextWordsFromBigrams() {
        val user =
            UserDataSnapshot(
                englishBigrams = mapOf("good" to mapOf("morning" to 5, "night" to 2, "luck" to 9)),
            )
        assertEquals(listOf("luck", "morning"), engine.nextWords("It was good ", user, 2).map { it.text })
        assertTrue(engine.nextWords("so Good ", user, 3).all { it.source == Source.NEXT_WORD })
        assertEquals(3, engine.nextWords("so Good ", user, 3).size)
        assertTrue(engine.nextWords("good. ", user, 3).isEmpty())
        assertTrue(engine.nextWords("bad ", user, 3).isEmpty())
        assertTrue(engine.nextWords("good ", UserDataSnapshot.EMPTY, 3).isEmpty())
    }
}
