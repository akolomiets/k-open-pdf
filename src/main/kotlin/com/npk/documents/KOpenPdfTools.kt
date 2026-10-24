package com.npk.documents

import com.npk.documents.DesignedTable.Companion.BORDER_WIDTH
import com.npk.documents.DesignedTable.Companion.EmptyDesign
import com.npk.documents.DesignedTable.Design
import com.npk.documents.DesignedTable.Designer
import org.openpdf.text.*
import org.openpdf.text.alignment.HorizontalAlignment
import org.openpdf.text.alignment.VerticalAlignment
import org.openpdf.text.error_messages.MessageLocalization
import org.openpdf.text.pdf.BaseFont
import java.awt.Color
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
     * Constructs an embedded Unicode encoding [org.openpdf.text.Font] object
     * @param fontName the name of the font
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
 * @see [WebColors]
 * @see [FontSize]
 *
 */
class TagPhrase : Phrase {

    fun interface TagPhraseHandler {

        operator fun invoke(token: String, phrase: TagPhrase): Boolean

    }

    companion object {

        const val TAG_BOLD =        "b"
        const val TAG_ITALIC =      "i"
        const val TAG_UNDERLINE =   "u"
        const val TAG_STRIKE_THRU = "s"
        const val TAG_COLOR =       "color"
        const val TAG_SIZE =        "size"

        private val TEXT_TOKEN_REGEX = "(?<=</?.{1,64}>)|(?=</?.{1,64}>)".toRegex()

        private val tagHandlers = mutableMapOf<String, TagPhraseHandler>().also {
            it += TAG_BOLD to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<$TAG_BOLD>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.BOLD
                        true
                    }
                    token.equals("</$TAG_BOLD>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.BOLD.inv()
                        true
                    }
                    else -> false
                }
            }
            it += TAG_ITALIC to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<$TAG_ITALIC>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.ITALIC
                        true
                    }
                    token.equals("</$TAG_ITALIC>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.ITALIC.inv()
                        true
                    }
                    else -> false
                }
            }
            it += TAG_UNDERLINE to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<$TAG_UNDERLINE>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.UNDERLINE
                        true
                    }
                    token.equals("</$TAG_UNDERLINE>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.UNDERLINE.inv()
                        true
                    }
                    else -> false
                }
            }
            it += TAG_STRIKE_THRU to TagPhraseHandler { token, phrase ->
                when {
                    token.equals("<$TAG_STRIKE_THRU>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style or Font.STRIKETHRU
                        true
                    }
                    token.equals("</$TAG_STRIKE_THRU>", ignoreCase = true) -> {
                        phrase.font.style = phrase.font.style and Font.STRIKETHRU.inv()
                        true
                    }
                    else -> false
                }
            }
            it += TAG_COLOR to TagPhraseHandler { token, phrase ->
                when {
                    token.startsWith("<$TAG_COLOR", ignoreCase = true) -> {
                        val colorText = token
                            .substring(6)
                            .removeSuffix(">")
                            .trim()

                        if (colorText.isNotEmpty()) {
                            val fontColors = phrase.getAttribute<Deque<java.awt.Color>>("font-colors")
                                ?: LinkedList<java.awt.Color>().also { phrase.setAttribute("font-colors", it) }

                            phrase.font.color?.let { fontColors.push(it) }
                            phrase.font.color = WebColors.parse(colorText)
                        }
                        true
                    }
                    token.equals("</$TAG_COLOR>", ignoreCase = true) -> {
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
            it += TAG_SIZE to TagPhraseHandler { token, phrase ->
                when {
                    token.startsWith("<$TAG_SIZE", ignoreCase = true) -> {
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
                                phrase.font.size = round((if (fontSize.relative) phrase.font.size else PdfDocumentFonts.DEFAULT_SIZE) * fontSize.scale)
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
                    token.equals("</$TAG_SIZE>", ignoreCase = true) -> {
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
         * Set the border for a table and header [org.openpdf.text.Rectangle] (TOP, BOTTOM, LEFT, RIGHT, NO_BORDER or BOX)
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
         * Set the stripes for data cells [org.openpdf.text.Rectangle]
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

/**
 * Named font sizes defined by CSS
 * <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/font-size">font-size</a> property
 */
enum class FontSize(val text: String, val scale: Float, val relative: Boolean) {
    // Absolute-size keywords, based on the user's default font size (which is medium).
    XX_SMALL("xx-small", 0.6f, false),
    X_SMALL("x-small", 0.75f, false),
    SMALL("small", 0.89f, false),
    MEDIUM("medium", 1.0f, false),
    LARGE("large", 1.2f, false),
    X_LARGE("x-large", 1.5f, false),
    XX_LARGE("xx-large", 2.0f, false),
    XXX_LARGE("xxx-large", 2.5f, false),

    // Relative-size keywords. The font will be larger or smaller relative to the parent element's font size,
    // roughly by the ratio used to separate the absolute-size keywords above.
    SMALLER("smaller", 0.89f, true),
    LARGER("larger", 1.2f, true);

    companion object {

        fun parse(text: String?): FontSize? {
            if (text.isNullOrEmpty() || text[0].isLetter()) {
                return null
            }
            return FontSize.entries.find { it.text.equals(text, true) }
        }

    }

}

/**
 * Source: Wikipedia http://en.wikipedia.org/wiki/Web_colors
 * CSS4 Implementation based on: https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
 */
object WebColors {

    private val webColors = mapOf(
        "aliceblue" to Color(0xf0, 0xf8, 0xff),
        "antiquewhite" to Color(0xfa, 0xeb, 0xd7),
        "aqua" to Color(0x00, 0xff, 0xff),
        "aquamarine" to Color(0x7f, 0xff, 0xd4),
        "azure" to Color(0xf0, 0xff, 0xff),
        "beige" to Color(0xf5, 0xf5, 0xdc),
        "bisque" to Color(0xff, 0xe4, 0xc4),
        "black" to Color(0x00, 0x00, 0x00),
        "blanchedalmond" to Color(0xff, 0xeb, 0xcd),
        "blue" to Color(0x00, 0x00, 0xff),
        "blueviolet" to Color(0x8a, 0x2b, 0xe2),
        "brown" to Color(0xa5, 0x2a, 0x2a),
        "burlywood" to Color(0xde, 0xb8, 0x87),
        "cadetblue" to Color(0x5f, 0x9e, 0xa0),
        "chartreuse" to Color(0x7f, 0xff, 0x00),
        "chocolate" to Color(0xd2, 0x69, 0x1e),
        "coral" to Color(0xff, 0x7f, 0x50),
        "cornflowerblue" to Color(0x64, 0x95, 0xed),
        "cornsilk" to Color(0xff, 0xf8, 0xdc),
        "crimson" to Color(0xdc, 0x14, 0x3c),
        "cyan" to Color(0x00, 0xff, 0xff),
        "darkblue" to Color(0x00, 0x00, 0x8b),
        "darkcyan" to Color(0x00, 0x8b, 0x8b),
        "darkgoldenrod" to Color(0xb8, 0x86, 0x0b),
        "darkgray" to Color(0xa9, 0xa9, 0xa9),
        "darkgreen" to Color(0x00, 0x64, 0x00),
        "darkkhaki" to Color(0xbd, 0xb7, 0x6b),
        "darkmagenta" to Color(0x8b, 0x00, 0x8b),
        "darkolivegreen" to Color(0x55, 0x6b, 0x2f),
        "darkorange" to Color(0xff, 0x8c, 0x00),
        "darkorchid" to Color(0x99, 0x32, 0xcc),
        "darkred" to Color(0x8b, 0x00, 0x00),
        "darksalmon" to Color(0xe9, 0x96, 0x7a),
        "darkseagreen" to Color(0x8f, 0xbc, 0x8f),
        "darkslateblue" to Color(0x48, 0x3d, 0x8b),
        "darkslategray" to Color(0x2f, 0x4f, 0x4f),
        "darkslategrey" to Color(0x2f, 0x4f, 0x4f),
        "darkturquoise" to Color(0x00, 0xce, 0xd1),
        "darkviolet" to Color(0x94, 0x00, 0xd3),
        "deeppink" to Color(0xff, 0x14, 0x93),
        "deepskyblue" to Color(0x00, 0xbf, 0xff),
        "dimgray" to Color(0x69, 0x69, 0x69),
        "dimgrey" to Color(0x69, 0x69, 0x69),
        "dodgerblue" to Color(0x1e, 0x90, 0xff),
        "firebrick" to Color(0xb2, 0x22, 0x22),
        "floralwhite" to Color(0xff, 0xfa, 0xf0),
        "forestgreen" to Color(0x22, 0x8b, 0x22),
        "fuchsia" to Color(0xff, 0x00, 0xff),
        "gainsboro" to Color(0xdc, 0xdc, 0xdc),
        "ghostwhite" to Color(0xf8, 0xf8, 0xff),
        "gold" to Color(0xff, 0xd7, 0x00),
        "goldenrod" to Color(0xda, 0xa5, 0x20),
        "gray" to Color(0x80, 0x80, 0x80),
        "green" to Color(0x00, 0x80, 0x00),
        "greenyellow" to Color(0xad, 0xff, 0x2f),
        "honeydew" to Color(0xf0, 0xff, 0xf0),
        "hotpink" to Color(0xff, 0x69, 0xb4),
        "indianred" to Color(0xcd, 0x5c, 0x5c),
        "indigo" to Color(0x4b, 0x00, 0x82),
        "ivory" to Color(0xff, 0xff, 0xf0),
        "khaki" to Color(0xf0, 0xe6, 0x8c),
        "lavender" to Color(0xe6, 0xe6, 0xfa),
        "lavenderblush" to Color(0xff, 0xf0, 0xf5),
        "lawngreen" to Color(0x7c, 0xfc, 0x00),
        "lemonchiffon" to Color(0xff, 0xfa, 0xcd),
        "lightblue" to Color(0xad, 0xd8, 0xe6),
        "lightcoral" to Color(0xf0, 0x80, 0x80),
        "lightcyan" to Color(0xe0, 0xff, 0xff),
        "lightgoldenrodyellow" to Color(0xfa, 0xfa, 0xd2),
        "lightgray" to Color(0xd3, 0xd3, 0xd3),
        "lightgreen" to Color(0x90, 0xee, 0x90),
        "lightgrey" to Color(0xd3, 0xd3, 0xd3),
        "lightpink" to Color(0xff, 0xb6, 0xc1),
        "lightsalmon" to Color(0xff, 0xa0, 0x7a),
        "lightseagreen" to Color(0x20, 0xb2, 0xaa),
        "lightskyblue" to Color(0x87, 0xce, 0xfa),
        "lightslategray" to Color(0x77, 0x88, 0x99),
        "lightslategrey" to Color(0x77, 0x88, 0x99),
        "lightsteelblue" to Color(0xb0, 0xc4, 0xde),
        "lightyellow" to Color(0xff, 0xff, 0xe0),
        "lime" to Color(0x00, 0xff, 0x00),
        "limegreen" to Color(0x32, 0xcd, 0x32),
        "linen" to Color(0xfa, 0xf0, 0xe6),
        "magenta" to Color(0xff, 0x00, 0xff),
        "maroon" to Color(0x80, 0x00, 0x00),
        "mediumaquamarine" to Color(0x66, 0xcd, 0xaa),
        "mediumblue" to Color(0x00, 0x00, 0xcd),
        "mediumorchid" to Color(0xba, 0x55, 0xd3),
        "mediumpurple" to Color(0x93, 0x70, 0xdb),
        "mediumseagreen" to Color(0x3c, 0xb3, 0x71),
        "mediumslateblue" to Color(0x7b, 0x68, 0xee),
        "mediumspringgreen" to Color(0x00, 0xfa, 0x9a),
        "mediumturquoise" to Color(0x48, 0xd1, 0xcc),
        "mediumvioletred" to Color(0xc7, 0x15, 0x85),
        "midnightblue" to Color(0x19, 0x19, 0x70),
        "mintcream" to Color(0xf5, 0xff, 0xfa),
        "mistyrose" to Color(0xff, 0xe4, 0xe1),
        "moccasin" to Color(0xff, 0xe4, 0xb5),
        "navajowhite" to Color(0xff, 0xde, 0xad),
        "navy" to Color(0x00, 0x00, 0x80),
        "oldlace" to Color(0xfd, 0xf5, 0xe6),
        "olive" to Color(0x80, 0x80, 0x00),
        "olivedrab" to Color(0x6b, 0x8e, 0x23),
        "orange" to Color(0xff, 0xa5, 0x00),
        "orangered" to Color(0xff, 0x45, 0x00),
        "orchid" to Color(0xda, 0x70, 0xd6),
        "palegoldenrod" to Color(0xee, 0xe8, 0xaa),
        "palegreen" to Color(0x98, 0xfb, 0x98),
        "paleturquoise" to Color(0xaf, 0xee, 0xee),
        "palevioletred" to Color(0xdb, 0x70, 0x93),
        "papayawhip" to Color(0xff, 0xef, 0xd5),
        "peachpuff" to Color(0xff, 0xda, 0xb9),
        "peru" to Color(0xcd, 0x85, 0x3f),
        "pink" to Color(0xff, 0xc0, 0xcb),
        "plum" to Color(0xdd, 0xa0, 0xdd),
        "powderblue" to Color(0xb0, 0xe0, 0xe6),
        "purple" to Color(0x80, 0x00, 0x80),
        "rebeccapurple" to Color(0x66, 0x33, 0x99),
        "red" to Color(0xff, 0x00, 0x00),
        "rosybrown" to Color(0xbc, 0x8f, 0x8f),
        "royalblue" to Color(0x41, 0x69, 0xe1),
        "saddlebrown" to Color(0x8b, 0x45, 0x13),
        "salmon" to Color(0xfa, 0x80, 0x72),
        "sandybrown" to Color(0xf4, 0xa4, 0x60),
        "seagreen" to Color(0x2e, 0x8b, 0x57),
        "seashell" to Color(0xff, 0xf5, 0xee),
        "sienna" to Color(0xa0, 0x52, 0x2d),
        "silver" to Color(0xc0, 0xc0, 0xc0),
        "skyblue" to Color(0x87, 0xce, 0xeb),
        "slateblue" to Color(0x6a, 0x5a, 0xcd),
        "slategray" to Color(0x70, 0x80, 0x90),
        "slategrey" to Color(0x70, 0x80, 0x90),
        "snow" to Color(0xff, 0xfa, 0xfa),
        "springgreen" to Color(0x00, 0xff, 0x7f),
        "steelblue" to Color(0x46, 0x82, 0xb4),
        "tan" to Color(0xd2, 0xb4, 0x8c),
        "transparent" to Color(0x00, 0x00, 0x00),
        "teal" to Color(0x00, 0x80, 0x80),
        "thistle" to Color(0xd8, 0xbf, 0xd8),
        "tomato" to Color(0xff, 0x63, 0x47),
        "turquoise" to Color(0x40, 0xe0, 0xd0),
        "violet" to Color(0xee, 0x82, 0xee),
        "wheat" to Color(0xf5, 0xde, 0xb3),
        "white" to Color(0xff, 0xff, 0xff),
        "whitesmoke" to Color(0xf5, 0xf5, 0xf5),
        "yellow" to Color(0xff, 0xff, 0x00),
        "yellowgreen" to Color(0x9a, 0xcd, 0x32)
    )

    fun parse(text: String): Color {
        require(text.isNotBlank()) { "text argument must not be empty" }
        return text.trim().lowercase().let {
            when {
                it.startsWith("#") ->
                    it.removePrefix("#")
                        .let { digits ->
                            require(
                                (digits.length == 3 || digits.length == 4 || digits.length == 6 || digits.length == 8)
                                        && digits.all { ch -> ch.isDigit() || ch in 'a'..'f' }
                            ) { "Unknown color format '$text'" }

                            val rgb = if (digits.length == 3 || digits.length == 4) {
                                buildString {
                                    digits.forEach { ch ->
                                        append(ch)
                                        append(ch)
                                    }
                                }
                            } else {
                                digits
                            }

                            Color(
                                rgb.substring(0, 2).toInt(16),
                                rgb.substring(2, 4).toInt(16),
                                rgb.substring(4, 6).toInt(16),
                                if (rgb.length == 8) rgb.substring(6, 8).toInt(16) else 0xff
                            )
                        }
                it.startsWith("rgb") -> {
                    val c = intArrayOf(0, 0, 0, 0xff)
                    val tok = StringTokenizer(it.removePrefix("rgba").removePrefix("rgb"), "()/, \t\r\n")

                    for (k in 0..2) {
                        val v = tok.nextToken()
                        if (v.endsWith("%")) {
                            c[k] = (v.removeSuffix("%").toDouble() * 255 / 100).toInt()
                        } else {
                            c[k] = v.toDouble().toInt()
                        }
                        if (c[k] < 0) {
                            c[k] = 0
                        } else if (c[k] > 255) {
                            c[k] = 255
                        }
                    }
                    if (tok.hasMoreElements()) {
                        // alpha
                        val v = tok.nextToken()
                        if (v.endsWith("%")) {
                            c[3] = (v.removeSuffix("%").toDouble() * 255 / 100).toInt()
                        } else {
                            c[3] = (v.toDouble() * 255).toInt()
                        }
                    }
                    return Color(c[0], c[1], c[2], c[3])
                }
                else -> requireNotNull(webColors[it]) { "Color '$text' not found" }
            }
        }
    }

}
