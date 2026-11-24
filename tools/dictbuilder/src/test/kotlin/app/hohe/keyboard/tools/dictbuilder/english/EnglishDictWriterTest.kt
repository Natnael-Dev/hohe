package app.hohe.keyboard.tools.dictbuilder.english

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishDictWriterTest {
    private val words =
        listOf(
            WordFreq("the", 222),
            WordFreq("then", 150),
            WordFreq("there", 160),
            WordFreq("The", 10),
            WordFreq("café", 103),
            WordFreq("don't", 185),
            WordFreq("a", 200),
        )

    @Test
    fun roundTripKeepsEveryWordAndHeader() {
        val bytes = EnglishDictWriter.write(words, "test-1", "Apache-2.0")
        val r = EnglishDictWriter.Reader(bytes)
        assertEquals("test-1", r.dataVersion)
        assertEquals("Apache-2.0", r.licenceId)
        assertEquals(words.size, r.wordCount)
        assertEquals(r.storedCrc, r.computedCrc())
        assertEquals(words.sortedBy { it.word }, r.entries().sortedBy { it.word })
        for (w in words) assertEquals(w.word, w.freq, r.frequency(w.word))
        assertEquals(0, r.frequency("th"))
        assertEquals(0, r.frequency("thex"))
    }

    @Test
    fun headerIsAlignedAndSizeMatchesNodes() {
        val bytes = EnglishDictWriter.write(words, "v", "Apache-2.0")
        val r = EnglishDictWriter.Reader(bytes)
        assertEquals(0, r.headerLen % 4)
        val chars = words.flatMap { it.word.toList() }.toSet().size
        assertEquals(r.headerLen + chars * 2 + r.nodeCount * EnglishDictWriter.NODE_SIZE, bytes.size)
    }

    @Test
    fun outputIsDeterministic() {
        val a = EnglishDictWriter.write(words, "v", "Apache-2.0")
        val b = EnglishDictWriter.write(words.reversed(), "v", "Apache-2.0")
        assertArrayEquals(a, b)
    }

    @Test
    fun corruptionChangesChecksum() {
        val bytes = EnglishDictWriter.write(words, "v", "Apache-2.0")
        bytes[bytes.size - 3] = (bytes[bytes.size - 3] + 1).toByte()
        val r = EnglishDictWriter.Reader(bytes)
        assertTrue(r.storedCrc != r.computedCrc())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeFrequency() {
        EnglishDictWriter.write(listOf(WordFreq("x", 0)), "v", "Apache-2.0")
    }
}
