package com.npk.documents

import com.lowagie.text.*
import com.lowagie.text.alignment.HorizontalAlignment
import com.lowagie.text.alignment.VerticalAlignment
import com.lowagie.text.error_messages.MessageLocalization
import com.lowagie.text.html.FontSize
import com.lowagie.text.html.HtmlTags
import com.lowagie.text.html.WebColors
import com.lowagie.text.pdf.BaseFont
import com.npk.documents.DesignedTable.Companion.BORDER_WIDTH
import com.npk.documents.DesignedTable.Companion.EmptyDesign
import com.npk.documents.DesignedTable.Design
import com.npk.documents.DesignedTable.Designer
import java.awt.Point
import java.util.*
import kotlin.math.max
import kotlin.math.round
import kotlin.properties.Delegates

object PdfDocumentFonts {

    const val DEFAULT_SIZE = 11f

    /**
     * Bunch of methods for base 14 PDF fonts
     */
    fun getCourier(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.COURIER, size, color)
    fun getCourierBold(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.COURIER_BOLD, size, color)
    fun getCourierOblique(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.COURIER_OBLIQUE, size, color)
    fun getCourierBoldOblique(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.COURIER_BOLDOBLIQUE, size, color)

    fun getHelvetica(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.HELVETICA, size, color)
    fun getHelveticaBold(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.HELVETICA_BOLD, size, color)
    fun getHelveticaOblique(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.HELVETICA_OBLIQUE, size, color)
    fun getHelveticaBoldOblique(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.HELVETICA_BOLDOBLIQUE, size, color)

    fun getTimes(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.TIMES_ROMAN, size, color)
    fun getTimesBold(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.TIMES_BOLD, size, color)
    fun getTimesItalic(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.TIMES_ITALIC, size, color)
    fun getTimesBoldItalic(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.TIMES_BOLDITALIC, size, color)

    fun getSymbol(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.SYMBOL, size, color)
    fun getZapfDingbats(size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font = getBaseFont(FontFactory.ZAPFDINGBATS, size, color)

    private fun getBaseFont(fontName: String, size: Float = DEFAULT_SIZE, color: java.awt.Color? = null) =
        FontFactory.getFont(fontName, FontFactory.defaultEncoding, FontFactory.defaultEmbedding, size, Font.UNDEFINED, color)

    /**
     * Register a font file and use an alias for the font contained in it if specified
     * @param path  the path to a font file
     * @param alias the alias you want to use for the font (optional)
     */
    fun register(path: String, alias: String = "") =
        FontFactory.register(path, alias.takeIf(String::isNotEmpty))

    /**
     * Constructs an embedded Unicode encoding [com.lowagie.text.Font] object
     * @param fontname the name of the font
     * @param size     the size of this font (optional)
     * @param color    the [java.awt.Color] of the font (optional)
     */
    fun getUnicodeFont(fontName: String, size: Float = DEFAULT_SIZE, color: java.awt.Color? = null): Font =
        FontFactory.getFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, Font.UNDEFINED, color)

}

/**
 * A Phrase with supporting some tags
 *
 * Supported tags:
 * - `<b></b>` - Make wrapped text as Bold
 * - `<i></i>` - Make wrapped text as Italic
 * - `<u></u>` - Make wrapped text as Underline
 * - `<s></s>` - Make wrapped text as Strikethru
 *
 * All these tags can be combined e.g. `<b><i>Bold Italic text</i></b>`
 *
 * `<color #000000></color>` - Set font color. The color can be specified either hex-code or rgb function or predefined web color name
 * e.g. `<color #ff0015>`, `<color rgb(0, 0, 128)>`, `<color DarkCyan>`
 *
 * `<size 11></size>` - Set font size. The value can be specified as one of the next:
 * - Float value - set the font size e.g. `<size 11.5>`
 * - Percent - relative size to the current font size e.g. `<size 80%>`
 * - Predefined constant:
 *
 * Absolute-size, based on the [PdfDocumentFonts.DEFAULT_SIZE] font size (which is medium)
 * 1. `xx-small`
 * 2. `x-small`
 * 3. `small`
 * 4. `medium`
 * 5. `large`
 * 6. `x-large`
 * 7. `xx-large`
 * 8. `xxx-large`
 *
 * Relative-size keywords. The font will be larger or smaller relative to the parent element's font size
 * 1. `smaller`
 * 2. `larger`
 *
 * Also, the custom tag handler can be registered.
 * Need to implement the [TagPhrase.TagPhraseHandler] interface and call the [TagPhrase.registerHandler("name", handler)] method
 *
 * @see [com.lowagie.text.html.WebColors]
 * @see [com.lowagie.text.html.FontSize]
 *
 */
class TagPhrase : Phrase {

    fun interface TagPhraseHandler {

        operator fun invoke(token: String, phrase: TagPhrase): Boolean

    }

    companion object {

        private val TEXT_TOKEN_REGEX = "(?<=</?.{1,64}>)|(?=</?.{1,64}>)".toRegex()

        private val tagHandlers = mutableMapOf<String, TagPhraseHandler>().also {
            it += HtmlTags.B to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<${HtmlTags.B}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.BOLD
                        true
                    }
                    token.equals("</${HtmlTags.B}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.BOLD.inv()
                        true
                    }
                    else -> false
                }
            }
            it += HtmlTags.I to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<${HtmlTags.I}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.ITALIC
                        true
                    }
                    token.equals("</${HtmlTags.I}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.ITALIC.inv()
                        true
                    }
                    else -> false
                }
            }
            it += HtmlTags.U to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<${HtmlTags.U}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.UNDERLINE
                        true
                    }
                    token.equals("</${HtmlTags.U}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.UNDERLINE.inv()
                        true
                    }
                    else -> false
                }
            }
            it += HtmlTags.S to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<${HtmlTags.S}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.STRIKETHRU
                        true
                    }
                    token.equals("</${HtmlTags.S}>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.STRIKETHRU.inv()
                        true
                    }
                    else -> false
                }
            }
            it += HtmlTags.COLOR to TagPhraseHandler { token, phrase ->
                when {
                    token.startsWith("<${HtmlTags.COLOR}", ignoreCase = true) -> {
                        val colorText = token
                            .substring(6)
                            .removeSuffix(">")
                            .trim()

                        if (colorText.isNotEmpty()) {
                            val fontColors = phrase.getAttribute<Deque<java.awt.Color>>("font-colors")
                                ?: LinkedList<java.awt.Color>().also { phrase.setAttribute("font-colors", it) }

                            phrase.font.color?.let { fontColors.push(it) }
                            phrase.font.color = WebColors.getRGBColor(colorText)
                        }
                        true
                    }
                    token.equals("</${HtmlTags.COLOR}>", ignoreCase = true) -> {
                        phrase.font.color = phrase.getAttribute<Deque<java.awt.Color>>("font-colors")?.let {
                            if (it.isNotEmpty()) {
                                it.pop()
                            } else {
                                null
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            it += "size" to TagPhraseHandler { token, phrase ->
                when {
                    token.startsWith("<size", ignoreCase = true) -> {
                        val fontSizeText = token
                            .substring(5)
                            .removeSuffix(">")
                            .trim()

                        if (fontSizeText.isNotEmpty()) {
                            val fontSizesAttr = phrase.getAttribute<Deque<Float>>("font-size")
                                ?: LinkedList<Float>().also { phrase.setAttribute("font-size", it) }

                            val fontSize = FontSize.parse(fontSizeText)
                            if (fontSize != null) {
                                fontSizesAttr.push(phrase.font.size)
                                phrase.font.size = round((if (fontSize.isRelative) phrase.font.size else PdfDocumentFonts.DEFAULT_SIZE) * fontSize.scale)
                                phrase.leading = max(phrase.leading, calculateLeading(phrase.font.size))
                            } else {
                                val isPercent = fontSizeText.endsWith("%")
                                fontSizeText.removeSuffix("%").toFloatOrNull()?.let { newFontSize ->
                                    if (newFontSize > 0.0f) {
                                        fontSizesAttr.push(phrase.font.size)
                                        phrase.font.size = if (isPercent) round(phrase.font.size * (newFontSize / 100)) else newFontSize
                                        phrase.leading = max(phrase.leading, calculateLeading(phrase.font.size))
                                    }
                                }
                            }
                        }
                        true
                    }
                    token.equals("</size>", ignoreCase = true) -> {
                        phrase.getAttribute<Deque<Float>>("font-size")?.let {
                            if (it.isNotEmpty()) {
                                phrase.font.size = it.pop()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        fun registerHandler(handlerId: String, handler: TagPhraseHandler) {
            synchronized(tagHandlers) {
                tagHandlers += handlerId to handler
            }
        }

    }

    private val attributes: MutableMap<String, Any?> = mutableMapOf()

    constructor(text: String?, font: Font) : this(calculateLeading(font.size), text, font)
    constructor(leading: Float, font: Font) : this(leading, null, font)

    constructor(leading: Float, text: String?, font: Font) : super() {
        this.leading = leading
        this.font = Font(font)
        add(text)
    }

    override fun add(text: String?): Boolean =
        if (!text.isNullOrEmpty()) {
            val currentStyle = font.style
            val currentColor = font.color
            if (font.style == Font.UNDEFINED) {
                font.style = Font.NORMAL
            }

            TEXT_TOKEN_REGEX
                .split(text)
                .filter(String::isNotEmpty)
                .forEach { token ->
                    if (token.startsWith('<')) {
                        if (!tagHandlers.values.any { handler -> handler(token, this) }) {
                            add(Chunk(token, font))
                        }
                    } else {
                        add(Chunk(token, font))
                    }
                }

            font.style = currentStyle
            font.color = currentColor

            true
        } else {
            add(Chunk(text.orEmpty(), font))
        }

    /**
     * Returns the value corresponding to the given [name] or `null`
     *
     * @param name attribute name
     * @return value stored in the object if exists otherwise `null`
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getAttribute(name: String): T? = attributes[name] as T?

    /**
     * Store the specified [value] with the specified attribute [name]
     *
     * @param name attribute name
     * @param value any value
     */
    fun <T> setAttribute(name: String, value: T?) {
        attributes[name] = value
    }

}

/**
 * The table implementation with Designer support.
 * A [Designer] is an implementation which styling the table according to the [Design]
 */
class DesignedTable(numColumns: Int, designer: Designer = NoStyle) : Table(numColumns) {

    companion object {

        const val BORDER_WIDTH = 0.5f

        val NoStyle: Designer = DefaultTableDesigner(EmptyDesign())
        val BareboneStyle: Designer = DefaultTableDesigner(
            EmptyDesign(
                border = Rectangle.BOX,
                borderColor = java.awt.Color(211, 211, 211),
                stripes = Rectangle.BOX,
                showColumnStripes = true,
                showRowStripes = true
            )
        )

        class EmptyDesign(
            override val border: Int = Rectangle.NO_BORDER,
            override val borderColor: java.awt.Color? = null,
            override val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.UNDEFINED,
            override val verticalAlignment: VerticalAlignment = VerticalAlignment.UNDEFINED,
            override val headerBackgroundColor: java.awt.Color? = null,
            override val headerHorizontalAlignment: HorizontalAlignment = HorizontalAlignment.UNDEFINED,
            override val headerVerticalAlignment: VerticalAlignment = VerticalAlignment.UNDEFINED,
            override val stripes: Int = Rectangle.NO_BORDER,
            override val stripesBackgroundColor: java.awt.Color? = null,
            override val showColumnStripes: Boolean = false,
            override val showRowStripes: Boolean = false
        ) : Design

    }

    interface Design {
        /**
         * Set the border for a table and header [com.lowagie.text.Rectangle] (TOP, BOTTOM, LEFT, RIGHT, NO_BORDER or BOX)
         */
        val border: Int

        /**
         * The color of the table border
         */
        val borderColor: java.awt.Color?

        /**
         * Set the horizontal alignment for cells
         */
        val horizontalAlignment: HorizontalAlignment

        /**
         * Set the vertical alignment for cells
         */
        val verticalAlignment: VerticalAlignment

        /**
         * Set the background color for header cells
         */
        val headerBackgroundColor: java.awt.Color?

        /**
         * Set horizontal alignment for header cells
         */
        val headerHorizontalAlignment: HorizontalAlignment

        /**
         * Set vertical alignment for header cells
         */
        val headerVerticalAlignment: VerticalAlignment

        /**
         * Set the stripes for data cells [com.lowagie.text.Rectangle]
         */
        val stripes: Int

        /**
         * Set the background color for data cells.
         */
        val stripesBackgroundColor: java.awt.Color?

        /**
         * Show column stripes with a background if specified
         */
        val showColumnStripes: Boolean

        /**
         * Show row stripes with a background if specified
         */
        val showRowStripes: Boolean

    }

    interface Designer {

        /**
         * The table [Design] definition
         */
        val design: Design

        /**
         * Paint the table
         */
        fun paintTable(table: Table)

        /**
         * Paint the header cell
         */
        fun paintHeaderCell(cell: Cell, location: Point)

        /**
         * Paint the data cell
         */
        fun paintCell(cell: Cell, location: Point)

        /**
         * Create a new [Designer] with specified [Design]
         */
        fun copy(design: Design): Designer

    }

    init {
        padding = 2f
        spacing = 0f
        borderWidth = BORDER_WIDTH
        setHorizontalAlignment(HorizontalAlignment.LEFT)
        designer.paintTable(this)
    }

    var designer: Designer by Delegates.observable(designer) { _, _, newDesigner -> newDesigner.paintTable(this) }

    override fun addCell(cell: Cell, location: Point) {
        if (cell.isHeader) {
            designer.paintHeaderCell(cell, location)
        } else {
            designer.paintCell(cell, location)
        }
        super.addCell(cell, location)
    }

}

/**
 * Predefined Designers
 */
object DesignedTableDesigners {

    val TableStyleLight1: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color.BLACK, java.awt.Color(217, 217, 217))) }
    val TableStyleLight2: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(68, 114, 196), java.awt.Color(217, 225, 242))) }
    val TableStyleLight3: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(237, 125, 49), java.awt.Color(252, 228, 214))) }
    val TableStyleLight4: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(165, 165, 165), java.awt.Color(237, 237, 237))) }
    val TableStyleLight5: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(255, 192, 0), java.awt.Color(255, 242, 204))) }
    val TableStyleLight6: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(91, 155, 213), java.awt.Color(221, 235, 247))) }
    val TableStyleLight7: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(112, 173, 71), java.awt.Color(226, 239, 218))) }

    val TableStyleLight8: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color.BLACK)) }
    val TableStyleLight9: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(68, 114, 196))) }
    val TableStyleLight10: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(237, 125, 49))) }
    val TableStyleLight11: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(165, 165, 165))) }
    val TableStyleLight12: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(255, 192, 0))) }
    val TableStyleLight13: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(91, 155, 213))) }
    val TableStyleLight14: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(112, 173, 71))) }

    val TableStyleLight15: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color.BLACK, java.awt.Color(217, 217, 217))) }
    val TableStyleLight16: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(68, 114, 196), java.awt.Color(217, 225, 242))) }
    val TableStyleLight17: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(237, 125, 49), java.awt.Color(252, 228, 214))) }
    val TableStyleLight18: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(165, 165, 165), java.awt.Color(237, 237, 237))) }
    val TableStyleLight19: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(255, 192, 0), java.awt.Color(255, 242, 204))) }
    val TableStyleLight20: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(91, 155, 213), java.awt.Color(221, 235, 247))) }
    val TableStyleLight21: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(112, 173, 71), java.awt.Color(226, 239, 218))) }

    val TableStyleLight3_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(192, 80, 77), java.awt.Color(242, 220, 219))) }
    val TableStyleLight5_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(128, 100, 162), java.awt.Color(228, 223, 236))) }
    val TableStyleLight6_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA1(java.awt.Color(75, 172, 198), java.awt.Color(218, 238, 243))) }

    val TableStyleLight10_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(192, 80, 77))) }
    val TableStyleLight12_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(128, 100, 162))) }
    val TableStyleLight13_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA2(java.awt.Color(75, 172, 198))) }

    val TableStyleLight17_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(192, 80, 77), java.awt.Color(242, 220, 219))) }
    val TableStyleLight19_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(128, 100, 162), java.awt.Color(228, 223, 236))) }
    val TableStyleLight20_2010: Designer by lazy { DefaultTableDesigner(createTableStyleLightDesignA3(java.awt.Color(75, 172, 198), java.awt.Color(218, 238, 243))) }


    fun findDesignerByName(designerName: String): Designer? =
        this::class.java.declaredFields
            .find { it.name.equals(designerName, true) || it.name.equals("$designerName\$delegate", true) }
            ?.let { field ->
                if (field.trySetAccessible()) {
                    val value = field.get(DesignedTableDesigners)
                    when (value) {
                        is Designer -> value
                        is Lazy<*> -> value.value as? Designer
                        else -> null
                    }
                } else {
                    null
                }
            }

    private fun createTableStyleLightDesignA1(borderColor: java.awt.Color, stripesBackgroundColor: java.awt.Color) =
        EmptyDesign(
            border = Rectangle.TOP or Rectangle.BOTTOM,
            borderColor = borderColor,
            horizontalAlignment = HorizontalAlignment.LEFT,
            headerHorizontalAlignment = HorizontalAlignment.LEFT,
            stripesBackgroundColor = stripesBackgroundColor,
            showRowStripes = true
        )

    private fun createTableStyleLightDesignA2(color: java.awt.Color) =
        EmptyDesign(
            border = Rectangle.BOX,
            borderColor = color,
            horizontalAlignment = HorizontalAlignment.LEFT,
            headerBackgroundColor = color,
            headerHorizontalAlignment = HorizontalAlignment.LEFT,
            showRowStripes = true
        )

    private fun createTableStyleLightDesignA3(borderColor: java.awt.Color, stripesBackgroundColor: java.awt.Color) =
        EmptyDesign(
            border = Rectangle.BOX,
            borderColor = borderColor,
            horizontalAlignment = HorizontalAlignment.LEFT,
            headerHorizontalAlignment = HorizontalAlignment.LEFT,
            stripes = Rectangle.BOX,
            stripesBackgroundColor = stripesBackgroundColor,
            showRowStripes = true
        )

}

private class DefaultTableDesigner(override val design: Design) : Designer {

    override fun paintTable(table: Table) {
        table.border = design.border
        table.borderWidth = BORDER_WIDTH
        table.borderColor = design.borderColor
    }

    override fun paintHeaderCell(cell: Cell, location: Point) {
        cell.border = design.border
        cell.borderWidth = BORDER_WIDTH
        if (cell.horizontalAlignment == HorizontalAlignment.UNDEFINED.id) {
            val horizontalAlignment = if (design.headerHorizontalAlignment != HorizontalAlignment.UNDEFINED) design.headerHorizontalAlignment else design.horizontalAlignment
            cell.setHorizontalAlignment(horizontalAlignment)
        }
        if (cell.verticalAlignment == VerticalAlignment.UNDEFINED.id) {
            val verticalAlignment = if (design.headerVerticalAlignment != VerticalAlignment.UNDEFINED) design.headerVerticalAlignment else design.verticalAlignment
            cell.setVerticalAlignment(verticalAlignment)
        }

        cell.borderColor = design.borderColor
        cell.backgroundColor = design.headerBackgroundColor
    }

    override fun paintCell(cell: Cell, location: Point) {
        cell.border = Rectangle.NO_BORDER
        cell.borderColor = design.borderColor
        cell.borderWidth = BORDER_WIDTH
        if (cell.horizontalAlignment == HorizontalAlignment.UNDEFINED.id) {
            cell.setHorizontalAlignment(design.horizontalAlignment)
        }
        if (cell.verticalAlignment == VerticalAlignment.UNDEFINED.id) {
            cell.setVerticalAlignment(design.verticalAlignment)
        }

        if (design.stripes == Rectangle.BOX && design.stripesBackgroundColor != null) {
            cell.border = Rectangle.BOX
        }

        if (design.showRowStripes && location.x % 2 == 1) {
            if (design.stripesBackgroundColor != null && (design.stripes == Rectangle.BOX || design.stripes == Rectangle.NO_BORDER)) {
                cell.backgroundColor = design.stripesBackgroundColor
                if (location.x == 1) {
                    cell.enableBorderSide(Rectangle.TOP)
                }
            } else {
                cell.enableBorderSide(Rectangle.TOP or Rectangle.BOTTOM)
            }
        }
        if (design.showColumnStripes && location.y % 2 == 0) {
            if (design.stripesBackgroundColor != null && (design.stripes == Rectangle.BOX || design.stripes == Rectangle.NO_BORDER)) {
                cell.backgroundColor = design.stripesBackgroundColor
                if (location.x == 1) {
                    cell.enableBorderSide(Rectangle.TOP)
                }
            } else {
                cell.enableBorderSide(if (location.y == 0) Rectangle.RIGHT else (Rectangle.LEFT or Rectangle.RIGHT))
            }
        }
    }

    override fun copy(design: Design): Designer = DefaultTableDesigner(design)

}

internal object CleanSections {

    private val PRINT_CHARS_REGEX = "<.*?>|</.*?>|[^\\p{Print}]".toRegex()

    class CleanChapter(title: Paragraph, number: Int) : Chapter(title, number) {

        init {
            isTriggerNewPage = false
        }

        override fun getBookmarkTitle(): Paragraph = super.getBookmarkTitle().cleaned()

        override fun addSection(indentation: Float, title: Paragraph, numberDepth: Int): Section {
            check(!isAddedCompletely) { MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document") }
            return addCleanTitleSection(indentation, title, numberDepth)
        }

    }

    class CleanChapterAutoNumber(title: Paragraph) : ChapterAutoNumber(title) {

        init {
            isTriggerNewPage = false
        }

        override fun getBookmarkTitle(): Paragraph = super.getBookmarkTitle().cleaned()

        override fun addSection(indentation: Float, title: Paragraph, numberDepth: Int): Section {
            check(!isAddedCompletely) { MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document") }
            return addCleanTitleSection(indentation, title, numberDepth)
        }

    }

    private class CleanSection(title: Paragraph, numberDepth: Int) : Section(title, numberDepth) {

        init {
            isTriggerNewPage = false
        }

        override fun getBookmarkTitle(): Paragraph = super.getBookmarkTitle().cleaned()

        override fun addSection(indentation: Float, title: Paragraph, numberDepth: Int): Section {
            check(!isAddedCompletely) { MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document") }
            return addCleanTitleSection(indentation, title, numberDepth)
        }

    }

    private fun Paragraph.cleaned(): Paragraph {
        val cleanContent = buildString {
            forEachChunks { chunk ->
                append(PRINT_CHARS_REGEX.replace(chunk.content, "").trimStart())
            }
        }
        return Paragraph(cleanContent.ifEmpty { "[${System.currentTimeMillis().toString(36)}]" })
    }

    private fun Section.addCleanTitleSection(indentation: Float, title: Paragraph, numberDepth: Int): CleanSection =
        CleanSection(title, numberDepth).also { section ->
            section.setIndentation(indentation)
            add(section)
        }

    fun Section.appendSpacing(spacing: Float) = add(Paragraph("").also { paragraph -> paragraph.spacingAfter = spacing })

}