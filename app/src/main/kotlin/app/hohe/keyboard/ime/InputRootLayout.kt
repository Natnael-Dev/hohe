package app.hohe.keyboard.ime

import android.content.Context
import android.view.View
import android.widget.FrameLayout

/**
 * Root of the input view: the [content] column decides the height, and the [overlay] (previews,
 * long-press panel) is laid over it at exactly that size. A plain FrameLayout would let the
 * MATCH_PARENT overlay stretch the keyboard to the full screen height.
 */
class InputRootLayout(
    context: Context,
    private val content: View,
    private val overlay: View,
) : FrameLayout(context) {
    init {
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(overlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        clipChildren = false
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val exactWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        content.measure(exactWidth, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val height = content.measuredHeight
        overlay.measure(exactWidth, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }
}
