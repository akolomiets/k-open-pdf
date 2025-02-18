package com.npk.documents

import com.lowagie.text.*
import com.lowagie.text.alignment.HorizontalAlignment
import com.lowagie.text.html.FontSize
import com.lowagie.text.pdf.draw.DottedLineSeparator
import com.lowagie.text.pdf.draw.LineSeparator
import com.npk.documents.KOpenPdfConstant.EMPTY_SYMBOL
import com.npk.documents.KOpenPdfConstant.HYPHEN_SYMBOL
import java.net.URL
import java.text.DecimalFormat
import java.text.NumberFormat

fun PdfBodyElementBuilder.newLine() = add(Chunk.NEWLINE)
fun PdfBodyElementBuilder.newPage() = add(Chunk.NEXTPAGE)

//region Headings

fun PdfBodyElementBuilder.h1(text: String) = hx(text, FontSize.XX_LARGE)
fun PdfBodyElementBuilder.h2(text: String) = hx(text, FontSize.X_LARGE)
fun PdfBodyElementBuilder.h3(text: String) = hx(text, FontSize.LARGE)
fun PdfBodyElementBuilder.h4(text: String) = hx(text, FontSize.MEDIUM)
fun PdfBodyElementBuilder.h5(text: String) = hx(text, FontSize.SMALL)
fun PdfBodyElementBuilder.h6(text: String) = hx(text, FontSize.X_SMALL)

private fun PdfBodyElementBuilder.hx(text: String, fontSize: FontSize) = paragraph {
    it.font = context.getFont(
        PdfDocumentFonts.DEFAULT_SIZE * fontSize.scale,
        context.fontColor,
        Font.BOLD
    )
    it.leading = calculateLeading(it.font.size)
    text(text)
}

//endregion

//region Paragraph

fun PdfBodyParagraphBuilder.text(vararg texts: String?) = text(texts.asIterable())

fun PdfBodyParagraphBuilder.text(lines: Iterable<String?>) {
    lines.forEach { line -> text(line) }
}

object BreakingLine {

    enum class LineStyle {
        Solid,
        Dotted
    }

    class BreakingLineConfigurator {
        var style: LineStyle = LineStyle.Solid
        var width: Float = 100f
        var height: Float = 0.8f
        var color: java.awt.Color? = null
        val align: Int = Element.ALIGN_CENTER
    }

    fun PdfBodyElementBuilder.breakingLine(block: BreakingLineConfigurator.() -> Unit = {}) =
        BreakingLineConfigurator()
            .apply(block)
            .also {
                val separator = when (it.style) {
                    LineStyle.Solid -> LineSeparator()
                    LineStyle.Dotted -> DottedLineSeparator().also { it.gap = 3f }
                }
                with(separator) {
                    lineWidth = it.height
                    percentage = it.width
                    lineColor = it.color
                    alignment = it.align
                }
                add(Paragraph(Chunk(separator)))
            }

}

//endregion

//region Image

fun PdfBodyElementBuilder.image(classpathResName: String, block: PdfBodyImageBuilder.(Image) -> Unit = {}) =
    image(Image.getInstanceFromClasspath(classpathResName), block)

fun PdfBodyElementBuilder.image(resourceUrl: URL, block: PdfBodyImageBuilder.(Image) -> Unit = {}) =
    image(Image.getInstance(resourceUrl), block)

//endregion

//region Table

fun PdfBodyElementBuilder.table(firstColumnWidth: Float, vararg restColumnWidths: Float, block: PdfBodyTableBuilder.(Table) -> Unit) {
    val widths = floatArrayOf(firstColumnWidth, *restColumnWidths)
    table(widths.size) { table ->
        columnWidths(*widths)
        apply { block(table) }
    }
}

fun <T : Number> PdfBodyTableBuilder.td(value: T?, decimalFormatPattern: String, block: (Cell) -> Unit = {}) =
    td(value, DecimalFormat(decimalFormatPattern), block)

fun <T : Number> PdfBodyTableBuilder.td(value: T?, numberFormat: NumberFormat = NumberFormat.getInstance(), block: (Cell) -> Unit = {}) {
    val text = value?.let { numberFormat.format(it) }
    td("$text$EMPTY_SYMBOL") {
        it.setHorizontalAlignment(HorizontalAlignment.RIGHT)
        block(it)
    }
}

fun PdfBodyTableBuilder.noData(text: String = HYPHEN_SYMBOL, block: (Cell) -> Unit = {}) =
    td(text) {
        it.colspan = columnSize
        it.setHorizontalAlignment(HorizontalAlignment.CENTER)
        block(it)
    }

//endregion

//region Typed Blockquote

enum class BlockquoteType(val icon: String, val caption: String, val color: java.awt.Color) {
    Note("fa-info-circle", "Note", java.awt.Color(13, 103, 221)),
    Tip("fa-lightbulb-o", "Tip", java.awt.Color(26, 127, 52)),
    Important("fa-commenting", "Important", java.awt.Color(128, 79, 225)),
    Warning("fa-warning", "Warning", java.awt.Color(155, 102, 0)),
    Caution("fa-exclamation-circle", "Caution", java.awt.Color(207, 44, 44))
}

fun PdfBodyElementBuilder.blockquote(type: BlockquoteType, block: PdfBodyElementBuilder.(Table) -> Unit) =
    blockquote { element ->
        stripColor = type.color
        paragraph {
            it.font.color = type.color
            fa(type.icon)
            add(Chunk(" "))
            text(type.caption)
        }
        apply { block(element) }
    }

//endregion
