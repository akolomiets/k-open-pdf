package com.npk.documents

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.io.JsonStringEncoder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.lowagie.text.*
import com.lowagie.text.alignment.HorizontalAlignment
import com.npk.documents.CodeBlockTable.Companion.monospacedFont
import com.npk.documents.KOpenPdfConstant.EMPTY_SYMBOL

@PdfBodyDslMarker
interface CodeBlockBuilder {

    fun add(code: Phrase): Boolean

    fun newLine()

}

@PdfBodyDslMarker
fun PdfBodyElementBuilder.codeBlock(block: CodeBlockBuilder.(Table) -> Unit) {
    val table = CodeBlockTable().apply { block(this) }
    table.buildCodeBlockContent()
    if (table.size() > 0) {
        add(table)
    }
}

fun CodeBlockBuilder.raw(code: String) =
    Phrase(CodeBlockTable.SRC_CODE_LEADING, null, monospacedFont(CodeBlockTable.CODE_FG_COLOR)).let { phrase ->
        phrase.add(code.replaceIndent(EMPTY_SYMBOL))
        add(phrase)
    }

fun CodeBlockBuilder.code(code: String) =
    TagPhrase(CodeBlockTable.SRC_CODE_LEADING, monospacedFont(CodeBlockTable.CODE_FG_COLOR)).let { phrase ->
        phrase.add(code.replaceIndent(EMPTY_SYMBOL))
        add(phrase)
    }

fun CodeBlockBuilder.json(code: String, objectMapper: ObjectMapper) =
    JsonCodeBlockPhrase(CodeBlockTable.SRC_CODE_LEADING, monospacedFont(CodeBlockTable.CODE_FG_COLOR), objectMapper).let { phrase ->
        phrase.add(code.replaceIndent(EMPTY_SYMBOL))
        add(phrase)
    }


private class CodeBlockTable : Table(2, 0), CodeBlockBuilder {

    companion object {

        const val SRC_CODE_FONT_SIZE =  PdfDocumentFonts.DEFAULT_SIZE - 1f
        const val SRC_CODE_LEADING =    SRC_CODE_FONT_SIZE * 1.3f

        val BORDER_COLOR =          java.awt.Color(217, 217, 217)
        val NUMBERS_FG_COLOR =      java.awt.Color(128, 128, 128)
        val NUMBERS_BG_COLOR =      java.awt.Color(242, 242, 242)
        val CODE_FG_COLOR =         java.awt.Color(22, 22, 22)
        val CODE_BG_COLOR =         java.awt.Color(253, 253, 253)

        fun monospacedFont(color: java.awt.Color? = null): Font = PdfDocumentFonts.getCourier(SRC_CODE_FONT_SIZE, color)

    }

    private val numbersContent = Phrase(SRC_CODE_LEADING, null, monospacedFont(NUMBERS_FG_COLOR))
    private val codeContent = Phrase(SRC_CODE_LEADING, null, monospacedFont(CODE_FG_COLOR))

    init {
        this.setWidths(floatArrayOf(4f, 96f))
        this.setHorizontalAlignment(HorizontalAlignment.LEFT)
        padding = 2f
        border = Rectangle.BOX
        borderWidth = 0.5f
        borderColor = BORDER_COLOR
    }

    override fun add(code: Phrase): Boolean {
        val linesCount = code.content.count { ch -> ch == '\n' }
        val numbersRange = if (numbersContent.isEmpty()) {
            0 ..< linesCount + 1
        } else {
            numbersContent.size ..< numbersContent.size + linesCount
        }
        numbersRange.forEach { num -> numbersContent.add("${num + 1}\n") }
        return codeContent.add(code)
    }

    override fun newLine() {
        numbersContent.add("${numbersContent.size + 1}\n")
        codeContent.add("\n")
    }

    fun buildCodeBlockContent() {
        if (numbersContent.isNotEmpty() && codeContent.isNotEmpty()) {
            addCell(Cell().apply {
                border = Rectangle.RIGHT
                borderWidthRight = 2f
                borderColorRight = NUMBERS_BG_COLOR
                backgroundColor = NUMBERS_BG_COLOR
                isUseBorderPadding = true
                setHorizontalAlignment(HorizontalAlignment.RIGHT)
                add(numbersContent.setTextRise(-padding))
            })
            addCell(Cell().apply {
                border = Rectangle.LEFT
                borderWidthLeft = 0.5f
                borderColorLeft = NUMBERS_FG_COLOR
                backgroundColor = CODE_BG_COLOR
                add(codeContent.setTextRise(-padding))
            })
        }
    }

}

private class JsonCodeBlockPhrase(leading: Float, font: Font, private val objectMapper: ObjectMapper) : Phrase(leading, null, font) {

    companion object {
        const val INDENT_SIZE = 2

        val FIELD_NAME_COLOR =      java.awt.Color(130, 39, 199)
        val STRING_VALUE_COLOR =    java.awt.Color(229, 53, 138)
        val NUMBER_VALUE_COLOR =    java.awt.Color(36, 91, 226)
        val BOOL_VALUE_COLOR =      java.awt.Color(86, 177, 107)
    }

    override fun add(json: String): Boolean {
        try {
            addColoredJsonCode(json)
        } catch (_: Throwable) {
            addText(json)
        }
        return true
    }

    private fun addColoredJsonCode(json: String) {
        val parser = with(objectMapper) {
            readTree<JsonNode>(createParser(json))
            createParser(json)
        }
        walkJsonTree(parser, JsonToken.NOT_AVAILABLE, 0)
    }

    private fun walkJsonTree(parser: JsonParser, parentToken: JsonToken, indent: Int): JsonToken {
        add(Chunk(EMPTY_SYMBOL))
        var prevToken = JsonToken.NOT_AVAILABLE
        while (!parser.isClosed) {
            var token = parser.nextToken()
            when (token) {
                JsonToken.START_OBJECT -> {
                    if (prevToken.isStructEnd || prevToken.isScalarValue) {
                        addText(",\n")
                        addTextIndented(indent, "{")
                    } else {
                        if (parentToken == JsonToken.START_ARRAY) {
                            addNewLine()
                            addTextIndented(indent, "{")
                        } else {
                            addText("{")
                        }
                    }
                    token = walkJsonTree(parser, token, indent + INDENT_SIZE)
                    addNewLine()
                    addTextIndented(indent, "}")
                }
                JsonToken.END_OBJECT -> {
                    if (parentToken == JsonToken.START_OBJECT) return token
                }
                JsonToken.START_ARRAY -> {
                    addText("[")
                    token = walkJsonTree(parser, token, indent + INDENT_SIZE)
                    if (token == JsonToken.END_ARRAY) {
                        addText(" ]")
                    } else {
                        addNewLine()
                        addTextIndented(indent, "]")
                        token = JsonToken.END_ARRAY
                    }
                }
                JsonToken.END_ARRAY -> {
                    if (parentToken == JsonToken.START_ARRAY) return if (prevToken.isStructEnd) JsonToken.END_OBJECT else token
                }
                JsonToken.FIELD_NAME -> {
                    if (prevToken != JsonToken.NOT_AVAILABLE) {
                        addText(",")
                    }
                    addNewLine()
                    addTextIndented(indent, "\"${parser.currentName()}\"", monospacedFont(FIELD_NAME_COLOR))
                    addText(" : ")
                }
                JsonToken.VALUE_STRING -> {
                    if (prevToken != JsonToken.NOT_AVAILABLE && parentToken == JsonToken.START_ARRAY) {
                        addText(", ")
                    }
                    val content = String(JsonStringEncoder.getInstance().quoteAsString(parser.valueAsString))
                    addText("\"$content\"", monospacedFont(STRING_VALUE_COLOR))
                }
                JsonToken.VALUE_NUMBER_INT -> {
                    if (prevToken != JsonToken.NOT_AVAILABLE && parentToken == JsonToken.START_ARRAY) {
                        addText(", ")
                    }
                    addText("${parser.valueAsLong}", monospacedFont(NUMBER_VALUE_COLOR))
                }
                JsonToken.VALUE_NUMBER_FLOAT -> {
                    if (prevToken != JsonToken.NOT_AVAILABLE && parentToken == JsonToken.START_ARRAY) {
                        addText(", ")
                    }
                    addText("${parser.valueAsDouble}", monospacedFont(NUMBER_VALUE_COLOR))
                }
                JsonToken.VALUE_TRUE,
                JsonToken.VALUE_FALSE -> {
                    if (prevToken != JsonToken.NOT_AVAILABLE && parentToken == JsonToken.START_ARRAY) {
                        addText(", ")
                    }
                    addText("${parser.valueAsBoolean}", monospacedFont(BOOL_VALUE_COLOR))
                }
                JsonToken.VALUE_NULL -> {
                    if (prevToken != JsonToken.NOT_AVAILABLE && parentToken == JsonToken.START_ARRAY) {
                        addText(", ")
                    }
                    addText("null", monospacedFont(BOOL_VALUE_COLOR))
                }
                else -> { }
            }
            prevToken = token ?: JsonToken.NOT_AVAILABLE
        }
        return prevToken
    }

    private fun addText(content: String, font: Font? = null) = add(Chunk(content, font ?: this.font))
    private fun addTextIndented(indent: Int, text: String, font: Font? = null) = addText("${EMPTY_SYMBOL.repeat(indent + 1)}$text", font)
    private fun addNewLine() = add(Chunk.NEWLINE)

}
