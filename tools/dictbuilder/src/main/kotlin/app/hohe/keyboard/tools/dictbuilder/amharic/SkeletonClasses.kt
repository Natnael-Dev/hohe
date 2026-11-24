package app.hohe.keyboard.tools.dictbuilder.amharic

/**
 * Consonant-skeleton classes per fidel (engine spec §3.2). This is a copy of the class table in
 * `lang/amharic` `Fidel.kt`: the builder module cannot depend on `:lang:amharic`. The engine's
 * `CommittedAssetTest` re-derives every shipped word's skeleton and fails if the copies drift.
 */
object SkeletonClasses {
    const val FIRST: Int = 0x1200
    const val LAST: Int = 0x135A

    private enum class Kind { NORMAL, GLOTTAL, LABIAL }

    private val SERIES: List<Triple<Int, String, Kind>> =
        listOf(
            Triple(0x1200, "h", Kind.NORMAL),
            Triple(0x1208, "l", Kind.NORMAL),
            Triple(0x1210, "h", Kind.NORMAL),
            Triple(0x1218, "m", Kind.NORMAL),
            Triple(0x1220, "s", Kind.NORMAL),
            Triple(0x1228, "r", Kind.NORMAL),
            Triple(0x1230, "s", Kind.NORMAL),
            Triple(0x1238, "S", Kind.NORMAL),
            Triple(0x1240, "k", Kind.NORMAL),
            Triple(0x1248, "k", Kind.LABIAL),
            Triple(0x1260, "b", Kind.NORMAL),
            Triple(0x1268, "v", Kind.NORMAL),
            Triple(0x1270, "t", Kind.NORMAL),
            Triple(0x1278, "c", Kind.NORMAL),
            Triple(0x1280, "h", Kind.NORMAL),
            Triple(0x1288, "h", Kind.LABIAL),
            Triple(0x1290, "n", Kind.NORMAL),
            Triple(0x1298, "gn", Kind.NORMAL),
            Triple(0x12A0, "", Kind.GLOTTAL),
            Triple(0x12A8, "k", Kind.NORMAL),
            Triple(0x12B0, "k", Kind.LABIAL),
            Triple(0x12B8, "k", Kind.NORMAL),
            Triple(0x12C0, "k", Kind.LABIAL),
            Triple(0x12C8, "w", Kind.NORMAL),
            Triple(0x12D0, "", Kind.GLOTTAL),
            Triple(0x12D8, "z", Kind.NORMAL),
            Triple(0x12E0, "z", Kind.NORMAL),
            Triple(0x12E8, "y", Kind.NORMAL),
            Triple(0x12F0, "d", Kind.NORMAL),
            Triple(0x1300, "j", Kind.NORMAL),
            Triple(0x1308, "g", Kind.NORMAL),
            Triple(0x1310, "g", Kind.LABIAL),
            Triple(0x1320, "t", Kind.NORMAL),
            Triple(0x1328, "c", Kind.NORMAL),
            Triple(0x1330, "p", Kind.NORMAL),
            Triple(0x1338, "Z", Kind.NORMAL),
            Triple(0x1340, "Z", Kind.NORMAL),
            Triple(0x1348, "f", Kind.NORMAL),
            Triple(0x1350, "p", Kind.NORMAL),
        )

    /** Series whose 8th code point is not a "Cwa" form (ቇ QOA, ኯ KOA, ...). */
    private val NO_WA_SERIES: Set<Int> = setOf(0x1200, 0x1240, 0x1280, 0x12A8, 0x12B8, 0x12C8, 0x12E8, 0x1308)
    private val LABIAL_INDICES: Set<Int> = setOf(0, 2, 3, 4, 5)

    private val table = arrayOfNulls<String>(LAST - FIRST + 1)

    init {
        for ((base, cls, kind) in SERIES) {
            for (idx in 0..7) {
                val cp = base + idx
                if (cp > LAST || !Character.isDefined(cp)) continue
                table[cp - FIRST] =
                    when (kind) {
                        Kind.NORMAL ->
                            when {
                                idx < 7 -> cls
                                base in NO_WA_SERIES -> continue
                                else -> cls + "w"
                            }
                        Kind.LABIAL -> if (idx in LABIAL_INDICES) cls + "w" else continue
                        Kind.GLOTTAL -> if (idx == 7 && base != 0x12A0) continue else ""
                    }
            }
        }
    }

    fun isSyllable(ch: Char): Boolean = ch.code in FIRST..LAST

    fun letters(ch: Char): String? = if (ch.code in FIRST..LAST) table[ch.code - FIRST] else null

    /** Skeleton of an Ethiopic word, or null if any character is outside the Amharic table. */
    fun skeletonOf(word: String): String? {
        val sb = StringBuilder(word.length + 2)
        for (ch in word) sb.append(letters(ch) ?: return null)
        return sb.toString()
    }
}
