package app.hohe.keyboard.tools.dictbuilder.amharic

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Builds `am_words.bin` (TASK-025).
 *
 * ```
 * gradlew :tools:dictbuilder:run --args="amharic --wiki <pages-articles.xml> --text <corpus.tar.gz|.txt>
 *     --out <am_words.bin> --data-version <id> [--wiki-weight 0.5] [--min-count 3] [--max-words 80000]"
 * ```
 * `--wiki` takes the decompressed XML dump (the JVM has no bzip2 decoder); `--text` takes plain
 * UTF-8 text or a `.tar.gz` of text files. Both options may repeat. Raw corpora stay outside the
 * repo; their sha256 values are printed for `sources/amharic/PROVENANCE.md`.
 */
object AmharicBuild {
    const val LICENCE_ID: String = "CC-BY-SA-4.0"
    const val ATTRIBUTION: String =
        "Word frequencies derived from Amharic Wikipedia (contributors, CC BY-SA 4.0, " +
            "https://am.wikipedia.org) and the Amharic corpus of Yimam et al. 2021 " +
            "(CC BY 4.0, https://doi.org/10.17632/dtywyf3sth.1). This word list is licensed CC BY-SA 4.0."

    data class Options(
        val wiki: List<File> = emptyList(),
        val text: List<File> = emptyList(),
        val out: File? = null,
        val dataVersion: String = "",
        val wikiWeight: Double = 0.5,
        val minCount: Long = 3,
        val maxWords: Int = 80_000,
        val maxBytes: Long = 5L * 1024 * 1024,
    )

    fun parseArgs(args: Array<String>): Options {
        var o = Options()
        var i = 0

        fun value(): String = args.getOrNull(++i) ?: throw IllegalArgumentException("missing value for ${args[i - 1]}")
        while (i < args.size) {
            o =
                when (val a = args[i]) {
                    "--wiki" -> o.copy(wiki = o.wiki + File(value()))
                    "--text" -> o.copy(text = o.text + File(value()))
                    "--out" -> o.copy(out = File(value()))
                    "--data-version" -> o.copy(dataVersion = value())
                    "--wiki-weight" -> o.copy(wikiWeight = value().toDouble())
                    "--min-count" -> o.copy(minCount = value().toLong())
                    "--max-words" -> o.copy(maxWords = value().toInt())
                    "--max-bytes" -> o.copy(maxBytes = value().toLong())
                    else -> throw IllegalArgumentException("unknown option $a")
                }
            i++
        }
        require(o.out != null) { "--out is required" }
        require(o.dataVersion.isNotBlank()) { "--data-version is required" }
        require(o.wiki.isNotEmpty() || o.text.isNotEmpty()) { "give at least one --wiki or --text input" }
        return o
    }

    fun run(args: Array<String>) {
        val o =
            try {
                parseArgs(args)
            } catch (e: IllegalArgumentException) {
                System.err.println("amharic: ${e.message}")
                System.err.println(
                    "usage: amharic --wiki <xml> --text <txt|tar.gz> --out <bin> --data-version <id> " +
                        "[--wiki-weight w] [--min-count n] [--max-words n] [--max-bytes n]",
                )
                kotlin.system.exitProcess(2)
            }
        for (f in o.wiki + o.text) println("input ${f.name} sha256=${sha256(f)} bytes=${f.length()}")

        val wiki = WordCounter()
        for (f in o.wiki) {
            f.bufferedReader(Charsets.UTF_8).use { r ->
                EthiopicText.wikiArticleLines(r) { line -> EthiopicText.words(line) { wiki.add(it) } }
            }
        }
        println("wikipedia: tokens=${wiki.tokens} types=${wiki.size} pruneFloor=${wiki.floor}")

        val text = WordCounter()
        for (f in o.text) {
            if (f.name.endsWith(".tar.gz") || f.name.endsWith(".tgz")) {
                f.inputStream().use { input ->
                    EthiopicText.forEachTarGzEntry(input) { name, s ->
                        println("  reading $name")
                        countLines(s, text)
                    }
                }
            } else {
                f.inputStream().use { countLines(it, text) }
            }
        }
        println("text corpus: tokens=${text.tokens} types=${text.size} pruneFloor=${text.floor}")

        val words = combine(wiki.snapshot(), text.snapshot(), o.wikiWeight, o.minCount, o.maxWords)
        val bytes = AmwlWriter.write(words, o.dataVersion, LICENCE_ID, ATTRIBUTION)
        check(bytes.size <= o.maxBytes) { "am_words.bin is ${bytes.size} bytes, over the ${o.maxBytes} budget" }
        val out = o.out!!
        out.absoluteFile.parentFile?.mkdirs()
        out.writeBytes(bytes)
        println(
            "wrote ${out.path}: words=${words.size} bytes=${bytes.size} " +
                "minFreq=${words.values.minOrNull()} sha256=${sha256(out)}",
        )
    }

    private fun countLines(
        s: InputStream,
        counter: WordCounter,
    ) {
        s.bufferedReader(Charsets.UTF_8).forEachLine { line -> EthiopicText.words(line) { counter.add(it) } }
    }

    /**
     * `freq = text + wikiWeight * wiki` (data-sources doc: Wikipedia ×0.5 to reduce encyclopedic
     * bias), drop words below [minCount] (hapax/typo noise), keep the [maxWords] most frequent.
     */
    fun combine(
        wiki: Map<String, Long>,
        text: Map<String, Long>,
        wikiWeight: Double,
        minCount: Long,
        maxWords: Int,
    ): Map<String, Long> {
        val merged = HashMap<String, Long>(text.size + wiki.size)
        for ((w, c) in text) merged[w] = c
        for ((w, c) in wiki) merged[w] = (merged[w] ?: 0L) + Math.round(c * wikiWeight)
        return merged.entries
            .asSequence()
            .filter { it.value >= minCount && SkeletonClasses.skeletonOf(it.key) != null }
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .take(maxWords)
            .associate { it.key to it.value }
    }

    fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { s ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
