package app.hohe.keyboard.core.layout

import app.hohe.keyboard.core.field.CommaVariant
import app.hohe.keyboard.core.field.FieldPolicy
import app.hohe.keyboard.core.field.LayoutKind
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.TypingLanguage

/** Pages the user can switch between inside one field. */
enum class KeyboardPage { LETTERS, SYMBOLS, SYMBOLS_SHIFT, NUMPAD }

/**
 * Builds the layout shown for a field, page, language and settings by applying the
 * transforms in a fixed order: base → number row → hint style → comma variant → Amharic.
 */
object LayoutFactory {
    /** Ethiopic punctuation offered first on long-press of `.` in Amharic mode (D-015, D-019). */
    val ETHIOPIC_PERIOD_MORE_KEYS = listOf("።", "፣", "፤", "፥", "፦", "፧", "?", "!", ",", "'", "\"", ":", ";", "-", "(", ")")

    /** Birr is offered first on long-press of `$` (REQ-031). */
    const val BIRR = "ብር"

    fun build(
        page: KeyboardPage,
        policy: FieldPolicy,
        settings: KeyboardSettings,
        language: TypingLanguage,
    ): Layout {
        val base =
            when (policy.layout) {
                LayoutKind.NUMBER -> return Layouts.number()
                LayoutKind.NUMBER_PASSWORD -> return Layouts.numberPassword()
                LayoutKind.PHONE -> return if (page == KeyboardPage.SYMBOLS) Layouts.phoneSymbols() else Layouts.phone()
                LayoutKind.QWERTY -> page
            }
        val layout =
            when (base) {
                KeyboardPage.LETTERS -> letters(policy, settings, language)
                KeyboardPage.SYMBOLS -> if (settings.numberRow) Layouts.symbolsWithNumberRow() else Layouts.symbols()
                KeyboardPage.SYMBOLS_SHIFT ->
                    if (settings.numberRow) Layouts.symbolsShiftWithNumberRow() else Layouts.symbolsShift()
                KeyboardPage.NUMPAD -> Layouts.number(fromTextMode = true)
            }
        return withBirr(layout)
    }

    private fun letters(
        policy: FieldPolicy,
        settings: KeyboardSettings,
        language: TypingLanguage,
    ): Layout {
        var layout = Layouts.qwerty()
        if (settings.numberRow) layout = LayoutTransforms.withNumberRow(layout)
        layout =
            if (settings.longPressForSymbols) {
                hintFirst(LayoutTransforms.withLongPressForSymbols(layout))
            } else {
                LayoutTransforms.withoutLongPressForSymbols(layout)
            }
        layout =
            when (policy.commaVariant) {
                CommaVariant.URL -> LayoutTransforms.forUrl(layout)
                CommaVariant.EMAIL -> LayoutTransforms.forEmail(layout)
                CommaVariant.NORMAL -> layout
            }
        if (language == TypingLanguage.AMHARIC && policy.amharicLookup) layout = forAmharic(layout)
        return layout
    }

    /** Puts a letter key's symbol hint first in its long-press panel so a long press yields the hint. */
    private fun hintFirst(layout: Layout): Layout =
        mapKeys(layout) { key ->
            val hint = key.hint
            if (key.style == KeyStyle.LETTER && hint != null && key.code > 0 && key.label.length == 1) {
                key.copy(moreKeys = listOf(MoreKey(hint)) + key.moreKeys.filter { it.label != hint })
            } else {
                key
            }
        }

    /** Amharic mode: `.` long-press offers Ethiopic punctuation first (tap stays `.`). */
    fun forAmharic(layout: Layout): Layout =
        mapKeys(layout) { key ->
            if (key.code == '.'.code && key.style == KeyStyle.FUNCTIONAL) {
                key.copy(moreKeys = ETHIOPIC_PERIOD_MORE_KEYS.map { MoreKey(it) }, moreKeysColumns = 8)
            } else {
                key
            }
        }

    /** Adds ብር as the first long-press option of every `$` key. */
    fun withBirr(layout: Layout): Layout =
        mapKeys(layout) { key ->
            if (key.code == '$'.code && key.moreKeys.none { it.label == BIRR }) {
                key.copy(moreKeys = listOf(MoreKey(BIRR, 0)) + key.moreKeys.filter { it.label != "₹" })
            } else {
                key
            }
        }

    private fun mapKeys(
        layout: Layout,
        transform: (Key) -> Key,
    ): Layout = layout.copy(rows = layout.rows.map { row -> row.copy(keys = row.keys.map(transform)) })
}
