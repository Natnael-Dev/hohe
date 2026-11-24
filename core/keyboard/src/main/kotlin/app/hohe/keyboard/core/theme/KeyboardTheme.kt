package app.hohe.keyboard.core.theme

import app.hohe.keyboard.core.settings.ThemeId

/**
 * Keyboard colour tokens (color-palette.md §1, §3). Colours are ARGB ints.
 * 60-30-10: [background]/[key] surface, [text]/[fnKey] structure, [accent]/[enter] accent.
 */
data class KeyboardTheme(
    val id: ThemeId,
    val isDark: Boolean,
    val background: Int,
    val key: Int,
    val fnKey: Int,
    val keyPressed: Int,
    val fnKeyPressed: Int,
    val text: Int,
    val fnText: Int,
    val hint: Int,
    val spaceLabel: Int,
    val icon: Int,
    val divider: Int,
    val enter: Int,
    val enterText: Int,
    val accent: Int,
    val indicator: Int,
    val langOff: Int,
    val pill: Int,
    val onPill: Int,
    val popup: Int,
    val keyShadow: Int? = null,
    /** High-contrast themes draw a 1.5 dp outline in this colour around every key. */
    val outline: Int? = null,
)

/** Built-in themes. AUTO resolves to Daylight or Night from the system dark-mode flag. */
object KeyboardThemes {
    private fun c(hex: Long): Int = (0xFF000000 or hex).toInt()

    val DAYLIGHT =
        KeyboardTheme(
            id = ThemeId.DAYLIGHT,
            isDark = false,
            background = c(0xe3e8e6),
            key = c(0xffffff),
            fnKey = c(0xc9d3cf),
            keyPressed = c(0xd3dcd8),
            fnKeyPressed = c(0xb7c3be),
            text = c(0x111816),
            fnText = c(0x1d2724),
            hint = c(0x4d5955),
            spaceLabel = c(0x4d5955),
            icon = c(0x33403b),
            divider = c(0xc3ccc8),
            enter = c(0x0b6b4f),
            enterText = c(0xffffff),
            accent = c(0x0b6b4f),
            indicator = c(0x0b6b4f),
            langOff = c(0x9aa6a1),
            pill = c(0xcdebdf),
            onPill = c(0x06402f),
            popup = c(0xffffff),
            keyShadow = c(0xb7c2bd),
        )

    val NIGHT =
        KeyboardTheme(
            id = ThemeId.NIGHT,
            isDark = true,
            background = c(0x0f1614),
            key = c(0x27302d),
            fnKey = c(0x1b2421),
            keyPressed = c(0x3a4541),
            fnKeyPressed = c(0x2e3935),
            text = c(0xf2f5f4),
            fnText = c(0xe3e9e6),
            hint = c(0xb3bfba),
            spaceLabel = c(0xaebab5),
            icon = c(0xc9d2ce),
            divider = c(0x2f3936),
            enter = c(0x5cc49f),
            enterText = c(0x06231a),
            accent = c(0x7dd8b6),
            indicator = c(0x5fd3a8),
            langOff = c(0x5d6965),
            pill = c(0x1e4a3c),
            onPill = c(0xc6f2e1),
            popup = c(0x3a4541),
        )

    private class Spec(
        val id: ThemeId,
        val dark: Boolean,
        val bg: Long,
        val key: Long,
        val fn: Long,
        val enter: Long,
        val enterText: Long,
        val text: Long,
        val accent: Long,
    )

    private fun colourTheme(s: Spec): KeyboardTheme {
        val base = if (s.dark) NIGHT else DAYLIGHT
        val key = c(s.key)
        val fn = c(s.fn)
        val text = c(s.text)
        val bg = c(s.bg)
        val accent = c(s.accent)
        return base.copy(
            id = s.id,
            background = bg,
            key = key,
            fnKey = fn,
            keyPressed = mix(key, text, 0.14f),
            fnKeyPressed = mix(fn, text, 0.14f),
            text = text,
            fnText = text,
            hint = mix(text, key, 0.3f),
            spaceLabel = mix(text, key, 0.3f),
            icon = mix(text, bg, 0.2f),
            divider = mix(text, bg, 0.75f),
            enter = c(s.enter),
            enterText = c(s.enterText),
            accent = accent,
            indicator = accent,
            langOff = mix(text, key, 0.6f),
            pill = mix(accent, key, 0.75f),
            onPill = text,
            popup = mix(key, text, 0.08f),
            keyShadow = if (s.dark) null else mix(key, text, 0.25f),
        )
    }

    val HIGHLAND = colourTheme(Spec(ThemeId.HIGHLAND, true, 0x123b2f, 0x1e4d3f, 0x173f33, 0x7ad3b4, 0x06231a, 0xf1f7f4, 0x9be3c8))
    val LAKE_TANA = colourTheme(Spec(ThemeId.LAKE_TANA, true, 0x0e2a47, 0x1a3a5c, 0x13314f, 0x7cc4ff, 0x04182b, 0xf1f6fb, 0x9ad2ff))
    val BUNA = colourTheme(Spec(ThemeId.BUNA, true, 0x2b1f18, 0x3b2c23, 0x33261e, 0xe0a86b, 0x2b1a0c, 0xf7efe8, 0xf0c08c))
    val TEFF = colourTheme(Spec(ThemeId.TEFF, false, 0xefe9dd, 0xfffdf8, 0xddd3c2, 0x8a5a1f, 0xffffff, 0x1f1a12, 0x7a4e16))
    val SLATE = colourTheme(Spec(ThemeId.SLATE, false, 0xe3e7ec, 0xffffff, 0xc9d0d8, 0x2d4a6b, 0xffffff, 0x121821, 0x2d4a6b))

    private fun highContrast(
        id: ThemeId,
        fgHex: Long,
    ): KeyboardTheme {
        val fg = c(fgHex)
        return NIGHT.copy(
            id = id,
            background = c(0x000000),
            key = c(0x000000),
            fnKey = c(0x1a1a1a),
            keyPressed = c(0x333333),
            fnKeyPressed = c(0x333333),
            text = fg,
            fnText = fg,
            hint = fg,
            spaceLabel = fg,
            icon = fg,
            divider = fg,
            enter = fg,
            enterText = c(0x000000),
            accent = fg,
            indicator = fg,
            langOff = c(0x8a8a8a),
            pill = c(0x000000),
            onPill = fg,
            popup = c(0x000000),
            outline = fg,
        )
    }

    val HC_YELLOW = highContrast(ThemeId.HC_YELLOW, 0xffe600)
    val HC_WHITE = highContrast(ThemeId.HC_WHITE, 0xffffff)

    /** Themes offered in the picker, in display order (AUTO and PHOTO are handled by the caller). */
    val ALL: List<KeyboardTheme> = listOf(DAYLIGHT, NIGHT, HIGHLAND, LAKE_TANA, BUNA, TEFF, SLATE, HC_YELLOW, HC_WHITE)

    /** Resolve a theme id; AUTO and PHOTO follow [systemDark]. */
    fun resolve(
        id: ThemeId,
        systemDark: Boolean,
        highContrast: Boolean = false,
        highContrastTheme: ThemeId = ThemeId.HC_YELLOW,
    ): KeyboardTheme {
        if (highContrast) return if (highContrastTheme == ThemeId.HC_WHITE) HC_WHITE else HC_YELLOW
        return ALL.firstOrNull { it.id == id } ?: if (systemDark) NIGHT else DAYLIGHT
    }

    /** Linear blend of two ARGB colours; t = 0 gives [a], t = 1 gives [b]. Result is opaque. */
    fun mix(
        a: Int,
        b: Int,
        t: Float,
    ): Int {
        val r = channel(a, 16) + ((channel(b, 16) - channel(a, 16)) * t).toInt()
        val g = channel(a, 8) + ((channel(b, 8) - channel(a, 8)) * t).toInt()
        val bl = channel(a, 0) + ((channel(b, 0) - channel(a, 0)) * t).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    /** WCAG 2.x contrast ratio between two opaque colours. */
    fun contrast(
        a: Int,
        b: Int,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun channel(
        v: Int,
        shift: Int,
    ): Int = (v shr shift) and 0xFF

    private fun linear(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }

    private fun luminance(c: Int): Double =
        0.2126 * linear(channel(c, 16)) + 0.7152 * linear(channel(c, 8)) + 0.0722 * linear(channel(c, 0))
}
