package app.hohe.keyboard.english.emoji

import app.hohe.keyboard.english.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiTest {
    private val data: EmojiData by lazy { TestData.emojiFile.inputStream().use { EmojiData.parse(it) } }
    private val suggester by lazy { HoheEmojiSuggester(data) }

    @Test
    fun catalogHasStandardCategoriesInOrder() {
        val catalog = EmojiCatalog(data)
        assertEquals(EmojiCatalog.IDS, catalog.categories.map { it.id })
        assertEquals("Smileys & Emotion", catalog.categories.first().displayName)
        assertEquals(
            "😀",
            catalog.categories
                .first()
                .emoji
                .first(),
        )
        assertTrue(data.entries.size > 1500)
        assertTrue(catalog.category("flags")!!.emoji.contains("🇪🇹"))
    }

    @Test
    fun skinToneVariantsAreNotInTheGrid() {
        val tones = (0x1F3FB..0x1F3FF).map { String(Character.toChars(it)) }
        assertFalse(data.entries.any { e -> tones.any { e.emoji.contains(it) } })
    }

    @Test
    fun versionFilterDropsNewerEmoji() {
        val catalog = EmojiCatalog(data)
        val all = catalog.categories.sumOf { it.entries.size }
        val old = catalog.categories(15.0)
        assertTrue(old.sumOf { it.entries.size } < all)
        assertTrue(old.all { c -> c.entries.all { it.versionNumber <= 15.0 } })
    }

    @Test
    fun suggestLoveGivesHearts() {
        val s = suggester.suggest("love")
        assertEquals(3, s.size)
        assertTrue(suggester.suggest("love", 64).contains("❤️"))
        assertEquals(s, suggester.suggest("Love"))
        assertTrue(suggester.suggest("lov").isEmpty())
        assertTrue(suggester.suggest("").isEmpty())
    }

    @Test
    fun nameMatchRanksFirst() {
        assertEquals("❤️", suggester.suggest("red heart").first())
    }

    @Test
    fun amharicKeywordSearch() {
        val s = suggester.search("ፍቅር")
        assertTrue(s.contains("🥰"))
        assertTrue(suggester.suggest("ፍቅር").isNotEmpty())
        // Prefix search works on Amharic too.
        assertTrue(suggester.search("ፍቅ").containsAll(s))
    }

    @Test
    fun englishPrefixSearch() {
        val s = suggester.search("lov")
        assertTrue(s.contains("💌"))
        assertTrue(suggester.search("lov", 5).size == 5)
        assertTrue(suggester.search("zzzzqq").isEmpty())
    }

    @Test
    fun acceptsCrlfCheckout() {
        val crlf =
            TestData.emojiFile
                .readText()
                .replace("\r\n", "\n")
                .replace("\n", "\r\n")
        assertEquals(data.entries.size, EmojiData.parse(crlf).entries.size)
    }

    @Test
    fun failsSoftOnBadData() {
        assertSame(EmojiData.EMPTY, EmojiData.loadOrEmpty(null))
        assertSame(EmojiData.EMPTY, EmojiData.loadOrEmpty("HOHE-EMOJI\t1\tx\ty\t00000000\nC\ta\tb\n".byteInputStream()))
        val empty = HoheEmojiSuggester(EmojiData.EMPTY)
        assertTrue(empty.suggest("love").isEmpty())
        assertTrue(empty.search("love").isEmpty())
    }
}
