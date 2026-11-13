package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConverterEdgeCaseTest {
    // --- §5 Samples ---

    @Test
    fun sampleSelam() {
        assertEquals("ሰላም", AmharicConverter.convert("selam"))
    }

    @Test
    fun sampleNy() {
        // Drop dead CONS['ny'] entry; verify ny -> ንይ
        assertEquals("ንይ", AmharicConverter.convert("ny"))
    }

    @Test
    fun sampleUa() {
        assertEquals("ኡአ", AmharicConverter.convert("ua"))
    }

    @Test
    fun sampleEi() {
        assertEquals("እ", AmharicConverter.convert("ei"))
    }

    @Test
    fun sampleEine() {
        assertEquals("እነ", AmharicConverter.convert("eine"))
    }

    @Test
    fun sampleHwe() {
        assertEquals("ህወ", AmharicConverter.convert("hwe"))
    }

    @Test
    fun sampleKw() {
        assertEquals("ክው", AmharicConverter.convert("kw"))
    }

    @Test
    fun sampleMw() {
        assertEquals("ምው", AmharicConverter.convert("mw"))
    }

    @Test
    fun sampleWua() {
        assertEquals("ዋ", AmharicConverter.convert("wua"))
    }

    @Test
    fun sampleBwie() {
        assertEquals("ብዌ", AmharicConverter.convert("bwie"))
    }

    @Test
    fun sampleYua() {
        assertEquals("ዯ", AmharicConverter.convert("yua"))
    }

    @Test
    fun sampleAa() {
        assertEquals("አአ", AmharicConverter.convert("aa"))
    }

    @Test
    fun sampleKkk() {
        assertEquals("ክክክ", AmharicConverter.convert("kkk"))
    }

    @Test
    fun sampleTsaWithApostrophe() {
        assertEquals("ትሳ", AmharicConverter.convert("t'sa"))
    }

    @Test
    fun sampleSeatWithApostrophe() {
        assertEquals("ሰአት", AmharicConverter.convert("se'at"))
    }

    @Test
    fun sampleAshunWithApostrophe() {
        assertEquals("አስሁን", AmharicConverter.convert("as'hun"))
    }

    @Test
    fun sampleSeLaMMixedCase() {
        assertEquals("ሰላም", AmharicConverter.convert("SeLaM"))
    }

    @Test
    fun sampleEmptyString() {
        assertEquals("", AmharicConverter.convert(""))
        assertTrue(AmharicConverter.nBest("", 4).isEmpty())
    }

    // --- §7 Edge cases ---

    @Test
    fun edgeKwAlone() {
        assertEquals("ክው", AmharicConverter.convert("kw"))
    }

    @Test
    fun edgeGwAlone() {
        assertEquals("ግው", AmharicConverter.convert("gw"))
    }

    @Test
    fun edgeQwAlone() {
        assertEquals("ቅው", AmharicConverter.convert("qw"))
    }

    @Test
    fun edgeHweHwiNotLabialised() {
        assertEquals("ህወ", AmharicConverter.convert("hwe"))
        assertEquals("ህዊ", AmharicConverter.convert("hwi"))
    }

    @Test
    fun edgeYuaYwaInRange() {
        val yua = AmharicConverter.convert("yua")
        val ywa = AmharicConverter.convert("ywa")
        assertEquals("ዯ", yua)
        assertEquals("ዯ", ywa)
        assertEquals(0x12EF, yua.codePointAt(0))
        assertEquals(0x12EF, ywa.codePointAt(0))
    }

    @Test
    fun edgeOnlyApostrophes() {
        assertEquals("", AmharicConverter.convert("'''"))
        assertEquals("", AmharicConverter.convert("'"))
        assertTrue(AmharicConverter.nBest("'''", 4).isEmpty())
    }

    @Test
    fun edge1000CharInputLinearNoStackOverflow() {
        val longInput = "selam".repeat(200) // 1000 chars
        val t0 = System.currentTimeMillis()
        val result = AmharicConverter.convert(longInput)
        val elapsed = System.currentTimeMillis() - t0
        assertEquals("ሰላም".repeat(200), result)
        assertTrue("1000 char convert should complete quickly (< 500ms), took $elapsed ms", elapsed < 500)
    }

    @Test
    fun edgeKZeroOrNegativeReturnsEmpty() {
        assertTrue(AmharicConverter.nBest("selam", 0).isEmpty())
        assertTrue(AmharicConverter.nBest("selam", -1).isEmpty())
        assertTrue(AmharicConverter.nBest("selam", -10).isEmpty())
    }

    @Test
    fun edgeDigitsSpacesEthiopicSkipped() {
        assertEquals("ሰላም", AmharicConverter.convert("se123lam"))
        assertEquals("ሰላም", AmharicConverter.convert("se lam"))
        assertEquals("ሰላም", AmharicConverter.convert("seሰlam"))
        assertEquals("ሰላም", AmharicConverter.convert("se\t\nlam!@#"))
    }

    // --- Additional Edge Cases ---

    @Test
    fun edgeNyaSequence() {
        assertEquals("ንያ", AmharicConverter.convert("nya"))
    }

    @Test
    fun edgeScoredProperties() {
        val scored = AmharicConverter.Scored("ሰላም", 0.0)
        assertEquals("ሰላም", scored.text)
        assertEquals(0.0, scored.cost, 1e-6)
    }

    @Test
    fun edgeNBestDeduplication() {
        val nbest = AmharicConverter.nBest("selam", 10)
        val texts = nbest.map { it.text }
        assertEquals("nBest entries must be unique", texts.toSet().size, texts.size)
    }

    @Test
    fun edgeNBestCostsAscending() {
        val nbest = AmharicConverter.nBest("selam", 4)
        assertTrue("At least 1 candidate", nbest.isNotEmpty())
        assertEquals(0.0, nbest[0].cost, 1e-6)
        for (i in 1 until nbest.size) {
            assertTrue("Costs must be non-decreasing: ${nbest[i - 1]} vs ${nbest[i]}", nbest[i].cost >= nbest[i - 1].cost)
        }
    }
}
