package app.hohe.keyboard.english

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer
import java.util.zip.CRC32

/**
 * Read-only view of `en_main.bin`, a frequency-annotated character trie.
 *
 * Layout (big-endian), written by `tools/dictbuilder` (`EnglishDictWriter`):
 * ```
 * 0   4  magic "HEND"
 * 4   2  format version (1)
 * 6   2  header length H (offset of the char table)
 * 8   4  word count
 * 12  4  node count
 * 16  2  char count
 * 18  2  reserved (0)
 * 20  4  CRC32 of bytes [H, end)
 * 24  .  data version (u8 length + UTF-8), licence id (u8 length + UTF-8), zero padding to H
 * H   .  char table: charCount UTF-16 code units, sorted ascending
 * .   .  nodes: nodeCount records of 7 bytes, node 0 is the root
 *        [charIdx u8][freq u8, 0 = not a word][subtree max freq u8][childCount u8][firstChild u24]
 * ```
 * Children of a node are stored contiguously (breadth-first order) and sorted by char.
 * The buffer may be heap, direct or memory-mapped; it is never modified.
 */
class EnglishDictionary private constructor(
    private val buf: ByteBuffer,
    val dataVersion: String,
    val licenceId: String,
    val wordCount: Int,
    val nodeCount: Int,
    private val chars: CharArray,
    private val nodesOffset: Int,
) {
    /** Lowercase, accent-stripped form of each char-table entry. */
    internal val foldTable: CharArray = CharArray(chars.size) { fold(chars[it]) }

    /** True when the char-table entry is uppercase. */
    internal val upperTable: BooleanArray = BooleanArray(chars.size) { chars[it].isUpperCase() }

    internal fun charAt(idx: Int): Char = chars[idx]

    internal fun charIdx(node: Int): Int = u8(nodesOffset + node * NODE_SIZE)

    internal fun freq(node: Int): Int = u8(nodesOffset + node * NODE_SIZE + 1)

    internal fun maxFreq(node: Int): Int = u8(nodesOffset + node * NODE_SIZE + 2)

    internal fun childCount(node: Int): Int = u8(nodesOffset + node * NODE_SIZE + 3)

    internal fun firstChild(node: Int): Int {
        val p = nodesOffset + node * NODE_SIZE + 4
        return (u8(p) shl 16) or (u8(p + 1) shl 8) or u8(p + 2)
    }

    private fun u8(pos: Int): Int = buf.get(pos).toInt() and 0xFF

    /** Frequency (1..255) of the exact word, or 0 when it is not in the lexicon. */
    fun frequency(word: String): Int {
        var node = ROOT
        for (ch in word) {
            node = child(node, ch)
            if (node < 0) return 0
        }
        return freq(node)
    }

    /** True when [word] is in the lexicon with exactly this spelling and case. */
    operator fun contains(word: String): Boolean = frequency(word) > 0

    private fun child(
        node: Int,
        ch: Char,
    ): Int {
        val first = firstChild(node)
        val count = childCount(node)
        for (i in 0 until count) {
            val c = first + i
            val cc = chars[charIdx(c)]
            if (cc == ch) return c
            if (cc > ch) return -1
        }
        return -1
    }

    companion object {
        const val MAGIC: Int = 0x48454E44 // "HEND"
        const val FORMAT_VERSION: Int = 1
        const val NODE_SIZE: Int = 7
        const val ROOT: Int = 0
        private const val FIXED_HEADER: Int = 24

        /** Parses and checksums [buffer]. Throws [IllegalArgumentException] if it is not a valid dictionary. */
        fun from(buffer: ByteBuffer): EnglishDictionary {
            val bb = buffer.duplicate().slice().order(ByteOrder.BIG_ENDIAN)
            val size = bb.limit()
            require(size >= FIXED_HEADER) { "dictionary too small: $size bytes" }
            require(bb.getInt(0) == MAGIC) { "bad magic" }
            val version = bb.getShort(4).toInt() and 0xFFFF
            require(version == FORMAT_VERSION) { "unsupported format version $version" }
            val headerLen = bb.getShort(6).toInt() and 0xFFFF
            val words = bb.getInt(8)
            val nodes = bb.getInt(12)
            val charCount = bb.getShort(16).toInt() and 0xFFFF
            val crc = bb.getInt(20)
            val nodesOffset = headerLen + charCount * 2
            require(headerLen in FIXED_HEADER..size && nodes > 0 && words >= 0) { "bad header" }
            require(nodesOffset.toLong() + nodes.toLong() * NODE_SIZE == size.toLong()) {
                "size mismatch: header says ${nodesOffset + nodes.toLong() * NODE_SIZE}, file has $size"
            }
            require(crc32(bb, headerLen, size) == crc) { "checksum mismatch" }
            var p = FIXED_HEADER
            val dvLen = bb.get(p).toInt() and 0xFF
            val dataVersion = utf8(bb, p + 1, dvLen)
            p += 1 + dvLen
            val licLen = bb.get(p).toInt() and 0xFF
            val licence = utf8(bb, p + 1, licLen)
            require(p + 1 + licLen <= headerLen) { "bad header strings" }
            val chars = CharArray(charCount) { bb.getChar(headerLen + it * 2) }
            return EnglishDictionary(bb, dataVersion, licence, words, nodes, chars, nodesOffset)
        }

        /** Reads the whole stream into a heap buffer and parses it. The stream is not closed. */
        fun from(input: InputStream): EnglishDictionary = from(ByteBuffer.wrap(input.readBytes()))

        /** Lowercase, accent-stripped base letter (e.g. 'É' -> 'e'); other chars are lowercased. */
        fun fold(c: Char): Char {
            val lower = c.lowercaseChar()
            if (lower.code < 0x80) return lower
            val d = Normalizer.normalize(lower.toString(), Normalizer.Form.NFD)
            return if (d.isNotEmpty() && d[0].code < 0x80) d[0] else lower
        }

        private fun utf8(
            bb: ByteBuffer,
            pos: Int,
            len: Int,
        ): String {
            val arr = ByteArray(len)
            for (i in 0 until len) arr[i] = bb.get(pos + i)
            return String(arr, Charsets.UTF_8)
        }

        private fun crc32(
            bb: ByteBuffer,
            from: Int,
            to: Int,
        ): Int {
            val crc = CRC32()
            val chunk = ByteArray(64 * 1024)
            val d = bb.duplicate()
            var p = from
            while (p < to) {
                val n = minOf(chunk.size, to - p)
                d.position(p)
                d.get(chunk, 0, n)
                crc.update(chunk, 0, n)
                p += n
            }
            return crc.value.toInt()
        }
    }
}
