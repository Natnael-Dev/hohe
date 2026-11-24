package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.amharic.dict.AmharicDictionary
import app.hohe.keyboard.amharic.dict.AmharicDictionaryWriter
import java.io.File
import java.nio.ByteBuffer

/** Small in-test dictionaries and access to the committed asset. */
object TestDicts {
    /** Synthetic frequencies shaped like the device tests A1-A7 (not corpus data). */
    val DEVICE_WORDS: Map<String, Long> =
        mapOf(
            "ሰላም" to 50_000L,
            "ሠላም" to 2_000L,
            "ሰል" to 50L,
            "ሴል" to 20L,
            "መልካም" to 8_000L,
            "መልካሙን" to 300L,
            "እንዴት" to 6_000L,
            "አበበ" to 900L,
            "አበበች" to 200L,
            "ነው" to 90_000L,
            "አመሰግናለሁ" to 4_000L,
        )

    fun bytes(
        words: Map<String, Long> = DEVICE_WORDS,
        version: String = "test-1",
    ): ByteArray = AmharicDictionaryWriter.write(words, version, "CC-BY-SA-4.0", "test data")

    fun dict(words: Map<String, Long> = DEVICE_WORDS): AmharicDictionary = AmharicDictionary.parse(ByteBuffer.wrap(bytes(words)))

    /** The shipped dictionary, resolved from the module directory Gradle runs tests in. */
    val ASSET: File =
        listOf("../../app/src/main/assets/dict/am_words.bin", "app/src/main/assets/dict/am_words.bin")
            .map { File(it) }
            .firstOrNull { it.isFile } ?: File("../../app/src/main/assets/dict/am_words.bin")

    fun assetDict(): AmharicDictionary? = if (ASSET.isFile) AmharicDictionary.load(ByteBuffer.wrap(ASSET.readBytes())) else null
}
