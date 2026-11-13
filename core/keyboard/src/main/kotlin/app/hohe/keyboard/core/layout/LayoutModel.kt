package app.hohe.keyboard.core.layout

/**
 * Top-level layout identifiers supported by the keyboard engine.
 */
enum class LayoutSet {
    QWERTY,
    SYMBOLS,
    SYMBOLS_SHIFT,
    NUMBER,
    NUMBER_PASSWORD,
    PHONE,
    PHONE_SYMBOLS,
}

/**
 * Variants for numeric text input fields.
 */
enum class NumberVariant {
    NORMAL,
    DATE,
    TIME,
    DATETIME,
    PASSWORD,
}

/**
 * Visual and behavioral styling applied to a key.
 */
enum class KeyStyle {
    LETTER,
    FUNCTIONAL,
    SPACE,
    ENTER,
    SHIFT,
    DELETE,
}

/**
 * Behavioral flags defining key capabilities.
 */
data class KeyFlags(
    val repeatable: Boolean = false,
    val noPanelAutoMoreKey: Boolean = false,
    val enterAdaptive: Boolean = false,
    val shiftAware: Boolean = false,
)

/**
 * Secondary character or action available via key long-press.
 */
data class MoreKey(
    val label: String,
    val code: Int = if (label.isNotEmpty()) label.codePointAt(0) else 0,
)

/**
 * Immutable key definition.
 */
data class Key(
    val code: Int,
    val label: String,
    val hint: String? = null,
    val moreKeys: List<MoreKey> = emptyList(),
    val widthPct: Float,
    val xPct: Float? = null,
    val style: KeyStyle = KeyStyle.LETTER,
    val flags: KeyFlags = KeyFlags(),
    val moreKeysColumns: Int = 5,
)

/**
 * Row of keys within a keyboard layout.
 */
data class Row(
    val keys: List<Key>,
    val xOffsetPct: Float = 0f,
    val heightFactor: Float = 1f,
)

/**
 * Global layout geometry parameters.
 */
data class LayoutParams(
    val horizontalGapPct: Float = 1.353f,
    val verticalGapDp: Float = 9f,
    val topPaddingDp: Float = 9f,
    val bottomPaddingDp: Float = 9f,
)

/**
 * Complete immutable keyboard layout definition.
 */
data class Layout(
    val id: LayoutSet,
    val rows: List<Row>,
    val params: LayoutParams = LayoutParams(),
)

/**
 * Aliases matching alternative design terminology.
 */
typealias KeyboardLayout = Layout
typealias KeyType = KeyStyle
typealias KeyAction = KeyFlags
