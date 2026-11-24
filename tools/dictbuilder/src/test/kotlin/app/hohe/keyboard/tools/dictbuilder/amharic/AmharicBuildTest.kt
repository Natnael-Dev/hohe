package app.hohe.keyboard.tools.dictbuilder.amharic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

class AmharicBuildTest {
    private fun words(s: String): List<String> = buildList { EthiopicText.words(s) { add(it) } }

    @Test
    fun tokeniserSplitsOnEthiopicPunctuationDigitsAndAscii() {
        assertEquals(listOf("ሰላም", "እንዴት", "ነህ"), words("ሰላም፡ እንዴት ነህ?"))
        assertEquals(listOf("አዲስ", "አበባ"), words("አዲስ፡አበባ።"))
        assertEquals(listOf("በ", "ዓመት"), words("በ2015 ዓመት ፩"))
        assertEquals(listOf("ሰላም"), words("[[ሰላም]]"))
    }

    @Test
    fun tokeniserDropsRunsGluedToOtherScriptsAndUnmappedFidels() {
        assertEquals(emptyList<String>(), words("ሰላምabc abcሰላም"))
        assertEquals(emptyList<String>(), words("ቐለ"))
        assertEquals(listOf("ሰላም"), words("ሰ፟ላም"))
        assertEquals(emptyList<String>(), words("ሀ".repeat(21)))
    }

    @Test
    fun wikiExtractorKeepsOnlyArticleText() {
        val xml =
            """
            <page>
              <title>ሙከራ</title>
              <ns>0</ns>
              <revision><text bytes="10" xml:space="preserve">ሰላም ዓለም
            [[መደብ:ሀገር]]
            ሁለተኛ መስመር</text></revision>
            </page>
            <page>
              <title>Talk</title>
              <ns>1</ns>
              <revision><text bytes="3">ውይይት</text></revision>
            </page>
            <page><ns>0</ns><revision><text bytes="0" /></revision></page>
            """.trimIndent()
        val got = mutableListOf<String>()
        EthiopicText.wikiArticleLines(xml.reader().buffered()) { line -> EthiopicText.words(line) { got.add(it) } }
        assertEquals(listOf("ሰላም", "ዓለም", "ሁለተኛ", "መስመር"), got)
    }

    @Test
    fun tarGzReaderStreamsEveryRegularFile() {
        val tar = ByteArrayOutputStream()

        fun file(
            name: String,
            body: ByteArray,
        ) {
            val h = ByteArray(512)
            name.toByteArray().copyInto(h, 0)
            "%011o".format(body.size).toByteArray().copyInto(h, 124)
            h[156] = '0'.code.toByte()
            tar.write(h)
            tar.write(body)
            tar.write(ByteArray(((body.size + 511) / 512 * 512) - body.size))
        }
        file("a/one.txt", "ሰላም ሰላም\n".toByteArray())
        file("a/two.txt", "ነው\n".repeat(300).toByteArray())
        tar.write(ByteArray(1024))
        val gz = ByteArrayOutputStream()
        GZIPOutputStream(gz).use { it.write(tar.toByteArray()) }

        val counter = WordCounter()
        val names = mutableListOf<String>()
        EthiopicText.forEachTarGzEntry(ByteArrayInputStream(gz.toByteArray())) { name, s ->
            names.add(name)
            s.bufferedReader().forEachLine { l -> EthiopicText.words(l) { counter.add(it) } }
        }
        assertEquals(listOf("a/one.txt", "a/two.txt"), names)
        assertEquals(mapOf("ሰላም" to 2L, "ነው" to 300L), counter.snapshot())
    }

    @Test
    fun counterPrunesRareWordsUnderMemoryBound() {
        val c = WordCounter(maxEntries = 100)
        repeat(50) { c.add("ሰላም") }
        for (i in 0 until 1000) c.add("ሀ" + "ለ".repeat(i % 400 + 1))
        assertTrue(c.size <= 100)
        assertEquals(50L, c.snapshot()["ሰላም"])
        assertTrue(c.prunes > 0)
    }

    @Test
    fun combineWeightsFiltersAndCaps() {
        val wiki = mapOf("ሰላም" to 10L, "ዓለም" to 8L, "ቐለ" to 100L)
        val text = mapOf("ሰላም" to 5L, "ነው" to 2L, "እና" to 50L)
        val out = AmharicBuild.combine(wiki, text, 0.5, 3, 2)
        assertEquals(mapOf("እና" to 50L, "ሰላም" to 10L), out)
        assertEquals(4L, AmharicBuild.combine(wiki, text, 0.5, 3, 10)["ዓለም"])
        assertNull(AmharicBuild.combine(wiki, text, 0.5, 3, 10)["ነው"])
    }

    @Test
    fun skeletonClassesMatchEngineSpec() {
        assertEquals("slm", SkeletonClasses.skeletonOf("ሰላም"))
        assertEquals("slm", SkeletonClasses.skeletonOf("ሠላም"))
        assertEquals("msgnlh", SkeletonClasses.skeletonOf("አመሰግናለሁ"))
        assertEquals("kwnkw", SkeletonClasses.skeletonOf("ቋንቋ"))
        assertEquals("Zhy", SkeletonClasses.skeletonOf("ፀሐይ"))
        assertEquals("", SkeletonClasses.skeletonOf("ኧ"))
        assertNull(SkeletonClasses.skeletonOf("ቇ"))
    }

    @Test
    fun writerProducesValidHeaderAndChecksum() {
        val words = mapOf("ሰላም" to 1000L, "ሠላም" to 40L, "ነው" to 9000L)
        val bytes = AmwlWriter.write(words, "v-test", AmharicBuild.LICENCE_ID, "attr")
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(AmwlWriter.MAGIC, b.getInt(0))
        assertEquals("AMWL", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals(1, b.getShort(4).toInt())
        assertEquals(3, b.getInt(8))
        assertEquals(2, b.getInt(12)) // skeletons "nw" and "slm"
        assertEquals(bytes.size, b.getInt(44))
        assertEquals(AmwlWriter.checksum(bytes), b.getInt(AmwlWriter.CRC_OFFSET))
        val text = String(bytes, Charsets.UTF_8)
        assertTrue(text.contains("v-test") && text.contains("CC-BY-SA-4.0"))
    }

    @Test
    fun endToEndBuildFromTextFile() {
        val dir = createTempDirSafe()
        val corpus = File(dir, "c.txt")
        corpus.writeText("ሰላም ሰላም ሰላም ነው ነው ነው ነው እንዴት\n", Charsets.UTF_8)
        val out = File(dir, "out/am_words.bin")
        AmharicBuild.run(arrayOf("--text", corpus.path, "--out", out.path, "--data-version", "e2e", "--min-count", "2"))
        val b = ByteBuffer.wrap(out.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(2, b.getInt(8))
        dir.deleteRecursively()
    }

    private fun createTempDirSafe(): File =
        kotlin.io.path
            .createTempDirectory("amwl")
            .toFile()
}
