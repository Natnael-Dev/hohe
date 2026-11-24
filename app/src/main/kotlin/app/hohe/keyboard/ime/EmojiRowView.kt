package app.hohe.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import app.hohe.keyboard.core.theme.KeyboardTheme
import app.hohe.keyboard.core.theme.KeyboardThemes

/** Recent-emoji row above the keys (REQ-061): 9 recents plus `⋯` that opens the emoji panel. */
class EmojiRowView(
    context: Context,
) : View(context) {
    var onEmoji: (String) -> Unit = {}
    var onMore: () -> Unit = {}
    private var emojis: List<String> = DEFAULTS
    private var theme: KeyboardTheme = KeyboardThemes.DAYLIGHT
    private val density = resources.displayMetrics.density
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var pressed = -1

    fun bind(
        recents: List<String>,
        t: KeyboardTheme,
    ) {
        emojis = (recents + DEFAULTS).distinct().take(CELLS - 1)
        theme = t
        contentDescription = emojis.joinToString(" ")
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (HEIGHT_DP * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(theme.background)
        val cw = width / CELLS.toFloat()
        text.textSize = 23f * density
        val base = height / 2f - (text.descent() + text.ascent()) / 2
        emojis.forEachIndexed { i, e ->
            if (i == pressed) {
                text.alpha = 140
            }
            canvas.drawText(e, cw * i + cw / 2, base, text)
            text.alpha = 255
        }
        text.color = theme.icon
        canvas.drawText("⋯", cw * (CELLS - 1) + cw / 2, base, text)
        text.color = 0xFF000000.toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val i = (e.x / (width / CELLS.toFloat())).toInt().coerceIn(0, CELLS - 1)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> pressed = i
            MotionEvent.ACTION_UP -> {
                if (i == CELLS - 1) onMore() else emojis.getOrNull(i)?.let(onEmoji)
                pressed = -1
            }
            MotionEvent.ACTION_CANCEL -> pressed = -1
        }
        invalidate()
        return true
    }

    companion object {
        const val CELLS = 10
        const val HEIGHT_DP = 44f
        val DEFAULTS = listOf("😂", "❤️", "😍", "🙏", "😊", "👍", "😭", "🥰", "😘")
    }
}
