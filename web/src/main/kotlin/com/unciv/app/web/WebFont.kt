package com.unciv.app.web

import com.badlogic.gdx.graphics.Pixmap
import com.unciv.ui.components.fonts.FontFamilyData
import com.unciv.ui.components.fonts.FontImplementation
import com.unciv.ui.components.fonts.FontMetricsCommon
import com.unciv.ui.components.fonts.Fonts

class WebFont : FontImplementation {
    private var fontSize = Fonts.ORIGINAL_FONT_SIZE.toInt()
    private val fontFamily = "Arial, Helvetica, sans-serif"
    private lateinit var metrics: FontMetricsCommon

    override fun setFontFamily(fontFamilyData: FontFamilyData, size: Int) {
        require(size > 0) { "Font size must be positive" }
        fontSize = size
        // The requested family is platform-specific; use the browser's standard sans stack.
        val measured = WebFontRasterizer.measureMetrics(fontSize, fontFamily)
        check(measured.size == 4) { "Canvas font metrics returned ${measured.size} values; expected 4" }
        metrics = FontMetricsCommon(
            ascent = measured[0],
            descent = measured[1],
            height = measured[2],
            leading = measured[3],
        )
        check(metrics.ascent.isFinite() && metrics.descent.isFinite() &&
            metrics.height.isFinite() && metrics.leading.isFinite() && metrics.height > 0f
        ) { "Canvas font metrics were invalid: ${measured.contentToString()}" }
    }

    override fun getFontSize(): Int = fontSize

    override fun getCharPixmap(symbolString: String): Pixmap {
        val rasterized = WebFontRasterizer.rasterizeGlyphAligned(
            symbolString,
            fontSize,
            fontFamily,
            metrics.ascent,
            metrics.height,
            metrics.leading,
        )
        check(rasterized.size >= 2) { "Canvas glyph rasterizer returned no dimensions for '$symbolString'" }
        val width = rasterized[0]
        val height = rasterized[1]
        check(width > 0 && height > 0) { "Canvas glyph rasterizer returned invalid dimensions ${width}x$height for '$symbolString'" }
        val expectedLength = 2L + width.toLong() * height.toLong() * 4L
        check(expectedLength == rasterized.size.toLong()) {
            "Canvas glyph rasterizer returned ${rasterized.size} values for ${width}x$height glyph '$symbolString'"
        }
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        try {
            var src = 2
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val r = rasterized[src++] and 0xFF
                    val g = rasterized[src++] and 0xFF
                    val b = rasterized[src++] and 0xFF
                    val a = rasterized[src++] and 0xFF
                    pixmap.drawPixel(x, y, (r shl 24) or (g shl 16) or (b shl 8) or a)
                }
            }
        } catch (ex: Exception) {
            pixmap.dispose()
            throw ex
        }
        return pixmap
    }

    override fun getSystemFonts(): Sequence<FontFamilyData> = sequenceOf(FontFamilyData.default)

    override fun getMetrics() = metrics
}
