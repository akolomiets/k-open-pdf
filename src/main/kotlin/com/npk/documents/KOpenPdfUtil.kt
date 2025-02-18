package com.npk.documents

import com.lowagie.text.Chunk
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.Phrase


inline fun <T : AbstractPdfDocument> T.useDocument(block: T.() -> Unit) = this.use(block)

fun calculateLeading(fontSize: Float): Float = fontSize * 1.4f

fun Phrase.setTextRise(rise: Float): Phrase = apply {
    if (rise != 0.0f) {
        this.forEach { element ->
            if (element.type() == Element.CHUNK) {
                (element as Chunk).textRise = rise
            }
        }
    }
}

fun java.awt.Color.formatAsHex() = "#%02X%02X%02X".format(red, green, blue)
fun java.awt.Color.formatAsRgb() = "rgb(%d, %d, %d)".format(red, green, blue)

inline fun Element.forEachChunks(action: (Chunk) -> Unit) = chunks.forEach { element -> (element as? Chunk)?.apply(action) }

fun String.wordWrap(width: Int): String =
    if (isNotBlank()) {
        buildString {
            this@wordWrap.lines().forEachIndexed { index, line ->
                if (index > 0) {
                    appendLine()
                }
                val words = line.split(" ")
                append(words[0])
                var spaceLeft = width - words[0].length
                words.drop(1).forEach { word ->
                    val len = word.length
                    if (len + 1 > spaceLeft) {
                        appendLine()
                        spaceLeft = width - len
                    } else {
                        append(" ")
                        spaceLeft -= (len + 1)
                    }
                    append(word)
                }
            }
        }
    } else {
        this
    }

fun String.wordWrapBy(width: Int, vararg delimiters: Char = charArrayOf(' ')): String =
    if (isNotBlank()) {
        val parts = delimiters.concatToString()
            .let { Regex("(?=[$it])|(?<=[$it])") }
            .split(this)

        buildString {
            append(parts[0])
            var spaceLeft = width - parts[0].length
            parts.drop(1).forEach { part ->
                if (part == "\n") {
                    spaceLeft = width
                } else {
                    val len = part.length
                    if (len + 1 > spaceLeft) {
                        append("\n")
                        spaceLeft = width - len
                    } else {
                        spaceLeft -= len
                    }
                }
                append(part)
            }
        }
    } else {
        this
    }

fun String.toAwtImage(
    font: java.awt.Font = java.awt.Font(FontFactory.HELVETICA, java.awt.Font.PLAIN, PdfDocumentFonts.DEFAULT_SIZE.toInt()),
    color: java.awt.Color = java.awt.Color.BLACK
): java.awt.image.BufferedImage {

    fun calculateMetrics(): Pair<Int, Int> {
        val g2d = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            .createGraphics()
            .also { it.font = font }
        try {
            return g2d.fontMetrics.stringWidth(this) to g2d.fontMetrics.height
        } finally {
            g2d.dispose()
        }
    }

    val (width, height) = calculateMetrics()
    val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g2d = image.createGraphics()
        .also {
            it.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION, java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            it.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            it.setRenderingHint(java.awt.RenderingHints.KEY_COLOR_RENDERING, java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            it.setRenderingHint(java.awt.RenderingHints.KEY_DITHERING, java.awt.RenderingHints.VALUE_DITHER_ENABLE);
            it.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            it.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            it.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            it.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);
        }
        .also {
            it.font = font
            it.color = color
        }

    g2d.drawString(this, 0, g2d.fontMetrics.ascent)
    g2d.dispose()

    return image
}
