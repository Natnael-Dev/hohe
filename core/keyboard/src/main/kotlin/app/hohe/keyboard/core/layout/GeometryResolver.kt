package app.hohe.keyboard.core.layout

import java.util.concurrent.ConcurrentHashMap

/**
 * Parameters controlling geometry layout calculation.
 */
data class GeometryParams(
    val heightMultiplier: Float = 1.0f,
    val swDp: Int = 360,
    val bottomSpaceDp: Float = 0f,
)

/**
 * Rectangle coordinates in floating-point pixels.
 */
data class KeyRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Resolved geometric layout for a single key.
 */
data class ResolvedKey(
    val key: Key,
    val drawRect: KeyRect,
    val hitRect: KeyRect,
    val row: Int,
    val col: Int,
)

/**
 * Complete resolved geometry for a keyboard layout at a specific display dimension.
 */
data class ResolvedKeyboard(
    val layout: Layout,
    val widthPx: Int,
    val heightPx: Float,
    val keys: List<ResolvedKey>,
    val rows: List<List<ResolvedKey>>,
)

/**
 * Resolves keyboard layouts into pixel rectangles for drawing and hit testing.
 */
object GeometryResolver {
    private data class CacheKey(
        val layout: Layout,
        val widthPx: Int,
        val density: Float,
        val params: GeometryParams,
    )

    private val cache = ConcurrentHashMap<CacheKey, ResolvedKeyboard>()

    /**
     * Resolves the given layout into pixel coordinates. Results are cached.
     */
    fun resolve(
        layout: Layout,
        widthPx: Int,
        density: Float,
        params: GeometryParams = GeometryParams(),
    ): ResolvedKeyboard {
        require(widthPx > 0) { "widthPx must be positive, got $widthPx" }
        require(density > 0f) { "density must be positive, got $density" }

        val cacheKey = CacheKey(layout, widthPx, density, params)
        return cache.computeIfAbsent(cacheKey) { computeResolvedKeyboard(layout, widthPx, density, params) }
    }

    /**
     * Clears the resolver cache.
     */
    fun clearCache() {
        cache.clear()
    }

    private fun computeResolvedKeyboard(
        layout: Layout,
        widthPx: Int,
        density: Float,
        params: GeometryParams,
    ): ResolvedKeyboard {
        val rowHeightDp =
            when {
                params.swDp >= 600 -> 68.2f
                params.swDp >= 400 -> 59.3f
                params.swDp >= 350 -> 50.5f
                else -> 48.0f
            }

        val topPaddingPx = layout.params.topPaddingDp * density
        val bottomPaddingPx = (layout.params.bottomPaddingDp + params.bottomSpaceDp) * density

        val verticalGapPx = layout.params.verticalGapDp * density
        val halfVGap = verticalGapPx / 2f

        val horizontalGapPx = (layout.params.horizontalGapPct / 100f) * widthPx
        val halfHGap = horizontalGapPx / 2f

        var currentRowTop = topPaddingPx
        val allResolvedKeys = mutableListOf<ResolvedKey>()
        val resolvedRows = mutableListOf<List<ResolvedKey>>()

        layout.rows.forEachIndexed { rowIndex, row ->
            val rowPitchPx = rowHeightDp * params.heightMultiplier * row.heightFactor * density
            val rowBottom = currentRowTop + rowPitchPx

            val rowKeys = mutableListOf<ResolvedKey>()
            var currentX = (row.xOffsetPct / 100f) * widthPx

            row.keys.forEachIndexed { colIndex, key ->
                val nominalLeft =
                    if (key.xPct != null) {
                        (key.xPct / 100f) * widthPx
                    } else {
                        currentX
                    }
                val nominalWidth = (key.widthPct / 100f) * widthPx
                val nominalRight = nominalLeft + nominalWidth

                val hitLeft =
                    if (colIndex == 0) {
                        nominalLeft
                    } else {
                        rowKeys[colIndex - 1].hitRect.right
                    }

                val hitRight =
                    if (colIndex + 1 < row.keys.size && row.keys[colIndex + 1].xPct != null) {
                        (row.keys[colIndex + 1].xPct!! / 100f) * widthPx
                    } else {
                        nominalRight
                    }

                val hitRect =
                    KeyRect(
                        left = hitLeft,
                        top = currentRowTop,
                        right = hitRight,
                        bottom = rowBottom,
                    )

                val drawRect =
                    KeyRect(
                        left = nominalLeft + halfHGap,
                        top = currentRowTop + halfVGap,
                        right = nominalRight - halfHGap,
                        bottom = rowBottom - halfVGap,
                    )

                val resolvedKey =
                    ResolvedKey(
                        key = key,
                        drawRect = drawRect,
                        hitRect = hitRect,
                        row = rowIndex,
                        col = colIndex,
                    )
                rowKeys.add(resolvedKey)
                allResolvedKeys.add(resolvedKey)

                currentX = nominalRight
            }

            resolvedRows.add(rowKeys)
            currentRowTop = rowBottom
        }

        val totalHeightPx = currentRowTop + bottomPaddingPx

        return ResolvedKeyboard(
            layout = layout,
            widthPx = widthPx,
            heightPx = totalHeightPx,
            keys = allResolvedKeys,
            rows = resolvedRows,
        )
    }
}
