package app.hohe.keyboard.amharic.converter

import java.util.Locale

/**
 * Deterministic Latin-to-Ethiopic transliteration converter.
 * Ported from reference implementation conv.py.
 */
object AmharicConverter {
    data class Scored(
        val text: String,
        val cost: Double,
    )

    /** consonant spelling -> fidel 1st-order base codepoint */
    private val CONS: Map<String, Int> =
        mapOf(
            "sh" to 0x1238,
            "ch" to 0x1278,
            "gn" to 0x1298, // "ny" dropped per SPEC-003 R2
            "ts" to 0x1340,
            "tz" to 0x1340,
            "zh" to 0x12E0,
            "kh" to 0x12B8,
            "ph" to 0x1330,
            "h" to 0x1200,
            "l" to 0x1208,
            "m" to 0x1218,
            "r" to 0x1228,
            "s" to 0x1230,
            "x" to 0x1238,
            "q" to 0x1240,
            "b" to 0x1260,
            "v" to 0x1268,
            "t" to 0x1270,
            "c" to 0x1278,
            "n" to 0x1290,
            "k" to 0x12A8,
            "w" to 0x12C8,
            "z" to 0x12D8,
            "y" to 0x12E8,
            "d" to 0x12F0,
            "j" to 0x1300,
            "g" to 0x1308,
            "f" to 0x1348,
            "p" to 0x1350,
        )

    private const val GLOT: Int = 0x12A0

    /** qwa/hwa/kwa/gwa series base to labialized base */
    private val KW: Map<Int, Int> =
        mapOf(
            0x1240 to 0x1248,
            0x1280 to 0x1288,
            0x12A8 to 0x12B0,
            0x1308 to 0x1310,
        )

    private val DIGRAPHS: List<String> = listOf("sh", "ch", "gn", "ts", "tz", "zh", "kh", "ph")

    private val VOWELS: List<String> =
        listOf(
            "wie",
            "ie",
            "ua",
            "wa",
            "we",
            "wi",
            "ea",
            "ei",
            "ee",
            "e",
            "u",
            "i",
            "a",
            "o",
        )

    private val SER_ALT: Map<String?, List<Pair<Int, Double>>> =
        mapOf(
            "t" to listOf(0x1320 to 1.0),
            "k" to listOf(0x1240 to 1.0),
            "c" to listOf(0x1328 to 1.0),
            "ch" to listOf(0x1328 to 1.2),
            "n" to listOf(0x1298 to 1.6),
            "h" to listOf(0x1210 to 1.8, 0x1280 to 2.6),
            "s" to listOf(0x1220 to 1.9, 0x1338 to 2.2),
            "ts" to listOf(0x1338 to 1.0),
            "tz" to listOf(0x1338 to 1.0),
            "p" to listOf(0x1330 to 1.5),
            "z" to listOf(0x12E0 to 2.5),
            null to listOf(0x12D0 to 2.0),
        )

    private data class OrdAltEntry(
        val from: Int,
        val to: Int,
        val cost: Double,
    )

    private val ORD_ALT: Map<String, List<OrdAltEntry>> =
        mapOf(
            "i" to listOf(OrdAltEntry(5, 2, 0.8)),
            "e" to listOf(OrdAltEntry(0, 4, 1.0), OrdAltEntry(4, 0, 0.9)),
            "" to listOf(OrdAltEntry(5, 2, 2.0), OrdAltEntry(5, 0, 2.2)),
            "a" to listOf(OrdAltEntry(3, 0, 1.6)),
        )

    private sealed class Order {
        data class Numeric(
            val value: Int,
        ) : Order()

        object Wa : Order()

        object Wi : Order()

        object We : Order()
    }

    private enum class Pos { INIT, MID, FINAL }

    private enum class Nxt { HASH, V, C }

    private data class Token(
        val c: String?,
        val v: String,
    )

    /** port of conv.py tokens() */
    private fun tokens(s: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        val n = s.length
        while (i < n) {
            // Explicit syllable boundary: "'" (U+0027) is never emitted
            if (s[i] == '\'') {
                i++
                continue
            }

            var c: String? = null
            for (d in DIGRAPHS) {
                if (s.startsWith(d, i)) {
                    c = d
                    break
                }
            }
            if (c == null) {
                val chStr = s[i].toString()
                if (CONS.containsKey(chStr)) {
                    c = chStr
                }
            }

            if (c != null) {
                i += c.length
            }

            var v = ""
            for (vv in VOWELS) {
                if (s.startsWith(vv, i)) {
                    if ((vv == "wa" || vv == "we" || vv == "wi" || vv == "wie") && c == null) {
                        continue
                    }
                    if ((vv == "we" || vv == "wi" || vv == "wie") && c != null && !KW.containsKey(CONS[c])) {
                        continue
                    }
                    if (vv == "ea" && c != null) {
                        // split_ea=True default
                        continue
                    }
                    v = vv
                    break
                }
            }

            // non-letter: skip
            if (c == null && v.isEmpty()) {
                i++
                continue
            }

            i += v.length
            if (c == "w" && (v == "ua" || v == "wa")) {
                v = "a"
            }
            out.add(Token(c, v))
        }
        return out
    }

    /** port of conv.py order_of() */
    private fun orderOf(
        c: String?,
        v: String,
        pos: Pos,
        nxt: Nxt,
    ): Order {
        if (v == "e" && c == "h") return Order.Numeric(4)
        if (v == "e") return Order.Numeric(0) // final_e5=False
        if (v == "u") return Order.Numeric(1)
        if (v == "a") return Order.Numeric(3) // h_a1=False
        if (v == "o") return Order.Numeric(6)
        if (v == "ie" || v == "ee") return Order.Numeric(4)
        if (v == "ei") return Order.Numeric(4)
        if (v == "i") {
            // i_mode='ctx'
            if (pos == Pos.FINAL) return Order.Numeric(2)
            return if (nxt == Nxt.C) Order.Numeric(5) else Order.Numeric(2)
        }
        if (v == "") return Order.Numeric(5)
        if (v == "ua" || v == "wa") return Order.Numeric(7)
        if (v == "we") return Order.Wa
        if (v == "wi") return Order.Wi
        if (v == "wie") return Order.We
        if (v == "ea") return Order.Numeric(0)
        return Order.Numeric(5)
    }

    /** port of conv.py glot() */
    private fun glot(
        v: String,
        pos: Pos,
    ): List<Int> {
        var o =
            when (v) {
                "a", "e" -> 0
                "i" -> 5
                "o" -> 6
                "u" -> 1
                "ie", "ee" -> 4
                "ei" -> 5
                "ea" -> 7
                else -> 0
            }
        if (v == "e" && pos == Pos.FINAL) {
            o = 4
        }
        if (v == "ea") return listOf(0x12A7)
        if (v == "ua") return listOf(0x12A1, 0x12A0)
        return listOf(GLOT + o)
    }

    /** port of conv.py cpo() */
    private fun cpo(
        b: Int,
        o: Order,
    ): Int {
        return when (o) {
            is Order.Numeric -> {
                if (o.value == 7) {
                    if (b == 0x1200) return 0x128B // hwa -> ኋ
                    if (b == 0x12B8) return 0x12C3 // khwa -> ዃ
                    if (KW.containsKey(b)) KW.getValue(b) + 3 else b + 7
                } else {
                    b + o.value
                }
            }
            is Order.Wa -> KW.getValue(b) + 0
            is Order.Wi -> KW.getValue(b) + 2
            is Order.We -> KW.getValue(b) + 4
        }
    }

    /** port of conv.py cand_syll() */
    private fun candSyll(
        c: String?,
        v: String,
        pos: Pos,
        nxt: Nxt,
    ): List<Pair<List<Int>, Double>> {
        if (c == null) {
            val prim = glot(v, pos)
            val opts = mutableListOf(prim to 0.0)
            if (v == "i") {
                opts.add(listOf(GLOT + 2) to 1.0) // ኢ
            }
            if (v == "e") {
                val altCodepoint = GLOT + (if (pos != Pos.FINAL) 5 else 0)
                opts.add(listOf(altCodepoint) to 1.2) // እ / አ
            }
            if ((v == "a" || v == "e" || v == "i" || v == "o" || v == "u") && prim.size == 1) {
                opts.add(listOf(prim[0] - GLOT + 0x12D0) to 2.0) // ዐ series
            }
            return opts
        }

        val base = CONS.getValue(c)
        val o = orderOf(c, v, pos, nxt)
        val orders = mutableListOf<Pair<Order, Double>>(o to 0.0)
        val altEntries = ORD_ALT[v]
        if (altEntries != null && o is Order.Numeric) {
            for (entry in altEntries) {
                if (entry.from == o.value) {
                    orders.add(Order.Numeric(entry.to) to entry.cost)
                }
            }
        }

        val bases = mutableListOf<Pair<Int, Double>>(base to 0.0)
        val serAlts = SER_ALT[c]
        if (serAlts != null) {
            bases.addAll(serAlts)
        }

        val res = mutableListOf<Pair<List<Int>, Double>>()
        for ((b, bc) in bases) {
            for ((oo, oc) in orders) {
                res.add(listOf(cpo(b, oo)) to (bc + oc))
            }
        }
        return res
    }

    /** port of conv.py convert() */
    fun convert(latin: String): String {
        if (latin.isEmpty()) return ""
        val s = latin.lowercase(Locale.ROOT)
        val toks = tokens(s)
        if (toks.isEmpty()) return ""
        val out = StringBuilder()
        for (k in toks.indices) {
            val (c, v) = toks[k]
            val pos =
                when {
                    k == 0 -> Pos.INIT
                    k == toks.size - 1 -> Pos.FINAL
                    else -> Pos.MID
                }
            val nxt =
                when {
                    k == toks.size - 1 -> Nxt.HASH
                    toks[k + 1].c == null -> Nxt.V
                    else -> Nxt.C
                }
            if (c == null) {
                val cps = glot(v, pos)
                for (cp in cps) {
                    out.appendCodePoint(cp)
                }
            } else {
                val base = CONS.getValue(c)
                val o = orderOf(c, v, pos, nxt)
                out.appendCodePoint(cpo(base, o))
            }
        }
        return out.toString()
    }

    /** port of conv.py nbest() */
    fun nBest(
        latin: String,
        k: Int = 4,
    ): List<Scored> {
        if (k <= 0 || latin.isEmpty()) return emptyList()
        val s = latin.lowercase(Locale.ROOT)
        val toks = tokens(s)
        if (toks.isEmpty()) return emptyList()

        var beam = listOf(Scored("", 0.0))
        for (i in toks.indices) {
            val (c, v) = toks[i]
            val pos =
                when {
                    i == 0 -> Pos.INIT
                    i == toks.size - 1 -> Pos.FINAL
                    else -> Pos.MID
                }
            val nxt =
                when {
                    i == toks.size - 1 -> Nxt.HASH
                    toks[i + 1].c == null -> Nxt.V
                    else -> Nxt.C
                }
            val opts = candSyll(c, v, pos, nxt)
            val nextBeam = ArrayList<Scored>(beam.size * opts.size)
            for (entry in beam) {
                for ((cps, cc) in opts) {
                    val sb = StringBuilder(entry.text.length + cps.size)
                    sb.append(entry.text)
                    for (cp in cps) {
                        sb.appendCodePoint(cp)
                    }
                    nextBeam.add(Scored(sb.toString(), entry.cost + cc))
                }
            }
            nextBeam.sortWith(compareBy({ it.cost }, { it.text }))
            val limit = maxOf(k * 4, 20)
            beam = if (nextBeam.size > limit) nextBeam.subList(0, limit) else nextBeam
        }

        val seen = HashSet<String>()
        val out = ArrayList<Scored>(minOf(k, beam.size))
        for (item in beam) {
            if (seen.add(item.text)) {
                val roundedCost = Math.round(item.cost * 100.0) / 100.0
                out.add(Scored(item.text, roundedCost))
                if (out.size == k) break
            }
        }
        return out
    }
}
