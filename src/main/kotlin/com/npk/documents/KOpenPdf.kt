package com.npk.documents

import com.lowagie.text.*
import com.lowagie.text.html.FontSize
import com.lowagie.text.pdf.*
import com.npk.documents.AbstractPdfDocument.PdfDocumentEncryption.EncryptionType
import java.awt.Color
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.max
import kotlin.properties.Delegates

abstract class AbstractPdfDocument protected constructor(outputStream: OutputStream, pageSize: Rectangle) : PdfPageEvent, AutoCloseable {

    companion object {
        const val DEFAULT_LEFT_MARGIN =     40f
        const val DEFAULT_RIGHT_MARGIN =    40f
        const val DEFAULT_TOP_MARGIN =      25f
        const val DEFAULT_BOTTOM_MARGIN =   25f
    }

    interface PdfDocumentInfo {
        var title: String
        var author: String
        var subject: String
        var keywords: List<String>
        var creator: String
        var modificationDate: LocalDateTime
    }

    interface PdfDocumentViewPreferences {

        enum class PageLayout(val code: Int) {
            SinglePage(PdfWriter.PageLayoutSinglePage),
            OneColumn(PdfWriter.PageLayoutOneColumn),
            TwoColumnLeft(PdfWriter.PageLayoutTwoColumnLeft),
            TwoColumnRight(PdfWriter.PageLayoutTwoColumnRight),
            TwoPageLeft(PdfWriter.PageLayoutTwoPageLeft),
            TwoPageRight(PdfWriter.PageLayoutTwoPageRight)
        }

        enum class PageMode(val code: Int) {
            UseNone(PdfWriter.PageModeUseNone),
            UseOutlines(PdfWriter.PageModeUseOutlines),
            UseThumbs(PdfWriter.PageModeUseThumbs),
            UseOC(PdfWriter.PageModeUseOC),
            UseAttachments(PdfWriter.PageModeUseAttachments)
        }

        enum class NonFullScreenPageMode(val code: Int) {
            UseNone(PdfWriter.NonFullScreenPageModeUseNone),
            UseOutlines(PdfWriter.NonFullScreenPageModeUseOutlines),
            UseThumbs(PdfWriter.NonFullScreenPageModeUseThumbs),
            UseOC(PdfWriter.NonFullScreenPageModeUseOC)
        }

        enum class Direction(val code: Int) {
            L2R(PdfWriter.DirectionL2R),
            R2L(PdfWriter.DirectionR2L)
        }

        var fullScreen: Boolean
        var pageLayout: PageLayout?
        var pageMode: PageMode?
        var nonFullScreenPageMode: NonFullScreenPageMode?
        var hideToolbar: Boolean
        var hideMenubar: Boolean
        var hideWindowUI: Boolean
        var fitWindow: Boolean
        var centerWindow: Boolean
        var displayDocTitle: Boolean
        var direction: Direction?
        var printScalingNone: Boolean

    }

    interface PdfDocumentEncryption {

        enum class EncryptionType(val code: Int) {
            STANDARD_ENCRYPTION_40(PdfWriter.STANDARD_ENCRYPTION_40),
            STANDARD_ENCRYPTION_128(PdfWriter.STANDARD_ENCRYPTION_128),
            ENCRYPTION_AES_128(PdfWriter.ENCRYPTION_AES_128),
            ENCRYPTION_AES_256_V3(PdfWriter.ENCRYPTION_AES_256_V3)
        }

        var encryptionType: EncryptionType
        var doNotEncryptMetadata: Boolean
        var embeddedFilesOnly: Boolean

        var allowPrinting: Boolean
        var allowModifyContents: Boolean
        var allowCopy: Boolean
        var allowModifyAnnotations: Boolean
        var allowFillIn: Boolean
        var allowScreenReaders: Boolean
        var allowAssembly: Boolean
        var allowDegradedPrinting: Boolean

        fun setUserPassword(bytes: ByteArray)

        fun setOwnerPassword(bytes: ByteArray)

    }

    protected val document: Document = Document(pageSize, DEFAULT_LEFT_MARGIN, DEFAULT_RIGHT_MARGIN, DEFAULT_TOP_MARGIN, DEFAULT_BOTTOM_MARGIN)
    protected val writer: PdfWriter = PdfWriter.getInstance(document, outputStream).also { it.pageEvent = this@AbstractPdfDocument }

    var pageSize: Rectangle
        get() = document.pageSize
        set(value) { document.pageSize = value }

    fun documentInfo(block: PdfDocumentInfo.() -> Unit) {
        PdfDocumentInfoBuilder(document).apply(block)
    }

    fun documentViewPreferences(block: PdfDocumentViewPreferences.() -> Unit) {
        PdfDocumentViewPreferencesBuilder(writer).let {
            it.apply(block)
            it.applyViewPreferences()
        }
    }

    fun documentEncryption(block: PdfDocumentEncryption.() -> Unit) {
        require(!document.isOpen) { "Encryption can only be added before opening the document" }
        PdfDocumentEncryptionBuilder(writer).use {
            it.apply(block)
            it.applyEncryption()
        }
    }

    override fun onOpenDocument(writer: PdfWriter, document: Document) { }

    override fun onStartPage(writer: PdfWriter, document: Document) { }

    override fun onEndPage(writer: PdfWriter, document: Document) { }

    override fun onCloseDocument(writer: PdfWriter, document: Document) { }

    override fun onParagraph(writer: PdfWriter, document: Document, paragraphPosition: Float) { }

    override fun onParagraphEnd(writer: PdfWriter, document: Document, paragraphPosition: Float) { }

    override fun onChapter(writer: PdfWriter, document: Document, paragraphPosition: Float, title: Paragraph) { }

    override fun onChapterEnd(writer: PdfWriter, document: Document, paragraphPosition: Float) { }

    override fun onSection(writer: PdfWriter, document: Document, paragraphPosition: Float, depth: Int, title: Paragraph) { }

    override fun onSectionEnd(writer: PdfWriter, document: Document, paragraphPosition: Float) { }

    override fun onGenericTag(writer: PdfWriter, document: Document, rect: Rectangle, text: String) { }

    override fun close() {
        if (document.isOpen) {
            document.close()
        }
    }

}

open class BasePdfDocument(outputStream: OutputStream, pageSize: Rectangle = PageSize.A4) : AbstractPdfDocument(outputStream, pageSize), PdfBodyDSL {

    inner class DefaultFontConfigurator {

        var size: Float
            get() = defaultFontSize
            set(value) {
                require(value > 2) { "The font size should be > 2" }
                defaultFontSize = value
            }

        var color: java.awt.Color?
            get() = defaultFontColor
            set(value) { defaultFontColor = value }

        fun setFactory(factory: (Float, java.awt.Color?, Int) -> Font) {
            defaultFontFactory = factory
        }

    }

    private class PdfBodyContextImpl(
        override val fontSize: Float,
        override val fontColor: Color?,
        private val fontFactory: (Float, java.awt.Color?, Int) -> Font,
        private val document: Document,
        private val writer: PdfWriter
    ) : PdfBodyContext {

        override fun getFont(size: Float, color: java.awt.Color?, style: Int): Font = fontFactory(size, color, style)

        override fun createTemplate(width: Float, height: Float): PdfTemplate = PdfTemplate.createTemplate(writer, width, height)

        override fun getPageSize(): Rectangle = RectangleReadOnly(document.pageSize)

        override fun getDirectContent(): PdfContentByte = writer.directContent

        override fun addAttachment(fileName: String, fileDescription: String, fileContent: ByteArray) {
            val fileSpecification = PdfFileSpecification.fileEmbedded(writer, null, fileName, fileContent, true)
            writer.addFileAttachment(fileDescription, fileSpecification)
        }

    }

    companion object {
        const val WATERMARK_FONT_SIZE = 42f
        const val FOOTER_RISE = DEFAULT_BOTTOM_MARGIN
    }

    private var headerTextLines: List<String> = emptyList()
    private var footerTextLines: List<String> = emptyList()
    private var watermarkImage: Lazy<Image?> = lazy(LazyThreadSafetyMode.NONE) { null }

    protected var defaultFontSize: Float = PdfDocumentFonts.DEFAULT_SIZE
    protected var defaultFontColor: java.awt.Color? = null
    protected var defaultFontFactory: (Float, java.awt.Color?, Int) -> Font = { size, color, style ->
        if (style == Font.UNDEFINED) {
            PdfDocumentFonts.getHelvetica(size, color)
        } else {
            val font = when {
                style and Font.BOLDITALIC == Font.BOLDITALIC -> PdfDocumentFonts.getHelveticaBoldOblique(size, color)
                style and Font.BOLD == Font.BOLD -> PdfDocumentFonts.getHelveticaBold(size, color)
                style and Font.ITALIC == Font.ITALIC -> PdfDocumentFonts.getHelveticaOblique(size, color)
                else -> PdfDocumentFonts.getHelvetica(size, color)
            }
            font.also {
                if (style and Font.UNDERLINE == Font.UNDERLINE) {
                    it.style = it.style or Font.UNDERLINE
                }
                if (style and Font.STRIKETHRU == Font.STRIKETHRU) {
                    it.style = it.style or Font.STRIKETHRU
                }
            }
        }
    }

    protected val headerFontSize: Float inline get() = defaultFontSize * FontSize.X_LARGE.scale
    protected val footerFontSize: Float inline get() = defaultFontSize * FontSize.X_SMALL.scale

    private val defaultFontConfigurator by lazy(LazyThreadSafetyMode.NONE) { DefaultFontConfigurator() }

    fun defaultFont(block: DefaultFontConfigurator.() -> Unit) {
        defaultFontConfigurator.apply(block)
        document.setMargins(document.leftMargin(), document.rightMargin(), calculateMarginTop(), calculateMarginBottom())
    }

    fun header(text: String) {
        headerTextLines = text.lines().filter(String::isNotBlank)
        document.setMargins(document.leftMargin(), document.rightMargin(), calculateMarginTop(), document.bottomMargin())
    }

    fun footer(text: String) {
        footerTextLines = text.lines().filter(String::isNotBlank)
        document.setMargins(document.leftMargin(), document.rightMargin(), document.topMargin(), calculateMarginBottom())
    }

    fun watermark(text: String) {
        watermarkImage = if (text.isNotBlank())
            lazy(LazyThreadSafetyMode.NONE) {
                val baseFont = defaultFontFactory(WATERMARK_FONT_SIZE, java.awt.Color.PINK, Font.UNDEFINED).baseFont
                val width = baseFont.getWidthPoint(text, WATERMARK_FONT_SIZE)
                val height = calculateLeading(WATERMARK_FONT_SIZE)

                val template = PdfTemplate.createTemplate(writer, width, height + 4).apply {
                    setGState(PdfGState().apply { setFillOpacity(0.5f) })
                    setColorFill(java.awt.Color.PINK)
                    beginText()
                    setFontAndSize(baseFont, WATERMARK_FONT_SIZE)
                    showTextAligned(Element.ALIGN_CENTER, text, width / 2, (height / 2) + 2f, 0f)
                    endText()
                }

                Image.getInstance(template).also { it.setRotationDegrees(45f) }
            }
        else
            lazy(LazyThreadSafetyMode.NONE) { null }
    }


    override fun body(block: PdfBodyBuilder.(PdfBodyContext) -> Unit) {
        if (!document.isOpen) {
            writer.setPdfVersion(PdfWriter.PDF_VERSION_1_7)
            writer.compressionLevel = PdfStream.BEST_COMPRESSION
            document.open()
        }
        val context = PdfBodyContextImpl(defaultFontSize, defaultFontColor, defaultFontFactory, document, writer)
        block(PdfBodyBuilder(document, context), context)
    }

    override fun onOpenDocument(writer: PdfWriter, document: Document) {
        super.onOpenDocument(writer, document)
        document.add(
            Phrase().apply {
                leading = calculateLeading(PdfDocumentFonts.DEFAULT_SIZE)
                add("")
            }
        )
    }

    override fun onStartPage(writer: PdfWriter, document: Document) {
        super.onStartPage(writer, document)
        drawHeader(writer)
        drawFooter(writer)
        drawWatermark(writer)
    }


    private fun drawHeader(writer: PdfWriter) {
        if (headerTextLines.isNotEmpty()) {
            val font = defaultFontFactory(headerFontSize, defaultFontColor, Font.BOLD)
            var y = document.pageSize.top - DEFAULT_TOP_MARGIN - 10
            headerTextLines.forEach { text ->
                ColumnText.showTextAligned(
                    writer.directContent,
                    Element.ALIGN_LEFT,
                    TagPhrase(text, font),
                    document.left(),
                    y,
                    0f
                )
                y -= calculateLeading(font.size)
            }
        }
    }

    private fun drawFooter(writer: PdfWriter) {
        var y = FOOTER_RISE
        if (footerTextLines.isNotEmpty()) {
            val font = defaultFontFactory(footerFontSize, defaultFontColor, Font.UNDEFINED)
            footerTextLines.reversed().forEach { text ->
                ColumnText.showTextAligned(
                    writer.directContent,
                    Element.ALIGN_LEFT,
                    TagPhrase(text, font),
                    document.left(),
                    y,
                    0f
                )
                y += calculateLeading(font.size)
            }
        }
    }

    private fun drawWatermark(writer: PdfWriter) {
        watermarkImage.value?.let { image ->
            writer.directContent.apply {
                saveState()
                val matrix = image.matrix()
                matrix[Image.CX] = (document.pageSize.getRight(image.scaledWidth) / 2) - matrix[Image.CX]
                matrix[Image.CY] = (document.pageSize.getTop(image.scaledHeight) / 2) - matrix[Image.CY]
                addImage(image, matrix[Image.AX], matrix[Image.AY], matrix[Image.BX], matrix[Image.BY], matrix[Image.CX], matrix[Image.CY])
                restoreState()
            }
        }
    }


    private fun calculateMarginTop(): Float {
        var margin = DEFAULT_TOP_MARGIN + calculateLeading(headerFontSize) * headerTextLines.size
        return max(DEFAULT_TOP_MARGIN, margin)
    }

    private fun calculateMarginBottom(): Float {
        var margin = DEFAULT_BOTTOM_MARGIN
        margin += if (footerTextLines.isNotEmpty()) calculateLeading(footerFontSize) * footerTextLines.size else 0f
        margin += footerFontSize
        return max(DEFAULT_BOTTOM_MARGIN, margin)
    }

}

open class NumberedPdfDocument(outputStream: OutputStream, pageSize: Rectangle = PageSize.A4) : BasePdfDocument(outputStream, pageSize) {

    companion object {
        const val PAGING_FORMAT = "Page %d of "
    }

    private lateinit var totalPagesTpl: PdfTemplate

    override fun onOpenDocument(writer: PdfWriter, document: Document) {
        val marginBottom = DEFAULT_BOTTOM_MARGIN + calculateLeading(defaultFontSize) + defaultFontSize
        document.setMargins(document.leftMargin(), document.rightMargin(), document.topMargin(), max(document.bottomMargin(), marginBottom))

        super.onOpenDocument(writer, document)
        totalPagesTpl = writer.directContent.createTemplate(18f, calculateLeading(defaultFontSize) + FOOTER_RISE)
    }

    override fun onStartPage(writer: PdfWriter, document: Document) {
        super.onStartPage(writer, document)
        drawPageNumber(writer)
    }

    override fun onCloseDocument(writer: PdfWriter, document: Document) {
        super.onCloseDocument(writer, document)
        drawPageTotal(writer)
    }

    private fun drawPageNumber(writer: PdfWriter) {
        val font = defaultFontFactory(footerFontSize, defaultFontColor, Font.UNDEFINED)
        writer.directContent.apply {
            saveState()
            beginText()
            font.color?.let { setColorFill(it) }
            setFontAndSize(font.baseFont, font.size)
            showTextAligned(
                Element.ALIGN_RIGHT,
                PAGING_FORMAT.format(writer.pageNumber),
                document.right() - 10f,
                FOOTER_RISE,
                0f
            )
            endText()
            addTemplate(totalPagesTpl, document.right() - 10f, 0f)
            restoreState()
        }

    }

    private fun drawPageTotal(writer: PdfWriter) {
        val font = defaultFontFactory(footerFontSize, defaultFontColor, Font.UNDEFINED)
        totalPagesTpl.apply {
            beginText()
            setTextMatrix(0f, FOOTER_RISE)
            font.color?.let { setColorFill(it) }
            setFontAndSize(font.baseFont, font.size)
            showText("${writer.pageNumber - 1}")
            endText()
        }
    }

}


private class PdfDocumentInfoBuilder(private val document: Document) : AbstractPdfDocument.PdfDocumentInfo {
    override var title: String by Delegates.observable("") { _, _, newValue -> document.addTitle(newValue) }
    override var author: String by Delegates.observable("") { _, _, newValue -> document.addAuthor(newValue) }
    override var subject: String by Delegates.observable("") { _, _, newValue -> document.addSubject(newValue) }
    override var keywords: List<String> by Delegates.observable(emptyList()) { _, _, newValue -> document.addKeywords(newValue.joinToString()) }
    override var creator: String by Delegates.observable("") { _, _, newValue -> document.addCreator(newValue) }
    override var modificationDate: LocalDateTime by Delegates.observable(LocalDateTime.now()) { _, _, newValue ->
        val pdfDate = PdfDate(
            Calendar.getInstance().also { calendar ->
                calendar.time = Date.from(newValue.atZone(ZoneId.systemDefault()).toInstant())
            }
        )
        document.addModificationDate(pdfDate)
    }
}

private class PdfDocumentViewPreferencesBuilder(private val writer: PdfWriter) : AbstractPdfDocument.PdfDocumentViewPreferences {

    override var fullScreen: Boolean = false

    override var pageLayout: AbstractPdfDocument.PdfDocumentViewPreferences.PageLayout? = null

    override var pageMode: AbstractPdfDocument.PdfDocumentViewPreferences.PageMode? = null

    override var nonFullScreenPageMode: AbstractPdfDocument.PdfDocumentViewPreferences.NonFullScreenPageMode? = null

    override var hideToolbar: Boolean = false
    override var hideMenubar: Boolean = false
    override var hideWindowUI: Boolean = false
    override var fitWindow: Boolean = false
    override var centerWindow: Boolean = false
    override var displayDocTitle: Boolean = false

    override var direction: AbstractPdfDocument.PdfDocumentViewPreferences.Direction? = null

    override var printScalingNone: Boolean = false

    fun applyViewPreferences() {
        var preferences = 0

        if (fullScreen) { preferences = preferences or PdfWriter.PageModeFullScreen }

        pageLayout?.let { preferences = preferences or it.code }
        pageMode?.let { preferences = preferences or it.code }
        nonFullScreenPageMode?.let { preferences = preferences or it.code }

        if (hideToolbar) { preferences = preferences or PdfWriter.HideToolbar }
        if (hideMenubar) { preferences = preferences or PdfWriter.HideMenubar }
        if (hideWindowUI) { preferences = preferences or PdfWriter.HideWindowUI }
        if (fitWindow) { preferences = preferences or PdfWriter.FitWindow }
        if (centerWindow) { preferences = preferences or PdfWriter.CenterWindow }
        if (displayDocTitle) { preferences = preferences or PdfWriter.DisplayDocTitle }

        direction?.let { preferences = preferences or it.code }

        if (printScalingNone) { preferences = preferences or PdfWriter.PrintScalingNone }

        writer.setViewerPreferences(preferences)
    }

}

private class PdfDocumentEncryptionBuilder(private val writer: PdfWriter) : AbstractPdfDocument.PdfDocumentEncryption, AutoCloseable {

    private var userPassword: ByteArray? = null
    private var ownerPassword: ByteArray? = null

    override var encryptionType: EncryptionType = EncryptionType.STANDARD_ENCRYPTION_128
    override var doNotEncryptMetadata: Boolean = false
    override var embeddedFilesOnly: Boolean = false

    override var allowPrinting: Boolean = false
    override var allowModifyContents: Boolean = false
    override var allowCopy: Boolean = false
    override var allowModifyAnnotations: Boolean = false
    override var allowFillIn: Boolean = false
    override var allowScreenReaders: Boolean = false
    override var allowAssembly: Boolean = false
    override var allowDegradedPrinting: Boolean = false

    override fun setUserPassword(bytes: ByteArray) {
        userPassword = bytes
    }
    override fun setOwnerPassword(bytes: ByteArray) {
        ownerPassword = bytes
    }

    fun applyEncryption() {
        var permissions = 0
        if (allowPrinting) permissions = permissions or PdfWriter.ALLOW_PRINTING
        if (allowModifyContents) permissions = permissions or PdfWriter.ALLOW_MODIFY_CONTENTS
        if (allowCopy) permissions = permissions or PdfWriter.ALLOW_COPY
        if (allowModifyAnnotations) permissions = permissions or PdfWriter.ALLOW_MODIFY_ANNOTATIONS
        if (allowFillIn) permissions = permissions or PdfWriter.ALLOW_FILL_IN
        if (allowScreenReaders) permissions = permissions or PdfWriter.ALLOW_SCREENREADERS
        if (allowAssembly) permissions = permissions or PdfWriter.ALLOW_ASSEMBLY
        if (allowDegradedPrinting) permissions = permissions or PdfWriter.ALLOW_DEGRADED_PRINTING

        var encryptType = 0
        if (doNotEncryptMetadata) encryptType = encryptType or PdfWriter.DO_NOT_ENCRYPT_METADATA
        if (embeddedFilesOnly) encryptType = encryptType or PdfWriter.EMBEDDED_FILES_ONLY

        writer.setEncryption(userPassword, ownerPassword, permissions, encryptionType.code or encryptType)
    }

    override fun close() {
        userPassword?.fill(0)
        userPassword = null
        ownerPassword?.fill(0)
        ownerPassword = null
    }

}
