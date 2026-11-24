package app.hohe.keyboard.tools.dictbuilder.english

import org.junit.Assert.assertEquals
import org.junit.Test

class LexiconBuilderTest {
    private val sample =
        """
        dictionary=main:en,locale=en,description=English,date=1414726273,version=54
         word=the,f=222,flags=,originalFreq=222
         word=to,f=215,flags=,originalFreq=208
         word=sex,f=135,flags=,originalFreq=135,possibly_offensive=true
         word=rape,f=115,flags=offensive,originalFreq=115
         word=acomodate,f=0,not_a_word=true
          shortcut=accommodate,f=whitelist
         word=rare,f=0,flags=,originalFreq=3
         word=I'll,f=150,flags=,originalFreq=150
         word=café,f=103,flags=,originalFreq=103
        """.trimIndent().lines()

    // trimIndent keeps the one-space indent of word lines, as in the AOSP file.
    private fun lines() = sample.asSequence()

    @Test
    fun parseSkipsOffensiveNotAWordAndZeroFrequency() {
        val p = LexiconBuilder.parse(lines())
        assertEquals("54", p.sourceVersion)
        assertEquals(listOf("the", "to", "I'll", "café"), p.entries.map { it.word })
        assertEquals(2, p.skippedOffensive)
        assertEquals(1, p.skippedNotAWord)
        assertEquals(1, p.skippedZero)
    }

    @Test
    fun selectKeepsTopByFrequencyDeterministically() {
        val sel = LexiconBuilder.select(LexiconBuilder.parse(lines()).entries, 3)
        assertEquals(listOf("the", "to", "I'll"), sel.map { it.word })
    }
}
