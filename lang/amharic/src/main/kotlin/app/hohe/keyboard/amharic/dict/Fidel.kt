package app.hohe.keyboard.amharic.dict

/**
 * Per-fidel data for the dictionary matcher (engine spec §3.2).
 *
 * For every Ethiopic syllable U+1200..U+135A this table holds:
 * - the consonant-skeleton letters (one class letter per series, `w` for labialised forms,
 *   nothing for the vowel-only አ/ዐ series), and
 * - the Latin spellings a user might type for it, each with an alignment cost.
 *
 * Class merges follow the spec: ሀ=ሐ=ኀ→h, ሰ=ሠ→s, ሸ→S, ጸ=ፀ→Z, ተ=ጠ→t, ከ=ቀ=ኸ→k, ቸ=ጨ→c,
 * ፐ=ጰ→p, ዘ=ዠ→z, ኘ→gn. Series outside Amharic (ቐ, ዸ, ጘ, ...) are unmapped.
 *
 * The dictionary builder (`tools/dictbuilder`) keeps an identical copy of the skeleton classes;
 * `CommittedAssetTest` checks that every shipped word is filed under [skeletonOf] of itself.
 */
object Fidel {
    const val FIRST: Int = 0x1200
    const val LAST: Int = 0x135A

    private const val SIZE = LAST - FIRST + 1

    private val skel = arrayOfNulls<String>(SIZE)
    private val spellings = arrayOfNulls<Array<String>>(SIZE)
    private val costs = arrayOfNulls<FloatArray>(SIZE)
    private val glottal = BooleanArray(SIZE)

    private enum class Kind { NORMAL, GLOTTAL, LABIAL }

    private class Series(
        val base: Int,
        val cls: String,
        val cons: List<Pair<String, Float>>,
        val kind: Kind = Kind.NORMAL,
        val extra: Float = 0f,
        val hSeries: Boolean = false,
    )

    private fun c(vararg p: Pair<String, Float>): List<Pair<String, Float>> = p.toList()

    private val SERIES: List<Series> =
        listOf(
            Series(0x1200, "h", c("h" to 0f), hSeries = true),
            Series(0x1208, "l", c("l" to 0f)),
            Series(0x1210, "h", c("h" to 1.8f), hSeries = true),
            Series(0x1218, "m", c("m" to 0f)),
            Series(0x1220, "s", c("s" to 1.9f)),
            Series(0x1228, "r", c("r" to 0f)),
            Series(0x1230, "s", c("s" to 0f)),
            Series(0x1238, "S", c("sh" to 0f, "x" to 0f)),
            Series(0x1240, "k", c("q" to 0f, "k" to 1.0f)),
            Series(0x1248, "k", c("q" to 0f, "k" to 1.0f), Kind.LABIAL),
            Series(0x1260, "b", c("b" to 0f)),
            Series(0x1268, "v", c("v" to 0f)),
            Series(0x1270, "t", c("t" to 0f)),
            Series(0x1278, "c", c("ch" to 0f, "c" to 0f)),
            Series(0x1280, "h", c("h" to 2.6f), hSeries = true),
            Series(0x1288, "h", c("h" to 0f), Kind.LABIAL),
            Series(0x1290, "n", c("n" to 0f)),
            Series(0x1298, "gn", c("gn" to 0f)),
            Series(0x12A0, "", c(), Kind.GLOTTAL),
            Series(0x12A8, "k", c("k" to 0f)),
            Series(0x12B0, "k", c("k" to 0f), Kind.LABIAL),
            Series(0x12B8, "k", c("kh" to 0f, "k" to 1.5f)),
            Series(0x12C0, "k", c("kh" to 0f), Kind.LABIAL),
            Series(0x12C8, "w", c("w" to 0f)),
            Series(0x12D0, "", c(), Kind.GLOTTAL, extra = 2.0f),
            Series(0x12D8, "z", c("z" to 0f)),
            Series(0x12E0, "z", c("zh" to 0f, "z" to 2.5f)),
            Series(0x12E8, "y", c("y" to 0f)),
            Series(0x12F0, "d", c("d" to 0f)),
            Series(0x1300, "j", c("j" to 0f)),
            Series(0x1308, "g", c("g" to 0f)),
            Series(0x1310, "g", c("g" to 0f), Kind.LABIAL),
            Series(0x1320, "t", c("t" to 1.0f)),
            Series(0x1328, "c", c("ch" to 1.0f, "c" to 1.0f)),
            Series(0x1330, "p", c("ph" to 0f, "p" to 1.5f)),
            Series(0x1338, "Z", c("ts" to 1.0f, "tz" to 1.0f)),
            Series(0x1340, "Z", c("ts" to 0f, "tz" to 0f)),
            Series(0x1348, "f", c("f" to 0f)),
            Series(0x1350, "p", c("p" to 0f)),
        )

    private val NO_WA_SERIES: Set<Int> = setOf(0x1200, 0x1240, 0x1280, 0x12A8, 0x12B8, 0x12C8, 0x12E8, 0x1308)

    private fun v(vararg p: Pair<String, Float>): List<Pair<String, Float>> = p.toList()

    /** Vowel spellings per order for consonant series; index 7 is the labialised "wa" form. */
    private val NORMAL_VOWELS: List<List<Pair<String, Float>>> =
        listOf(
            v("e" to 0f, "a" to 1.6f, "ie" to 1.0f, "ee" to 1.0f, "" to 2.2f),
            v("u" to 0f, "o" to 1.5f, "" to 2.5f),
            v("i" to 0f, "ii" to 0.2f, "ee" to 1.0f, "ie" to 1.2f, "" to 2.0f),
            v("a" to 0f, "aa" to 0.2f, "e" to 2.5f, "" to 2.5f),
            v("ie" to 0f, "ee" to 0f, "ei" to 0f, "e" to 0.9f, "i" to 1.2f),
            v("" to 0f, "i" to 0.4f, "e" to 1.5f),
            v("o" to 0f, "oo" to 0.2f, "u" to 1.5f),
            v("wa" to 0f, "ua" to 0f),
        )

    /** ሀ is read "ha" and users write `he` for ሄ (spec §2.3 h-series exception). */
    private val H_VOWELS: List<List<Pair<String, Float>>> =
        NORMAL_VOWELS.toMutableList().also {
            it[0] = v("e" to 0.9f, "a" to 0.8f, "" to 2.2f)
            it[4] = v("e" to 0f, "ie" to 0f, "ee" to 0f, "ei" to 0f, "i" to 1.2f)
        }

    /** Labialised blocks (ቈ ኈ ኰ ዀ ጐ): index -> vowel spellings; indices 1, 6, 7 are unassigned. */
    private val LABIAL_VOWELS: Map<Int, List<Pair<String, Float>>> =
        mapOf(
            0 to v("we" to 0f, "wa" to 1.0f),
            2 to v("wi" to 0f),
            3 to v("wa" to 0f, "ua" to 0f),
            4 to v("wie" to 0f, "we" to 1.0f),
            5 to v("w" to 0f, "wu" to 1.0f),
        )

    /** Vowel-only series አ/ዐ: index -> spellings (order 7 of አ is ኧ). */
    private val GLOTTAL_VOWELS: List<List<Pair<String, Float>>?> =
        listOf(
            v("a" to 0f, "e" to 0f),
            v("u" to 0f),
            v("i" to 1.0f, "ii" to 0.2f, "ee" to 1.0f),
            v("a" to 1.6f, "aa" to 0.5f),
            v("ie" to 0f, "ee" to 0f, "e" to 0.8f, "ei" to 0.5f),
            v("i" to 0f, "e" to 0.5f, "ei" to 0f),
            v("o" to 0f),
            v("ea" to 0f, "a" to 1.0f, "e" to 1.0f),
        )

    init {
        for (s in SERIES) {
            for (idx in 0..7) {
                val cp = s.base + idx
                if (cp > LAST || !Character.isDefined(cp)) continue
                val vowels: List<Pair<String, Float>>
                val letters: String
                when (s.kind) {
                    Kind.NORMAL -> {
                        // Order 7 of these series is not "Cwa" (ቇ QOA, ኯ KOA, ...): the labialised
                        // forms live in the separate LABIAL blocks.
                        if (idx == 7 && s.base in NO_WA_SERIES) continue
                        vowels = (if (s.hSeries) H_VOWELS else NORMAL_VOWELS)[idx]
                        letters = if (idx == 7) s.cls + "w" else s.cls
                    }
                    Kind.LABIAL -> {
                        vowels = LABIAL_VOWELS[idx] ?: continue
                        letters = s.cls + "w"
                    }
                    Kind.GLOTTAL -> {
                        if (idx == 7 && s.extra > 0f) continue
                        vowels = GLOTTAL_VOWELS[idx] ?: continue
                        letters = ""
                        glottal[cp - FIRST] = true
                    }
                }
                val out = LinkedHashMap<String, Float>()
                val consonants =
                    if (s.kind == Kind.GLOTTAL) {
                        listOf("" to 0f)
                    } else {
                        s.cons + s.cons.map { (sp, cost) -> sp[0] + sp to cost + 0.3f }
                    }
                for ((cs, cc) in consonants) {
                    for ((vs, vc) in vowels) {
                        val sp = cs + vs
                        if (sp.isEmpty()) continue
                        val cost = cc + vc + s.extra
                        val prev = out[sp]
                        if (prev == null || cost < prev) out[sp] = cost
                    }
                }
                skel[cp - FIRST] = letters
                spellings[cp - FIRST] = out.keys.toTypedArray()
                costs[cp - FIRST] = out.values.toFloatArray()
            }
        }
    }

    /** True if [ch] is an Ethiopic syllable this table understands. */
    fun isMapped(ch: Char): Boolean = ch.code in FIRST..LAST && skel[ch.code - FIRST] != null

    /** True for the vowel-only series (አ, ዐ). */
    fun isGlottal(ch: Char): Boolean = ch.code in FIRST..LAST && glottal[ch.code - FIRST]

    /** Skeleton letters for one fidel, or null if unmapped. */
    fun skeletonLetters(ch: Char): String? = if (ch.code in FIRST..LAST) skel[ch.code - FIRST] else null

    internal fun spellingsOf(ch: Char): Array<String>? = if (ch.code in FIRST..LAST) spellings[ch.code - FIRST] else null

    internal fun costsOf(ch: Char): FloatArray? = if (ch.code in FIRST..LAST) costs[ch.code - FIRST] else null

    /** Consonant skeleton of an Ethiopic word, or null if it contains an unmapped character. */
    fun skeletonOf(word: String): String? {
        val sb = StringBuilder(word.length + 2)
        for (ch in word) {
            sb.append(skeletonLetters(ch) ?: return null)
        }
        return sb.toString()
    }

    private val SINGLE: Map<Char, Char> =
        mapOf(
            'h' to 'h',
            'l' to 'l',
            'm' to 'm',
            'r' to 'r',
            's' to 's',
            'x' to 'S',
            'q' to 'k',
            'b' to 'b',
            'v' to 'v',
            't' to 't',
            'c' to 'c',
            'n' to 'n',
            'k' to 'k',
            'w' to 'w',
            'z' to 'z',
            'y' to 'y',
            'd' to 'd',
            'j' to 'j',
            'g' to 'g',
            'f' to 'f',
            'p' to 'p',
        )

    private val DIGRAPHS: List<Pair<String, String>> =
        listOf(
            "sh" to "S",
            "ch" to "c",
            "gn" to "gn",
            "ts" to "Z",
            "tz" to "Z",
            "zh" to "z",
            "kh" to "k",
            "ph" to "p",
        )

    /**
     * Consonant skeleton of typed lowercase Latin: digraphs first, vowels dropped, `ua` after a
     * consonant gives `w`. Returns the skeleton plus, when the input ends in a letter that may be
     * the first half of a digraph with its own class (`s`→`sh`, `t`→`ts`), the alternative skeleton.
     * Directly repeated consonant letters (`addis`, gemination is not written in fidel) also give a
     * skeleton with the repeat collapsed (`dds` and `ds`).
     */
    fun latinSkeletons(latin: String): List<String> {
        val sb = StringBuilder(latin.length)
        val collapsed = StringBuilder(latin.length)
        var i = 0
        var afterConsonant = false
        var lastSingle: Char? = null
        while (i < latin.length) {
            val ch = latin[i]
            val di = DIGRAPHS.firstOrNull { latin.startsWith(it.first, i) }
            if (di != null) {
                sb.append(di.second)
                collapsed.append(di.second)
                i += 2
                afterConsonant = true
                lastSingle = null
                continue
            }
            val single = SINGLE[ch]
            if (single != null) {
                sb.append(single)
                if (!(afterConsonant && lastSingle == ch)) collapsed.append(single)
                i++
                afterConsonant = true
                lastSingle = ch
                continue
            }
            if (afterConsonant && latin.startsWith("ua", i)) {
                if (sb.isEmpty() || sb[sb.length - 1] != 'w') sb.append('w')
                if (collapsed.isEmpty() || collapsed[collapsed.length - 1] != 'w') collapsed.append('w')
                i += 2
            } else {
                i++
            }
            afterConsonant = false
            lastSingle = null
        }
        val out = ArrayList<String>(4)
        for (base in listOf(sb.toString(), collapsed.toString())) {
            if (base in out) continue
            out.add(base)
            when (lastSingle) {
                's' -> out.add(base.dropLast(1) + "S")
                't' -> out.add(base.dropLast(1) + "Z")
            }
        }
        return out
    }
}
