package app.hohe.keyboard.amharic.dict

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Read-only view of `am_words.bin` (see [AmharicDictionaryFormat]). Reads use absolute
 * [ByteBuffer] accessors only, so one instance may be shared by readers on several threads.
 */
class AmharicDictionary private constructor(
    private val buf: ByteBuffer,
    val wordCount: Int,
    val skeletonCount: Int,
    private val offWordStart: Int,
    private val offFreq: Int,
    private val offChars: Int,
    private val offSkelStart: Int,
    private val offSkelFirst: Int,
    private val offSkelBytes: Int,
    val dataVersion: String,
    val licenceId: String,
    val attribution: String,
) {
    fun wordStart(i: Int): Int = buf.getInt(offWordStart + 4 * i)

    fun wordLength(i: Int): Int = wordStart(i + 1) - wordStart(i)

    /** Char [k] of word [i]. */
    fun charAt(
        i: Int,
        k: Int,
    ): Char = buf.getChar(offChars + 2 * (wordStart(i) + k))

    fun word(i: Int): String {
        val start = wordStart(i)
        val len = wordStart(i + 1) - start
        val chars = CharArray(len)
        for (k in 0 until len) chars[k] = buf.getChar(offChars + 2 * (start + k))
        return String(chars)
    }

    /** Quantised frequency, `round(64 * ln(freq + 1))`. */
    fun freqQ(i: Int): Int = buf.getShort(offFreq + 2 * i).toInt() and 0xFFFF

    fun skeleton(s: Int): String {
        val a = buf.getInt(offSkelStart + 4 * s)
        val b = buf.getInt(offSkelStart + 4 * (s + 1))
        val chars = CharArray(b - a)
        for (k in chars.indices) chars[k] = (buf.get(offSkelBytes + a + k).toInt() and 0x7F).toChar()
        return String(chars)
    }

    /** First word index of skeleton bucket [s]; `firstWord(skeletonCount) == wordCount`. */
    fun firstWord(s: Int): Int = buf.getInt(offSkelFirst + 4 * s)

    private fun compareSkel(
        s: Int,
        key: String,
    ): Int {
        val a = buf.getInt(offSkelStart + 4 * s)
        val b = buf.getInt(offSkelStart + 4 * (s + 1))
        val n = minOf(b - a, key.length)
        for (k in 0 until n) {
            val c = (buf.get(offSkelBytes + a + k).toInt() and 0xFF) - key[k].code
            if (c != 0) return c
        }
        return (b - a) - key.length
    }

    private fun lowerBound(key: String): Int {
        var lo = 0
        var hi = skeletonCount
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compareSkel(mid, key) < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** Word index range of the bucket whose skeleton equals [skel] (empty if none). */
    fun exactRange(skel: String): IntRange {
        val s = lowerBound(skel)
        if (s >= skeletonCount || compareSkel(s, skel) != 0) return IntRange.EMPTY
        return firstWord(s) until firstWord(s + 1)
    }

    /** Word index range of every bucket whose skeleton starts with [prefix]. */
    fun prefixRange(prefix: String): IntRange {
        val lo = lowerBound(prefix)
        val hi = lowerBound(prefix + '\u007F')
        if (lo >= hi) return IntRange.EMPTY
        return firstWord(lo) until firstWord(hi)
    }

    companion object {
        /** CRC-32 of bytes [0, 48) and [52, limit) of a little-endian dictionary buffer. */
        fun checksum(source: ByteBuffer): Int {
            val crc = CRC32()
            val chunk = ByteArray(64 * 1024)

            fun feed(
                from: Int,
                to: Int,
            ) {
                val view = source.duplicate()
                view.limit(to)
                view.position(from)
                while (view.hasRemaining()) {
                    val n = minOf(chunk.size, view.remaining())
                    view.get(chunk, 0, n)
                    crc.update(chunk, 0, n)
                }
            }
            feed(0, AmharicDictionaryFormat.CRC_OFFSET)
            feed(AmharicDictionaryFormat.HEADER_SIZE, source.limit())
            return crc.value.toInt()
        }

        /** Parses and validates [source]; throws [DictionaryFormatException] on any problem. */
        fun parse(source: ByteBuffer): AmharicDictionary {
            val buf = source.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            buf.position(0)
            val size = buf.limit()

            fun fail(msg: String): Nothing = throw DictionaryFormatException(msg)
            if (size < AmharicDictionaryFormat.HEADER_SIZE) fail("file too short: $size")
            if (buf.getInt(0) != AmharicDictionaryFormat.MAGIC) fail("bad magic")
            val version = buf.getShort(4).toInt() and 0xFFFF
            if (version != AmharicDictionaryFormat.VERSION) fail("unsupported format version $version")
            val nWords = buf.getInt(8)
            val nSkel = buf.getInt(12)
            val offs = IntArray(7) { buf.getInt(16 + 4 * it) }
            val total = buf.getInt(44)
            if (total != size) fail("length mismatch: header $total, actual $size")
            if (nWords < 0 || nSkel < 0 || nSkel > nWords) fail("bad counts")
            for (o in offs) if (o < AmharicDictionaryFormat.HEADER_SIZE || o > size) fail("offset out of range")
            if (checksum(buf) != buf.getInt(AmharicDictionaryFormat.CRC_OFFSET)) fail("checksum mismatch")

            val offWordStart = offs[0]
            val offFreq = offs[1]
            val offChars = offs[2]
            val offSkelStart = offs[3]
            val offSkelFirst = offs[4]
            val offSkelBytes = offs[5]

            fun within(
                off: Int,
                len: Long,
            ) {
                if (off + len > size) fail("section out of range")
            }
            within(offWordStart, 4L * (nWords + 1))
            within(offFreq, 2L * nWords)
            within(offSkelStart, 4L * (nSkel + 1))
            within(offSkelFirst, 4L * (nSkel + 1))
            val totalChars = buf.getInt(offWordStart + 4 * nWords)
            within(offChars, 2L * totalChars)
            within(offSkelBytes, buf.getInt(offSkelStart + 4 * nSkel).toLong())
            var prev = 0
            for (i in 0..nWords) {
                val w = buf.getInt(offWordStart + 4 * i)
                if (w < prev || (i == 0 && w != 0)) fail("word offsets not monotonic")
                prev = w
            }
            prev = 0
            for (s in 0..nSkel) {
                val f = buf.getInt(offSkelFirst + 4 * s)
                if (f < prev || f > nWords || (s == 0 && f != 0) || (s == nSkel && f != nWords)) {
                    fail("skeleton buckets inconsistent")
                }
                prev = f
            }
            var p = offs[6]
            val meta = ArrayList<String>(3)
            repeat(3) {
                within(p, 2)
                val len = buf.getShort(p).toInt() and 0xFFFF
                within(p + 2, len.toLong())
                val raw = ByteArray(len)
                for (k in 0 until len) raw[k] = buf.get(p + 2 + k)
                meta.add(String(raw, Charsets.UTF_8))
                p += 2 + len
            }
            return AmharicDictionary(
                buf,
                nWords,
                nSkel,
                offWordStart,
                offFreq,
                offChars,
                offSkelStart,
                offSkelFirst,
                offSkelBytes,
                meta[0],
                meta[1],
                meta[2],
            )
        }

        /** Fail-soft load: returns null for a missing, corrupt or version-mismatched file. */
        fun load(source: ByteBuffer?): AmharicDictionary? =
            try {
                source?.let { parse(it) }
            } catch (e: DictionaryFormatException) {
                null
            } catch (e: RuntimeException) {
                null
            }

        /** Reads the whole stream (closing it) and loads it fail-soft. */
        fun load(input: InputStream?): AmharicDictionary? =
            try {
                input?.use { load(ByteBuffer.wrap(it.readBytes())) }
            } catch (e: java.io.IOException) {
                null
            }
    }
}
