package com.npk.documents

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.lowagie.text.Image
import com.lowagie.text.pdf.*
import com.lowagie.text.pdf.codec.CCITTG4Encoder
import kotlin.experimental.or

typealias BarcodeBuilder = (PdfBodyContext) -> Image

fun PdfBodyElementBuilder.barcode(builder: BarcodeBuilder, block: PdfBodyImageBuilder.(Image) -> Unit = {}) =
    image(builder(context), block)


object Barcodes {

    enum class Code128Type(val code: Int) {
        Code128(Barcode.CODE128),
        Code128UCC(Barcode.CODE128_UCC),
        Code128RAW(Barcode.CODE128_RAW)
    }

    enum class EanType(val code: Int) {
        EAN8(Barcode.EAN8),
        EAN13(Barcode.EAN13),
        UPCA(Barcode.UPCA),
        UPCE(Barcode.UPCE)
    }


    fun code39(content: String, block: (Barcode39) -> Unit = {}): BarcodeBuilder = { context ->
        val barcode = Barcode39()
            .apply {
                code = content
                font = context.getFont().baseFont
            }
            .apply(block)
        barcode.createImageWithBarcode(context.getDirectContent(), context.fontColor, context.fontColor)
    }

    fun code128(content: String, type: Code128Type = Code128Type.Code128, block: (Barcode128) -> Unit = {}): BarcodeBuilder = { context ->
        val barcode = Barcode128()
            .apply {
                codeType = type.code
                code = content
                font = context.getFont().baseFont
            }
            .apply(block)
        barcode.createImageWithBarcode(context.getDirectContent(), context.fontColor, context.fontColor)
    }

    fun ean(content: String, type: EanType = EanType.EAN13, block: (BarcodeEAN) -> Unit = {}): BarcodeBuilder = { context ->
        val barcode = BarcodeEAN()
            .apply {
                codeType = type.code
                code = content
                font = context.getFont().baseFont
            }
            .apply(block)
        barcode.createImageWithBarcode(context.getDirectContent(), context.fontColor, context.fontColor)
    }

    fun interleaved(content: String, block: (BarcodeInter25) -> Unit = {}): BarcodeBuilder = { context ->
        val barcode = BarcodeInter25()
            .apply {
                code = content
                font = context.getFont().baseFont
            }
            .apply(block)
        barcode.createImageWithBarcode(context.getDirectContent(), context.fontColor, context.fontColor)
    }

    fun postnet(content: String, block: (BarcodePostnet) -> Unit = {}): BarcodeBuilder = { context ->
        val barcode = BarcodePostnet()
            .apply {
                codeType = Barcode.POSTNET
                code = content
                font = context.getFont().baseFont
            }
            .apply(block)
        barcode.createImageWithBarcode(context.getDirectContent(), context.fontColor, context.fontColor)
    }

    fun planet(content: String, block: (BarcodePostnet) -> Unit = {}): BarcodeBuilder = { context ->
        val barcode = BarcodePostnet()
            .apply {
                codeType = Barcode.PLANET
                code = content
                font = context.getFont().baseFont
            }
            .apply(block)
        barcode.createImageWithBarcode(context.getDirectContent(), context.fontColor, context.fontColor)
    }

    fun pdf417(content: String, block: (BarcodePDF417) -> Unit = {}): BarcodeBuilder = {
        val barcode = BarcodePDF417()
            .apply { setText(content) }
            .apply(block)
        barcode.image
    }

    fun qrcode(content: String): BarcodeBuilder = {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val data = CCITTG4Encoder.compress(bitMatrix.toByteArray(), bitMatrix.width, bitMatrix.height)
        Image.getInstance(bitMatrix.width, bitMatrix.height, false, Image.CCITTG4, Image.CCITT_BLACKIS1, data, null)
    }


    private fun BitMatrix.toByteArray(): ByteArray {
        val stride = (width + 7) / 8;
        val array = ByteArray(stride * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!get(x, y)) {
                    val offset = stride * y + x / 8
                    array[offset] = array[offset] or (0x80 shr (x % 8)).toByte()
                }
            }
        }
        return array
    }

}
