package app.hohe.keyboard.tools.dictbuilder.amharic

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Writes `am_words.bin`, format "AMWL" version 1. The layout is documented in
 * `lang/amharic/.../dict/AmharicDictionaryFormat.kt`, which is the reader; keep both in sync.
 */
object AmwlWriter {
    const val MAGIC: Int = 0x4C574D41
    const val VERSION: Int = 1
    const val HEADER_SIZE: Int = 52
    const val CRC_OFFSET: Int = 48

    fun quantise(freq: Long): Int = Math.round(64.0 * Math.log(freq.toDouble() + 1.0)).toInt().coerceIn(0, 0xFFFF)

    fun checksum(bytes: ByteArray): Int {
        val crc = CRC32()
        crc.update(bytes, 0, CRC_OFFSET)
        crc.update(bytes, HEADER_SIZE, bytes.size - HEADER_SIZE)
        return crc.value.toInt()
    }

    private class Entry(
        val word: String,
        val skel: String,
        val q: Int,
    )

    fun write(
        words: Map<String, Long>,
        dataVersion: String,
        licenceId: String,
        attribution: String,
    ): ByteArray {
        val entries =
            words
                .mapNotNull { (w, f) ->
                    if (w.isEmpty()) return@mapNotNull null
                    val s = SkeletonClasses.skeletonOf(w) ?: return@mapNotNull null
                    Entry(w, s, quantise(f))
                }.sortedWith(compareBy<Entry>({ it.skel }, { -it.q }, { it.word }))

        val skels = ArrayList<String>()
        val firsts = ArrayList<Int>()
        entries.forEachIndexed { i, e ->
            if (skels.isEmpty() || skels.last() != e.skel) {
                skels.add(e.skel)
                firsts.add(i)
            }
        }
        val nWords = entries.size
        val nSkel = skels.size
        val totalChars = entries.sumOf { it.word.length }
        val skelBytes = skels.sumOf { it.length }
        val meta = listOf(dataVersion, licenceId, attribution).map { it.toByteArray(Charsets.UTF_8) }
        require(meta.all { it.size <= 0xFFFF }) { "metadata string too long" }

        val offWordStart = HEADER_SIZE
        val offFreq = offWordStart + 4 * (nWords + 1)
        val offChars = offFreq + 2 * nWords
        val offSkelStart = offChars + 2 * totalChars
        val offSkelFirst = offSkelStart + 4 * (nSkel + 1)
        val offSkelBytes = offSkelFirst + 4 * (nSkel + 1)
        val offMeta = offSkelBytes + skelBytes
        val total = offMeta + meta.sumOf { 2 + it.size }

        val b = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(0, MAGIC)
        b.putShort(4, VERSION.toShort())
        b.putShort(6, 0)
        b.putInt(8, nWords)
        b.putInt(12, nSkel)
        intArrayOf(offWordStart, offFreq, offChars, offSkelStart, offSkelFirst, offSkelBytes, offMeta)
            .forEachIndexed { k, o -> b.putInt(16 + 4 * k, o) }
        b.putInt(44, total)

        var chars = 0
        entries.forEachIndexed { i, e ->
            b.putInt(offWordStart + 4 * i, chars)
            b.putShort(offFreq + 2 * i, e.q.toShort())
            for (ch in e.word) {
                b.putChar(offChars + 2 * chars, ch)
                chars++
            }
        }
        b.putInt(offWordStart + 4 * nWords, chars)

        var sb = 0
        skels.forEachIndexed { s, sk ->
            b.putInt(offSkelStart + 4 * s, sb)
            b.putInt(offSkelFirst + 4 * s, firsts[s])
            for (ch in sk) b.put(offSkelBytes + sb++, ch.code.toByte())
        }
        b.putInt(offSkelStart + 4 * nSkel, sb)
        b.putInt(offSkelFirst + 4 * nSkel, nWords)

        var p = offMeta
        for (m in meta) {
            b.putShort(p, m.size.toShort())
            for (k in m.indices) b.put(p + 2 + k, m[k])
            p += 2 + m.size
        }
        val bytes = b.array()
        b.putInt(CRC_OFFSET, checksum(bytes))
        return bytes
    }
}
