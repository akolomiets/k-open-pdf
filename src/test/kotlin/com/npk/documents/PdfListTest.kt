package com.npk.documents

import com.npk.GeneratedDocumentPath
import com.npk.generatePdfDocument
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class PdfListTest {

    @GeneratedDocumentPath
    private lateinit var generatedPdfPath: Path

    @Test
    fun `generate a pdf list`() = generatedPdfPath.generatePdfDocument("pdf_list.pdf") { output ->
        BasePdfDocument(output).useDocument {
            body {
                paragraph {
                    text("<b>Basic Lists</b>")
                    newLine()

                    list {
                        symbolIndent = 15f

                        li("<b>First</b> Item")
                        li("<b>Second</b> Item")
                        li("<b>Third</b> Item")
                        list(PdfBodyListBuilder.ListType.Letter) {
                            postSymbol = ") "

                            li("<b>First</b> Sub Item")
                            li("<b>Second</b> Sub Item")
                            li("<b>Third</b> Sub Item")
                            list(PdfBodyListBuilder.ListType.Symbol) {
                                li("<b>First</b> Sub Sub Item")
                                li("<b>Second</b> Sub Sub Item")
                                li("<b>Third</b> Sub Sub Item")
                            }
                            li("<b>Forth</b> Sub Item")
                        }
                        li("<b>Forth</b> Sub Item")
                    }
                }

                newLine()

                paragraph {
                    text("<b>Fancy Lists</b>")
                    newLine()

                    list(PdfBodyListBuilder.ListType.Roman) {
                        symbolIndent = 15f

                        li("<b>First</b> Sub Item")
                        li("<b>Second</b> Sub Item")
                        li("<b>Third</b> Sub Item")
                        list(PdfBodyListBuilder.ListType.Greek) {
                            postSymbol = ") "
                            isLowercase = true

                            li("<b>First</b> Sub Item")
                            li("<b>Second</b> Sub Item")
                            li("<b>Third</b> Sub Item")
                        }
                        li("<b>Forth</b> Sub Item")
                    }
                }
            }
        }
    }

}