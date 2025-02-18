package com.npk.documents

import com.lowagie.text.*
import com.lowagie.text.alignment.HorizontalAlignment
import com.lowagie.text.html.WebColors
import com.npk.GeneratedDocumentPath
import com.npk.TestValueGenerator.CountryType
import com.npk.documents.DesignedTable.Companion.EmptyDesign
import com.npk.generatePdfDocument
import com.npk.random
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.awt.Point
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DesignedTableTest {

    @GeneratedDocumentPath
    private lateinit var generatedPdfPath: Path

    fun `provide generate char codes table`() = buildList {
        PdfDocumentFonts.register("/META-INF/resources/webjars/font-awesome/4.7.0/fonts/fontawesome-webfont.ttf")
        add(Arguments.of(PdfDocumentFonts.getUnicodeFont("FontAwesome", size = 21f), '\uf000'.code .. '\uf2e0'.code))
        add(Arguments.of(PdfDocumentFonts.getZapfDingbats(size = 22f), 32 .. 255))
        add(Arguments.of(PdfDocumentFonts.getSymbol(size = 22f), 32 .. 255))
    }

    @MethodSource("provide generate char codes table")
    @ParameterizedTest
    fun `generate char codes table`(font: Font, range: IntRange) = generatedPdfPath.generatePdfDocument("char_codes_${font.familyname}.pdf") { output ->
        BasePdfDocument(output).useDocument {
            documentInfo {
                title = "KOpenPdf"
                author = System.getProperty("user.name", "undefined")
                subject = "Char Codes Table ${font.familyname}"
                creator = DesignedTableTest::class.java.name
            }

            header("<color DarkCyan>${font.familyname}</color>")
            footer("Generated on ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now())}")

            body {
                table(16) {
                    it.offset = 0f
                    it.isTableFitsPage = true

                    useAllAvailableWidth()
                    designer(DesignedTable::BareboneStyle.name)

                    range.forEach { code ->
                        val phrase = Phrase("\n").apply {
                            add(Chunk(Char(code), font))
                            add(
                                Chunk(String.format("%n%X", code)).also {
                                    it.font.color = java.awt.Color.GRAY
                                    it.font.size = 8f
                                }
                            )
                        }
                        td(phrase) { it.setHorizontalAlignment(HorizontalAlignment.CENTER) }
                    }
                }
            }
        }
    }

    @Test
    fun `generate table with all design`() = generatedPdfPath.generatePdfDocument("all_design_table.pdf") { output ->
        BasePdfDocument(output).useDocument {
            documentInfo {
                title = "KOpenPdf"
                author = System.getProperty("user.name", "undefined")
                subject = "All Design Table"
                creator = DesignedTableTest::class.java.name
            }

            header("<color DarkCyan>All Design Table</color>")
            footer("Generated on ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now())}")

            body {
                sequence {
                    yield("NoStyle")
                    yield("BareboneStyle")
                    yieldAll((1 .. 21).map { "TableStyleLight$it" })
                    yieldAll(arrayOf(3, 5, 6, 10, 12, 13, 17, 19, 20).map { "TableStyleLight${it}_2010" })
                }.forEach { designerName ->
                    paragraph {
                        val title = when {
                            designerName == "NoStyle" -> "<b>DesignedTable.NoStyle</b> (By Default)"
                            designerName == "BareboneStyle" -> "<b>DesignedTable.BareboneStyle</b>"
                            else -> "<b>DesignedTableDesigners.$designerName</b>"
                        }
                        text(title)
                    }

                    table(5) {
                        it.offset = 0f
                        it.isTableFitsPage = true

                        useAllAvailableWidth()

                        designer(designerName)

                        columnHeaders("#", "ID", "Name", "Email", "Country")
                        columnWidths(5f, 20f, 15f, 30f, 30f)

                        (1 .. 16).forEach { index ->
                            td(index)
                            td(random { id().take(8) })
                            td(random { name() })
                            td(random { email() })
                            td(random { country(CountryType.Name) })
                        }
                    }

                    newLine()
                }
            }
        }
    }

    @Test
    fun `generate table with tuned design`() = generatedPdfPath.generatePdfDocument("tune_design_table.pdf") { output ->
        BasePdfDocument(output).useDocument {
            documentInfo {
                title = "KOpenPdf"
                author = System.getProperty("user.name", "undefined")
                subject = "Tune Design Table"
                creator = DesignedTableTest::class.java.name
            }

            footer("Generated on ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now())}")

            header("<color DarkCyan>Tune Design Table (LightDesignA1)</color>")
            body {
                sequence {
                    yieldAll((1 .. 7).map { "TableStyleLight$it" })
                    yieldAll(arrayOf(3, 5, 6).map { "TableStyleLight${it}_2010" })
                }.forEach { designerName ->
                    paragraph("<b>DesignedTableDesigners.$designerName</b>")

                    table(5) {
                        it.offset = 0f
                        it.isTableFitsPage = true

                        useAllAvailableWidth()

                        designer(designerName)

                        val fontColor = (design.borderColor ?: java.awt.Color.BLACK).formatAsHex()
                        fun colored(value: Any) = "<color $fontColor>$value</color>"

                        columnHeaders(
                            colored("<b>#</b>"),
                            colored("<b>ID</b>"),
                            colored("<b>Name</b>"),
                            colored("<b>Email</b>"),
                            colored("<b>Country</b>")
                        )
                        columnWidths(5f, 20f, 15f, 30f, 30f)

                        (1 .. 16).forEach { index ->
                            td(colored("$index")) { it.setHorizontalAlignment(HorizontalAlignment.RIGHT) }
                            td(colored(random { id().take(8) }))
                            td(colored(random { name() }))
                            td(colored(random { email() }))
                            td(colored(random { country(CountryType.Name) }))
                        }
                    }

                    newLine()
                }
            }

            header("<color DarkCyan>Tune Design Table (LightDesignA2)</color>")
            body {
                newPage()

                sequence {
                    yieldAll((8 .. 14).map { "TableStyleLight$it" })
                    yieldAll(arrayOf(10, 12, 13).map { "TableStyleLight${it}_2010" })
                }.forEach { designerName ->
                    paragraph("<b>DesignedTableDesigners.$designerName</b>")

                    table(5) {
                        it.offset = 0f
                        it.isTableFitsPage = true

                        useAllAvailableWidth()

                        designer(designerName)

                        fun colored(value: Any) = "<color White>$value</color>"

                        columnHeaders(
                            colored("<b>#</b>"),
                            colored("<b>ID</b>"),
                            colored("<b>Name</b>"),
                            colored("<b>Email</b>"),
                            colored("<b>Country</b>")
                        )
                        columnWidths(5f, 20f, 15f, 30f, 30f)

                        (1 .. 16).forEach { index ->
                            td(index)
                            td(random { id().take(8) })
                            td(random { name() })
                            td(random { email() })
                            td(random { country(CountryType.Name) })
                        }
                    }

                    newLine()
                }
            }

            header("<color DarkCyan>Tune Design Table (LightDesignA3)</color>")
            body {
                newPage()

                sequence {
                    yieldAll((15 .. 21).map { "TableStyleLight$it" })
                    yieldAll(arrayOf(17, 19, 20).map { "TableStyleLight${it}_2010" })
                }.forEach { designerName ->
                    paragraph("<b>DesignedTableDesigners.$designerName</b>")

                    table(5) {
                        it.offset = 0f
                        it.isTableFitsPage = true

                        useAllAvailableWidth()

                        designer(designerName)

                        columnHeaders("<b>#</b>", "<b>ID</b>", "<b>Name</b>", "<b>Email</b>", "<b>Country</b>")
                        columnWidths(5f, 20f, 15f, 30f, 30f)

                        (1 .. 16).forEach { index ->
                            td(index)
                            td(random { id().take(8) })
                            td(random { name() })
                            td(random { email() })
                            td(random { country(CountryType.Name) })
                        }
                    }

                    newLine()
                }
            }
        }
    }

    @Test
    fun `generate table with custom design`() = generatedPdfPath.generatePdfDocument("custom_design_table.pdf") { output ->
        BasePdfDocument(output).useDocument {
            documentInfo {
                title = "KOpenPdf"
                author = System.getProperty("user.name", "undefined")
                subject = "Custom Design Table"
                creator = DesignedTableTest::class.java.name
            }

            header("<color DarkCyan>Custom Design Table</color>")
            footer("Generated on ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now())}")

            body {
                table(5) {
                    useAllAvailableWidth()

                    designer {
                        border = Rectangle.BOX
                        borderColor = java.awt.Color.BLACK
                        headerBackgroundColor = WebColors.getRGBColor("LightSteelBlue")
                        headerHorizontalAlignment = HorizontalAlignment.CENTER
                        showColumnStripes = true
                        showRowStripes = true
                    }

                    columnHeaders("<b>#</b>", "<b>ID</b>", "<b>Name</b>", "<b>Email</b>", "<b>Country</b>")
                    columnWidths(5f, 20f, 15f, 30f, 30f)

                    (1 .. 16).forEach { index ->
                        td(index)
                        td(random { id().take(8) })
                        td(random { name() })
                        td(random { email() })
                        td(random { country(CountryType.Name) })
                    }
                }
            }
        }
    }

    @Test
    fun `generate table with custom designer`() = generatedPdfPath.generatePdfDocument("custom_designer_table.pdf") { output ->
        BasePdfDocument(output).useDocument {
            documentInfo {
                title = "KOpenPdf"
                author = System.getProperty("user.name", "undefined")
                subject = "Custom Designer Table"
                creator = DesignedTableTest::class.java.name
            }

            header("<color DarkCyan>Custom Designer Table</color>")
            footer("Generated on ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now())}")

            body {

                val customDesigner = object : DesignedTable.Designer {

                    override val design: DesignedTable.Design = EmptyDesign()

                    override fun paintTable(table: Table) {
                        table.isUseVariableBorders = true
                        table.borderWidthTop = 1f
                        table.borderWidthBottom = 1f
                        table.borderColor = WebColors.getRGBColor("CornflowerBlue")
                    }

                    override fun paintHeaderCell(cell: Cell, location: Point) {
                        cell.borderColor = WebColors.getRGBColor("CornflowerBlue")
                        cell.isUseVariableBorders = true
                        cell.setHorizontalAlignment(HorizontalAlignment.CENTER)
                        if (location.y > 0) {
                            cell.borderWidthLeft = 0.5f
                        }
                        cell.borderWidthBottom = 1f
                    }

                    override fun paintCell(cell: Cell, location: Point) {
                        cell.borderColor = WebColors.getRGBColor("SkyBlue")
                        cell.isUseVariableBorders = true
                        if (location.y > 0) {
                            cell.borderWidthLeft = 0.5f
                        }
                        cell.borderWidthBottom = 0.5f
                    }

                    override fun copy(design: DesignedTable.Design): DesignedTable.Designer {
                        throw UnsupportedOperationException()
                    }

                }

                table(6) {
                    it.offset = 0f
                    it.isTableFitsPage = true

                    useAllAvailableWidth()

                    designer(customDesigner)

                    columnHeaders("<b>#</b>", "<b>ID</b>", "<b>Name</b>", "<b>Email</b>", "<b>Country</b>", "<b>%</b>") {
                        it.setHorizontalAlignment(HorizontalAlignment.CENTER)
                    }
                    columnWidths(5f, 15f, 15f, 30f, 30f, 5f)

                    (1 .. 16).forEach { index ->
                        td(index)
                        td(random { id().take(8) })
                        td(random { name() })
                        td(random { email() })
                        td(random { country(CountryType.Name) })

                        val percent = random { int(1 .. 100) }
                        val percentText = when (percent) {
                            in 1 .. 20 -> "<color DarkRed>$percent</color>"
                            in 21 .. 40 -> "<color DarkOrange>$percent</color>"
                            in 41 .. 60 -> "<color DarkGreen>$percent</color>"
                            in 61 .. 80 -> "<color DarkCyan>$percent</color>"
                            in 81 .. 100 -> "<color DarkBlue>$percent</color>"
                            else -> "$percent"
                        }
                        td(percentText) {
                            it.setHorizontalAlignment(HorizontalAlignment.RIGHT)
                        }
                    }
                }
            }
        }
    }

}
