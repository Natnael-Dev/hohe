package app.hohe.keyboard.tools.dictbuilder.english

import java.io.File
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.GZIPInputStream
import kotlin.system.exitProcess

/**
 * English + emoji data builder.
 *
 * ```
 * gradlew :tools:dictbuilder:run --args="english lexicon [--src F] [--out F] [--max N] [--manifest F]"
 * gradlew :tools:dictbuilder:run --args="english emoji [--emoji-test F] [--cldr-dir D] [--out F] [--manifest F]"
 * ```
 * Paths are relative to `tools/dictbuilder` (Gradle's working dir for `run`). Every input
 * must match the SHA-256 and licence recorded in its manifest, or the build fails.
 */
object EnglishBuild {
    private const val DATA_ROOT = "C:/dev/hohe-data"
    private const val ASSETS = "../../app/src/main/assets/dict"

    fun run(args: Array<String>) {
        val cmd = args.firstOrNull()
        try {
            val opts = options(args.drop(1))
            when (cmd) {
                "lexicon" -> lexicon(opts)
                "emoji" -> emoji(opts)
                else -> {
                    System.err.println("usage: dictbuilder english <lexicon|emoji> [--option value ...]")
                    exitProcess(2)
                }
            }
        } catch (e: IllegalArgumentException) {
            System.err.println("english build failed: ${e.message}")
            exitProcess(1)
        }
    }

    private fun lexicon(opts: Map<String, String>) {
        val manifest = loadManifest(File(opts["manifest"] ?: "sources/english/source.properties"))
        val src = File(opts["src"] ?: "$DATA_ROOT/english/${manifest.need("file")}")
        val out = File(opts["out"] ?: "$ASSETS/en_main.bin")
        val max = opts["max"]?.toInt() ?: manifest.getProperty("maxWords", "150000").toInt()
        verify(src, manifest.need("sha256"))
        val parsed =
            src.inputStream().use { raw ->
                val stream = if (src.name.endsWith(".gz")) GZIPInputStream(raw) else raw
                stream.bufferedReader(Charsets.UTF_8).useLines { LexiconBuilder.parse(it) }
            }
        val selected = LexiconBuilder.select(parsed.entries, max)
        val bytes = EnglishDictWriter.write(selected, manifest.need("dataVersion"), manifest.need("licence"))
        selfCheck(bytes, selected)
        out.parentFile?.mkdirs()
        out.writeBytes(bytes)
        println(
            "en_main.bin: ${selected.size} words (of ${parsed.entries.size} eligible; skipped " +
                "${parsed.skippedOffensive} offensive, ${parsed.skippedNotAWord} not_a_word, " +
                "${parsed.skippedZero} zero-frequency/invalid), source version ${parsed.sourceVersion}, " +
                "${EnglishDictWriter.Reader(bytes).nodeCount} nodes, ${bytes.size} bytes -> ${out.path}",
        )
    }

    private fun emoji(opts: Map<String, String>) {
        val manifest = loadManifest(File(opts["manifest"] ?: "sources/emoji/source.properties"))
        val testFile = File(opts["emoji-test"] ?: "$DATA_ROOT/emoji/emoji-test.txt")
        val cldrDir = File(opts["cldr-dir"] ?: "$DATA_ROOT/emoji")
        val out = File(opts["out"] ?: "$ASSETS/emoji.tsv")
        verify(testFile, manifest.need("sha256.emoji-test.txt"))
        for (name in CLDR_FILES) verify(File(cldrDir, name), manifest.need("sha256.$name"))
        val text = EmojiBuilder.buildFromFiles(testFile, cldrDir, manifest.need("dataVersion"), manifest.need("licence"))
        out.parentFile?.mkdirs()
        out.writeText(text, Charsets.UTF_8)
        val emojiCount = text.lineSequence().count { it.startsWith("E\t") }
        println("emoji.tsv: $emojiCount emoji, ${out.length()} bytes -> ${out.path}")
    }

    private val CLDR_FILES =
        listOf("annotations_en.xml", "annotations_am.xml", "annotationsDerived_en.xml", "annotationsDerived_am.xml")

    private fun selfCheck(
        bytes: ByteArray,
        selected: List<WordFreq>,
    ) {
        val r = EnglishDictWriter.Reader(bytes)
        require(r.computedCrc() == r.storedCrc) { "self-check: checksum" }
        for (e in selected.take(SELF_CHECK_WORDS)) {
            require(r.frequency(e.word) == e.freq) { "self-check: '${e.word}' lost" }
        }
    }

    private const val SELF_CHECK_WORDS = 1000

    private fun loadManifest(file: File): Properties {
        require(file.isFile) { "missing source manifest ${file.path} (every source needs a licence record)" }
        val p = Properties()
        file.reader(Charsets.UTF_8).use { p.load(it) }
        require(!p.getProperty("licence").isNullOrBlank()) { "manifest ${file.path} has no licence" }
        return p
    }

    private fun Properties.need(key: String): String =
        getProperty(key)?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("manifest key '$key' missing")

    internal fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { s ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun verify(
        file: File,
        expected: String,
    ) {
        require(file.isFile) { "missing source ${file.path}" }
        val actual = sha256(file)
        require(actual.equals(expected, ignoreCase = true)) { "sha256 mismatch for ${file.name}: $actual" }
    }

    internal fun options(args: List<String>): Map<String, String> {
        val out = HashMap<String, String>()
        var i = 0
        while (i < args.size) {
            val a = args[i]
            require(a.startsWith("--") && i + 1 < args.size) { "bad option near '$a'" }
            out[a.removePrefix("--")] = args[i + 1]
            i += 2
        }
        return out
    }
}
