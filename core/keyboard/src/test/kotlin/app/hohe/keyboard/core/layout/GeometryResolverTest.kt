package app.hohe.keyboard.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests for GeometryResolver fulfilling KB-010, KB-011, caching, padding, and edge cases.
 */
class GeometryResolverTest {
    private val qwerty = Layouts.qwerty()

    /**
     * AC-2 / KB-010: 720 px, density 2.0, swDp 360, x1.0 -> letter 9.9 %p, shift 14.8, space 39.5
     * (+-0.5 %p of width); row pitch 50.5 dp +-1.
     */
    @Test
    fun testKb010() {
        val widthPx = 720
        val density = 2.0f
        val params =
            GeometryParams(
                heightMultiplier = 1.0f,
                swDp = 360,
                bottomSpaceDp = 0f,
            )

        val resolved = GeometryResolver.resolve(qwerty, widthPx, density, params)

        // Row pitch in dp = rowPitchPx / density
        // For swDp 360, row height is 50.5 dp.
        val row0Keys = resolved.rows[0]
        val row0PitchPx = row0Keys[0].hitRect.height
        val row0PitchDp = row0PitchPx / density
        assertEquals(50.5f, row0PitchDp, 1.0f)

        // Check key widths as %p of keyboard width
        // Letter 'q' (row 0, col 0)
        val letterQ = row0Keys[0]
        val letterWidthPct = (letterQ.hitRect.width / widthPx) * 100f
        assertEquals(9.9f, letterWidthPct, 0.5f)

        // Shift key (row 2, col 0)
        val shiftKey = resolved.rows[2][0]
        val shiftWidthPct = (shiftKey.hitRect.width / widthPx) * 100f
        assertEquals(14.8f, shiftWidthPct, 0.5f)

        // Space key (row 3, col 3)
        val spaceKey = resolved.rows[3][3]
        val spaceWidthPct = (spaceKey.hitRect.width / widthPx) * 100f
        assertEquals(39.5f, spaceWidthPct, 0.5f)
    }

    /**
     * AC-3 / KB-011: 1080 and 1440 px -> widths proportional (hitRect.width/widthPx equal within 1e-4);
     * per row, adjacent hit rects touch (gap <= 1 px) and the last key's right edge <= widthPx.
     * Verified over QWERTY, SYMBOLS, and NUMBER (F-005-01).
     */
    @Test
    fun testKb011() {
        val layouts = listOf(qwerty, Layouts.symbols(), Layouts.number())
        layouts.forEach { layout ->
            val resolved1080 = GeometryResolver.resolve(layout, 1080, 1080 / 360f, GeometryParams(swDp = 360))
            val resolved1440 = GeometryResolver.resolve(layout, 1440, 1440 / 360f, GeometryParams(swDp = 360))

            assertEquals(resolved1080.rows.size, resolved1440.rows.size)
            for (r in resolved1080.rows.indices) {
                val row1080 = resolved1080.rows[r]
                val row1440 = resolved1440.rows[r]
                assertEquals(row1080.size, row1440.size)

                for (c in row1080.indices) {
                    val prop1080 = row1080[c].hitRect.width / 1080f
                    val prop1440 = row1440[c].hitRect.width / 1440f
                    assertEquals(
                        "Key [r=$r, c=$c] proportional width should match between 1080 and 1440 px for layout ${layout.id}",
                        prop1080,
                        prop1440,
                        0.0001f,
                    )
                }
            }

            listOf(1080, 1440).forEach { widthPx ->
                val density = widthPx / 360f
                val params = GeometryParams(heightMultiplier = 1.0f, swDp = 360)
                val resolved = GeometryResolver.resolve(layout, widthPx, density, params)

                resolved.rows.forEach { row ->
                    // Per row, adjacent hit rects touch (gap <= 1 px)
                    for (i in 0 until row.size - 1) {
                        val currentKey = row[i]
                        val nextKey = row[i + 1]
                        val gap = kotlin.math.abs(nextKey.hitRect.left - currentKey.hitRect.right)
                        assertTrue(
                            "Gap between adjacent hit rects at width $widthPx should be <= 1 px, was $gap",
                            gap <= 1.0f,
                        )
                    }

                    // Last key's right edge <= widthPx
                    val lastKey = row.last()
                    assertTrue(
                        "Last key right edge ${lastKey.hitRect.right} exceeds widthPx $widthPx",
                        lastKey.hitRect.right <= widthPx.toFloat() + 0.01f,
                    )
                }
            }
        }
    }

    @Test
    fun testIllegalArgumentOnInvalidInputs() {
        try {
            GeometryResolver.resolve(qwerty, 0, 2.0f)
            fail("Expected IllegalArgumentException for widthPx <= 0")
        } catch (_: IllegalArgumentException) {
        }

        try {
            GeometryResolver.resolve(qwerty, -100, 2.0f)
            fail("Expected IllegalArgumentException for negative widthPx")
        } catch (_: IllegalArgumentException) {
        }

        try {
            GeometryResolver.resolve(qwerty, 720, 0f)
            fail("Expected IllegalArgumentException for density <= 0")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun testResolverCache() {
        val params = GeometryParams(heightMultiplier = 1.0f, swDp = 360)
        val first = GeometryResolver.resolve(qwerty, 720, 2.0f, params)
        val second = GeometryResolver.resolve(qwerty, 720, 2.0f, params)
        assertSame("GeometryResolver must return cached instance for identical inputs", first, second)
    }

    @Test
    fun testHeightMultipliers() {
        val density = 2.0f
        val m09 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(heightMultiplier = 0.9f))
        val m10 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(heightMultiplier = 1.0f))
        val m112 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(heightMultiplier = 1.12f))
        val m123 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(heightMultiplier = 1.23f))

        val pitch09 = m09.rows[0][0].hitRect.height
        val pitch10 = m10.rows[0][0].hitRect.height
        val pitch112 = m112.rows[0][0].hitRect.height
        val pitch123 = m123.rows[0][0].hitRect.height

        assertEquals(pitch10 * 0.9f, pitch09, 0.1f)
        assertEquals(pitch10 * 1.12f, pitch112, 0.1f)
        assertEquals(pitch10 * 1.23f, pitch123, 0.1f)
    }

    @Test
    fun testSwDpBreakpoints() {
        val density = 1.0f
        val r320 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(swDp = 320))
        assertEquals(48.0f, r320.rows[0][0].hitRect.height, 0.1f)

        val r360 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(swDp = 360))
        assertEquals(50.5f, r360.rows[0][0].hitRect.height, 0.1f)

        val r400 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(swDp = 400))
        assertEquals(59.3f, r400.rows[0][0].hitRect.height, 0.1f)

        val r600 = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(swDp = 600))
        assertEquals(68.2f, r600.rows[0][0].hitRect.height, 0.1f)
    }

    @Test
    fun testDrawRectInsetFromHitRect() {
        val widthPx = 720
        val density = 2.0f
        val resolved = GeometryResolver.resolve(qwerty, widthPx, density, GeometryParams())

        val key = resolved.rows[0][0]
        val hit = key.hitRect
        val draw = key.drawRect

        // Horizontal gap = 1.353 %p of 720 = 9.7416 px. Half = 4.8708 px.
        val expectedHInset = (1.353f / 100f * widthPx) / 2f
        assertEquals(hit.left + expectedHInset, draw.left, 0.05f)
        assertEquals(hit.right - expectedHInset, draw.right, 0.05f)

        // Vertical gap = 9 dp * 2.0 = 18 px. Half = 9 px.
        val expectedVInset = (9f * density) / 2f
        assertEquals(hit.top + expectedVInset, draw.top, 0.05f)
        assertEquals(hit.bottom - expectedVInset, draw.bottom, 0.05f)
    }

    @Test
    fun testBottomSpaceDpIncreasesTotalHeight() {
        val density = 2.0f
        val base = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(bottomSpaceDp = 0f))
        val extra = GeometryResolver.resolve(qwerty, 720, density, GeometryParams(bottomSpaceDp = 16f))

        val expectedDiffPx = 16f * density
        assertEquals(expectedDiffPx, extra.heightPx - base.heightPx, 0.1f)
    }
}
