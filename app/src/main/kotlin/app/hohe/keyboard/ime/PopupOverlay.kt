package app.hohe.keyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import app.hohe.keyboard.core.layout.MoreKey
import app.hohe.keyboard.core.layout.ResolvedKey
import app.hohe.keyboard.core.theme.KeyboardTheme
import kotlin.math.ceil
import kotlin.math.min

/**
 * Draws key previews, the long-press panel and short banners above the keys, in one
 * non-touchable layer covering the whole input view (no PopupWindow per press).
 */
class PopupOverlay(
    context: Context,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private var theme: KeyboardTheme? = null

    private var previewRect: RectF? = null
    private var previewLabel = ""

    private var panelRect: RectF? = null
    private var items: List<MoreKey> = emptyList()
    private var cells: List<RectF> = emptyList()
    private var selected = -1

    private var banner: String? = null

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    fun showPreview(
        rk: ResolvedKey,
        label: String,
        t: KeyboardTheme,
        offsetY: Float,
    ) {
        theme = t
        val r = rk.drawRect
        val w = r.width + 12f * density
        val h = r.height * 2.15f
        val cx = (r.left + r.right) / 2
        val bottom = r.bottom + offsetY
        previewRect = RectF(cx - w / 2, bottom - h, cx + w / 2, bottom).also { clampX(it) }
        previewLabel = label
        invalidate()
    }

    fun hidePreview() {
        if (previewRect != null) {
            previewRect = null
            invalidate()
        }
    }

    fun showMoreKeys(
        rk: ResolvedKey,
        keys: List<MoreKey>,
        columns: Int,
        t: KeyboardTheme,
        offsetY: Float,
        totalWidth: Int,
    ) {
        theme = t
        items = keys
        val cols = min(columns.coerceAtLeast(1), keys.size)
        val rows = ceil(keys.size / cols.toFloat()).toInt()
        val cellW = maxOf(rk.drawRect.width, MIN_CELL_W_DP * density)
        val cellH = maxOf(rk.drawRect.height, MIN_CELL_H_DP * density)
        val pad = 4f * density
        val w = cols * cellW + pad * 2
        val h = rows * cellH + pad * 2
        val cx = (rk.drawRect.left + rk.drawRect.right) / 2
        val bottom = rk.drawRect.top + offsetY - 2f * density
        val panel = RectF(cx - w / 2, bottom - h, cx + w / 2, bottom)
        if (panel.top < 0) panel.offset(0f, -panel.top)
        clampX(panel, totalWidth.toFloat())
        panelRect = panel
        cells =
            keys.indices.map { i ->
                val row = i / cols
                val col = i % cols
                RectF(
                    panel.left + pad + col * cellW,
                    panel.top + pad + row * cellH,
                    panel.left + pad + (col + 1) * cellW,
                    panel.top + pad + (row + 1) * cellH,
                )
            }
        selected = 0
        invalidate()
    }

    /** Moves the selection to the cell under ([x], [y]) in overlay coordinates. */
    fun selectMoreKeyAt(
        x: Float,
        y: Float,
    ) {
        if (cells.isEmpty()) return
        // The finger usually sits below the panel (on the key), so clamp it into the panel
        // vertically and take the nearest cell: the row follows the finger, the column follows x.
        val panel = panelRect ?: return
        val cy = y.coerceIn(panel.top + 1f, panel.bottom - 1f)
        val idx = cells.indices.minByOrNull { i -> kotlin.math.hypot(cells[i].centerX() - x, cells[i].centerY() - cy) } ?: return
        if (idx != selected) {
            selected = idx
            invalidate()
        }
    }

    fun selectedMoreKey(): MoreKey? = items.getOrNull(selected)

    fun showBanner(
        message: String,
        t: KeyboardTheme,
    ) {
        theme = t
        banner = message
        invalidate()
    }

    fun hideAll() {
        previewRect = null
        panelRect = null
        items = emptyList()
        cells = emptyList()
        selected = -1
        banner = null
        invalidate()
    }

    private fun clampX(
        r: RectF,
        maxW: Float = width.toFloat(),
    ) {
        if (maxW <= 0f) return
        if (r.left < 0) r.offset(-r.left, 0f)
        if (r.right > maxW) r.offset(maxW - r.right, 0f)
    }

    override fun onDraw(canvas: Canvas) {
        val t = theme ?: return
        previewRect?.let { r ->
            fill.color = t.popup
            setShadow(true)
            canvas.drawRoundRect(r, 16f * density, 16f * density, fill)
            setShadow(false)
            text.color = t.text
            text.textSize = r.width() * 0.5f
            val keyFaceH = r.height() / 2.15f
            val cy = r.top + (r.height() - keyFaceH) / 2
            canvas.drawText(previewLabel, r.centerX(), cy - (text.descent() + text.ascent()) / 2, text)
        }
        panelRect?.let { p ->
            fill.color = t.popup
            setShadow(true)
            canvas.drawRoundRect(p, 12f * density, 12f * density, fill)
            setShadow(false)
            cells.forEachIndexed { i, c ->
                if (i == selected) {
                    fill.color = t.enter
                    rect.set(c)
                    rect.inset(2f * density, 2f * density)
                    canvas.drawRoundRect(rect, 8f * density, 8f * density, fill)
                }
                text.color = if (i == selected) t.enterText else t.text
                val label = items[i].label
                text.textSize = if (label.codePointCount(0, label.length) > 1) c.height() * 0.32f else c.height() * 0.44f
                canvas.drawText(label, c.centerX(), c.centerY() - (text.descent() + text.ascent()) / 2, text)
            }
        }
        banner?.let { msg ->
            text.textSize = 14f * density
            val w = text.measureText(msg) + 32f * density
            val h = 36f * density
            rect.set(width / 2f - w / 2, 6f * density, width / 2f + w / 2, 6f * density + h)
            fill.color = t.popup
            setShadow(true)
            canvas.drawRoundRect(rect, h / 2, h / 2, fill)
            setShadow(false)
            text.color = t.text
            canvas.drawText(msg, rect.centerX(), rect.centerY() - (text.descent() + text.ascent()) / 2, text)
        }
    }

    private fun setShadow(on: Boolean) {
        if (on) fill.setShadowLayer(3f * density, 0f, density, 0x33000000) else fill.clearShadowLayer()
    }

    companion object {
        const val MIN_CELL_W_DP = 34f
        const val MIN_CELL_H_DP = 44f
    }
}
