package app.hohe.keyboard.tools.dictbuilder.amharic

import java.io.BufferedReader
import java.io.InputStream
import java.util.zip.GZIPInputStream

/** Tokenising and corpus readers for the Amharic word-list build. */
object EthiopicText {
    const val MAX_WORD_CHARS: Int = 20

    private fun isCombiningMark(ch: Char): Boolean = ch.code in 0x135D..0x135F

    /**
     * Calls [emit] for every Amharic word in [line]: a maximal run of Ethiopic syllables
     * (U+1200..U+135A; gemination marks U+135D..U+135F are dropped) that is not glued to letters
     * of another script, contains only mapped fidels, and has at most [MAX_WORD_CHARS] chars.
     * Ethiopic punctuation (፡ ። ፣ ...), digits and ASCII separate words.
     */
    fun words(
        line: CharSequence,
        emit: (String) -> Unit,
    ) {
        var i = 0
        val n = line.length
        val sb = StringBuilder(32)
        while (i < n) {
            val ch = line[i]
            if (!SkeletonClasses.isSyllable(ch)) {
                i++
                continue
            }
            val start = i
            sb.setLength(0)
            var ok = true
            while (i < n && (SkeletonClasses.isSyllable(line[i]) || isCombiningMark(line[i]))) {
                val c = line[i]
                if (!isCombiningMark(c)) {
                    if (SkeletonClasses.letters(c) == null) ok = false
                    sb.append(c)
                }
                i++
            }
            val gluedBefore = start > 0 && Character.isLetter(line[start - 1])
            val gluedAfter = i < n && Character.isLetter(line[i])
            if (ok && !gluedBefore && !gluedAfter && sb.isNotEmpty() && sb.length <= MAX_WORD_CHARS) {
                emit(sb.toString())
            }
        }
    }

    /**
     * Streams article text out of a MediaWiki `pages-articles` XML dump: only `<text>` of
     * namespace-0 pages, skipping category/file link lines (`[[መደብ:...]]`).
     */
    fun wikiArticleLines(
        reader: BufferedReader,
        emit: (String) -> Unit,
    ) {
        var ns = -1
        var inText = false
        while (true) {
            var line = reader.readLine() ?: break
            if (!inText) {
                val t = line.trim()
                if (t.startsWith("<page>")) ns = -1
                if (t.startsWith("<ns>")) ns = t.removePrefix("<ns>").removeSuffix("</ns>").toIntOrNull() ?: -1
                val open = line.indexOf("<text")
                if (open < 0) continue
                val gt = line.indexOf('>', open)
                if (gt < 0 || line.substring(open, gt).endsWith("/")) continue
                line = line.substring(gt + 1)
                inText = true
            }
            val close = line.indexOf("</text>")
            val body = if (close >= 0) line.substring(0, close) else line
            if (close >= 0) inText = false
            if (ns != 0) continue
            val tb = body.trimStart()
            if (tb.startsWith("[[") && tb.indexOf(':') in 2..40) continue
            emit(body)
        }
    }

    /** Calls [entry] with a stream for every regular file in a `.tar.gz` archive (ustar/GNU). */
    fun forEachTarGzEntry(
        input: InputStream,
        entry: (name: String, stream: InputStream) -> Unit,
    ) {
        GZIPInputStream(input, 1 shl 16).use { gz ->
            val header = ByteArray(512)
            var longName: String? = null
            while (true) {
                if (!readFully(gz, header)) return
                if (header.all { it.toInt() == 0 }) return
                val rawName = cString(header, 0, 100)
                val prefix = cString(header, 345, 155)
                val size = cString(header, 124, 12).trim().ifEmpty { "0" }.toLong(8)
                val type = header[156].toInt().toChar()
                val padded = (size + 511) / 512 * 512
                when (type) {
                    'L' -> {
                        val buf = ByteArray(padded.toInt())
                        readFully(gz, buf)
                        longName = String(buf, 0, size.toInt(), Charsets.UTF_8).trimEnd('\u0000')
                        continue
                    }
                    '0', '\u0000' -> {
                        val name = longName ?: if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName
                        longName = null
                        val bounded = BoundedStream(gz, size)
                        entry(name, bounded)
                        bounded.drain()
                        skip(gz, padded - size)
                    }
                    else -> {
                        longName = null
                        skip(gz, padded)
                    }
                }
            }
        }
    }

    private fun cString(
        b: ByteArray,
        off: Int,
        len: Int,
    ): String {
        var end = off
        while (end < off + len && b[end].toInt() != 0) end++
        return String(b, off, end - off, Charsets.UTF_8)
    }

    private fun readFully(
        s: InputStream,
        b: ByteArray,
    ): Boolean {
        var n = 0
        while (n < b.size) {
            val r = s.read(b, n, b.size - n)
            if (r < 0) return false
            n += r
        }
        return true
    }

    private fun skip(
        s: InputStream,
        count: Long,
    ) {
        var left = count
        val buf = ByteArray(8192)
        while (left > 0) {
            val r = s.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (r < 0) return
            left -= r
        }
    }

    private class BoundedStream(
        private val inner: InputStream,
        private var left: Long,
    ) : InputStream() {
        override fun read(): Int {
            if (left <= 0) return -1
            val r = inner.read()
            if (r >= 0) left--
            return r
        }

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
            if (left <= 0) return -1
            val r = inner.read(b, off, minOf(len.toLong(), left).toInt())
            if (r > 0) left -= r
            return r
        }

        fun drain() = skip(inner, left).also { left = 0 }

        override fun close() {
            // the archive stream is owned by forEachTarGzEntry
        }
    }
}
