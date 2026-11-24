package app.hohe.keyboard.ime

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Simple stroked icons drawn into a square box, coloured by the given paint. */
enum class Icon {
    SHIFT,
    SHIFT_FILLED,
    CAPS_LOCK,
    BACKSPACE,
    ENTER,
    SEARCH,
    SEND,
    NEXT,
    PREVIOUS,
    DONE,
    GO,
    GRID,
    EMOJI,
    CLIPBOARD,
    SETTINGS,
    MIC,
    CLOSE,
    PIN,
    KEYBOARD,
}

object Icons {
    private val path = Path()
    private val rect = RectF()

    /** Draws [icon] centred at ([cx], [cy]) inside a box of [size] px. [paint] colour is used for both stroke and fill. */
    fun draw(
        canvas: Canvas,
        icon: Icon,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint,
    ) {
        val s = size / 24f
        val l = cx - size / 2
        val t = cy - size / 2

        fun x(v: Float) = l + v * s

        fun y(v: Float) = t + v * s
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth
        paint.strokeWidth = 2f * s
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = Paint.Style.STROKE
        path.reset()
        when (icon) {
            Icon.SHIFT, Icon.SHIFT_FILLED, Icon.CAPS_LOCK -> {
                path.moveTo(x(12f), y(3.5f))
                path.lineTo(x(20.5f), y(12.5f))
                path.lineTo(x(15.5f), y(12.5f))
                path.lineTo(x(15.5f), y(18f))
                path.lineTo(x(8.5f), y(18f))
                path.lineTo(x(8.5f), y(12.5f))
                path.lineTo(x(3.5f), y(12.5f))
                path.close()
                if (icon != Icon.SHIFT) paint.style = Paint.Style.FILL_AND_STROKE
                canvas.drawPath(path, paint)
                if (icon == Icon.CAPS_LOCK) {
                    paint.style = Paint.Style.FILL_AND_STROKE
                    canvas.drawRect(x(8.5f), y(20.5f), x(15.5f), y(21.5f), paint)
                }
            }
            Icon.BACKSPACE -> {
                path.moveTo(x(8f), y(5f))
                path.lineTo(x(21f), y(5f))
                path.lineTo(x(21f), y(19f))
                path.lineTo(x(8f), y(19f))
                path.lineTo(x(2.5f), y(12f))
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawLine(x(11.5f), y(9f), x(17.5f), y(15f), paint)
                canvas.drawLine(x(17.5f), y(9f), x(11.5f), y(15f), paint)
            }
            Icon.ENTER -> {
                path.moveTo(x(19f), y(5f))
                path.lineTo(x(19f), y(13f))
                path.lineTo(x(5f), y(13f))
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(x(9f), y(9f))
                path.lineTo(x(5f), y(13f))
                path.lineTo(x(9f), y(17f))
                canvas.drawPath(path, paint)
            }
            Icon.SEARCH -> {
                canvas.drawCircle(x(10f), y(10f), 6f * s, paint)
                canvas.drawLine(x(14.5f), y(14.5f), x(20f), y(20f), paint)
            }
            Icon.SEND -> {
                path.moveTo(x(3f), y(4f))
                path.lineTo(x(21f), y(12f))
                path.lineTo(x(3f), y(20f))
                path.lineTo(x(6f), y(12f))
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawLine(x(6f), y(12f), x(13f), y(12f), paint)
            }
            Icon.NEXT, Icon.GO -> {
                canvas.drawLine(x(4f), y(12f), x(20f), y(12f), paint)
                path.moveTo(x(14f), y(6f))
                path.lineTo(x(20f), y(12f))
                path.lineTo(x(14f), y(18f))
                canvas.drawPath(path, paint)
            }
            Icon.PREVIOUS -> {
                canvas.drawLine(x(4f), y(12f), x(20f), y(12f), paint)
                path.moveTo(x(10f), y(6f))
                path.lineTo(x(4f), y(12f))
                path.lineTo(x(10f), y(18f))
                canvas.drawPath(path, paint)
            }
            Icon.DONE -> {
                path.moveTo(x(4.5f), y(12.5f))
                path.lineTo(x(9.5f), y(17.5f))
                path.lineTo(x(19.5f), y(6.5f))
                canvas.drawPath(path, paint)
            }
            Icon.GRID -> {
                for (i in 0..1) {
                    for (j in 0..1) {
                        rect.set(x(4f + i * 9f), y(4f + j * 9f), x(11f + i * 9f), y(11f + j * 9f))
                        canvas.drawRoundRect(rect, 1.5f * s, 1.5f * s, paint)
                    }
                }
            }
            Icon.EMOJI -> {
                canvas.drawCircle(x(12f), y(12f), 9f * s, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x(9f), y(10f), 1.2f * s, paint)
                canvas.drawCircle(x(15f), y(10f), 1.2f * s, paint)
                paint.style = Paint.Style.STROKE
                rect.set(x(7.5f), y(9.5f), x(16.5f), y(17f))
                canvas.drawArc(rect, 20f, 140f, false, paint)
            }
            Icon.CLIPBOARD -> {
                rect.set(x(5f), y(5f), x(19f), y(21f))
                canvas.drawRoundRect(rect, 2f * s, 2f * s, paint)
                rect.set(x(9f), y(3f), x(15f), y(7f))
                canvas.drawRoundRect(rect, 1f * s, 1f * s, paint)
                canvas.drawLine(x(9f), y(12f), x(15f), y(12f), paint)
                canvas.drawLine(x(9f), y(16f), x(13f), y(16f), paint)
            }
            Icon.SETTINGS -> {
                val teeth = 8
                for (i in 0 until teeth) {
                    val a = Math.toRadians((i * 360.0 / teeth))
                    canvas.drawLine(
                        x(12f + 6.5f * cos(a).toFloat()),
                        y(12f + 6.5f * sin(a).toFloat()),
                        x(12f + 9.5f * cos(a).toFloat()),
                        y(12f + 9.5f * sin(a).toFloat()),
                        paint,
                    )
                }
                canvas.drawCircle(x(12f), y(12f), 6.5f * s, paint)
                canvas.drawCircle(x(12f), y(12f), 2.5f * s, paint)
            }
            Icon.MIC -> {
                rect.set(x(9f), y(3f), x(15f), y(14f))
                canvas.drawRoundRect(rect, 3f * s, 3f * s, paint)
                rect.set(x(5.5f), y(6f), x(18.5f), y(17f))
                canvas.drawArc(rect, 0f, 180f, false, paint)
                canvas.drawLine(x(12f), y(17f), x(12f), y(21f), paint)
            }
            Icon.CLOSE -> {
                canvas.drawLine(x(6f), y(6f), x(18f), y(18f), paint)
                canvas.drawLine(x(18f), y(6f), x(6f), y(18f), paint)
            }
            Icon.PIN -> {
                path.moveTo(x(9f), y(3f))
                path.lineTo(x(15f), y(3f))
                path.lineTo(x(14f), y(10f))
                path.lineTo(x(18f), y(14f))
                path.lineTo(x(6f), y(14f))
                path.lineTo(x(10f), y(10f))
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawLine(x(12f), y(14f), x(12f), y(21f), paint)
            }
            Icon.KEYBOARD -> {
                rect.set(x(2.5f), y(6f), x(21.5f), y(18f))
                canvas.drawRoundRect(rect, 2f * s, 2f * s, paint)
                canvas.drawLine(x(7f), y(14.5f), x(17f), y(14.5f), paint)
                paint.style = Paint.Style.FILL
                for (i in 0..3) canvas.drawCircle(x(6.5f + i * 3.7f), y(10f), 0.9f * s, paint)
            }
        }
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    /** Largest square icon size that fits [w] x [h] with the given fraction. */
    fun fit(
        w: Float,
        h: Float,
        fraction: Float,
    ): Float = min(w, h) * fraction
}
