package com.npk.documents

import com.lowagie.text.*
import com.lowagie.text.alignment.HorizontalAlignment
import com.lowagie.text.alignment.VerticalAlignment
import com.lowagie.text.html.FontSize
import com.lowagie.text.pdf.PdfAction
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfTemplate
import com.npk.documents.CleanSections.appendSpacing
import com.npk.documents.DesignedTable.Design
import com.npk.documents.KOpenPdfConstant.EMPTY_SYMBOL
import com.npk.documents.KOpenPdfConstant.LIST_SYMBOL
import com.npk.documents.PdfBodyListBuilder.Companion.createPdfList
import com.npk.documents.PdfBodyParagraphBuilder.TextAlignment.Undefined
import java.net.URI


interface PdfBodyContext {

    val fontSize: Float

    val fontColor: java.awt.Color?

    fun getFont(size: Float = fontSize, color: java.awt.Color? = fontColor, style: Int = Font.UNDEFINED): Font

    fun createTemplate(width: Float, height: Float): PdfTemplate

    fun getPageSize(): Rectangle

    fun getDirectContent(): PdfContentByte

    fun addAttachment(fileName: String, fileDescription: String = "", fileContent: ByteArray)

}

private fun PdfBodyContext.getChapterFont() = getFont(fontSize * FontSize.LARGE.scale, style = Font.BOLD)
private fun PdfBodyContext.getSectionFont() = getFont(fontSize + FontSize.LARGE.scale, style = Font.BOLD)
private fun PdfBodyContext.getParagraphFont() = getFont()
private fun PdfBodyContext.getTableFont() = getFont()
private fun PdfBodyContext.getListFont() = getFont()


@PdfBodyDslMarker
interface PdfBodyDSL {

    fun body(block: PdfBodyBuilder.(PdfBodyContext) -> Unit)

}

typealias PdfList = com.lowagie.text.List

object KOpenPdfConstant {

    const val HYPHEN_SYMBOL =   "\u2013"
    const val LIST_SYMBOL =     "\u2022"
    const val EMPTY_SYMBOL =    "\u00a0"

}

@DslMarker
annotation class PdfBodyDslMarker

@PdfBodyDslMarker
interface PdfBodyElementBuilder {

    val context: PdfBodyContext

    fun add(element: Element): Boolean

    fun paragraph(text: String = "", block: PdfBodyParagraphBuilder.(Paragraph) -> Unit = {}) {
        val paragraph = Paragraph()
        PdfBodyParagraphBuilder(text, paragraph, context).apply { block(paragraph) }
        add(paragraph)
    }

    fun table(numColumns: Int, block: PdfBodyTableBuilder.(Table) -> Unit) {
        val table = DesignedTable(numColumns)
        PdfBodyTableBuilder(table, context).apply { block(table) }
        add(table)
    }

    fun image(img: Image, block: PdfBodyImageBuilder.(Image) -> Unit = {}) {
        PdfBodyImageBuilder(img, context).apply { block(img) }
        add(img)
    }

    fun list(listType: PdfBodyListBuilder.ListType = PdfBodyListBuilder.ListType.Number, block: PdfBodyListBuilder.(PdfList) -> Unit) {
        val list = listType.createPdfList()
        PdfBodyListBuilder(list, context).apply { block(list) }
        add(list)
    }

    fun blockquote(block: PdfBodyBlockquoteBuilder.(Table) -> Unit) {
        val table = Table(1, 1)
        PdfBodyBlockquoteBuilder(table, context).apply { block(table) }
        add(table)
    }

}

@PdfBodyDslMarker
class PdfBodyBuilder(private val document: Document, override val context: PdfBodyContext) : PdfBodyElementBuilder {

    enum class ChapterType {
        NoNumber,
        AutoNumber
    }

    fun chapter(title: String, type: ChapterType = ChapterType.NoNumber, block: PdfBodySectionBuilder.(Chapter) -> Unit) {
        val chapterFont = context.getChapterFont()

        val phrase = TagPhrase(calculateLeading(chapterFont.size), title, chapterFont)
        val paragraph = Paragraph(phrase).also { paragraph -> paragraph.spacingAfter = chapterFont.size }

        val chapter = when (type) {
            ChapterType.NoNumber -> CleanSections.CleanChapter(paragraph, 0).also { it.numberDepth = 0 }
            ChapterType.AutoNumber -> CleanSections.CleanChapterAutoNumber(paragraph)
        }

        PdfBodySectionBuilder(chapter, context).apply {
            block(chapter)
            chapter.appendSpacing(chapterFont.size + 2)
        }

        add(chapter)
    }

    fun chapter(title: String, number: Int, block: PdfBodySectionBuilder.(Chapter) -> Unit) {
        val chapterFont = context.getChapterFont()

        val phrase = TagPhrase(calculateLeading(chapterFont.size), title, chapterFont)
        val paragraph = Paragraph(phrase).also { paragraph -> paragraph.spacingAfter = chapterFont.size }

        val chapter = CleanSections.CleanChapter(paragraph, number)

        PdfBodySectionBuilder(chapter, context).apply {
            block(chapter)
            chapter.appendSpacing(chapterFont.size + 2)
        }

        add(chapter)
    }

    fun section(title: String, block: PdfBodySectionBuilder.(Section) -> Unit) {
        val sectionFont = context.getSectionFont()

        val phrase = TagPhrase(calculateLeading(sectionFont.size), title, sectionFont)
        val paragraph = Paragraph(phrase).also { paragraph -> paragraph.spacingAfter = sectionFont.size }

        val chapter = CleanSections.CleanChapter(paragraph, 0).also {
            it.numberDepth = 0
        }
        PdfBodySectionBuilder(chapter, context).apply {
            block(chapter)
            chapter.appendSpacing(sectionFont.size + 2)
        }

        add(chapter)
    }

    override fun add(element: Element): Boolean = document.add(element)

}

@PdfBodyDslMarker
class PdfBodySectionBuilder(private val section: Section, override val context: PdfBodyContext) : PdfBodyElementBuilder {

    fun triggerNewPage() {
        section.isTriggerNewPage = true
    }

    fun section(title: String, block: PdfBodySectionBuilder.(Section) -> Unit) {
        val sectionFont = context.getSectionFont()

        val phrase = TagPhrase(calculateLeading(sectionFont.size), title, sectionFont)
        val paragraph = Paragraph(phrase).also { paragraph -> paragraph.spacingAfter = sectionFont.size }
        val numberDepth = if (section.numberDepth == 0) section.numberDepth else section.numberDepth + 1

        section.addSection(section.indentation, paragraph, numberDepth)
            .also { nestedSection ->
                PdfBodySectionBuilder(nestedSection, context)
                    .apply {
                        block(nestedSection)
                        nestedSection.appendSpacing(sectionFont.size + 2)
                    }
            }
    }

    override fun add(element: Element): Boolean = section.add(element)

}

@PdfBodyDslMarker
class PdfBodyParagraphBuilder(text: String, private val paragraph: Paragraph, override val context: PdfBodyContext) : PdfBodyElementBuilder {

    enum class TextAlignment(val code: Int) {
        Left(Element.ALIGN_LEFT),
        Center(Element.ALIGN_CENTER),
        Right(Element.ALIGN_RIGHT),
        Justified(Element.ALIGN_JUSTIFIED),
        JustifiedAll(Element.ALIGN_JUSTIFIED_ALL),
        Undefined(Element.ALIGN_UNDEFINED)
    }

    init {
        paragraph.font = context.getParagraphFont()
        paragraph.leading = calculateLeading(paragraph.font.size)
        paragraph.spacingAfter = paragraph.font.size
        if (text.isNotEmpty()) {
            add(createPhrase(text))
        }
    }

    var textAlignment: TextAlignment
        get() = paragraph.alignment.let { code -> TextAlignment.entries.firstOrNull { it.code == code } ?: Undefined }
        set(value) = paragraph.setAlignment(value.code)

    fun text(text: String?) = text?.let { add(createPhrase(it)) }

    fun link(text: String, uri: URI, block: Anchor.() -> Unit = {}) {
        val anchor = Anchor(createPhrase(text)).also { it.reference = uri.toASCIIString() }
        add(anchor.apply(block))
    }

    fun action(text: String, block: () -> PdfAction) {
        val phrase = createPhrase(text).also {
            val action = block()
            it.forEachChunks { chunk -> chunk.setAction(action) }
        }
        add(phrase)
    }

    fun localDestination(text: String, label: String) {
        val phrase = createPhrase(text).also {
            it.firstChunk()?.setLocalDestination(label)
        }
        add(phrase)
    }

    fun localGoto(text: String, label: String) {
        val phrase = createPhrase(text).also {
            it.forEachChunks { chunk -> chunk.setLocalGoto(label) }
        }
        add(phrase)
    }

    override fun add(element: Element): Boolean = paragraph.add(element)

    private fun createPhrase(text: String) = TagPhrase(paragraph.leading, text, paragraph.font)

    private fun TagPhrase.firstChunk(): Chunk? = firstOrNull { element -> element.type() == Element.CHUNK } as? Chunk

}

@PdfBodyDslMarker
class PdfBodyTableBuilder(private val table: DesignedTable, val context: PdfBodyContext) {

    class DesignConfigurator(
        override var border: Int,
        override var borderColor: java.awt.Color?,
        override var horizontalAlignment: HorizontalAlignment,
        override var verticalAlignment: VerticalAlignment,
        override var headerBackgroundColor: java.awt.Color?,
        override var headerHorizontalAlignment: HorizontalAlignment,
        override var headerVerticalAlignment: VerticalAlignment,
        override var stripes: Int,
        override var stripesBackgroundColor: java.awt.Color?,
        override var showColumnStripes: Boolean,
        override var showRowStripes: Boolean
    ) : Design


    val columnSize: Int get() = table.columns

    val rowSize: Int get() = table.size()

    var emptyCellValue: String = EMPTY_SYMBOL

    val design: Design get() = table.designer.design


    fun designer(designer: DesignedTable.Designer) {
        table.designer = designer
    }

    fun designer(designName: String) {
        val designer = when {
            designName.equals(DesignedTable::NoStyle.name, true) -> DesignedTable.NoStyle
            designName.equals(DesignedTable::BareboneStyle.name, true) -> DesignedTable.BareboneStyle
            else -> DesignedTableDesigners.findDesignerByName(designName)
        }
        table.designer = requireNotNull(designer) { "The Table Designer with a '$designName' not found" }
    }

    fun designer(designName: String = DesignedTable::NoStyle.name, block: DesignConfigurator.() -> Unit = {}) {
        val designer = when {
            designName.equals(DesignedTable::NoStyle.name, true) -> DesignedTable.NoStyle
            designName.equals(DesignedTable::BareboneStyle.name, true) -> DesignedTable.BareboneStyle
            else -> DesignedTableDesigners.findDesignerByName(designName) ?: DesignedTable.NoStyle
        }
        val baseDesign = designer.design
        val configurator = DesignConfigurator(
            border = baseDesign.border,
            borderColor = baseDesign.borderColor,
            horizontalAlignment = baseDesign.horizontalAlignment,
            verticalAlignment = baseDesign.verticalAlignment,
            headerBackgroundColor = baseDesign.headerBackgroundColor,
            headerHorizontalAlignment = baseDesign.headerHorizontalAlignment,
            headerVerticalAlignment = baseDesign.headerVerticalAlignment,
            stripes = baseDesign.stripes,
            stripesBackgroundColor = baseDesign.stripesBackgroundColor,
            showColumnStripes = baseDesign.showColumnStripes,
            showRowStripes = baseDesign.showRowStripes
        )
        table.designer = designer.copy(configurator.apply(block))
    }


    fun useAllAvailableWidth() {
        table.width = 100f
    }

    fun columnWidths(vararg widths: Float) {
        table.setWidths(widths)
    }

    fun columnHeaders(vararg names: String, block: (Cell) -> Unit = {}) {
        require(names.size == table.columns) { "Wrong number of names" }
        names.forEach { name -> th(name, block) }
    }

    fun th(text: String, block: (Cell) -> Unit = {}) {
        val font = context.getTableFont()
        val phrase = TagPhrase(calculateLeading(font.size), text, font).setTextRise(-table.padding)
        val cell = Cell(phrase).apply { isHeader = true }
        table.addCell(cell.apply(block))
    }

    fun td(text: String?, block: (Cell) -> Unit = {}) {
        val font = context.getTableFont()
        val phrase = TagPhrase(calculateLeading(font.size), if (text.isNullOrEmpty()) emptyCellValue else text, font).setTextRise(-table.padding)
        table.addCell(Cell(phrase).apply(block))
    }

    fun td(phrase: Phrase, block: (Cell) -> Unit = {}) =
        table.addCell(Cell(phrase).apply(block))

    fun td(block: PdfBodyElementBuilder.(Cell) -> Unit) {
        val cell = Cell()
        cell.toPdfBodyElementBuilder().apply { block(cell) }
        table.addCell(cell)
    }

    private fun TextElementArray.toPdfBodyElementBuilder(): PdfBodyElementBuilder =
        object : PdfBodyElementBuilder {
            override val context: PdfBodyContext = this@PdfBodyTableBuilder.context
            override fun add(element: Element): Boolean = this@toPdfBodyElementBuilder.add(element)
        }

}

@PdfBodyDslMarker
class PdfBodyImageBuilder(private val image: Image, val context: PdfBodyContext) {

    init {
        image.border = Rectangle.NO_BORDER
        image.borderWidth = 0f
    }

    fun scaleAbsolute(width: Float, height: Float = width) {
        image.scaleAbsolute(width, height)
    }

    fun scalePercent(percent: Float) {
        image.scalePercent(percent)
    }

    fun scaleToFit(width: Float, height: Float = width) {
        image.scaleToFit(width, height)
    }

    fun scaleToPageSize(marginTop: Float = 0f, marginRight: Float = 0f, marginBottom: Float = marginTop, marginLeft: Float = marginRight) {
        val pageSize = context.getPageSize()
        image.scaleToFit(pageSize.width - marginLeft - marginRight, pageSize.height - marginTop - marginBottom)
    }

    fun rotationDegrees(degrees: Float) {
        image.setRotationDegrees(degrees)
    }

    fun absolutePosition(horizontal: HorizontalAlignment, vertical: VerticalAlignment) {
        val pageSize = context.getPageSize()
        val x = when (horizontal) {
            HorizontalAlignment.LEFT -> pageSize.left
            HorizontalAlignment.CENTER -> pageSize.getRight(image.scaledWidth) / 2
            HorizontalAlignment.RIGHT -> pageSize.getRight(image.scaledWidth)
            else -> throw IllegalArgumentException("Unsupported horizontal alignment: $horizontal. Should be one of [LEFT, CENTER, RIGHT]")
        }
        val y = when (vertical) {
            VerticalAlignment.TOP -> pageSize.getTop(image.scaledHeight)
            VerticalAlignment.CENTER -> pageSize.getTop(image.scaledHeight) / 2
            VerticalAlignment.BOTTOM -> pageSize.bottom
            else -> throw IllegalArgumentException("Unsupported vertical alignment: $vertical. Should be one of [TOP, CENTER, BOTTOM]")
        }
        absolutePosition(x, y)
    }

    fun absolutePosition(x: Float, y: Float) {
        image.setAbsolutePosition(x, y)
    }

    fun alignLeft() {
        image.alignment = Element.ALIGN_LEFT
    }

    fun alignCenter() {
        image.alignment = Element.ALIGN_CENTER
    }

    fun alignRight() {
        image.alignment = Element.ALIGN_RIGHT
    }

}

@PdfBodyDslMarker
class PdfBodyListBuilder(private val list: PdfList, val context: PdfBodyContext) {

    enum class ListType {
        Number,
        Letter,
        Symbol,
        Roman,
        Greek
    }

    companion object {

        private const val DEFAULT_SYMBOL = "$LIST_SYMBOL "
        private const val DEFAULT_INDENT = 10f

        fun ListType.createPdfList(): PdfList = when(this) {
            ListType.Number -> PdfList(true, false, DEFAULT_INDENT)
            ListType.Letter -> PdfList(false, true, DEFAULT_INDENT)
            ListType.Symbol -> PdfList(false, false, DEFAULT_INDENT)
            ListType.Roman -> RomanList(false, DEFAULT_INDENT.toInt())
            ListType.Greek -> GreekList(false, DEFAULT_INDENT.toInt())
        }

    }

    init {
        list.isAutoindent = true
        list.setListSymbol(Chunk(DEFAULT_SYMBOL, context.getListFont()))
        if (list is GreekList) {
            val font = PdfDocumentFonts.getSymbol(context.fontSize, context.fontColor).also { it.style = Font.NORMAL }
            list.symbol.setFont(font)
        }
    }

    var symbol: Chunk
        get() = list.symbol
        set(value) { list.setListSymbol(value) }

    var symbolIndent: Float by list::symbolIndent

    var preSymbol: String by list::preSymbol

    var postSymbol: String by list::postSymbol

    var isLowercase: Boolean
        get() = list.isLowercase
        set(value) { list.isLowercase = value }

    fun li(text: String) {
        val font = context.getListFont()
        val phrase = TagPhrase(calculateLeading(font.size), text, font)
        list.add(ListItem(phrase))
    }

    fun list(listType: ListType = ListType.Number, block: PdfBodyListBuilder.(PdfList) -> Unit) {
        val nestedList = listType.createPdfList()
        nestedList.symbolIndent = list.symbolIndent
        PdfBodyListBuilder(nestedList, context).apply { block(nestedList) }
        list.add(nestedList)
    }

}

@PdfBodyDslMarker
class PdfBodyBlockquoteBuilder(private val table: Table, override val context: PdfBodyContext) : PdfBodyElementBuilder {

    init {
        with(table) {
            border = Rectangle.LEFT
            borderColorLeft = java.awt.Color(165, 165, 165)
            borderWidthLeft = 2f
            width = 100f
            setHorizontalAlignment(HorizontalAlignment.LEFT)
        }
    }

    var stripColor: java.awt.Color
        get() = table.borderColorLeft
        set(value) { table.borderColorLeft = value }

    var backgroundColor: java.awt.Color? = null

    override fun add(element: Element): Boolean {
        table.addCell(Cell().also { cell ->
            cell.border = Rectangle.NO_BORDER
            cell.borderColorLeft = backgroundColor ?: java.awt.Color.WHITE
            cell.borderWidthLeft = 10f
            cell.leading = calculateLeading(context.fontSize) + 2f
            cell.isUseBorderPadding = true
            cell.setVerticalAlignment(VerticalAlignment.CENTER)
            cell.backgroundColor = backgroundColor ?: java.awt.Color.WHITE
            cell.add(element)
        })
        return true
    }

}
