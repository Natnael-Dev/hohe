package app.hohe.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import app.hohe.keyboard.core.field.EnterAction
import app.hohe.keyboard.core.ime.KeyboardUiState
import app.hohe.keyboard.core.input.ShiftState
import app.hohe.keyboard.core.layout.GeometryParams
import app.hohe.keyboard.core.layout.GeometryResolver
import app.hohe.keyboard.core.layout.Key
import app.hohe.keyboard.core.layout.KeyCodes
import app.hohe.keyboard.core.layout.KeyStyle
import app.hohe.keyboard.core.layout.KeyboardPage
import app.hohe.keyboard.core.layout.MoreKey
import app.hohe.keyboard.core.layout.ResolvedKey
import app.hohe.keyboard.core.layout.ResolvedKeyboard
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.theme.KeyboardTheme
import app.hohe.keyboard.core.theme.KeyboardThemes
import kotlin.math.abs
import kotlin.math.hypot

/** Callbacks from the key area to the input controller. */
interface KeyActionListener {
    fun onKeyDown(code: Int)

    fun onKey(
        code: Int,
        neighbours: String,
    )

    fun onText(text: String)

    fun onShiftDown()

    fun onShiftUp()

    fun onShiftHeld()

    fun onSpaceSlide(steps: Int)

    fun onDeleteWords(words: Int)
}

/**
 * Draws the resolved layout on one canvas (keyboard-engine-architecture §2) and turns touches into
 * key events (§3). Holds no typing state; everything comes from [KeyboardUiState].
 */
class KeyboardView(
    context: Context,
    private val overlay: PopupOverlay,
) : View(context) {
    var listener: KeyActionListener? = null
    private var state: KeyboardUiState? = null
    private var settings = KeyboardSettings()
    private var theme: KeyboardTheme = KeyboardThemes.DAYLIGHT
    private var resolved: ResolvedKeyboard? = null
    private val density = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    // ---- touch tracking ----
    private enum class Mode { NONE, KEY, MORE_KEYS, SPACE_SLIDE, DELETE_SWIPE, REPEAT, CONSUMED }

    private class Pointer(
        val id: Int,
        var key: ResolvedKey,
        val downX: Float,
        val downY: Float,
        var mode: Mode = Mode.KEY,
        var slideSteps: Int = 0,
        var deleteWords: Int = 0,
    )

    private var active: Pointer? = null
    private var pressed: ResolvedKey? = null
    private var shiftDownAt = 0L

    private val longPress = Runnable { onLongPress() }
    private val repeat =
        object : Runnable {
            override fun run() {
                val p = active ?: return
                if (p.key.key.code != KeyCodes.DELETE) return
                p.mode = Mode.REPEAT
                listener?.onKey(KeyCodes.DELETE, "")
                handler.postDelayed(this, REPEAT_INTERVAL_MS)
            }
        }
    private val shiftHeld = Runnable { listener?.onShiftHeld() }

    init {
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun bind(
        newState: KeyboardUiState,
        newSettings: KeyboardSettings,
        newTheme: KeyboardTheme,
    ) {
        val relayout =
            state?.layout != newState.layout ||
                settings.heightMultiplier != newSettings.heightMultiplier ||
                settings.bottomSpaceDp != newSettings.bottomSpaceDp
        state = newState
        settings = newSettings
        theme = newTheme
        if (relayout) {
            cancelTouch()
            resolve(width)
            requestLayout()
        }
        invalidate()
    }

    private fun resolve(w: Int) {
        val s = state ?: return
        if (w <= 0) return
        val swDp = resources.configuration.smallestScreenWidthDp
        resolved =
            GeometryResolver.resolve(
                s.layout,
                w,
                density,
                GeometryParams(settings.heightMultiplier, swDp, settings.bottomSpaceDp.toFloat()),
            )
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        if (resolved?.widthPx != w) resolve(w)
        val h = resolved?.heightPx?.toInt() ?: (KEYBOARD_FALLBACK_DP * density).toInt()
        setMeasuredDimension(w, h)
    }

    // ---- drawing ----

    override fun onDraw(canvas: Canvas) {
        val kb = resolved ?: return
        val s = state ?: return
        canvas.drawColor(theme.background)
        val corner = 6f * density
        for (rk in kb.keys) drawKey(canvas, rk, s, corner)
    }

    private fun keyFill(
        key: Key,
        s: KeyboardUiState,
        isPressed: Boolean,
    ): Int {
        val fnStyle = key.style == KeyStyle.FUNCTIONAL || key.style == KeyStyle.SHIFT || key.style == KeyStyle.DELETE
        return when {
            key.style == KeyStyle.ENTER -> if (isPressed) KeyboardThemes.mix(theme.enter, theme.text, 0.2f) else theme.enter
            key.code == KeyCodes.LANGUAGE && s.amharicActive -> if (isPressed) theme.fnKeyPressed else theme.pill
            key.style == KeyStyle.SHIFT && s.shift == ShiftState.LOCKED -> theme.fnKeyPressed
            fnStyle -> if (isPressed) theme.fnKeyPressed else theme.fnKey
            else -> if (isPressed) theme.keyPressed else theme.key
        }
    }

    private fun drawKey(
        canvas: Canvas,
        rk: ResolvedKey,
        s: KeyboardUiState,
        corner: Float,
    ) {
        val key = rk.key
        val r = rk.drawRect
        val isPressed = pressed === rk
        rect.set(r.left, r.top, r.right, r.bottom)
        if (isPressed && key.style == KeyStyle.LETTER) rect.inset(r.width * 0.015f, r.height * 0.015f)
        if (settings.keyBorders && theme.keyShadow != null) {
            fill.color = theme.keyShadow!!
            rect.offset(0f, density)
            canvas.drawRoundRect(rect, corner, corner, fill)
            rect.offset(0f, -density)
        }
        fill.color = keyFill(key, s, isPressed)
        canvas.drawRoundRect(rect, corner, corner, fill)
        theme.outline?.let {
            fill.style = Paint.Style.STROKE
            fill.strokeWidth = 1.5f * density
            fill.color = it
            canvas.drawRoundRect(rect, corner, corner, fill)
            fill.style = Paint.Style.FILL
        }
        drawContent(canvas, rk, s)
    }

    private fun letterSize(r: ResolvedKey): Float = r.drawRect.height * 0.46f * settings.keyTextSize

    private fun drawContent(
        canvas: Canvas,
        rk: ResolvedKey,
        s: KeyboardUiState,
    ) {
        val key = rk.key
        val r = rk.drawRect
        val cx = (r.left + r.right) / 2
        val cy = (r.top + r.bottom) / 2
        val fnColor = theme.fnText
        icon.color = fnColor
        val iconSize = Icons.fit(r.width, r.height, 0.5f)
        when {
            key.style == KeyStyle.SHIFT || key.code == KeyCodes.SHIFT -> {
                val ic =
                    when (s.shift) {
                        ShiftState.OFF -> Icon.SHIFT
                        ShiftState.LOCKED -> Icon.CAPS_LOCK
                        else -> Icon.SHIFT_FILLED
                    }
                Icons.draw(canvas, ic, cx, cy, iconSize, icon)
            }
            key.code == KeyCodes.DELETE -> Icons.draw(canvas, Icon.BACKSPACE, cx, cy, iconSize, icon)
            key.code == KeyCodes.ENTER -> drawEnter(canvas, rk, s.enterAction)
            key.code == KeyCodes.SPACE -> {
                label.typeface = if (s.amharicActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                label.color = if (s.amharicActive) theme.accent else theme.spaceLabel
                label.textSize = r.height * 0.3f * settings.keyTextSize
                val text = if (s.amharicActive) "አማርኛ" else "English"
                canvas.drawText(text, cx, baseline(cy), label)
            }
            key.code == KeyCodes.LANGUAGE -> {
                label.typeface = Typeface.DEFAULT
                label.color = if (s.amharicActive) theme.onPill else fnColor
                label.textSize = letterSize(rk) * 0.95f
                canvas.drawText("ሕ", cx, baseline(cy - r.height * 0.06f), label)
                fill.color = if (s.amharicActive) theme.indicator else theme.langOff
                val bw = r.width * 0.48f
                val by = r.bottom - r.height * 0.18f
                rect.set(cx - bw / 2, by, cx + bw / 2, by + 3f * density)
                canvas.drawRoundRect(rect, 1.5f * density, 1.5f * density, fill)
            }
            key.code == KeyCodes.EMOJI -> Icons.draw(canvas, Icon.EMOJI, cx, cy, iconSize, icon)
            else -> drawLabel(canvas, rk, s)
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        rk: ResolvedKey,
        s: KeyboardUiState,
    ) {
        val key = rk.key
        val r = rk.drawRect
        val cx = (r.left + r.right) / 2
        val cy = (r.top + r.bottom) / 2
        val isLetter = key.style == KeyStyle.LETTER
        val text = displayLabel(key, s)
        label.typeface = Typeface.DEFAULT
        label.color = if (isLetter) theme.text else theme.fnText
        val single = text.codePointCount(0, text.length) <= 1
        label.textSize = if (single) letterSize(rk) else letterSize(rk) * 0.62f
        if (key.code == ','.code && key.style == KeyStyle.FUNCTIONAL) {
            icon.color = theme.hint
            Icons.draw(canvas, Icon.EMOJI, cx, r.top + r.height * 0.26f, r.height * 0.26f, icon)
            canvas.drawText(text, cx, baseline(cy + r.height * 0.1f), label)
            return
        }
        canvas.drawText(text, cx, baseline(cy), label)
        val hint = key.hint
        if (hint != null && s.page == KeyboardPage.LETTERS && hint.length <= 2) {
            label.color = theme.hint
            label.textSize = letterSize(rk) * 0.42f
            canvas.drawText(hint, r.right - 7f * density, r.top + 4f * density + label.textSize, label)
        }
        if (key.code == '.'.code && key.moreKeys.isNotEmpty() && key.style == KeyStyle.FUNCTIONAL) {
            fill.color = theme.hint
            val t = 4f * density
            val path = android.graphics.Path()
            path.moveTo(r.right - 3f * density - t, r.top + 3f * density)
            path.lineTo(r.right - 3f * density, r.top + 3f * density)
            path.lineTo(r.right - 3f * density, r.top + 3f * density + t)
            path.close()
            canvas.drawPath(path, fill)
        }
    }

    private fun displayLabel(
        key: Key,
        s: KeyboardUiState,
    ): String =
        if (key.style == KeyStyle.LETTER && s.page == KeyboardPage.LETTERS && s.shift != ShiftState.OFF) {
            key.label.uppercase()
        } else {
            key.label
        }

    private fun drawEnter(
        canvas: Canvas,
        rk: ResolvedKey,
        action: EnterAction,
    ) {
        val r = rk.drawRect
        val cx = (r.left + r.right) / 2
        val cy = (r.top + r.bottom) / 2
        icon.color = theme.enterText
        val ic =
            when (action) {
                EnterAction.NEWLINE -> Icon.ENTER
                EnterAction.SEARCH -> Icon.SEARCH
                EnterAction.SEND -> Icon.SEND
                EnterAction.NEXT -> Icon.NEXT
                EnterAction.PREVIOUS -> Icon.PREVIOUS
                EnterAction.DONE -> Icon.DONE
                EnterAction.GO -> Icon.GO
            }
        Icons.draw(canvas, ic, cx, cy, Icons.fit(r.width, r.height, 0.48f), icon)
    }

    private fun baseline(cy: Float): Float = cy - (label.descent() + label.ascent()) / 2

    // ---- touch ----

    private fun keyAt(
        x: Float,
        y: Float,
    ): ResolvedKey? {
        val kb = resolved ?: return null
        return kb.keys.firstOrNull { k ->
            x >= k.hitRect.left && x < k.hitRect.right && y >= k.hitRect.top && y < k.hitRect.bottom
        } ?: kb.keys.minByOrNull { k ->
            hypot(x - (k.hitRect.left + k.hitRect.right) / 2, y - (k.hitRect.top + k.hitRect.bottom) / 2)
        }
    }

    /** Nearby letter keys for proximity correction, nearest first, excluding [hit]. */
    private fun neighbours(
        hit: ResolvedKey,
        x: Float,
        y: Float,
    ): String {
        val kb = resolved ?: return ""
        val radius = hit.drawRect.width * 1.6f
        return kb.keys
            .asSequence()
            .filter { it !== hit && it.key.style == KeyStyle.LETTER && it.key.label.length == 1 && it.key.label[0].isLetter() }
            .map { it to hypot(x - (it.drawRect.left + it.drawRect.right) / 2, y - (it.drawRect.top + it.drawRect.bottom) / 2) }
            .filter { it.second <= radius }
            .sortedBy { it.second }
            .take(4)
            .joinToString("") { it.first.key.label }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val idx = event.actionIndex
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> down(event.getPointerId(idx), event.getX(idx), event.getY(idx))
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Two-thumb typing: the first key commits as soon as the second finger lands.
                active?.let { p ->
                    if (p.mode == Mode.KEY && p.key.key.style == KeyStyle.LETTER) {
                        release(p, p.downX, p.downY)
                    }
                }
                down(event.getPointerId(idx), event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                val p = active ?: return true
                val i = event.findPointerIndex(p.id)
                if (i >= 0) move(p, event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val p = active ?: return true
                if (event.getPointerId(idx) == p.id) release(p, event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_CANCEL -> cancelTouch()
        }
        return true
    }

    private fun down(
        id: Int,
        x: Float,
        y: Float,
    ) {
        val rk = keyAt(x, y) ?: return
        val p = Pointer(id, rk, x, y)
        active = p
        press(rk)
        val code = rk.key.code
        listener?.onKeyDown(code)
        when {
            code == KeyCodes.SHIFT -> {
                shiftDownAt = System.currentTimeMillis()
                listener?.onShiftDown()
                handler.postDelayed(shiftHeld, SHIFT_LOCK_MS)
            }
            code == KeyCodes.DELETE -> handler.postDelayed(repeat, REPEAT_START_MS)
            rk.key.moreKeys.isNotEmpty() -> handler.postDelayed(longPress, settings.longPressDelayMs.toLong())
        }
    }

    private fun press(rk: ResolvedKey?) {
        pressed = rk
        if (rk != null && settings.keyPopup && rk.key.style == KeyStyle.LETTER) {
            overlay.showPreview(rk, displayLabel(rk.key, state ?: return), theme, offsetY())
        } else {
            overlay.hidePreview()
        }
        invalidate()
    }

    private fun move(
        p: Pointer,
        x: Float,
        y: Float,
    ) {
        val dx = x - p.downX
        when (p.mode) {
            Mode.MORE_KEYS -> overlay.selectMoreKeyAt(x, y + offsetY())
            Mode.SPACE_SLIDE -> {
                val steps = (dx / (SLIDE_STEP_DP * density)).toInt()
                if (steps != p.slideSteps) {
                    listener?.onSpaceSlide(steps - p.slideSteps)
                    p.slideSteps = steps
                }
            }
            Mode.DELETE_SWIPE -> {
                val words = ((-dx) / (DELETE_WORD_DP * density)).toInt().coerceAtLeast(1)
                if (words != p.deleteWords) {
                    p.deleteWords = words
                    overlay.showBanner(context.getString(app.hohe.keyboard.R.string.ime_delete_words, words), theme)
                }
            }
            Mode.KEY -> {
                val code = p.key.key.code
                if (code == KeyCodes.SPACE && settings.spaceCursorControl && abs(dx) > touchSlop * 1.5f) {
                    handler.removeCallbacks(longPress)
                    p.mode = Mode.SPACE_SLIDE
                } else if (code == KeyCodes.DELETE && settings.swipeToDelete && dx < -touchSlop * 1.5f) {
                    handler.removeCallbacks(repeat)
                    p.mode = Mode.DELETE_SWIPE
                    p.deleteWords = 1
                    overlay.showBanner(context.getString(app.hohe.keyboard.R.string.ime_delete_words, 1), theme)
                } else if (p.key.key.style == KeyStyle.LETTER) {
                    val now = keyAt(x, y)
                    if (now != null && now !== p.key && now.key.style == KeyStyle.LETTER) {
                        handler.removeCallbacks(longPress)
                        p.key = now
                        press(now)
                        if (now.key.moreKeys.isNotEmpty()) handler.postDelayed(longPress, settings.longPressDelayMs.toLong())
                    }
                }
            }
            else -> Unit
        }
    }

    private fun release(
        p: Pointer,
        x: Float,
        y: Float,
    ) {
        handler.removeCallbacks(longPress)
        handler.removeCallbacks(repeat)
        handler.removeCallbacks(shiftHeld)
        val code = p.key.key.code
        when (p.mode) {
            Mode.KEY -> {
                if (code == KeyCodes.SHIFT) {
                    listener?.onShiftUp()
                } else {
                    sendAccessibility(p.key.key)
                    listener?.onKey(code, if (p.key.key.style == KeyStyle.LETTER) neighbours(p.key, x, y) else "")
                }
            }
            Mode.MORE_KEYS -> overlay.selectedMoreKey()?.let { commitMoreKey(it) }
            Mode.DELETE_SWIPE -> listener?.onDeleteWords(p.deleteWords)
            Mode.REPEAT, Mode.SPACE_SLIDE, Mode.CONSUMED, Mode.NONE -> Unit
        }
        if (code == KeyCodes.SHIFT && p.mode != Mode.KEY) listener?.onShiftUp()
        overlay.hideAll()
        active = null
        pressed = null
        invalidate()
    }

    private fun onLongPress() {
        val p = active ?: return
        if (p.mode != Mode.KEY) return
        val key = p.key.key
        val more = key.moreKeys
        if (more.isEmpty()) return
        val direct = key.flags.noPanelAutoMoreKey || (more.size == 1 && more[0].code < 0)
        if (direct) {
            p.mode = Mode.CONSUMED
            overlay.hideAll()
            pressed = null
            invalidate()
            listener?.onKey(more[0].code, "")
            return
        }
        val s = state ?: return
        val upper = key.style == KeyStyle.LETTER && s.page == KeyboardPage.LETTERS && s.shift != ShiftState.OFF
        val items = more.map { if (upper) it.copy(label = it.label.uppercase()) else it }
        p.mode = Mode.MORE_KEYS
        overlay.hidePreview()
        overlay.showMoreKeys(p.key, items, key.moreKeysColumns, theme, offsetY(), width)
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    private fun commitMoreKey(m: MoreKey) {
        when {
            m.code < 0 -> listener?.onKey(m.code, "")
            m.code == 0 || m.label.codePointCount(0, m.label.length) > 1 -> listener?.onText(m.label)
            else -> listener?.onKey(m.code, "")
        }
    }

    private fun cancelTouch() {
        handler.removeCallbacks(longPress)
        handler.removeCallbacks(repeat)
        if (active?.key?.key?.code == KeyCodes.SHIFT) listener?.onShiftUp()
        handler.removeCallbacks(shiftHeld)
        active = null
        pressed = null
        overlay.hideAll()
    }

    override fun onDetachedFromWindow() {
        cancelTouch()
        super.onDetachedFromWindow()
    }

    /** Distance from the overlay's top to this view's top, for drawing popups in overlay space. */
    private fun offsetY(): Float {
        val a = IntArray(2)
        val b = IntArray(2)
        getLocationInWindow(a)
        overlay.getLocationInWindow(b)
        return (a[1] - b[1]).toFloat()
    }

    private fun sendAccessibility(key: Key) {
        if (!isAttachedToWindow) return
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        if (am?.isEnabled == true) {
            @Suppress("DEPRECATION")
            val e = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED)
            e.text.add(KeyDescriptions.describe(context, key))
            am.sendAccessibilityEvent(e)
        }
    }

    companion object {
        const val REPEAT_START_MS = 400L
        const val REPEAT_INTERVAL_MS = 50L
        const val SHIFT_LOCK_MS = 1200L
        const val SLIDE_STEP_DP = 12f
        const val DELETE_WORD_DP = 28f
        const val KEYBOARD_FALLBACK_DP = 240f
    }
}
