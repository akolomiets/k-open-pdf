package com.npk.documents

import com.lowagie.text.Font
import com.lowagie.text.Paragraph
import com.lowagie.text.html.FontSize
import com.lowagie.text.html.HtmlTags
import com.lowagie.text.html.WebColors
import com.npk.GeneratedDocumentPath
import com.npk.documents.BreakingLine.breakingLine
import com.npk.generatePdfDocument
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.math.round


internal class TagPhraseTest {

    @GeneratedDocumentPath
    private lateinit var generatedPdfPath: Path

    @Test
    fun `generate a tag phrase`() = generatedPdfPath.generatePdfDocument("tag_phrase.pdf") { output ->
        BasePdfDocument(output).useDocument {
            body {
                add(tagPhrase("Simple text line\n"))

                newLine()
                newLine()
                add(tagPhrase("<b>Bold</b>\n"))
                add(tagPhrase("<i>Italic</i>\n"))
                add(tagPhrase("<u>Underline</u>\n"))
                add(tagPhrase("<s>Strikethru</s>\n"))
                add(tagPhrase("<b><i><u><s>Bold + Italic + Underline + Strikethru</s></u></i></b>\n"))

                newLine()
                newLine()
                add(tagPhrase("Color #FF358A - <color #FF358A>RED</color>\n"))
                add(tagPhrase("Color #56A76B - <color #56A76B>GREEN</color>\n"))
                add(tagPhrase("Color rgb(53, 116, 240) - <color rgb(53, 116, 240)>BLUE</color>\n"))

                newLine()
                add(tagPhrase("Color red - <color red>java.awt.Color.RED</color>\n"))
                add(tagPhrase("Color Green - <color Green>java.awt.Color.GREEN</color>\n"))
                add(tagPhrase("Color BLUE - <color BLUE>java.awt.Color.BLUE</color>\n"))

                newLine()
                add(tagPhrase("Nested colors: <color #FF358A>RED Chunk { <color #56A76B>GREEN Chunk { <color #3574F0>BLUE Chunk { abc } </color> } </color> } </color> plain text\n"))

                newLine()
                newLine()
                add(Paragraph(
                    tagPhrase(
                        "Default Font Size = ${PdfDocumentFonts.DEFAULT_SIZE}" +
                        "  <size ${PdfDocumentFonts.DEFAULT_SIZE + 2}>Font size = ${PdfDocumentFonts.DEFAULT_SIZE + 2}" +
                        "  <size ${PdfDocumentFonts.DEFAULT_SIZE - 2}>Font size = ${PdfDocumentFonts.DEFAULT_SIZE - 2}"
                    )
                ))
                add(Paragraph(
                    tagPhrase(
                        "Default Font Size = ${PdfDocumentFonts.DEFAULT_SIZE}" +
                        "  <size 120%>Font size = 120%" +
                        "  <size 80%>Font size = 80%"
                    )
                ))
                add(Paragraph(tagPhrase("<size medium>Medium <size small>Small <size x-small>X-Small <size xx-small>XX-Small </size>X-Small </size>Small </size>Medium</size>")))
                add(Paragraph(
                    tagPhrase(
                        "<size medium>Medium <size large>Large <size x-large>X-Large <size xx-large>XX-Large <size xxx-large>XXX-Large " +
                        "</size>XX-Large </size>X-Large </size>Large </size>Medium</size>"
                    )
                ))
                add(Paragraph(tagPhrase("<color IndianRed>Default font size text (Added to set leading to default)</color>")))


                TagPhrase.registerHandler(HtmlTags.PRE) { token, phrase ->
                    when {
                        token.equals("<${HtmlTags.PRE}>", ignoreCase = true) -> {
                            if (phrase.getAttribute<Font>("font") == null) {
                                phrase.setAttribute("font", phrase.font)
                                phrase.font = PdfDocumentFonts.getCourier(phrase.font.size, phrase.font.color)
                            }
                            true
                        }
                        token.equals("</${HtmlTags.PRE}>", ignoreCase = true) -> {
                            phrase.getAttribute<Font>("font")?.let {
                                phrase.font = PdfDocumentFonts.getHelvetica(it.size, it.color)
                            }
                            true
                        }
                        else -> false
                    }
                }
                newLine()
                newLine()
                add(tagPhrase("Register custom tag handler to change the font to <size large><pre>monospace</pre></size> name"))

                breakingLine { color = WebColors.getRGBColor("DarkCyan") }

                h1("Heading 1 (${round(PdfDocumentFonts.DEFAULT_SIZE * FontSize.XX_LARGE.scale)})")
                h2("Heading 2 (${round(PdfDocumentFonts.DEFAULT_SIZE * FontSize.X_LARGE.scale)})")
                h3("Heading 3 (${round(PdfDocumentFonts.DEFAULT_SIZE * FontSize.LARGE.scale)})")
                h4("Heading 4 (${round(PdfDocumentFonts.DEFAULT_SIZE * FontSize.MEDIUM.scale)}) Default size")
                h5("Heading 5 (${round(PdfDocumentFonts.DEFAULT_SIZE * FontSize.SMALL.scale)})")
                h6("Heading 6 (${round(PdfDocumentFonts.DEFAULT_SIZE * FontSize.X_SMALL.scale)})")
            }
        }
    }

    private fun tagPhrase(text: String) = TagPhrase(calculateLeading(PdfDocumentFonts.DEFAULT_SIZE), text, PdfDocumentFonts.getHelvetica())

}