package app.hohe.keyboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses the app's backup rules (TASK-022) and checks which files Android would back up.
 * Auto Backup semantics: once any `<include>` exists, only included paths (and their children)
 * are backed up, minus `<exclude>` entries.
 */
class BackupRulesTest {
    private data class Rule(
        val domain: String,
        val path: String,
    )

    private class RuleSet(
        val includes: Set<Rule>,
        val excludes: Set<Rule>,
    ) {
        fun backsUp(
            domain: String,
            path: String,
        ): Boolean {
            fun Rule.covers() = this.domain == domain && (path == this.path || path.startsWith(this.path.trimEnd('/') + "/"))
            val included = includes.isEmpty() || includes.any { it.covers() }
            return included && excludes.none { it.covers() }
        }
    }

    private val expectedIncludes =
        setOf(
            Rule("file", DataStoreSettingsStore.BACKUP_PATH),
            Rule("database", UserWordsDb.NAME),
        )

    private fun xml(name: String): File =
        listOf(File("../app/src/main/res/xml/$name"), File("app/src/main/res/xml/$name"))
            .firstOrNull { it.isFile }
            ?: error("$name not found from ${File(".").absolutePath}")

    private fun parse(
        file: File,
        sectionTag: String?,
    ): RuleSet {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root =
            if (sectionTag == null) {
                doc.documentElement
            } else {
                val nodes = doc.documentElement.getElementsByTagName(sectionTag)
                assertEquals("exactly one <$sectionTag> in ${file.name}", 1, nodes.length)
                nodes.item(0) as Element
            }

        fun rules(tag: String): Set<Rule> {
            val nodes = root.getElementsByTagName(tag)
            return (0 until nodes.length)
                .map { i ->
                    val e = nodes.item(i) as Element
                    Rule(e.getAttribute("domain"), e.getAttribute("path"))
                }.toSet()
        }
        return RuleSet(rules("include"), rules("exclude"))
    }

    private fun assertPolicy(
        label: String,
        rules: RuleSet,
    ) {
        assertEquals("$label includes", expectedIncludes, rules.includes)
        assertTrue("$label backs up settings", rules.backsUp("file", "datastore/settings.preferences_pb"))
        assertTrue("$label backs up user words", rules.backsUp("database", "userwords.db"))
        val neverBackedUp =
            listOf(
                Rule("database", "history.db"),
                Rule("database", "history.db-journal"),
                Rule("database", "history.db-wal"),
                Rule("database", "history.db-shm"),
                Rule("file", "themes/photo_1234.webp"),
                Rule("file", "datastore/device_state.preferences_pb"),
                Rule("sharedpref", "anything.xml"),
                Rule("root", "no_backup/x"),
                Rule("external", "x"),
            )
        for (r in neverBackedUp) {
            assertFalse("$label must not back up ${r.domain}:${r.path}", rules.backsUp(r.domain, r.path))
        }
    }

    @Test
    fun fullBackupContentForApi23To30() {
        val file = xml("backup_rules.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        assertEquals("full-backup-content", doc.documentElement.tagName)
        assertPolicy("backup_rules", parse(file, null))
    }

    @Test
    fun dataExtractionRulesForApi31Plus() {
        val file = xml("data_extraction_rules.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        assertEquals("data-extraction-rules", doc.documentElement.tagName)
        assertPolicy("cloud-backup", parse(file, "cloud-backup"))
        assertPolicy("device-transfer", parse(file, "device-transfer"))
    }

    @Test
    fun ruleFileNamesMatchStorageFileNames() {
        assertEquals("datastore/settings.preferences_pb", DataStoreSettingsStore.BACKUP_PATH)
        assertEquals("userwords.db", UserWordsDb.NAME)
        assertEquals("history.db", HistoryDb.NAME)
    }
}
