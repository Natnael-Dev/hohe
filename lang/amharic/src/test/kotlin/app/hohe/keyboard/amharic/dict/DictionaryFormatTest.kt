package app.hohe.keyboard.amharic.dict

import app.hohe.keyboard.amharic.engine.TestDicts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DictionaryFormatTest {
    @Test
    fun roundTripKeepsEveryWordFrequencyAndMetadata() {
        val words = TestDicts.DEVICE_WORDS + mapOf("ቋንቋ" to 77L, "ኧረ" to 5L)
        val d = TestDicts.dict(words)
        assertEquals(words.size, d.wordCount)
        assertEquals("test-1", d.dataVersion)
        assertEquals("CC-BY-SA-4.0", d.licenceId)
        assertEquals("test data", d.attribution)
        val back = (0 until d.wordCount).associate { d.word(it) to d.freqQ(it) }
        for ((w, f) in words) {
            assertEquals(w, AmharicDictionaryFormat.quantise(f), back[w])
        }
    }

    @Test
    fun bucketsAreSortedAndHoldTheirSkeleton() {
        val d = TestDicts.dict()
        for (s in 0 until d.skeletonCount) {
            if (s > 0) assertTrue(d.skeleton(s - 1) < d.skeleton(s))
            for (i in d.firstWord(s) until d.firstWord(s + 1)) {
                assertEquals(d.skeleton(s), Fidel.skeletonOf(d.word(i)))
                if (i > d.firstWord(s)) assertTrue(d.freqQ(i - 1) >= d.freqQ(i))
            }
        }
    }

    @Test
    fun exactAndPrefixRanges() {
        val d = TestDicts.dict()
        val exact = d.exactRange("slm").map { d.word(it) }.toSet()
        assertEquals(setOf("ሰላም", "ሠላም"), exact)
        val prefix = d.prefixRange("ml").map { d.word(it) }.toSet()
        assertEquals(setOf("መልካም", "መልካሙን"), prefix)
        assertTrue(d.exactRange("zzz").isEmpty())
        assertTrue(d.prefixRange("x").isEmpty())
        assertEquals(d.wordCount, d.prefixRange("").count())
    }

    @Test
    fun unmappedWordsAreDropped() {
        val d = TestDicts.dict(mapOf("ሰላም" to 3L, "ቐለ" to 9L, "abc" to 4L))
        assertEquals(1, d.wordCount)
    }

    @Test
    fun emptyDictionaryIsValid() {
        val d = TestDicts.dict(emptyMap())
        assertEquals(0, d.wordCount)
        assertTrue(d.prefixRange("s").isEmpty())
    }

    private fun mutate(f: (ByteBuffer) -> Unit): ByteBuffer {
        val b = ByteBuffer.wrap(TestDicts.bytes().copyOf()).order(ByteOrder.LITTLE_ENDIAN)
        f(b)
        return b
    }

    private fun assertRejected(
        why: String,
        buf: ByteBuffer,
    ) {
        try {
            AmharicDictionary.parse(buf)
            fail("accepted: $why")
        } catch (e: DictionaryFormatException) {
            // expected
        }
        assertNull(why, AmharicDictionary.load(buf))
    }

    @Test
    fun headerValidationRejectsBadFiles() {
        assertRejected("bad magic", mutate { it.putInt(0, 0x12345678) })
        assertRejected("future version", mutate { it.putShort(4, 2) })
        assertRejected("flipped payload byte", mutate { it.put(it.limit() - 1, (it.get(it.limit() - 1) + 1).toByte()) })
        assertRejected("bad crc", mutate { it.putInt(AmharicDictionaryFormat.CRC_OFFSET, 0) })
        val full = TestDicts.bytes()
        assertRejected("truncated", ByteBuffer.wrap(full.copyOf(full.size - 3)))
        assertRejected("header only", ByteBuffer.wrap(full.copyOf(20)))
        assertRejected("empty", ByteBuffer.allocate(0))
    }

    @Test
    fun loadFailsSoftForNullAndBrokenStreams() {
        assertNull(AmharicDictionary.load(null as ByteBuffer?))
        assertNull(AmharicDictionary.load(null as InputStream?))
        assertNull(AmharicDictionary.load(ByteArrayInputStream(ByteArray(100))))
        val broken =
            object : InputStream() {
                override fun read(): Int = throw IOException("boom")
            }
        assertNull(AmharicDictionary.load(broken))
        assertNotNull(AmharicDictionary.load(ByteArrayInputStream(TestDicts.bytes())))
    }

    @Test
    fun directBufferWorks() {
        val bytes = TestDicts.bytes()
        val direct = ByteBuffer.allocateDirect(bytes.size)
        direct.put(bytes)
        direct.flip()
        val d = AmharicDictionary.parse(direct)
        assertEquals(TestDicts.DEVICE_WORDS.size, d.wordCount)
    }
}
