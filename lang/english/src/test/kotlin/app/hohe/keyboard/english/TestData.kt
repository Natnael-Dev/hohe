package app.hohe.keyboard.english

import java.io.File
import java.nio.ByteBuffer

/** Loads the committed assets once per test JVM (Gradle runs tests from the module dir). */
object TestData {
    private val assets = File("../../app/src/main/assets/dict")

    val enBytes: ByteArray by lazy { File(assets, "en_main.bin").readBytes() }

    val dictionary: EnglishDictionary by lazy { EnglishDictionary.from(ByteBuffer.wrap(enBytes)) }

    val engine: HoheEnglishEngine by lazy { HoheEnglishEngine(dictionary) }

    val emojiFile: File get() = File(assets, "emoji.tsv")
}
