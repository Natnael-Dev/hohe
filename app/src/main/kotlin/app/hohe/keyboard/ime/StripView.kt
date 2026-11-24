package app.hohe.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.hohe.keyboard.R
import app.hohe.keyboard.core.ime.KeyboardUiState
import app.hohe.keyboard.core.ime.StripItem
import app.hohe.keyboard.core.ime.StripItemKind
import app.hohe.keyboard.core.theme.KeyboardTheme
import app.hohe.keyboard.core.theme.KeyboardThemes
import kotlin.math.abs

/** Toolbar actions in the top bar (keyboard-visual-specification §5). */
enum class ToolbarAction { CHOOSER, EMOJI, CLIPBOARD, SETTINGS, MIC }

interface StripListener {
    fun onSuggestion(item: StripItem)

    fun onToolbar(action: ToolbarAction)
}

/**
 * The 48 dp top bar: toolbar when idle, suggestions while composing. Slot 0 is drawn as a chip
 * and is exactly what space commits (REQ-021a); letter-by-letter results get a dotted underline.
 */
class StripView(
    context: Context,
) : View(context) {
    var listener: StripListener? = null
    private var state: KeyboardUiState? = null
    private var theme: KeyboardTheme = KeyboardThemes.DAYLIGHT
    private val density = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotted = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private val sideW get() = 48f * density
    private var slots: List<Pair<StripItem, RectF>> = emptyList()
    private var toolbarCells: List<Pair<ToolbarAction, RectF>> = emptyList()
    private var scrollX = 0f
    private var contentW = 0f
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var dragging = false
    private var pressedIndex = -1

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun bind(
        s: KeyboardUiState,
        t: KeyboardTheme,
    ) {
        val itemsChanged = state?.strip != s.strip
        state = s
        theme = t
        if (itemsChanged) scrollX = 0f
        layoutSlots()
        contentDescription = s.strip.items.joinToString(", ") { it.text }
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (HEIGHT_DP * density).toInt())
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        layoutSlots()
    }

    private fun layoutSlots() {
        val s = state ?: return
        val h = (HEIGHT_DP * density)
        text.textSize = 17f * density
        if (s.strip.showsToolbar) {
            slots = emptyList()
            val actions = mutableListOf(ToolbarAction.CHOOSER, ToolbarAction.EMOJI, ToolbarAction.CLIPBOARD)
            val right = mutableListOf(ToolbarAction.SETTINGS)
            if (s.voiceAvailable) right.add(ToolbarAction.MIC)
            val cellW = maxOf(56f * density, 0f)
            val cells = mutableListOf<Pair<ToolbarAction, RectF>>()
            actions.forEachIndexed { i, a -> cells.add(a to RectF(i * cellW, 0f, (i + 1) * cellW, h)) }
            right.reversed().forEachIndexed { i, a -> cells.add(a to RectF(width - (i + 1) * cellW, 0f, width - i * cellW, h)) }
            toolbarCells = cells
            return
        }
        toolbarCells = listOf(ToolbarAction.CHOOSER to RectF(0f, 0f, sideW, h))
        var x = sideW
        val out = mutableListOf<Pair<StripItem, RectF>>()
        val avail = width - sideW * 2
        val minW = 64f * density
        val widths =
            s.strip.items.map { item ->
                maxOf(minW, measure(item) + 28f * density)
            }
        val total = widths.sum()
        val stretch = if (total < avail && widths.isNotEmpty()) (avail - total) / widths.size else 0f
        s.strip.items.forEachIndexed { i, item ->
            val w = widths[i] + stretch
            out.add(item to RectF(x, 0f, x + w, h))
            x += w
        }
        slots = out
        contentW = x - sideW
    }

    private fun measure(item: StripItem): Float {
        text.typeface = if (item.kind == StripItemKind.PRIMARY) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return text.measureText(item.text)
    }

    override fun onDraw(canvas: Canvas) {
        val s = state ?: return
        canvas.drawColor(theme.background)
        icon.color = theme.icon
        val iconSize = 22f * density
        if (s.strip.showsToolbar) {
            for ((a, r) in toolbarCells) Icons.draw(canvas, toolbarIcon(a), r.centerX(), r.centerY(), iconSize, icon)
            if (s.incognito) {
                text.color = theme.hint
                text.textSize = 13f * density
                text.typeface = Typeface.DEFAULT
                canvas.drawText(
                    context.getString(R.string.ime_incognito),
                    width / 2f,
                    height / 2f - (text.descent() + text.ascent()) / 2,
                    text,
                )
            }
            return
        }
        Icons.draw(canvas, Icon.GRID, sideW / 2, height / 2f, iconSize, icon)
        canvas.save()
        canvas.clipRect(sideW, 0f, width - sideW, height.toFloat())
        canvas.translate(-scrollX, 0f)
        slots.forEachIndexed { i, (item, r) ->
            drawSlot(canvas, item, r, i == pressedIndex)
            if (i < slots.size - 1) {
                fill.color = theme.divider
                val dh = r.height() * 0.44f
                canvas.drawRect(r.right - density / 2, r.centerY() - dh / 2, r.right + density / 2, r.centerY() + dh / 2, fill)
            }
        }
        canvas.restore()
        if (s.voiceAvailable) Icons.draw(canvas, Icon.MIC, width - sideW / 2, height / 2f, iconSize, icon)
    }

    private fun drawSlot(
        canvas: Canvas,
        item: StripItem,
        r: RectF,
        pressed: Boolean,
    ) {
        text.textSize = 17f * density
        if (item.kind == StripItemKind.PRIMARY) {
            fill.color = if (pressed) KeyboardThemes.mix(theme.pill, theme.text, 0.15f) else theme.pill
            val ch = 36f * density
            rect.set(r.left + 4f * density, r.centerY() - ch / 2, r.right - 4f * density, r.centerY() + ch / 2)
            canvas.drawRoundRect(rect, 10f * density, 10f * density, fill)
            text.color = theme.onPill
            text.typeface = Typeface.DEFAULT_BOLD
        } else {
            if (pressed) {
                fill.color = theme.keyPressed
                canvas.drawRect(r, fill)
            }
            text.color = theme.text
            text.typeface = Typeface.DEFAULT
        }
        val baseline = r.centerY() - (text.descent() + text.ascent()) / 2
        canvas.drawText(item.text, r.centerX(), baseline, text)
        if (item.isFallback) {
            dotted.color = text.color
            dotted.strokeWidth = 1.5f * density
            dotted.style = Paint.Style.STROKE
            dotted.pathEffect = DashPathEffect(floatArrayOf(2f * density, 2f * density), 0f)
            val tw = text.measureText(item.text)
            val y = baseline + 4f * density
            canvas.drawLine(r.centerX() - tw / 2, y, r.centerX() + tw / 2, y, dotted)
        }
    }

    private fun toolbarIcon(a: ToolbarAction): Icon =
        when (a) {
            ToolbarAction.CHOOSER -> Icon.GRID
            ToolbarAction.EMOJI -> Icon.EMOJI
            ToolbarAction.CLIPBOARD -> Icon.CLIPBOARD
            ToolbarAction.SETTINGS -> Icon.SETTINGS
            ToolbarAction.MIC -> Icon.MIC
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val s = state ?: return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                lastX = e.x
                dragging = false
                pressedIndex = slotIndexAt(e.x)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!s.strip.showsToolbar && (dragging || abs(e.x - downX) > slop)) {
                    dragging = true
                    pressedIndex = -1
                    val max = maxOf(0f, contentW - (width - sideW * 2))
                    scrollX = (scrollX - (e.x - lastX)).coerceIn(0f, max)
                    invalidate()
                }
                lastX = e.x
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) tap(e.x, s)
                pressedIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
            }
        }
        return true
    }

    private fun slotIndexAt(x: Float): Int {
        if (x < sideW || x > width - sideW) return -1
        val cx = x + scrollX
        return slots.indexOfFirst { cx >= it.second.left && cx < it.second.right }
    }

    private fun tap(
        x: Float,
        s: KeyboardUiState,
    ) {
        if (s.strip.showsToolbar) {
            toolbarCells.firstOrNull { x >= it.second.left && x < it.second.right }?.let { listener?.onToolbar(it.first) }
            return
        }
        when {
            x < sideW -> listener?.onToolbar(ToolbarAction.CHOOSER)
            x > width - sideW -> if (s.voiceAvailable) listener?.onToolbar(ToolbarAction.MIC)
            else -> slots.getOrNull(slotIndexAt(x))?.let { listener?.onSuggestion(it.first) }
        }
    }

    companion object {
        const val HEIGHT_DP = 48f
    }
}
