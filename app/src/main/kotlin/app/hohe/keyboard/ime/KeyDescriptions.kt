package app.hohe.keyboard.ime

import android.content.Context
import app.hohe.keyboard.R
import app.hohe.keyboard.core.layout.Key
import app.hohe.keyboard.core.layout.KeyCodes

/** Spoken names for keys whose label is an icon or a symbol (accessibility, REQ AC-NF2). */
object KeyDescriptions {
    fun describe(
        context: Context,
        key: Key,
    ): String =
        when (key.code) {
            KeyCodes.SHIFT -> context.getString(R.string.key_shift)
            KeyCodes.DELETE -> context.getString(R.string.key_delete)
            KeyCodes.ENTER -> context.getString(R.string.key_enter)
            KeyCodes.SPACE -> context.getString(R.string.key_space)
            KeyCodes.LANGUAGE -> context.getString(R.string.key_language)
            KeyCodes.SYMBOLS -> context.getString(R.string.key_symbols)
            KeyCodes.ABC -> context.getString(R.string.key_letters)
            KeyCodes.EMOJI -> context.getString(R.string.key_emoji)
            else -> key.label
        }
}
