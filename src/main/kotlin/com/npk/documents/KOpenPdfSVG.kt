package com.npk.documents

import com.lowagie.text.BadElementException
import com.lowagie.text.Image
import com.lowagie.text.ImgTemplate
import com.lowagie.text.pdf.PdfTemplate
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.DocumentLoader
import org.apache.batik.bridge.GVTBuilder
import org.apache.batik.bridge.UserAgentAdapter
import org.apache.batik.gvt.GraphicsNode
import org.apache.batik.util.XMLResourceDescriptor
import java.io.IOException
import java.net.URL

/*
 * SVG Image support
 * To use a svg need to add a dependency
 *
 *```
 *  <dependency>
 *    <groupId>org.apache.xmlgraphics</groupId>
 *    <artifactId>batik-transcoder</artifactId>
 *    <version>1.18</version>
 *  </dependency>
 *```
 */

/**
 * Add a svg image to a pdf document
 * @param classpathResName path to the svg file
 */
fun PdfBodyElementBuilder.svg(classpathResName: String, block: PdfBodyImageBuilder.(Image) -> Unit = {}) =
    svg(
        requireNotNull(PdfBodyElementBuilder::class.java.getResource("/$classpathResName")) { "The resource $classpathResName not found" },
        block
    )

/**
 * Add a svg image to a pdf document
 * @param resourceUrl svg url
 */
fun PdfBodyElementBuilder.svg(resourceUrl: URL, block: PdfBodyImageBuilder.(Image) -> Unit = {}) =
    image(ImageSVGHelper.createSvgImage(resourceUrl, context), block)


private object ImageSVGHelper {

    val factory = SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName())
    val builder = GVTBuilder()
    val bridgeContext = UserAgentAdapter().let { userAgent ->
        // Notice, that you should use org.apache.batik.bridge.svg12.SVG12BridgeContext for the svg version 1.2
        org.apache.batik.bridge.BridgeContext(userAgent, DocumentLoader(userAgent)).apply { setDynamicState(org.apache.batik.bridge.BridgeContext.DYNAMIC) }
    }

    /**
     * For more Info see https://github.com/LibrePDF/OpenPDF/issues/82
     */
    fun createSvgImage(url: URL, pdfContext: PdfBodyContext): Image {
        val imageGraphics: GraphicsNode = try {
            builder.build(bridgeContext, factory.createSVGDocument(url.toString()))
        } catch (ex: IOException) {
            throw RuntimeException("Couldn't load SVG resource", ex)
        }

        val width = imageGraphics.sensitiveBounds.width.toFloat()
        val height = imageGraphics.sensitiveBounds.height.toFloat()

        val template: PdfTemplate = pdfContext.createTemplate(width, height)
        val graphics = template.createGraphics(width, height)
        try {
            // SVGs can have their corner at coordinates other than (0,0).
            val bounds = imageGraphics.bounds

            //TODO: Is this in the right coordinate space even?
            graphics.translate(-bounds.x, -bounds.y)

            imageGraphics.paint(graphics)

            return ImgTemplate(template)
        } catch (ex: BadElementException) {
            throw RuntimeException("Couldn't generate PDF from SVG", ex)
        } finally {
            graphics.dispose()
        }
    }

}
