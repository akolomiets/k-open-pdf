package com.npk.documents

import com.lowagie.text.Chapter
import com.lowagie.text.Chunk
import com.lowagie.text.alignment.HorizontalAlignment
import com.lowagie.text.alignment.VerticalAlignment
import com.npk.GeneratedDocumentPath
import com.npk.TestValueGenerator.CountryType
import com.npk.documents.AbstractPdfDocument.PdfDocumentEncryption.EncryptionType
import com.npk.documents.AbstractPdfDocument.PdfDocumentViewPreferences.PageLayout
import com.npk.documents.AbstractPdfDocument.PdfDocumentViewPreferences.PageMode
import com.npk.documents.KOpenPdfConstant.EMPTY_SYMBOL
import com.npk.documents.KOpenPdfConstant.HYPHEN_SYMBOL
import com.npk.documents.PdfBodyBuilder.ChapterType
import com.npk.documents.PdfBodyListBuilder.ListType
import com.npk.documents.PdfBodyParagraphBuilder.TextAlignment
import com.npk.generatePdfDocument
import com.npk.random
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class KOpenPdfShowcaseTest {

    @GeneratedDocumentPath
    private lateinit var generatedPdfPath: Path

    @Test
    fun `generate a DSL showcase`() = generatedPdfPath.generatePdfDocument("k_open_pdf_dsl_showcase.pdf") { output ->
        TagPhrase.registerFAIconHandler()
        NumberedPdfDocument(output).use(::generateShowcaseDocument)

        NumberedPdfDocument(output).useDocument {
            defaultFont {}
        }

    }

    private fun generateShowcaseDocument(document: BasePdfDocument): Unit = with(document) {
        defaultFont {
            /* Change default font configuration */
            // size = 10f
            // color = com.lowagie.text.html.WebColors.getRGBColor("DarkGray")
            // setFactory { size, color, style -> PdfDocumentFonts.getUnicodeFont("font-name", size, color) }
        }

        documentInfo {
            title = "KOpenPdf"
            author = System.getProperty("user.name", "undefined")
            subject = "DSL Showcase"
            keywords = listOf("pageSize = $pageSize")
            creator = KOpenPdfShowcaseTest::class.java.name
        }

        documentViewPreferences {
            pageLayout = PageLayout.OneColumn
            pageMode = PageMode.UseOutlines
            fitWindow = true
        }

        documentEncryption {
            encryptionType = EncryptionType.STANDARD_ENCRYPTION_128
            setOwnerPassword("${System.currentTimeMillis()}".toByteArray(Charsets.UTF_8))

            allowPrinting = true
            allowModifyAnnotations = true
            allowFillIn = true
            allowAssembly = true
        }

        header(
            "<color DarkCyan>K Open Pdf Showcase</color>\n" +
            "<color DarkCyan><size 80%>Simple Kotlin DSL for creating simple Pdf document</size></color>"
        )
        footer("Generated on ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now())}\nVersion 2024.12")
        body {
            barcode(Barcodes.qrcode("https://github.com/akolomiets/k-open-pdf")) {
                scaleToFit(68f)
                absolutePosition(HorizontalAlignment.RIGHT, VerticalAlignment.TOP)
            }

            documentInfoChapter()
            documentViewPreferencesChapter()
            documentEncryptionChapter()
            documentWatermark()
        }

        header("<color DarkCyan>K Open Pdf Showcase</color>")
        body {
            headerAndFooterChapter()
            chapterAndSectionChapter()
            paragraphChapter()
            tableChapter()
            imageChapter()
            listChapter()
            blockquoteChapter()
            barcodeChapter()

            simpleDocumentCode()

            copyrightChapter()
        }
    }


    private fun PdfBodyBuilder.documentInfoChapter() = chapter("", "Document properties (metadata)") {
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "A document properties shows basic information about the document. The title, author, subject, and keywords ",
                "may have been set by the person who created the document in the source application.",
            )
            seeAlso("PDF properties and metadata", "https://helpx.adobe.com/uk/acrobat/using/pdf-properties-metadata.html")
        }

        paragraphCode(
            "Set the document properties",
            """
            documentInfo {
              title = "Title is here"
              author = "Author is here"
              subject = "Subject is here"
              keywords = listOf("key1", "key2", ...)
              creator = "Creator Info"
            }
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.documentViewPreferencesChapter() = chapter("", "Viewing PDFs and viewing preferences") {
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "The initial view of the PDF depends on how its creator set the document properties. " +
                "For example, a document may open at a particular page or magnification."
            )
            seeAlso("Viewing Preferences", "https://helpx.adobe.com/acrobat/using/viewing-pdfs-viewing-preferences.html")
        }

        paragraphCode(
            "Set the view preferences",
            """
            documentViewPreferences {
              pageLayout = PageLayout.OneColumn
              pageMode = PageMode.UseNone
              fitWindow = true
            }
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.documentEncryptionChapter() = chapter("", "Overview of security in Acrobat and PDFs") {
        paragraph("Security applies in two general contexts: application (software) security and content security.")
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "<b>Application security</b> involves customizing security features to protect Acrobat and Reader against vulnerabilities, ",
                "malicious attacks, and other risks. Advanced users can customize the application through the user interface. Enterprise ",
                "administrators can also configure the registry. See the following articles for details."
            )
        }
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "<b>Content security</b> involves using product features to protect the integrity of PDF content. ",
                "These features safeguard against the unwanted alteration of PDFs, keep sensitive information private, ",
                "prevent the printing of PDFs, and so on. See the following articles for details."
            )
            seeAlso("Security in PDF", "https://helpx.adobe.com/acrobat/using/overview-security-acrobat-pdfs.html")
        }

        paragraphCode(
            "Set document encryption",
            """
            documentEncryption {
              encryptionType = EncryptionType.STANDARD_ENCRYPTION_128
              setOwnerPassword("password".toByteArray(Charsets.UTF_8))
    
              allowPrinting = true
              allowModifyAnnotations = true
              allowFillIn = true
              allowAssembly = true
            }
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.documentWatermark() = chapter("", "Watermark") {
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "A <b>watermark</b> is an identifying image or pattern in paper that appears as various shades of lightness/darkness when viewed by transmitted " +
                "light (or when viewed by reflected light, atop a dark background), caused by thickness or density variations in the paper. Watermarks " +
                "have been used on postage stamps, currency, and other government documents to discourage counterfeiting. There are two main ways of " +
                "producing watermarks in paper; the dandy roll process, and the more complex cylinder mould process."
            )
        }
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "Watermarks vary greatly in their visibility; while some are obvious on casual inspection, others require some study to pick out. " +
                "Various aids have been developed, such as watermark fluid that wets the paper without damaging it. A watermark is very useful in " +
                "the examination of paper because it can be used for dating documents and artworks, identifying sizes, mill trademarks and locations, " +
                "and determining the quality of a sheet of paper. The word is also used for digital practices that share similarities with physical watermarks. " +
                "In one case, overprint on computer-printed output may be used to identify output from an unlicensed trial version of a program. " +
                "In another instance, identifying codes can be encoded as a digital watermark for a music, video, picture, or other file. " +
                "Or an artist adding their identifying digital Signature, graphic, logo in their digital artworks as an identifier or anti-counterfeit measure."
            )
            seeAlso("Watermark", "https://en.wikipedia.org/wiki/Watermark")
        }
        paragraphCode(
            "Add a Watermark",
            """
            watermark("Text")    
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.headerAndFooterChapter() = chapter("<fa-file-text-o>", "Page Header and Footer") {
        triggerNewPage()

        paragraph("<size 110%><b>Page header</b></size>")
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "In typography and word processing, a page header (or simply header) is text that is separated from the body " +
                "text and appears at the top of a printed page. Word-processing programs usually allow for the configuration of " +
                "page headers, which are typically identical throughout a work except in aspects such as page numbers."
            )
            seeAlso("Page header", "https://en.wikipedia.org/wiki/Page_header")
        }

        paragraph("<size 110%><b>Page footer</b></size>")
        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "In typography and word processing, the page footer (or simply footer) of a printed page is a section located under " +
                "the main text, or body. It is typically used as the space for the page number. In the earliest printed books it also " +
                "contained the first words of the next page; in this case they preferred to place the page number in the page header, " +
                "in the top margin. Because of the lack of a set standard, in modern times the header and footer are sometimes " +
                "interchangeable. In some instances, there are elements of the header inserted into the footer, such as the book or " +
                "chapter title, the name of the author or other information. In the publishing industry the page footer is traditionally " +
                "known as the running foot, whereas the page header is the running head."
            )
            seeAlso("Page footer", "https://en.wikipedia.org/wiki/Page_footer")
        }

        paragraphCode(
            "Add a page header and footer",
            """
            header("<color DarkCyan>K Open Pdf Showcase</color>")
            footer("Version 2024.12")
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.chapterAndSectionChapter() = chapter("\u00A7", "Chapter and Section") {
        triggerNewPage()

        paragraph {
            textAlignment = TextAlignment.Justified

            text(
                "A chapter is any of the main thematic divisions within a writing of relative length. ",
                "A section is a subdivision, especially of a chapter."
            )
            blockquote {
                backgroundColor = java.awt.Color(242, 242, 242)
                paragraph {
                    text("See also ")
                    link(linkText("Chapter"), URI.create("https://en.wikipedia.org/wiki/Chapter_(books)"))
                    text(", ")
                    link(linkText("Section"), URI.create("https://en.wikipedia.org/wiki/Section_(typography)"))
                }
            }
        }

        paragraphCode(
            "Create a Chapter",
            """
            body {
              chapter("Title", 1) { 
                triggerNewPage()
                ... 
              }
              chapter("Title", 2) {
                triggerNewPage()
                ... 
              }
            }    
            """.trimIndent()
        )

        paragraphCode(
            "Create a Section",
            """
            body {
              section("Title 1") { ... }
              section("Title 2") { ... }
            }    
            """.trimIndent()
        )

        paragraphCode(
            "Add a Section inside the Chapter",
            """
            body {
              chapter("Title", ChapterType.AutoNumber) {
                section("Sub Title 1") { ... }
                section("Sub Title 2") { ... }
              }
            }    
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.paragraphChapter() = chapter("<fa-paragraph>", "Paragraph") {
        triggerNewPage()

        paragraph {
            text(
                "A paragraph is a self-contained unit of discourse in writing dealing with a particular point or idea. Though not required by the ",
                "orthographic conventions of any language with a writing system, paragraphs are a conventional means of organizing extended segments of prose."
            )
            seeAlso("Paragraph", "https://en.wikipedia.org/wiki/Paragraph")
        }

        paragraph("Inline Text")
        paragraphCode(
            "Add a paragraph",
            """
            body {    
              paragraph("Inline text")
              paragraph {
                text("Any text")
              }
            }                              
            """.trimIndent()
        )

        paragraph {
            text("<b>Bold</b>\n")
            text("<i>Italic</i>\n")
            text("<u>Underline</u>\n")
            text("<s>Strikethru</s>\n")
            text("<b><i><u><s>Bold + Italic + Underline + Strikethru</s></u></i></b>\n")
            newLine()
            text("Color <color #FF358A>RED</color>\n")
            text("Color <color #56A76B>GREEN</color>\n")
            text("Color <color rgb(53, 116, 240)>BLUE</color>\n")
        }
        paragraphCode(
            "Use some tags inside text",
            """
            body {
              paragraph(""${'"'}
                <b>Bold</b>
                <i>Italic</i>
                <u>Underline</u>
                <s>Strikethru</s>
                <b><i><u><s>Bold + Italic + Underline + Strikethru</s></u></i></b>
                
                <color #FF358A>RED</color>
                <color #56A76B>GREEN</color>
                <color rgb(53, 116, 240)>BLUE</color>
              ""${'"'})
            }
            """.trimIndent()
        )

        h1("Heading 1")
        h2("Heading 2")
        h3("Heading 3")
        h4("Heading 4")
        h5("Heading 5")
        h6("Heading 6")

        paragraphCode(
            "Add a heading",
            """
            h1("Heading 1")
            h2("Heading 2")
            h3("Heading 3")
            h4("Heading 4")
            h5("Heading 5")
            h6("Heading 6")
            """.trimIndent()
        )

        paragraph {
            fa('\uF087', color = java.awt.Color(86, 167, 107))
        }
        paragraphCode(
            "Add an icon (Font Awesome 4.7.0)",
            """
            body {
           
              TagPhrase.registerFAIconHandler()  
                
              paragraph {
                text("...")
                fa('\uF087', 11f, java.awt.Color.GREEN)
                text("...")
              }
              paragraph("Icon <fa \uF087> inside text line!")
            }
            """.trimIndent()
        )

        paragraph {
            text("Some long text to be wrapped by specified length".wordWrap(40))
            newLine()
            text("Some long text to be wrapped by specified length and different delimiters".wordWrapBy(40, ' ', ','))
        }
        paragraphCode(
            "Use word wrap extension method",
            """
            text("...".wordWrap(40))
            text("...".wordWrapBy(40, ' ', ','))    
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.tableChapter() = chapter("<fa-table>", "Table") {
        triggerNewPage()

        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "A <b>table</b> is an arrangement of information or data, typically in rows and columns, or possibly in a more complex " +
                "structure. Tables are widely used in communication, research, and data analysis. Tables appear in print media, handwritten notes, " +
                "computer software, architectural ornamentation, traffic signs, and many other places. The precise conventions and terminology " +
                "for describing tables vary depending on the context. Further, tables differ significantly in variety, structure, flexibility, " +
                "notation, representation and use. Information or data conveyed in table form is said to be in tabular format (adjective). " +
                "In books and technical articles, tables are typically presented apart from the main text in numbered and captioned floating blocks."
            )
            seeAlso("Table (information)", "https://en.wikipedia.org/wiki/Table_(information)")
        }

        paragraph("<b>Simple table</b>")
        paragraph {
            table(6) {
                it.offset = 0f

                useAllAvailableWidth()
                designer(DesignedTable::BareboneStyle.name)

                columnHeaders("No", "Country", "Name", "Email", "Phone Number", "Currency")
                columnWidths(5f, 25f, 15f, 25f, 20f, 10f)

                (1 .. 3).forEach {
                    td(it)
                    td(random { country(CountryType.Name) })
                    td(random { name() })
                    td(random { email() })
                    td(random { phoneNumber() })
                    td(random { currency().numericCodeAsString }) {
                        it.setHorizontalAlignment(HorizontalAlignment.CENTER)
                    }
                }
            }
        }

        paragraph("<b>No data table</b>")
        paragraph {
            table(5) {
                it.offset = 0f

                useAllAvailableWidth()
                designer(DesignedTable::BareboneStyle.name)

                columnHeaders("Digital code", "Letter code", "Unit", "Currency name", "Rate")

                noData("<i>No Exchange Rates</i>")
            }
        }

        paragraph("<b>Transposed table</b>")
        paragraph {
            table(30f, 70f) {
                it.offset = 0f
                it.width = 60f

                designer(DesignedTableDesigners::TableStyleLight13.name)
                emptyCellValue = HYPHEN_SYMBOL

                th("<color White>Id</color>")
                td(random { id() })

                th("<color White>First Name</color>")
                td(random { name() })

                th("<color Pink>Second Name</color>")
                td("")

                th("<color White>Email</color>")
                td(random { email() })
            }
        }

        paragraph("<b>Designed table (TableStyleLight16)</b>")
        paragraph {
            table(4) {
                it.offset = 0f

                useAllAvailableWidth()
                designer(DesignedTableDesigners::TableStyleLight16.name)

                columnHeaders("<b>No</b>", "<b>Id</b>", "<b>First Name</b>", "<b>Second Name</b>")
                columnWidths(5f, 45f, 25f, 25f)

                (1 .. 3).forEach {
                    td(it)
                    td(random { id() })
                    td(random { name() })
                    td(random { name() })
                }
            }
        }

        paragraph("<b>Custom Designed table</b>")
        paragraph {
            table(4) {
                it.offset = 0f
                it.isTableFitsPage = true

                useAllAvailableWidth()
                designer(DesignedTableDesigners::TableStyleLight19_2010.name) {
                    headerHorizontalAlignment = HorizontalAlignment.CENTER
                    stripesBackgroundColor = null
                    showColumnStripes = true
                }

                val colorHex = (design.borderColor ?: java.awt.Color.BLACK).formatAsHex()

                fun styleHeader(value: String) = "<b><color $colorHex>$value</color></b>"
                fun styleCell(value: String) = "<color $colorHex>$value</color>"

                columnHeaders(styleHeader("No"), styleHeader("Id"), styleHeader("First Name"), styleHeader("Second Name"))
                columnWidths(5f, 45f, 25f, 25f)

                (1 .. 3).forEach {
                    td(styleCell(it.toString()) + EMPTY_SYMBOL) { it.setHorizontalAlignment(HorizontalAlignment.RIGHT) }
                    td(styleCell(random { id() }))
                    td(styleCell(random { name() }))
                    td(styleCell(random { name() }))
                }
            }
        }

        paragraph("<b>Available Designers</b>")
        table(5) {
            it.offset = 0f
            it.isTableFitsPage = true

            designer(DesignedTable::BareboneStyle.name)

            columnWidths(10f, 60f, 10f, 10f, 10f)

            th("<b>No</b>") {
                it.rowspan = 2
                it.setHorizontalAlignment(HorizontalAlignment.CENTER)
            }
            th("<b>Table Designer Name</b>") {
                it.rowspan = 2
                it.setHorizontalAlignment(HorizontalAlignment.CENTER)
            }
            th("<b>Colors</b>") {
                it.colspan = 3
                it.setHorizontalAlignment(HorizontalAlignment.CENTER)
            }
            th("<b>Border</b>") {
                it.setHorizontalAlignment(HorizontalAlignment.CENTER)
            }
            th("<b>Header</b>") {
                it.setHorizontalAlignment(HorizontalAlignment.CENTER)
            }
            th("<b>Stripes</b>") {
                it.setHorizontalAlignment(HorizontalAlignment.CENTER)
            }

            sequence {
                yield("NoStyle")
                yield("BareboneStyle")
                yieldAll((1 .. 21).map { "TableStyleLight$it" })
                yieldAll(arrayOf(3, 5, 6, 10, 12, 13, 17, 19, 20).map { "TableStyleLight${it}_2010" })
            }.forEachIndexed { index, name ->
                val designer = if (name == "BareboneStyle") DesignedTable.BareboneStyle else DesignedTableDesigners.findDesignerByName(name)

                td(index + 1)
                td(name)

                if (designer != null) {
                    td("") {
                        it.backgroundColor = designer.design.borderColor
                    }
                    td("") {
                        it.backgroundColor = designer.design.headerBackgroundColor
                    }
                    td("") {
                        it.backgroundColor = designer.design.stripesBackgroundColor
                    }
                } else {
                    td("<color IndianRed>Not Found</color>") {
                        it.colspan = 3
                        it.setHorizontalAlignment(HorizontalAlignment.CENTER)
                    }
                }
            }
        }

        newLine()

        paragraphCode(
            "Add a table",
            """
            table(3) {
              useAllAvailableWidth()
              designer("BareboneStyle")
                
              columnHeaders("<b>Country</b>", "<b>Name</b>", "<b>Currency</b>")
              columnWidths(40f, 40f, 20f)
                
              if (hasData) {
                td(country)
                td(name)                
                td(currency)                
              } else {
                noData("<i>No Data</i>")                    
              }
            }    
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.imageChapter() = chapter("<fa-image>", "Image") {
        triggerNewPage()

        paragraph {
            image("img/chart.png") {
                scaleToPageSize(0f, 80f)
                alignCenter()
            }
        }

        paragraphCode(
            "Add an image to document",
            """
            ...
            image("chart.png") {
              scaleToPageSize(0f, 80f)
              alignCenter()
            }
            ...                
            """.trimIndent()
        )

        newPage()

        image("img/high-quality-stamp.png") {
            scaleToPageSize(40f, 40f)
            rotationDegrees(45f)
            absolutePosition(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
        }

        // PNG
        paragraph {
            link("<color #006DCC><size large><b>PNG</b></size> <size small><fa-external-link></size></color>", URI.create("https://en.wikipedia.org/wiki/PNG"))
        }
        paragraph(
            "<b>Portable Network Graphics</b> (PNG, officially pronounced /pɪŋ/ PING, colloquially pronounced /ˌpiːɛnˈdʒiː/ PEE-en-JEE) " +
            "is a raster-graphics file format that supports lossless data compression. PNG was developed as an improved, non-patented " +
            "replacement for Graphics Interchange Format (GIF)—unofficially, the initials PNG stood for the recursive acronym \"<b>PNG's not GIF</b>\"."
        ) { textAlignment = TextAlignment.Justified }
        paragraph(
            "PNG supports palette-based images (with palettes of 24-bit RGB or 32-bit RGBA colors), grayscale images (with or without an " +
            "alpha channel for transparency), and full-color non-palette-based RGB or RGBA images. The PNG working group designed the " +
            "format for transferring images on the Internet, not for professional-quality print graphics; therefore, non-RGB color spaces " +
            "such as CMYK are not supported. A PNG file contains a single image in an extensible structure of chunks, encoding the basic " +
            "pixels and other information such as textual comments and integrity checks documented in RFC 2083."
        ) { textAlignment = TextAlignment.Justified }
        paragraph(
            "PNG files have the \".png\" file extension and the \"image/png\" MIME media type. " +
            "PNG was published as an informational RFC 2083 in March 1997 and as an ISO/IEC 15948 standard in 2004."
        ) { textAlignment = TextAlignment.Justified }

        paragraph("<size large><b>History and development<b></size>")
        paragraph(
            "The motivation for creating the PNG format was the announcement on 28 December 1994, that implementations of the Graphics " +
            "Interchange Format (GIF) format would have to pay royalties to Unisys due to their patent of the Lempel–Ziv–Welch (LZW) data " +
            "compression algorithm used in GIF. This led to a flurry of criticism from Usenet users. One of them was Thomas Boutell, " +
            "who on 4 January 1995 posted a precursory discussion thread on the Usenet newsgroup \"comp.graphics\" in which he devised " +
            "a plan for a free alternative to GIF. Other users in that thread put forth many propositions that would later be part of the " +
            "final file format. Oliver Fromme, author of the popular JPEG viewer QPEG, proposed the PING name, eventually becoming PNG, " +
            "a recursive acronym meaning PING is not GIF, and also the .png extension. Other suggestions later implemented included the " +
            "deflate compression algorithm and 24-bit color support, the lack of the latter in GIF also motivating the team to create " +
            "their file format. The group would become known as the PNG Development Group, and as the discussion rapidly expanded, " +
            "it later used a mailing list associated with a CompuServe forum."
        ) { textAlignment = TextAlignment.Justified }
        paragraph(
            "The full specification of PNG was released under the approval of W3C on 1 October 1996, and later as RFC 2083 on 15 January 1997. " +
            "The specification was revised on 31 December 1998 as version 1.1, which addressed technical problems for gamma and color correction. " +
            "Version 1.2, released on 11 August 1999, added the iTXt chunk as the specification's only change, and a reformatted version of 1.2 " +
            "was released as a second edition of the W3C standard on 10 November 2003, and as an International Standard (ISO/IEC 15948:2004) on 3 March 2004."
        ) { textAlignment = TextAlignment.Justified }
        paragraph(
            "Although GIF allows for animation, it was initially decided that PNG should be a single-image format. In 2001, the developers " +
            "of PNG published the Multiple-image Network Graphics (MNG) format, with support for animation. MNG achieved moderate application " +
            "support, but not enough among mainstream web browsers and no usage among web site designers or publishers. In 2008, certain Mozilla " +
            "developers published the Animated Portable Network Graphics (APNG) format with similar goals. APNG is a format that is natively " +
            "supported by Gecko- and Presto-based web browsers and is also commonly used for thumbnails on Sony's PlayStation Portable system " +
            "(using the normal PNG file extension). In 2017, Chromium based browsers adopted APNG support. In January 2020, Microsoft Edge became " +
            "Chromium based, thus inheriting support for APNG. With this all major browsers now support APNG."
        ) { textAlignment = TextAlignment.Justified }

        // SVG
        paragraph {
            link("<color #006DCC><size large><b>SVG</b></size> <size small><fa-external-link></size></color>", URI.create("https://en.wikipedia.org/wiki/SVG"))
        }
        paragraph(
            "<b>Scalable Vector Graphics (SVG)</b> is an XML-based vector image format for defining two-dimensional graphics, having support for " +
            "interactivity and animation. The SVG specification is an open standard developed by the World Wide Web Consortium since 1999."
        ) { textAlignment = TextAlignment.Justified }
        paragraph(
            "SVG images are defined in a vector graphics format and stored in XML text files. SVG images can thus be scaled in size without " +
            "loss of quality, and SVG files can be searched, indexed, scripted, and compressed. The XML text files can be created and edited " +
            "with text editors or vector graphics editors, and are rendered by most web browsers. If used for images, SVG can host scripts or " +
            "CSS, potentially leading to cross-site scripting attacks or other security vulnerabilities."
        ) { textAlignment = TextAlignment.Justified }

        table(3) {
            useAllAvailableWidth()

            td {
                svg("svg/cat.svg") {
                    scaleToFit(120f)
                    alignCenter()
                }
            }
            td {
                it.setVerticalAlignment(VerticalAlignment.BOTTOM)
                svg("svg/duck.svg") {
                    scaleToFit(120f)
                    alignCenter()
                }
            }
            td {
                svg("svg/dog.svg") {
                    scaleToFit(120f)
                    alignCenter()
                }
            }
        }

        newLine()

        paragraphCode(
            "Add a svg image to the document",
            """
            <!-- Add a dependency -->     
            <dependency>
                <groupId>org.apache.xmlgraphics</groupId>
                <artifactId>batik-transcoder</artifactId>
                <version>1.18</version>
            </dependency>
            
            ...
            svg("cat.svg") {
              scaleToFit(100f)
            }
            ...    
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.listChapter() = chapter("<fa-list>", "List") {
        triggerNewPage()

        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "A list is a set of discrete items of information collected and set forth in some format for utility, entertainment, or other purposes. ",
                "A list may be memorialized in any number of ways, including existing only in the mind of the list-maker, but lists are frequently ",
                "written down on paper, or maintained electronically. Lists are \"most frequently a tool\", and \"one does not read but only uses a list: ",
                "one looks up the relevant information in it, but usually does not need to deal with it as a whole\"."
            )
            seeAlso("List", "https://en.wikipedia.org/wiki/List")
        }
        section("Basic Lists") {
            paragraph {
                list {
                    li("First Item")
                    li("Second Item")
                    li("Third Item")
                    list(ListType.Letter) {
                        postSymbol = ") "

                        li("Sub Item 1")

                        list(ListType.Symbol) {
                            li("<b>Bold Item</b>")
                            li("<i>Italic Item</i>")
                            li("<u>Underline Item</u>")
                            li("<s>Strikethru Item</s>")
                        }

                        li("Sub Item 2")
                        li("Sub Item 3")
                    }
                }
            }
        }
        section("Fancy Lists") {
            paragraph {
                list(ListType.Roman) {
                    symbolIndent = 20f

                    li("First Item")
                    li("Second Item")
                    li("Third Item")

                    list(ListType.Greek) {
                        postSymbol = ") "
                        isLowercase = true

                        li("Alpha")
                        li("Beta")
                        li("Gamma")
                    }

                    li("Fourth Item")
                }
            }
        }
        section("Source Code") {
            paragraphCode(
                "Add a list to the document",
                """
                list {
                  li("First Item")
                  li("Second Item ")
                  li("Third Item")
                  list(ListType.Letter) {
                    postSymbol = ") "
                    
                    li("Sub-Item 1")
                    
                    list(ListType.Symbol) {
                      li("<b>Item 1</b>")
                      li("<i>Item 2</i>")
                      li("<u>Item 3</u>")
                      li("<s>Item 4</s>")
                    }
                    
                    li("Sub-Item 2")
                    li("Sub-Item 3")
                  }
                }                        
                """.trimIndent()
            )
        }
    }

    private fun PdfBodyBuilder.blockquoteChapter() = chapter("<fa-chevron-right>", "Blockquote") {
        triggerNewPage()

        paragraph {
            textAlignment = TextAlignment.Justified
            text(
                "A <b>block quotation</b> (also known as a long quotation or extract) is a quotation in a written document that is set off from " +
                "the main text as a paragraph, or block of text, and typically distinguished visually using indentation and a different typeface " +
                "or smaller size font. This is in contrast to setting it off with quotation marks in a run-in quote. Block quotations are used " +
                "for long quotations. The Chicago Manual of Style recommends using a block quotation when extracted text is 100 words or more, " +
                "or approximately six to eight lines in a typical manuscript."
            )
            seeAlso("Block quotation", "https://en.wikipedia.org/wiki/Block_quotation")
        }

        section("Default Block Quotation") {
            blockquote {
                paragraph("The Default Block Quotation element")
            }
            blockquote {
                backgroundColor = java.awt.Color(242, 242, 242)
                paragraph {
                    text("The Block Quotation element with background color ${java.awt.Color(242, 242, 242).formatAsRgb()}")
                }
            }
            blockquote {
                paragraph {
                    it.font.color = this@blockquote.stripColor
                    text("The Block Quotation element with font color ${it.font.color.formatAsRgb()}")
                }
            }

            newLine()

            paragraphCode(
                "Add a blockquote",
                """
                ...
                blockquote {
                  paragraph("The Default Block Quotation element")
                }
                ...    
                """.trimIndent()
            )
        }

        section("Typed Block Quotation") {
            blockquote(BlockquoteType.Note) {
                paragraph {
                    text("Useful information that users should know.")
                }
            }
            blockquote(BlockquoteType.Tip) {
                paragraph {
                    text("Helpful advice for doing things better or more easily.")
                }
            }
            blockquote(BlockquoteType.Important) {
                paragraph {
                    text("Key information users need to know to achieve their goal.")
                }
            }
            blockquote(BlockquoteType.Warning) {
                paragraph {
                    text("Urgent info that needs immediate user attention to avoid problems.")
                }
            }
            blockquote(BlockquoteType.Caution) {
                paragraph {
                    text("Advises about risks or negative outcomes of certain actions.")
                }
            }

            newLine()

            paragraphCode(
                "Add a Typed blockquote",
                """
                ...
                blockquote(BlockquoteType.Note) { }
                blockquote(BlockquoteType.Tip) { } 
                blockquote(BlockquoteType.Important) { }
                blockquote(BlockquoteType.Warning) { }
                blockquote(BlockquoteType.Caution) { }
                ...
                """.trimIndent()
            )
        }
    }

    private fun PdfBodyBuilder.barcodeChapter() = chapter("<fa-barcode>", "Barcode") {
        triggerNewPage()

        paragraph("A <b>barcode</b> or <b>bar code</b> is a method of representing data in a visual, machine-readable form.")

        section("Code 39") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "<b>Code 39</b> (also known as <b>Alpha39</b>, <b>Code 3 of 9</b>, <b>Code 3/9</b>, <b>Type 39</b>, <b>USS Code 39</b>, or <b>USD-3</b>) ",
                    "is a variable length, discrete barcode symbology defined in ISO/IEC 16388:2007."
                )
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "The Code 39 specification defines 43 characters, consisting of uppercase letters (A through Z), numeric digits (0 through 9) and ",
                    "a number of special characters (-, ., $, /, +, %, and space). An additional character (denoted '*') is used for both start ",
                    "and stop delimiters. Each character is composed of nine elements: five bars and four spaces. Three of the nine elements in ",
                    "each character are wide (binary value 1), and six elements are narrow (binary value 0)."
                )
                seeAlso("Code 39", "https://en.wikipedia.org/wiki/Code_39")
            }

            barcode(Barcodes.code39("CODE 39"))

            newLine()

            paragraphCode(
                "Add a CODE 39 barcode",
                """
                ...
                barcode(Barcodes.code39("CODE 39"))
                ...
                """.trimIndent()
            )
        }

        section("Code 128") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "<b>Code 128</b> is a high-density linear barcode symbology defined in ISO/IEC 15417:2007. It is used for alphanumeric or " +
                    "numeric-only barcodes. It can encode all 128 characters of ASCII and, by use of an extension symbol (FNC4), the Latin-1 " +
                    "characters defined in ISO/IEC 8859-1.[citation needed] It generally results in more compact barcodes compared to other methods " +
                    "like Code 39, especially when the texts contain mostly digits. Code 128 was developed by the Computer Identics Corporation in 1981."
                )
                seeAlso("Code 128", "https://en.wikipedia.org/wiki/Code_128")
            }

            barcode(Barcodes.code128("RI 476 394 652 CH"))

            newLine()

            paragraphCode(
                "Add a CODE 128 barcode",
                """
                ...
                barcode(Barcodes.code128("RI 476 394 652 CH"))
                ...
                """.trimIndent()
            )
        }

        section("International Article Number") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "The <b>International Article Number</b> (also known as <b>European Article Number</b> or <b>EAN</b>) is a standard describing a barcode " +
                    "symbology and numbering system used in global trade to identify a specific retail product type, in a specific packaging configuration, " +
                    "from a specific manufacturer. The standard has been subsumed in the Global Trade Item Number standard from the GS1 organization; the same " +
                    "numbers can be referred to as GTINs and can be encoded in other barcode symbologies, defined by GS1. EAN barcodes are used worldwide for " +
                    "lookup at retail point of sale, but can also be used as numbers for other purposes such as wholesale ordering or accounting. " +
                    "These barcodes only represent the digits 0–9, unlike some other barcode symbologies which can represent additional characters."
                )
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "The most commonly used EAN standard is the thirteen-digit <b>EAN-13</b>, a superset of the original 12-digit Universal " +
                    "Product Code (UPC-A) standard developed in 1970 by George J. Laurer. An EAN-13 number includes a 3-digit GS1 prefix " +
                    "(indicating country of registration or special type of product). A prefix with a first digit of \"0\" indicates a 12-digit " +
                    "UPC-A code follows. A prefix with first two digits of \"45\" or \"49\" indicates a Japanese Article Number (JAN) follows."
                )
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "The less commonly used 8-digit <b>EAN-8</b> barcode was introduced for use on small packages, where EAN-13 would be too large. " +
                    "2-digit EAN-2 and 5-digit EAN-5 are supplemental barcodes, placed on the right-hand side of EAN-13 or UPC. These are generally " +
                    "used in periodicals, like magazines and books, to indicate the current year's issue number and in weighed products like food, " +
                    "to indicate the manufacturer's suggested retail price."
                )
                seeAlso("International Article Number", "https://en.wikipedia.org/wiki/International_Article_Number")
            }

            table(4) {
                th("EAN-8")
                th("EAN-13")
                th("UPC-A")
                th("UPC-E")

                td { barcode(Barcodes.ean("48212342", Barcodes.EanType.EAN8)) }
                td { barcode(Barcodes.ean("4820000000222")) }
                td { barcode(Barcodes.ean("614141000036", Barcodes.EanType.UPCA)) }
                td { barcode(Barcodes.ean("06141939", Barcodes.EanType.UPCE)) }
            }

            newLine()

            paragraphCode(
                "Add an EAN barcode",
                """
                ...
                barcode(Barcodes.ean("4820000000222"))
                barcode(Barcodes.ean("48212342", Barcodes.EanType.EAN8))
                ...
                """.trimIndent()
            )
        }

        section("Interleaved 2 of 5") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "<b>Interleaved 2 of 5 (ITF)</b> is a continuous two-width barcode symbology encoding digits. " +
                    "It is used commercially on 135 film, for ITF-14 barcodes, and on cartons of some products, while the products inside " +
                    "are labeled with UPC or EAN. ITF was created by David Allais, who also invented barcodes Code 39, Code 11, Code 93, and Code 49."
                )
                seeAlso("Interleaved 2 of 5", "https://en.wikipedia.org/wiki/Interleaved_2_of_5")
            }

            barcode(Barcodes.interleaved("41-120007604-001"))

            newLine()

            paragraphCode(
                "Add an Interleaved barcode",
                """
                ...
                barcode(Barcodes.interleaved("41-120007604-001"))
                ...
                """.trimIndent()
            )
        }

        section("POSTNET") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "<b>POSTNET</b> (<b>Post</b>al <b>N</b>umeric <b>E</b>ncoding <b>T</b>echnique) is a barcode symbology used by " +
                    "the United States Postal Service to assist in directing mail. The ZIP Code or ZIP+4 code is encoded in half- and full-height bars. " +
                    "Most often, the delivery point is added, usually being the last two digits of the address or PO box number."
                )
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "The barcode starts and ends with a full bar (often called a guard rail or frame bar and represented as " +
                    "the letter \"S\" in one version of the USPS TrueType Font) and has a check digit after the ZIP, ZIP+4, " +
                    "or delivery point. The encoding table is shown on the right."
                )
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "Each individual digit is represented by a set of five bars, two of which are full bars (i.e. two-out-of-five code). " +
                    "The full bars represent \"on\" bits in a pseudo-binary code in which the places represent, from left to right: 7, 4, 2, 1, and 0. " +
                    "(Though in this scheme, zero is encoded as 11 in decimal, or in POSTNET \"binary\" as 11000.)"
                )
                seeAlso("Postal Numeric Encoding Technique", "https://en.wikipedia.org/wiki/POSTNET")
            }

            barcode(Barcodes.postnet("5552357072"))

            newLine()

            paragraphCode(
                "Add a POSTNET barcode",
                """
                ...
                barcode(Barcodes.postnet("5552357072"))
                ...
                """.trimIndent()
            )
        }

        section("PLANET") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "The <b>Postal Alpha Numeric Encoding Technique</b> (PLANET) barcode was used by " +
                    "the United States Postal Service to identify and track pieces of mail during delivery – the Post Office's \"CONFIRM\" services. " +
                    "It was fully superseded by Intelligent Mail Barcode by January 28, 2013."
                )
                seeAlso("Postal Alpha Numeric Encoding Technique", "https://en.wikipedia.org/wiki/Postal_Alpha_Numeric_Encoding_Technique")
            }

            barcode(Barcodes.planet("4012345235636"))

            newLine()

            paragraphCode(
                "Add a PLANET barcode",
                """
                ...
                barcode(Barcodes.planet("4012345235636"))
                ...
                """.trimIndent()
            )
        }

        section("PDF417") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "<b>PDF417</b> is a stacked linear barcode format used in a variety of applications such as transport, identification cards, " +
                    "and inventory management. \"PDF\" stands for Portable Data File. The \"417\" signifies that each pattern in the code consists " +
                    "of 4 bars and spaces in a pattern that is 17 units (modules) long. The PDF417 symbology was invented by Dr. Ynjiun P. Wang at " +
                    "Symbol Technologies in 1991. It is defined in ISO 15438."
                )
                seeAlso("PDF417", "https://en.wikipedia.org/wiki/PDF417")
            }

            barcode(Barcodes.pdf417("PDF417 is a stacked linear barcode format used in a variety of applications such as transport")) {
                scalePercent(120f)
            }

            newLine()

            paragraphCode(
                "Add a PDF417 barcode",
                """
                ...
                barcode(Barcodes.pdf417("Any text"))
                ...
                """.trimIndent()
            )
        }

        section("QR Code") {
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "A <b>QR code</b>, <b>quick-response code</b>, is a type of two-dimensional matrix barcode invented in 1994 by " +
                    "Masahiro Hara of Japanese company Denso Wave for labelling automobile parts. It features black squares on a white " +
                    "background with fiducial markers, readable by imaging devices like cameras, and processed using Reed–Solomon error " +
                    "correction until the image can be appropriately interpreted. The required data is then extracted from patterns that " +
                    "are present in both the horizontal and the vertical components of the QR image."
                )
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "Whereas a barcode is a machine-readable optical image that contains information specific to the labeled item, " +
                    "the QR code contains the data for a locator, an identifier, and web-tracking. To store data efficiently, " +
                    "QR codes use four standardized modes of encoding:"
                )
            }
            paragraph {
                it.indentationLeft = 15f
                list {
                    li("numeric,")
                    li("alphanumeric,")
                    li("byte or binary, and")
                    li("kanji.")
                }
            }
            paragraph {
                textAlignment = TextAlignment.Justified
                text(
                    "Compared to standard UPC barcodes, the QR labeling system was applied beyond the automobile industry because " +
                    "of faster reading of the optical image and greater data-storage capacity in applications such as product tracking, " +
                    "item identification, time tracking, document management, and general marketing."
                )
                seeAlso("QR code", "https://en.wikipedia.org/wiki/QR_code")
            }

            table(1) {
                td {
                    barcode(Barcodes.qrcode("https://en.wikipedia.org/wiki/QR_code")) {
                        scaleToFit(100f)
                    }
                }
            }

            paragraphCode(
                "Add a QR Code",
                """
                <!-- Add a dependency -->     
                <dependency>
                    <groupId>com.google.zxing</groupId>
                    <artifactId>core</artifactId>
                    <version>3.5.3</version>
                </dependency>
                
                ...
                barcode(Barcodes.qrcode("https://en.wikipedia.org/wiki/QR_code"))
                ...
                """.trimIndent()
            )
        }
    }

    private fun PdfBodyBuilder.simpleDocumentCode() = chapter("<fa-file-code-o>", "Simple Document Structure (Source Code)") {
        triggerNewPage()

        paragraphCode(
            "",
            """
            NumberedPdfDocument(output).useDocument {
              
              defaultFont {
                size = 8f
                setFactory { size, color, style -> 
                  PdfDocumentFonts.getUnicodeFont("Noto Sans", size, color) 
                }
              }
              
              documentInfo {
                title = "..."
                author = "..."
              }
              
              documentViewPreferences {
                pageMode = PageMode.UseOutlines
                fitWindow = true
              }
              
              header("Document Header")
              footer("Document Footer\nVersion")
               
              body {
                chapter("Chapter", 1) {
                  paragraph {
                    text("...")
                    newLine()
                    text("...")
                  }
                  image("...") {
                    scaleToPageSize(40f, 40f)
                  }                
                }
                chapter("Chapter", 2) {
                  paragraph("...")
                  table(3) {
                    useAllAvailableWidth()
                    designer(DesignedTable::BareboneStyle.name)
                    
                    th("")
                    th("")
                    th("")
                    
                    noData("...")
                  }
                }
              } 
            }    
            """.trimIndent()
        )
    }

    private fun PdfBodyBuilder.copyrightChapter() = chapter("<fa-copyright>", "Copyright") {
        triggerNewPage()

        section("OpenPDF License") {
            paragraph {
                link(linkText("OpenPDF"), URI.create("https://github.com/LibrePDF/OpenPDF?tab=License-1-ov-file"))
                text(" uses dual licensing: when using the library, you may choose either Mozilla Public License Version 2.0 or GNU Lesser General Public License 2.1.")
                text("\n\n")
                text("The SPDX license identifier for OpenPDF licensing is  ")
                add(Chunk("MPL-2.0 OR LGPL-2.1+").also {
                    it.font = PdfDocumentFonts.getHelvetica()
                    it.setBackground(java.awt.Color(220, 220, 220), 2f, 0.5f, 2f, 2f)
                })
            }
            paragraph {
                text("Please see ")
                link(linkText("Mozilla Public License Version 2.0"), URI.create("https://www.mozilla.org/en-US/MPL/2.0/"))
                text(" and ")
                link(linkText("GNU Lesser General Public License 2.1"), URI.create("https://www.mozilla.org/en-US/MPL/2.0/"))
            }
        }
        section("KOpenPDF License") {
            paragraph {
                link(linkText("KOpenPDF"), URI.create("https://github.com/akolomiets/k-open-pdf?tab=MIT-1-ov-file"))
                text(" uses MIT licensing")
            }
            blockquote {
                paragraph {
                    it.font.color = this@blockquote.stripColor

                    link("<color #4C8CBC>MIT License <size xx-small><fa-external-link></size></color>", URI.create("https://opensource.org/license/mit"))

                    text("\n\n")
                    text("Copyright (c) 2025 Andrey Kolomiets")
                    text("\n\n")
                    text(
                        "Permission is hereby granted, free of charge, to any person obtaining a copy",
                        "of this software and associated documentation files (the \"Software\"), to deal",
                        "in the Software without restriction, including without limitation the rights",
                        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell",
                        "copies of the Software, and to permit persons to whom the Software is",
                        "furnished to do so, subject to the following conditions:"
                    )
                    text("\n\n")
                    text(
                        "The above copyright notice and this permission notice shall be included in all ",
                        "copies or substantial portions of the Software."
                    )
                    text("\n\n")
                    text(
                        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR",
                        "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,",
                        "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE",
                        "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER",
                        "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,",
                        "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE",
                        "SOFTWARE."
                    )
                }
            }
        }
    }


    private fun PdfBodyBuilder.chapter(icon: String, title: String, block: PdfBodySectionBuilder.(Chapter) -> Unit) {
        val chapterTitle = buildString {
            if (icon.isNotEmpty()) {
                val color = listOf("DarkCyan", "DarkMagenta", "DarkGreen", "DarkBlue", "DarkGoldenrod", "DarkGray", "DarkRed").random()
                append("<color $color>")
                append(icon)
                append("</color> ")
            }
            append(title)
        }
        chapter(chapterTitle, ChapterType.AutoNumber, block)
    }

    private fun PdfBodyParagraphBuilder.seeAlso(text: String, link: String) = blockquote {
        backgroundColor = java.awt.Color(242, 242, 242)
        paragraph {
            text("See also ")
            link(linkText(text), URI.create(link))
        }
    }

    private fun PdfBodyElementBuilder.paragraphCode(title: String, content: String) {
        if (title.isNotEmpty()) {
            paragraph("<i>Code: $title</i>") { it.spacingAfter = 0f }
        }
        paragraph {
            codeBlock {
                it.width = 100f
                it.offset = 5f
                raw(content)
            }
        }
    }

    private fun linkText(text: String) = "<color #006DCC>$text <size xx-small><fa-external-link></size></color>"

}
