package app.hohe.keyboard.tools.dictbuilder.english

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/** One lexicon entry: exact spelling and frequency 1..255. */
data class WordFreq(
    val word: String,
    val freq: Int,
)

/**
 * Writes the `en_main.bin` trie. The format is documented on `EnglishDictionary` in
 * `:lang:english`, which reads it; keep both in sync (`FORMAT_VERSION`).
 */
object EnglishDictWriter {
    const val MAGIC: Int = 0x48454E44 // "HEND"
    const val FORMAT_VERSION: Int = 1
    const val NODE_SIZE: Int = 7
    private const val FIXED_HEADER = 24
    private const val MAX_CHILD_INDEX = 0xFFFFFF

    private class Node(
        val ch: Char,
    ) {
        var freq = 0
        var maxFreq = 0
        val children = java.util.TreeMap<Char, Node>()
    }

    fun write(
        entries: List<WordFreq>,
        dataVersion: String,
        licenceId: String,
    ): ByteArray {
        val root = Node('\u0000')
        val words = HashSet<String>()
        for ((word, freq) in entries) {
            require(word.isNotEmpty()) { "empty word" }
            require(freq in 1..255) { "frequency out of range for '$word': $freq" }
            var node = root
            for (c in word) node = node.children.getOrPut(c) { Node(c) }
            node.freq = maxOf(node.freq, freq)
            words.add(word)
        }
        computeMax(root)

        val charSet = sortedSetOf<Char>()
        collectChars(root, charSet)
        require(charSet.size <= 255) { "too many distinct chars: ${charSet.size}" }
        val chars = charSet.toCharArray()
        val charIdx = HashMap<Char, Int>()
        chars.forEachIndexed { i, c -> charIdx[c] = i }

        // Breadth-first order keeps each node's children contiguous.
        val order = ArrayList<Node>()
        order.add(root)
        var head = 0
        while (head < order.size) {
            order.addAll(order[head].children.values)
            head++
        }
        require(order.size <= MAX_CHILD_INDEX) { "too many nodes: ${order.size}" }

        val dv = dataVersion.toByteArray(Charsets.UTF_8)
        val lic = licenceId.toByteArray(Charsets.UTF_8)
        require(dv.size <= 255 && lic.size <= 255) { "header strings too long" }
        val headerLen = (FIXED_HEADER + 2 + dv.size + lic.size + 3) and 3.inv()
        val bodyLen = chars.size * 2 + order.size * NODE_SIZE
        val out = ByteBuffer.allocate(headerLen + bodyLen).order(ByteOrder.BIG_ENDIAN)
        out.position(headerLen)
        for (c in chars) out.putChar(c)
        var nextChild = 1
        for (node in order) {
            require(node.children.size <= 255) { "node has ${node.children.size} children" }
            out.put(if (node === root) 0 else charIdx.getValue(node.ch).toByte())
            out.put(node.freq.toByte())
            out.put(node.maxFreq.toByte())
            out.put(node.children.size.toByte())
            val first = if (node.children.isEmpty()) 0 else nextChild
            out.put((first ushr 16).toByte())
            out.put((first ushr 8).toByte())
            out.put(first.toByte())
            nextChild += node.children.size
        }
        val crc = CRC32()
        crc.update(out.array(), headerLen, bodyLen)

        out.putInt(0, MAGIC)
        out.putShort(4, FORMAT_VERSION.toShort())
        out.putShort(6, headerLen.toShort())
        out.putInt(8, words.size)
        out.putInt(12, order.size)
        out.putShort(16, chars.size.toShort())
        out.putShort(18, 0)
        out.putInt(20, crc.value.toInt())
        var p = FIXED_HEADER
        out.put(p++, dv.size.toByte())
        for (b in dv) out.put(p++, b)
        out.put(p++, lic.size.toByte())
        for (b in lic) out.put(p++, b)
        return out.array()
    }

    private fun computeMax(node: Node): Int {
        var m = node.freq
        for (c in node.children.values) m = maxOf(m, computeMax(c))
        node.maxFreq = m
        return m
    }

    private fun collectChars(
        node: Node,
        into: MutableSet<Char>,
    ) {
        for ((c, child) in node.children) {
            into.add(c)
            collectChars(child, into)
        }
    }

    /** Minimal reader used by builder tests and the post-build self-check. */
    class Reader(
        bytes: ByteArray,
    ) {
        private val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val headerLen = bb.getShort(6).toInt() and 0xFFFF
        val wordCount = bb.getInt(8)
        val nodeCount = bb.getInt(12)
        private val charCount = bb.getShort(16).toInt() and 0xFFFF
        val storedCrc = bb.getInt(20)
        val dataVersion: String
        val licenceId: String
        private val chars = CharArray(charCount) { bb.getChar(headerLen + it * 2) }
        private val nodes = headerLen + charCount * 2

        init {
            require(bb.getInt(0) == MAGIC) { "bad magic" }
            var p = FIXED_HEADER
            val dvLen = bb.get(p).toInt() and 0xFF
            dataVersion = String(bytes, p + 1, dvLen, Charsets.UTF_8)
            p += 1 + dvLen
            val licLen = bb.get(p).toInt() and 0xFF
            licenceId = String(bytes, p + 1, licLen, Charsets.UTF_8)
        }

        fun computedCrc(): Int {
            val crc = CRC32()
            crc.update(bb.array(), headerLen, bb.limit() - headerLen)
            return crc.value.toInt()
        }

        fun frequency(word: String): Int {
            var node = 0
            for (c in word) {
                val first = u24(nodes + node * NODE_SIZE + 4)
                val count = u8(nodes + node * NODE_SIZE + 3)
                node = (first until first + count).firstOrNull { chars[u8(nodes + it * NODE_SIZE)] == c } ?: return 0
            }
            return u8(nodes + node * NODE_SIZE + 1)
        }

        /** Every (word, freq) in the trie, depth-first. */
        fun entries(): List<WordFreq> {
            val out = ArrayList<WordFreq>()

            fun walk(
                node: Int,
                prefix: String,
            ) {
                val f = u8(nodes + node * NODE_SIZE + 1)
                if (f > 0) out.add(WordFreq(prefix, f))
                val first = u24(nodes + node * NODE_SIZE + 4)
                for (c in first until first + u8(nodes + node * NODE_SIZE + 3)) {
                    walk(c, prefix + chars[u8(nodes + c * NODE_SIZE)])
                }
            }
            walk(0, "")
            return out
        }

        private fun u8(p: Int) = bb.get(p).toInt() and 0xFF

        private fun u24(p: Int) = (u8(p) shl 16) or (u8(p + 1) shl 8) or u8(p + 2)
    }
}
