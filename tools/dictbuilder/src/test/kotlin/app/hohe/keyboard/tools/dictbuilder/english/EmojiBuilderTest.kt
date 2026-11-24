package app.hohe.keyboard.tools.dictbuilder.english

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiBuilderTest {
    private val emojiTest =
        """
        # group: Smileys & Emotion
        # subgroup: face-smiling
        1F600                                                  ; fully-qualified     # 😀 E1.0 grinning face
        # group: People & Body
        1F44B                                                  ; fully-qualified     # 👋 E0.6 waving hand
        1F44B 1F3FB                                            ; fully-qualified     # 👋🏻 E1.0 waving hand: light skin tone
        # group: Component
        1F3FB                                                  ; component           # 🏻 E1.0 light skin tone
        # group: Symbols
        2764 FE0F                                              ; fully-qualified     # ❤️ E0.6 red heart
        2764                                                   ; unqualified         # ❤ E0.6 red heart
        """.trimIndent()

    private val en =
        """
        <?xml version="1.0" encoding="UTF-8" ?>
        <!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">
        <ldml><annotations>
        <annotation cp="😀">face | grin | grinning face</annotation>
        <annotation cp="😀" type="tts">grinning face</annotation>
        <annotation cp="❤">emotion | heart | love | red</annotation>
        <annotation cp="❤" type="tts">red heart</annotation>
        <annotation cp="👋">hand | hello | wave</annotation>
        <annotation cp="👋" type="tts">waving hand</annotation>
        </annotations></ldml>
        """.trimIndent()

    private val am =
        """
        <?xml version="1.0" encoding="UTF-8" ?>
        <ldml><annotations>
        <annotation cp="❤">ልብ | ፍቅር</annotation>
        <annotation cp="❤" type="tts">ቀይ ልብ</annotation>
        </annotations></ldml>
        """.trimIndent()

    @Test
    fun buildsCategoriesInPanelOrderWithoutSkinTonesOrComponents() {
        val groups = EmojiBuilder.parseEmojiTest(emojiTest.lineSequence())
        assertEquals(listOf("Smileys & Emotion", "People & Body", "Symbols"), groups.keys.toList())
        assertEquals(listOf("👋"), groups.getValue("People & Body").map { it.emoji })
        assertEquals("❤️", groups.getValue("Symbols").single().emoji)

        val enA = EmojiBuilder.parseAnnotations(en.byteInputStream())
        val amA = EmojiBuilder.parseAnnotations(am.byteInputStream())
        val out = EmojiBuilder.build(groups, listOf(enA), listOf(amA), "t1", "Unicode-3.0")
        val lines = out.lines()
        assertTrue(lines[0].startsWith("HOHE-EMOJI\t1\tt1\tUnicode-3.0\t"))
        assertEquals("C\tsmileys_emotion\tSmileys & Emotion", lines[1])
        assertEquals("E\t😀\t1.0\tgrinning face|face|grin\t", lines[2])
        assertEquals("E\t❤️\t0.6\tred heart|emotion|heart|love|red\tቀይ ልብ|ልብ|ፍቅር", lines[6])
    }
}
